package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.entities.User;

public interface IKeycloakProvisioningService {
    void provisionUser(User user, String rawPassword);

    void syncUserState(User user);
}
