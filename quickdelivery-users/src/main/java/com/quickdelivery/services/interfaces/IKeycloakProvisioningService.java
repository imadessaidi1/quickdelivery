package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.entities.User;

import java.util.Collection;
import java.util.Set;

public interface IKeycloakProvisioningService {
    void provisionUser(User user, String rawPassword);

    void syncUserState(User user);

    Set<String> loadActiveUserEmailsByClient(String clientId);

    Set<String> loadUserEmailsByRealmRoles(Collection<String> roleNames);
}
