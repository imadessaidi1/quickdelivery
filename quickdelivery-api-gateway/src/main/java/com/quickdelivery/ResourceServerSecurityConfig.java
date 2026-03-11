package com.quickdelivery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableReactiveMethodSecurity
public class ResourceServerSecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:8084",
                "http://localhost:8080",
                "https://localhost:8084",
                "https://localhost:8080"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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
