package com.quickdelivery.bootstrap;

import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.repositories.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminUserBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserBootstrap.class);

    @Bean
    public ApplicationRunner adminUserInitializer(
            Users users,
            @Value("${quickdelivery.bootstrap.admin.enabled:true}") boolean enabled,
            @Value("${quickdelivery.bootstrap.admin.email:admin.test@quickdelivery.local}") String email,
            @Value("${quickdelivery.bootstrap.admin.first-name:Admin}") String firstName,
            @Value("${quickdelivery.bootstrap.admin.last-name:Test}") String lastName,
            @Value("${quickdelivery.bootstrap.admin.type:CUSTOMER}") String type,
            @Value("${quickdelivery.bootstrap.admin.phone:}") String phone
    ) {
        return args -> {
            if (!enabled) {
                logger.info("Admin bootstrap is disabled.");
                return;
            }

            User adminUser = users.findByEmail(email);
            boolean creating = adminUser == null;
            if (creating) {
                adminUser = new User();
                adminUser.setEmailAddress(email);
            }

            adminUser.setFirstName(firstName);
            adminUser.setLastName(lastName);
            adminUser.setType(type);
            adminUser.setPhone(phone == null || phone.isBlank() ? null : phone);
            adminUser.setActiveAccount(true);
            adminUser.setEmailAddressValidation(true);
            adminUser.setPhoneValidation(phone != null && !phone.isBlank());
            adminUser.setPassword(null);

            users.save(adminUser);
            logger.info("{} admin user in QuickDeliveryDB for {}", creating ? "Created" : "Synchronized", email);
        };
    }
}
