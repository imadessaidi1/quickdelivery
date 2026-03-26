package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PackagesServiceValidationTest {

    private final PackagesService packagesService = new PackagesService();

    @Test
    void createPackageRejectsMissingDeclaredValueWhenInsuranceIsSelected() {
        PackageDTO packageDTO = buildValidPackage();
        packageDTO.setInsuranceSelected(true);
        packageDTO.setDeclaredValue(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ReflectionTestUtils.invokeMethod(packagesService, "validatePackageCreationRequest", packageDTO)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Declared value is required when insurance is selected", exception.getReason());
    }

    @Test
    void createPackageRejectsMissingPickupElevatorSelectionWhenFloorIsAboveZero() {
        PackageDTO packageDTO = buildValidPackage();
        packageDTO.getAddresses().get(0).setFloor(3);
        packageDTO.getAddresses().get(0).setHasElevator(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ReflectionTestUtils.invokeMethod(packagesService, "validatePackageCreationRequest", packageDTO)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Elevator selection is required for pickup", exception.getReason());
    }

    @Test
    void createPackageRejectsMissingDeliveryElevatorSelectionWhenFloorIsAboveZero() {
        PackageDTO packageDTO = buildValidPackage();
        packageDTO.getAddresses().get(1).setFloor(2);
        packageDTO.getAddresses().get(1).setHasElevator(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ReflectionTestUtils.invokeMethod(packagesService, "validatePackageCreationRequest", packageDTO)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Elevator selection is required for delivery", exception.getReason());
    }

    private PackageDTO buildValidPackage() {
        PackageDTO packageDTO = new PackageDTO();
        packageDTO.setDeliverySpeed("STANDARD");
        packageDTO.setInsuranceSelected(false);
        packageDTO.setDeclaredValue(null);
        packageDTO.setAddresses(List.of(
                buildAddress(ADDRESS_TYPE.DEPARTURE, "pickup@example.com"),
                buildAddress(ADDRESS_TYPE.ARRIVAL, "delivery@example.com")
        ));
        return packageDTO;
    }

    private AddressDTO buildAddress(ADDRESS_TYPE type, String email) {
        AddressDTO address = new AddressDTO();
        address.setType(type);
        address.setFirstName("Jane");
        address.setLastName("Doe");
        address.setPhone("+33123456789");
        address.setEmail(email);
        address.setAddressAuto("3 Rue Pasteur, 94450 Limeil-Brevannes, France");
        address.setLine1("3 Rue Pasteur");
        address.setZipCode("94450");
        address.setTown("Limeil-Brevannes");
        address.setCountry("France");
        address.setFloor(0);
        address.setHasElevator(null);
        return address;
    }
}
