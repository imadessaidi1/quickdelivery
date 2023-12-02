package com.quickdelivery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

@Configuration
public class ResourceServerSecurityConfig {

    @Bean
    public SecurityWebFilterChain configureResourceServer(ServerHttpSecurity httpSecurity, ServerLogoutSuccessHandler handler) throws Exception {

        httpSecurity
                .authorizeExchange()
                .pathMatchers("/actuator/**", "/","/logout.html")
                .permitAll()
                .and()
                .authorizeExchange()
                .anyExchange()
                .authenticated()
                .and()
                .oauth2Login() // to redirect to oauth2 login page.
                .and()
                .logout()
                .logoutSuccessHandler(handler)
        ;

        return httpSecurity.build();
    }
}