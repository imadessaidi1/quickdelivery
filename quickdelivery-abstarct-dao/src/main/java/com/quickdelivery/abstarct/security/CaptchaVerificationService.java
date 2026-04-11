package com.quickdelivery.abstarct.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class CaptchaVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(CaptchaVerificationService.class);

    private final RestTemplate restTemplate;

    @Value("${quickdelivery.security.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${quickdelivery.security.captcha.provider:turnstile}")
    private String captchaProvider;

    @Value("${quickdelivery.security.captcha.turnstile.secret-key:}")
    private String turnstileSecretKey;

    @Value("${quickdelivery.security.captcha.turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String turnstileVerifyUrl;

    public CaptchaVerificationService() {
        this.restTemplate = new RestTemplate();
    }

    public void validateOrThrow(String token, String remoteIp, String action) {
        if (!captchaEnabled) {
            return;
        }

        if (!"turnstile".equalsIgnoreCase(captchaProvider)) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Unsupported captcha provider");
        }

        if (turnstileSecretKey == null || turnstileSecretKey.isBlank()) {
            logger.error("Captcha is enabled but QUICKDELIVERY_SECURITY_CAPTCHA_TURNSTILE_SECRET_KEY is missing");
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Captcha is temporarily unavailable");
        }

        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Captcha token is required");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", turnstileSecretKey);
        formData.add("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            formData.add("remoteip", remoteIp);
        }

        TurnstileVerifyResponse response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            response = restTemplate.postForObject(
                    turnstileVerifyUrl,
                    new HttpEntity<>(formData, headers),
                    TurnstileVerifyResponse.class
            );
        } catch (Exception exception) {
            logger.warn("Captcha verification call failed for action {}: {}", action, exception.getMessage());
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Captcha verification failed");
        }

        if (response == null || !response.success()) {
            logger.info("Captcha rejected for action {} with codes {}", action, response == null ? List.of("unknown") : response.errorCodes());
            throw new ResponseStatusException(BAD_REQUEST, "Captcha verification rejected");
        }
    }

    public boolean isEnabled() {
        return captchaEnabled;
    }

    public String resolveClientIp(String xForwardedFor, String remoteAddr) {
        String candidate = xForwardedFor;
        if (candidate == null || candidate.isBlank()) {
            candidate = remoteAddr;
        }
        if (candidate == null) {
            return "";
        }
        String[] chain = candidate.split(",");
        return chain[0].trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TurnstileVerifyResponse(
            boolean success,
            String action,
            String hostname,
            List<String> errorCodes
    ) {
        public List<String> errorCodes() {
            return errorCodes == null ? List.of() : errorCodes;
        }
    }
}
