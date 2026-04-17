package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.AddressBookEntryDTO;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.entities.CustomerAddressBookEntry;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.repositories.CustomerAddressBookEntries;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.services.interfaces.IAddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class AddressBookService implements IAddressBookService {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    @Autowired
    private CustomerAddressBookEntries addressBookEntries;
    @Autowired
    private Users users;
    @Autowired
    private GeoApiContext geoApiContext;

    @Override
    @Transactional(readOnly = true)
    public List<AddressBookEntryDTO> search(Long ownerUserId, String query, Integer limit) {
        validateOwnerUserId(ownerUserId);
        String normalizedQuery = normalizeText(query);
        if (normalizedQuery.length() < 2) {
            return List.of();
        }
        int resolvedLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
        return addressBookEntries.searchActiveEntries(ownerUserId, normalizedQuery, PageRequest.of(0, resolvedLimit))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AddressBookEntryDTO save(Long ownerUserId, AddressBookEntryDTO request) {
        validateOwnerUserId(ownerUserId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address book entry is required");
        }
        validateRequest(request);

        User owner = users.findById(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner user not found"));
        Timestamp now = Timestamp.from(Instant.now());
        String normalizedEmail = normalizeEmail(request.getEmail());
        String requestedSignature = addressSignature(request);
        CustomerAddressBookEntry entry = addressBookEntries.findActiveByOwnerAndEmail(ownerUserId, normalizedEmail)
                .stream()
                .filter(candidate -> Objects.equals(addressSignature(candidate), requestedSignature))
                .findFirst()
                .orElseGet(CustomerAddressBookEntry::new);

        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(now);
        }
        entry.setOwner(owner);
        entry.setActive(true);
        entry.setUpdatedAt(now);
        entry.setLastUsedAt(now);
        applyRequest(entry, request, normalizedEmail);
        geocodeIfNeeded(entry);
        return toDto(addressBookEntries.save(entry));
    }

    private void validateOwnerUserId(Long ownerUserId) {
        if (ownerUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user id is required");
        }
    }

    private void validateRequest(AddressBookEntryDTO request) {
        if (isBlank(request.getFirstName()) || isBlank(request.getLastName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient first name and last name are required");
        }
        if (isBlank(request.getEmail()) || !normalizeEmail(request.getEmail()).contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient email is required");
        }
        if (isBlank(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient phone is required");
        }
        if (isBlank(request.getAddressAuto()) && (isBlank(request.getLine1()) || isBlank(request.getTown())
                || isBlank(request.getZipCode()) || isBlank(request.getCountry()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient address is required");
        }
    }

    private void applyRequest(CustomerAddressBookEntry entry, AddressBookEntryDTO request, String normalizedEmail) {
        entry.setFirstName(clean(request.getFirstName()));
        entry.setLastName(clean(request.getLastName()));
        entry.setEmail(normalizedEmail);
        entry.setPhone(clean(request.getPhone()));
        entry.setLine1(clean(request.getLine1()));
        entry.setLine2(clean(request.getLine2()));
        entry.setTown(clean(request.getTown()));
        entry.setZipCode(clean(request.getZipCode()));
        entry.setCountry(clean(request.getCountry()));
        entry.setFloor(request.getFloor());
        entry.setHasElevator(request.getHasElevator());
        entry.setLatitude(request.getLatitude());
        entry.setLongitude(request.getLongitude());
        entry.setAddressAuto(clean(request.getAddressAuto()));
    }

    private void geocodeIfNeeded(CustomerAddressBookEntry entry) {
        if (entry.getLatitude() != null && entry.getLongitude() != null) {
            return;
        }
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setLine1(entry.getLine1());
        addressDTO.setLine2(entry.getLine2());
        addressDTO.setZipCode(entry.getZipCode());
        addressDTO.setTown(entry.getTown());
        addressDTO.setCountry(entry.getCountry());
        try {
            GeoHelper.AddressGeoCoding(geoApiContext, addressDTO);
            entry.setLatitude(addressDTO.getLatitude());
            entry.setLongitude(addressDTO.getLongitude());
        } catch (IOException | InterruptedException | ApiException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private AddressBookEntryDTO toDto(CustomerAddressBookEntry entry) {
        AddressBookEntryDTO dto = new AddressBookEntryDTO();
        dto.setId(entry.getId());
        dto.setVersion(entry.getVersion());
        dto.setOwnerUserId(entry.getOwner() == null ? null : entry.getOwner().getId());
        dto.setFirstName(entry.getFirstName());
        dto.setLastName(entry.getLastName());
        dto.setEmail(entry.getEmail());
        dto.setPhone(entry.getPhone());
        dto.setLine1(entry.getLine1());
        dto.setLine2(entry.getLine2());
        dto.setTown(entry.getTown());
        dto.setZipCode(entry.getZipCode());
        dto.setCountry(entry.getCountry());
        dto.setFloor(entry.getFloor());
        dto.setHasElevator(entry.getHasElevator());
        dto.setLatitude(entry.getLatitude());
        dto.setLongitude(entry.getLongitude());
        dto.setAddressAuto(resolveAddressAuto(entry));
        dto.setDisplayLabel(displayLabel(entry));
        dto.setActive(entry.getActive());
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setUpdatedAt(entry.getUpdatedAt());
        dto.setLastUsedAt(entry.getLastUsedAt());
        return dto;
    }

    private String displayLabel(CustomerAddressBookEntry entry) {
        return Stream.of(
                        Stream.of(entry.getFirstName(), entry.getLastName()).filter(value -> !isBlank(value)).reduce((left, right) -> left + " " + right).orElse(""),
                        entry.getEmail(),
                        compactAddress(entry)
                )
                .filter(value -> !isBlank(value))
                .reduce((left, right) -> left + " - " + right)
                .orElse("");
    }

    private String resolveAddressAuto(CustomerAddressBookEntry entry) {
        if (!isBlank(entry.getAddressAuto())) {
            return entry.getAddressAuto();
        }
        return compactAddress(entry);
    }

    private String compactAddress(CustomerAddressBookEntry entry) {
        return Stream.of(entry.getLine1(), entry.getZipCode(), entry.getTown(), entry.getCountry())
                .filter(value -> !isBlank(value))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String addressSignature(AddressBookEntryDTO dto) {
        return Stream.of(dto.getLine1(), dto.getLine2(), dto.getZipCode(), dto.getTown(), dto.getCountry(), dto.getAddressAuto())
                .map(this::normalizeText)
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
    }

    private String addressSignature(CustomerAddressBookEntry entry) {
        return Stream.of(entry.getLine1(), entry.getLine2(), entry.getZipCode(), entry.getTown(), entry.getCountry(), entry.getAddressAuto())
                .map(this::normalizeText)
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String normalizeEmail(String value) {
        return normalizeText(value);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
