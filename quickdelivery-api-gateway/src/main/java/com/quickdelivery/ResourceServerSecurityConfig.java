package com.quickdelivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
@EnableReactiveMethodSecurity
public class ResourceServerSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://localhost:18443/auth/realms/quickdelivery}")
    private String primaryIssuerUri;

    @Value("${quickdelivery.security.additional-issuer-uris:}")
    private String additionalIssuerUris;

    @Value("${quickdelivery.frontend.base-urls:}")
    private String frontendBaseUrls;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.copyOf(PublicEndpointResolver.resolveFrontendOrigins(frontendBaseUrls)));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        String jwkSetUri = primaryIssuerUri + "/protocol/openid-connect/certs";
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();

        Set<String> allowedIssuers = PublicEndpointResolver.resolveIssuerUris(primaryIssuerUri, additionalIssuerUris);

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
            if (allowedIssuers.contains(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Unsupported issuer: " + issuer, null));
        };

        decoder.setJwtValidator(token -> {
            OAuth2TokenValidatorResult defaultResult = defaultValidator.validate(token);
            if (defaultResult.hasErrors()) {
                return defaultResult;
            }
            return issuerValidator.validate(token);
        });

        return decoder;
    }

    @Bean
    public SecurityWebFilterChain configureResourceServer(ServerHttpSecurity httpSecurity) throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        return httpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {
                })
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info", "/").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/v1/create").permitAll()
                        .pathMatchers(HttpMethod.GET, "/users/v1/userByEmail**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/users/v1/public-update-profile**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/v1/public-update**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/users/v1/validateEmail**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/packages/v1/getPackage**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/packages/v1/getGuestPackage**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/packages/v1/packages-around-address**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/packages/v1/checkGuestOTPForPickup**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/packages/v1/checkGuestOTPForDelivery**").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/packages/v1/confirm-guest-payment**").permitAll()
                        .pathMatchers("/ws/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/packages/v1/create").permitAll()
                        .pathMatchers(HttpMethod.POST, "/packages/v1/bulk-create").hasAnyAuthority("ROLE_CLIENT_PRO", "ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/packages/v1/update-packages-status").hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_PRO", "ROLE_ADMIN")
                        .pathMatchers("/users/v1/usersForValidation", "/users/v1/validateUser").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/packages/v1/reserve**", "/packages/v1/pickup**", "/packages/v1/deliver**").hasAnyAuthority("ROLE_LIVREUR", "ROLE_ADMIN")
                        .pathMatchers("/packages/v1/checkOTPForPickup**", "/packages/v1/checkOTPForDelivery**")
                        .hasAnyAuthority("ROLE_LIVREUR", "ROLE_ADMIN")
                        .pathMatchers("/packages/v1/getPackagesByDeliveryPerson**")
                        .hasAnyAuthority("ROLE_CLIENT", "ROLE_CLIENT_PRO", "ROLE_LIVREUR", "ROLE_ADMIN")
                        .pathMatchers("/packages/v1/packages-around**", "/packages/v1/packages-around-me**", "/packages/v1/packages-on-my-road**")
                        .hasAnyAuthority("ROLE_LIVREUR", "ROLE_ADMIN")
                        .pathMatchers("/users/v1/**", "/packages/v1/**", "/ws/**").authenticated()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, denied) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter))
                ))
                .build();
    }
}
