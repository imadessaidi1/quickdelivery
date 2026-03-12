package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.services.interfaces.IKeycloakProvisioningService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class KeycloakProvisioningService implements IKeycloakProvisioningService {

    private static final String DELIVERY_PERSON = "DELIVERY_PERSON";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String ROLE_LIVREUR = "ROLE_LIVREUR";
    private static final String ROLE_CLIENT = "ROLE_CLIENT";

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

    private final RestTemplate restTemplate = new RestTemplate();
    private final Logger logger;

    public KeycloakProvisioningService(Logger logger) {
        this.logger = logger;
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
        syncUserState(accessToken, existingUserId, user);
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

    private void validateProvisioningInput(User user, String rawPassword) {
        if (user == null || isBlank(user.getEmailAddress())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User email is required for identity provisioning");
        }
        if (isBlank(rawPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required for identity provisioning");
        }
    }

    private String getAdminAccessToken() {
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("grant_type", "password");
        payload.add("client_id", adminClientId);
        payload.add("username", adminUsername);
        payload.add("password", adminPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.exchange(
                authBaseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class
        );

        Object token = response.getBody() == null ? null : response.getBody().get("access_token");
        if (!(token instanceof String accessToken) || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to retrieve Keycloak admin access token");
        }
        return accessToken;
    }

    private String findUserIdByEmail(String accessToken, String email) {
        String encodedEmail = UriUtils.encodeQueryParam(email, StandardCharsets.UTF_8);
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(accessToken));

        ResponseEntity<List> response = restTemplate.exchange(
                authBaseUrl + "/admin/realms/" + realm + "/users?email=" + encodedEmail,
                HttpMethod.GET,
                entity,
                List.class
        );

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

    private String createUser(String accessToken, User user) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(
                    authBaseUrl + "/admin/realms/" + realm + "/users",
                    HttpMethod.POST,
                    new HttpEntity<>(buildUserPayload(user), headers),
                    Void.class
            );
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
            restTemplate.exchange(
                    authBaseUrl + "/admin/realms/" + realm + "/users/" + userId,
                    HttpMethod.PUT,
                    new HttpEntity<>(buildUserPayload(user), headers),
                    Void.class
            );
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
            restTemplate.exchange(
                    authBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password",
                    HttpMethod.PUT,
                    new HttpEntity<>(credential, headers),
                    Void.class
            );
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to set Keycloak password", ex);
        }
    }

    private void assignRealmRole(String accessToken, String userId, String roleName) {
        HttpHeaders headers = bearerHeaders(accessToken);

        Map roleRepresentation;
        try {
            ResponseEntity<Map> roleResponse = restTemplate.exchange(
                    authBaseUrl + "/admin/realms/" + realm + "/roles/" + roleName,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            roleRepresentation = roleResponse.getBody();
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to resolve Keycloak role " + roleName, ex);
        }

        if (roleRepresentation == null || roleRepresentation.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak role not found: " + roleName);
        }

        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.exchange(
                    authBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm",
                    HttpMethod.POST,
                    new HttpEntity<>(Collections.singletonList(roleRepresentation), headers),
                    Void.class
            );
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to assign Keycloak role " + roleName, ex);
        }
    }

    private void syncUserState(String accessToken, String userId, User user) {
        HttpHeaders headers = bearerHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(
                    authBaseUrl + "/admin/realms/" + realm + "/users/" + userId,
                    HttpMethod.PUT,
                    new HttpEntity<>(buildUserPayload(user), headers),
                    Void.class
            );
        } catch (HttpClientErrorException ex) {
            throw keycloakError("Unable to sync Keycloak user state", ex);
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
}
