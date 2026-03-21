package com.quickdelivery;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            addRoles(realmAccessMap.get("roles"), authorities);
        }

        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
            resourceAccessMap.values().stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .forEach(clientAccess -> addRoles(clientAccess.get("roles"), authorities));
        }

        return authorities;
    }

    private void addRoles(Object rolesObject, Set<GrantedAuthority> authorities) {
        if (!(rolesObject instanceof Collection<?> roles)) {
            return;
        }
        roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }
}
