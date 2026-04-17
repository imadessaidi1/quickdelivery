package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserOnboardingValidationServiceTest {

    private final UserOnboardingValidationService validationService = new UserOnboardingValidationService();

    @Test
    void customerAccountCreateRequiresResidenceAddress() {
        UserDTO user = validCustomer();
        user.setAddressAuto("");
        user.setPersonalAddress(List.of());

        UserOnboardingValidationService.ValidationResult result =
                validationService.validateForAccountCreate(user, Locale.FRENCH);

        assertFalse(result.valid());
        assertTrue(result.messages().contains("L'adresse de residence est obligatoire."));
    }

    @Test
    void customerAccountCreateAcceptsResidenceAddress() {
        UserDTO user = validCustomer();
        user.setPersonalAddress(List.of(validResidenceAddress()));

        UserOnboardingValidationService.ValidationResult result =
                validationService.validateForAccountCreate(user, Locale.FRENCH);

        assertTrue(result.valid());
    }

    @Test
    void courierAccountCreateStillDoesNotRequireResidenceAddressAtProfileStep() {
        UserDTO user = validCustomer();
        user.setType("DELIVERY_PERSON");
        user.setDeliveryMode("BIKE");
        user.setBirthDate(Date.valueOf("1990-01-01"));
        user.setPersonalAddress(List.of());

        UserOnboardingValidationService.ValidationResult result =
                validationService.validateForAccountCreate(user, Locale.FRENCH);

        assertTrue(result.valid());
    }

    private UserDTO validCustomer() {
        UserDTO user = new UserDTO();
        user.setType("CUSTOMER");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmailAddress("jane.doe@example.com");
        user.setEmailAddressConfirmation("jane.doe@example.com");
        user.setPhone("+33612345678");
        user.setPhoneConfirmation("+33612345678");
        user.setPassword("Quickdelivery123@");
        user.setPasswordConfirmation("Quickdelivery123@");
        user.setAddressAuto("33 Rue Pasteur, 59243 Quarouble, France");
        return user;
    }

    private AddressDTO validResidenceAddress() {
        AddressDTO address = new AddressDTO();
        address.setType(ADDRESS_TYPE.RESIDENCE);
        address.setLine1("33 Rue Pasteur");
        address.setZipCode("59243");
        address.setTown("Quarouble");
        address.setCountry("France");
        return address;
    }
}
