package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.services.interfaces.IKeycloakProvisioningService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class KeycloakProvisioningService implements IKeycloakProvisioningService {

    private static final String DELIVERY_PERSON = "DELIVERY_PERSON";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String ROLE_LIVREUR = "ROLE_LIVREUR";
    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_CLIENT_PRO = "ROLE_CLIENT_PRO";
    private static final int USER_SESSION_PAGE_SIZE = 200;
    private static final int ROLE_USERS_PAGE_SIZE = 200;
    private static final int ADMIN_REQUEST_RETRY_ATTEMPTS = 3;

    @Value("${quickdelivery.auth.base-url}")
    private String authBaseUrl;

    @Value("${quickdelivery.auth.realm}")
    private String realm;

    @Value("${quickdelivery.auth.admin.realm}")
    private String adminRealm;

    @Value("${quickdelivery.auth.admin.client-id}")
    private String adminClientId;

    @Value("${quickdelivery.auth.admin.username}")
    private String adminUsername;

    @Value("${quickdelivery.auth.admin.password}")
    private String adminPassword;

    @Value("${quickdelivery.auth.admin.timeout-ms:5000}")
    private int adminRequestTimeoutMs;
    @Value("${quickdelivery.auth.admin.cooldown-seconds:5}")
    private long adminCooldownSeconds;
    @Value("${quickdelivery.auth.admin.token-refresh-skew-seconds:30}")
    private long adminTokenRefreshSkewSeconds;

    private RestTemplate restTemplate;
    private final Logger logger;
    private volatile String cachedAdminAccessToken;
    private volatile long cachedAdminAccessTokenExpiresAtEpochMs;
    private final AtomicLong adminUnavailableUntilEpochMs = new AtomicLong(0L);
    private final Map<String, String> cachedClientUuidByClientId = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> cachedRoleRepresentations = new ConcurrentHashMap<>();

    public KeycloakProvisioningService(Logger logger) {
        this.logger = logger;
    }

    @jakarta.annotation.PostConstruct
    void initializeRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(adminRequestTimeoutMs);
        requestFactory.setReadTimeout(adminRequestTimeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public void provisionUser(User user, String rawPassword) {
        validateProvisioningInput(user, rawPassword);
        logger.info("Provisioning Keycloak user for {}", user.getEmailAddress());

        String accessToken = getAdminAccessToken();
        String existingUserId = findUserIdByEmail(accessToken, user.getEmailAddress());

        if (existingUserId == null) {
            existingUserId = createUser(accessToken, user);
            logger.info("Created Keycloak user {} with id {}", user.getEmailAddress(), existingUserId);
        } else {
            updateUser(accessToken, existingUserId, user);
            logger.info("Updated existing Keycloak user {} with id {}", user.getEmailAddress(), existingUserId);
        }

        updatePassword(accessToken, existingUserId, rawPassword);
        assignRealmRole(accessToken, existingUserId, mapRealmRole(user.getType()));
        logger.info("Provisioned Keycloak user {} successfully", user.getEmailAddress());
    }

    @Override
    public void syncUserState(User user) {
        if (user == null || isBlank(user.getEmailAddress())) {
            return;
        }
        logger.info("Syncing Keycloak user state for {}", user.getEmailAddress());

        String accessToken = getAdminAccessToken();
        String userId = findUserIdByEmail(accessToken, user.getEmailAddress());
        if (userId == null) {
            logger.warn("Keycloak user not found for email {}", user.getEmailAddress());
            return;
        }
        syncUserState(accessToken, userId, user);
        assignRealmRole(accessToken, userId, mapRealmRole(user.getType()));
        logger.info("Synchronized Keycloak user state for {}", user.getEmailAddress());
    }

    @Override
    public Set<String> loadActiveUserEmailsByClient(String clientId) {
        if (isBlank(clientId)) {
            return Set.of();
        }

        String accessToken = getAdminAccessToken();
        String resolvedClientId = resolveClientUuid(accessToken, clientId);
        if (resolvedClientId == null) {
            logger.warn("Keycloak client not found for active session lookup: {}", clientId);
            return Set.of();
        }

        Set<String> emails = new HashSet<>();
        int first = 0;
        while (true) {
            List<Map<String, Object>> sessions = loadClientUserSessions(accessToken, resolvedClientId, first, USER_SESSION_PAGE_SIZE);
            if (sessions.isEmpty()) {
                break;
            }
            sessions.forEach(session -> {
                String userId = asString(session.get("userId"));
                String username = asString(session.get("username"));
                if (!isBlank(username) && username.contains("@")) {
                    emails.add(username.toLowerCase());
                    return;
                }
                if (!isBlank(userId)) {
                    String email = resolveUserEmail(accessToken, userId);
                    if (!isBlank(email)) {
                        emails.add(email.toLowerCase());
                    }
                }
            });
            if (sessions.size() < USER_SESSION_PAGE_SIZE) {
                break;
            }
            first += USER_SESSION_PAGE_SIZE;
        }
        return emails;
    }

    @Override
    public Set<String> loadUserEmailsByRealmRoles(java.util.Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }
        String accessToken = getAdminAccessToken();
        Set<String> emails = new HashSet<>();
        roleNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(roleName -> !roleName.isBlank())
                .forEach(roleName -> emails.addAll(loadUserEmailsByRealmRole(accessToken, roleName)));
        return emails;
    }

    private void validateProvisioningInput(User user, String rawPassword) {
        if (user == null || isBlank(user.getEmailAddress())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User email is required for identity provisioning");
        }
        if (isBlank(rawPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required for identity provisioning");
        }
    }

    private synchronized String getAdminAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAdminAccessToken != null && now < cachedAdminAccessTokenExpiresAtEpochMs) {
            return cachedAdminAccessToken;
        }
        ensureAdminApiAvailable();

        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("grant_type", "password");
        payload.add("client_id", adminClientId);
        payload.add("username", adminUsername);
        payload.add("password", adminPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = executeKeycloakRequest(() -> restTemplate.exchange(
                        authBaseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token",
                        HttpMethod.POST,
                        new HttpEntity<>(payload, headers),
                        Map.class
                ),
                "retrieve Keycloak admin access token");

        Object token = response.getBody() == null ? null : response.getBody().get("access_token");
        if (!(token instanceof String accessToken) || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to retrieve Keycloak admin access token");
        }
        long expiresInSeconds = response.getBody() == null ? 0L : asLong(response.getBody().get("expires_in"));
        long refreshWindowSeconds = Math.max(5L, Math.min(adminTokenRefreshSkewSeconds, Math.max(5L, expiresInSeconds / 2)));
        cachedAdminAccessToken = accessToken;
        cachedAdminAccessTokenExpiresAtEpochMs = System.currentTimeMillis()
                + Math.max(5L, expiresInSeconds - refreshWindowSeconds) * 1000L;
        return accessToken;
    }

    private String findUserIdByEmail(String accessToken, String email) {
        String encodedEmail = UriUtils.encodeQueryParam(email, StandardCharsets.UTF_8);
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(accessToken));

        ResponseEntity<List> response = executeKeycloakRequest(() -> restTemplate.exchange(
                        authBaseUrl + "/admin/realms/" + realm + "/users?email=" + encodedEmail,
                        HttpMethod.GET,
                        entity,
                        List.class
                ),
                "resolve Keycloak user by email");

        List<?> users = response.getBody();
        if (users == null) {
            return null;
        }

        for (Object candidate : users) {
            if (candidate instanceof Map<?, ?> map) {
                Object candidateEmail = map.get("email");
                if (candidateEmail instanceof String existingEmail && existingEmail.equalsIgnoreCase(email)) {
                    Object id = map.get("id");
                    if (id instanceof String userId && !userId.isBlank()) {
                        return userId;
                    }
                }
            }
        }

        return null;
    }

    private String resolveClientUuid(String accessToken, String clientId) {
        String cachedUuid = cachedClientUuidByClientId.get(clientId);
        if (cachedUuid != null && !cachedUuid.isBlank()) {
            return cachedUuid;
        }
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(accessToken));
        ResponseEntity<List> response = executeKeycloakRequest(() -> restTemplate.exchange(
                        authBaseUrl + "/admin/realms/" + realm + "/clients?clientId=" + UriUtils.encodeQueryParam(clientId, StandardCharsets.UTF_8),
                        HttpMethod.GET,
                        entity,
                        List.class
                ),
                "resolve Keycloak client uuid");

        List<?> clients = response.getBody();
        if (clients == null) {
            return null;
        }
        for (Object candidate : clients) {
            if (candidate instanceof Map<?, ?> map) {
                Object candidateClientId = map.get("clientId");
                Object id = map.get("id");
                if (candidateClientId instanceof String existingClientId
                        && existingClientId.equalsIgnoreCase(clientId)
                        && id instanceof String uuid
                        && !uuid.isBlank()) {
                    cachedClientUuidByClientId.put(clientId, uuid);
                    return uuid;
                }
            }
        }
        return null;
    }

    private Set<String> loadUserEmailsByRealmRole(String accessToken, String roleName) {
        Set<String> emails = new HashSet<>();
        int first = 0;
        while (true) {
            int currentFirst = first;
            HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(accessToken));
            ResponseEntity<List> response = executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/roles/"
                                    + UriUtils.encodePathSegment(roleName, StandardCharsets.UTF_8)
                                    + "/users?first=" + currentFirst + "&max=" + ROLE_USERS_PAGE_SIZE,
                            HttpMethod.GET,
                            entity,
                            List.class
                    ),
                    "load Keycloak users by role");
            List<?> users = response.getBody();
            if (users == null || users.isEmpty()) {
                break;
            }
            for (Object candidate : users) {
                if (candidate instanceof Map<?, ?> map) {
                    String email = asString(map.get("email"));
                    String username = asString(map.get("username"));
                    if (!isBlank(email)) {
                        emails.add(email.toLowerCase());
                    } else if (!isBlank(username) && username.contains("@")) {
                        emails.add(username.toLowerCase());
                    }
                }
            }
            if (users.size() < ROLE_USERS_PAGE_SIZE) {
                break;
            }
            first += ROLE_USERS_PAGE_SIZE;
        }
        return emails;
    }

    private List<Map<String, Object>> loadClientUserSessions(String accessToken, String clientUuid, int first, int max) {
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(accessToken));
        ResponseEntity<List> response = executeKeycloakRequest(() -> restTemplate.exchange(
                        authBaseUrl + "/admin/realms/" + realm + "/clients/" + clientUuid + "/user-sessions?first=" + first + "&max=" + max,
                        HttpMethod.GET,
                        entity,
                        List.class
                ),
                "load Keycloak client sessions");
        List<?> sessions = response.getBody();
        if (sessions == null) {
            return List.of();
        }
        return sessions.stream()
                .filter(Map.class::isInstance)
                .map(session -> (Map<String, Object>) session)
                .toList();
    }

    private String resolveUserEmail(String accessToken, String userId) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(accessToken));
            ResponseEntity<Map> response = executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/users/" + userId,
                            HttpMethod.GET,
                            entity,
                            Map.class
                    ),
                    "resolve Keycloak user email");
            return asString(response.getBody() == null ? null : response.getBody().get("email"));
        } catch (HttpClientErrorException ex) {
            logger.warn("Unable to resolve Keycloak user email for {}: {}", userId, ex.getStatusCode());
            return null;
        }
    }

    private String createUser(String accessToken, User user) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response;
        try {
            response = executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/users",
                            HttpMethod.POST,
                            new HttpEntity<>(buildUserPayload(user), headers),
                            Void.class
                    ),
                    "create Keycloak user");
        } catch (HttpClientErrorException.Conflict ex) {
            String existingUserId = findUserIdByEmail(accessToken, user.getEmailAddress());
            if (existingUserId != null) {
                return existingUserId;
            }
            throw ex;
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to create Keycloak user", ex);
        }

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location != null && location.contains("/")) {
            return location.substring(location.lastIndexOf('/') + 1);
        }

        String userId = findUserIdByEmail(accessToken, user.getEmailAddress());
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak user created but could not be resolved");
        }
        return userId;
    }

    private void updateUser(String accessToken, String userId, User user) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/users/" + userId,
                            HttpMethod.PUT,
                            new HttpEntity<>(buildUserPayload(user), headers),
                            Void.class
                    ),
                    "update Keycloak user");
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to update Keycloak user", ex);
        }
    }

    private void updatePassword(String accessToken, String userId, String rawPassword) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", rawPassword,
                "temporary", false
        );

        try {
            executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password",
                            HttpMethod.PUT,
                            new HttpEntity<>(credential, headers),
                            Void.class
                    ),
                    "reset Keycloak password");
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to set Keycloak password", ex);
        }
    }

    private void assignRealmRole(String accessToken, String userId, String roleName) {
        HttpHeaders headers = bearerHeaders(accessToken);

        Map roleRepresentation = resolveRealmRoleRepresentation(accessToken, roleName, headers);

        if (roleRepresentation == null || roleRepresentation.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak role not found: " + roleName);
        }

        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm",
                            HttpMethod.POST,
                            new HttpEntity<>(Collections.singletonList(roleRepresentation), headers),
                            Void.class
                    ),
                    "assign Keycloak role");
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to assign Keycloak role " + roleName, ex);
        }
    }

    private void syncUserState(String accessToken, String userId, User user) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/users/" + userId,
                            HttpMethod.PUT,
                            new HttpEntity<>(buildUserPayload(user), headers),
                            Void.class
                    ),
                    "sync Keycloak user state");
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to sync Keycloak user state", ex);
        }
    }

    private <T> ResponseEntity<T> executeKeycloakRequest(Supplier<ResponseEntity<T>> requestSupplier, String action) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= ADMIN_REQUEST_RETRY_ATTEMPTS; attempt++) {
            ensureAdminApiAvailable();
            try {
                ResponseEntity<T> response = requestSupplier.get();
                adminUnavailableUntilEpochMs.set(0L);
                return response;
            } catch (HttpClientErrorException.Unauthorized unauthorized) {
                cachedAdminAccessToken = null;
                cachedAdminAccessTokenExpiresAtEpochMs = 0L;
                lastFailure = unauthorized;
                break;
            } catch (HttpClientErrorException httpClientErrorException) {
                if (!httpClientErrorException.getStatusCode().is5xxServerError()) {
                    throw httpClientErrorException;
                }
                lastFailure = httpClientErrorException;
                if (attempt >= ADMIN_REQUEST_RETRY_ATTEMPTS) {
                    markAdminApiUnavailable(action, httpClientErrorException);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak admin API temporarily unavailable", httpClientErrorException);
                }
                sleepBeforeRetry(attempt, action, httpClientErrorException);
            } catch (ResourceAccessException resourceAccessException) {
                lastFailure = resourceAccessException;
                if (attempt >= ADMIN_REQUEST_RETRY_ATTEMPTS) {
                    markAdminApiUnavailable(action, resourceAccessException);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak admin API temporarily unavailable", resourceAccessException);
                }
                sleepBeforeRetry(attempt, action, resourceAccessException);
            } catch (RestClientException restClientException) {
                lastFailure = restClientException;
                if (attempt >= ADMIN_REQUEST_RETRY_ATTEMPTS) {
                    markAdminApiUnavailable(action, restClientException);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak admin API temporarily unavailable", restClientException);
                }
                sleepBeforeRetry(attempt, action, restClientException);
            }
        }
        if (lastFailure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak admin API temporarily unavailable");
    }

    private void ensureAdminApiAvailable() {
        long unavailableUntil = adminUnavailableUntilEpochMs.get();
        if (System.currentTimeMillis() < unavailableUntil) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak admin API temporarily unavailable");
        }
    }

    private void markAdminApiUnavailable(String action, Exception exception) {
        cachedAdminAccessToken = null;
        cachedAdminAccessTokenExpiresAtEpochMs = 0L;
        long unavailableUntil = System.currentTimeMillis() + (adminCooldownSeconds * 1000L);
        adminUnavailableUntilEpochMs.set(unavailableUntil);
        logger.warn("Temporarily disabling Keycloak admin calls until {} after failure on {}: {}",
                new java.util.Date(unavailableUntil), action, exception.getMessage());
    }

    private void sleepBeforeRetry(int attempt, String action, Exception exception) {
        long backoffMs = Math.min(1000L, 200L * attempt);
        logger.warn("Retrying Keycloak admin action {} after attempt {}/{} failed: {}",
                action, attempt, ADMIN_REQUEST_RETRY_ATTEMPTS, exception.getMessage());
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> resolveRealmRoleRepresentation(String accessToken, String roleName, HttpHeaders headers) {
        Map<String, Object> cachedRepresentation = cachedRoleRepresentations.get(roleName);
        if (cachedRepresentation != null && !cachedRepresentation.isEmpty()) {
            return new HashMap<>(cachedRepresentation);
        }
        try {
            ResponseEntity<Map> roleResponse = executeKeycloakRequest(() -> restTemplate.exchange(
                            authBaseUrl + "/admin/realms/" + realm + "/roles/" + roleName,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            Map.class
                    ),
                    "resolve Keycloak role");
            Map roleRepresentation = roleResponse.getBody();
            if (roleRepresentation != null && !roleRepresentation.isEmpty()) {
                cachedRoleRepresentations.put(roleName, new HashMap<>(roleRepresentation));
            }
            return roleRepresentation;
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to resolve Keycloak role " + roleName, ex);
        }
    }

    private Map<String, Object> buildUserPayload(User user) {
        return Map.of(
                "username", user.getEmailAddress(),
                "email", user.getEmailAddress(),
                "firstName", Objects.toString(user.getFirstName(), ""),
                "lastName", Objects.toString(user.getLastName(), ""),
                "enabled", Boolean.TRUE.equals(user.getActiveAccount()),
                "emailVerified", Boolean.TRUE.equals(user.getEmailAddressValidation())
        );
    }

    private HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String mapRealmRole(String userType) {
        if (DELIVERY_PERSON.equalsIgnoreCase(userType)) {
            return ROLE_LIVREUR;
        }
        if (CUSTOMER.equalsIgnoreCase(userType)) {
            return ROLE_CLIENT;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported user type: " + userType);
    }

    private ResponseStatusException keycloakError(String message, HttpClientErrorException ex) {
        logger.error("{}: {}", message, ex.getResponseBodyAsString());
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message, ex);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String asString(Object value) {
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : null;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
