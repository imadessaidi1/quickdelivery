package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PaymentDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.parameters.DOCUMENT_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.parameters.PAYMENT_TYPE;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserOnboardingValidationService {
    private static final int MINIMUM_COURIER_AGE = 18;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{5,14}$");
    private static final Set<String> DELIVERY_MODES = Set.of("CAR", "SCOOTER", "BIKE", "ON_FOOT");

    public ValidationResult validateForAccountCreate(UserDTO user, Locale locale) {
        List<String> messages = new ArrayList<>();
        boolean courier = "DELIVERY_PERSON".equalsIgnoreCase(user.getType());

        validateCommonProfile(user, messages, locale);
        validateCredentials(user, messages, locale);

        if (courier) {
            if (!hasValidAdultBirthDate(user.getBirthDate())) {
                messages.add(localize("COURIER_MINIMUM_AGE", locale));
            }
            String deliveryMode = Objects.toString(user.getDeliveryMode(), "").trim().toUpperCase(Locale.ROOT);
            if (!DELIVERY_MODES.contains(deliveryMode)) {
                messages.add(localize("DELIVERY_MODE_INVALID", locale));
            }
        } else if (!hasResidenceAddress(user.getPersonalAddress(), user.getAddressAuto())) {
            messages.add(localize("ADDRESS_REQUIRED", locale));
        }

        return new ValidationResult(messages.isEmpty(), messages);
    }

    public ValidationResult validateForCreate(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        return validate(user, vehicleDTO, filesMap, null, locale);
    }

    public ValidationResult validateForUpdate(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, User existingUser, Locale locale) {
        return validate(user, vehicleDTO, filesMap, existingUser, locale);
    }

    public ValidationResult validateForDraftStep(UserDTO user,
                                                 VehicleDTO vehicleDTO,
                                                 MultiValueMap<String, MultipartFile> filesMap,
                                                 User existingUser,
                                                 Locale locale,
                                                 int step) {
        List<String> messages = new ArrayList<>();
        boolean courier = "DELIVERY_PERSON".equalsIgnoreCase(user.getType());
        validateCommonProfile(user, messages, locale);
        if (!courier) {
            return new ValidationResult(messages.isEmpty(), messages);
        }

        if (!hasValidAdultBirthDate(user.getBirthDate())) {
            messages.add(localize("COURIER_MINIMUM_AGE", locale));
        }

        String deliveryMode = Objects.toString(user.getDeliveryMode(), "").trim().toUpperCase(Locale.ROOT);
        if (!DELIVERY_MODES.contains(deliveryMode)) {
            messages.add(localize("DELIVERY_MODE_INVALID", locale));
            return new ValidationResult(false, messages);
        }

        if (step >= 2 && !hasResidenceAddress(user.getPersonalAddress(), user.getAddressAuto())) {
            messages.add(localize("ADDRESS_REQUIRED", locale));
        }

        if (step >= 3) {
            if (!hasValidPaymentMethod(user.getPaymentModes(), existingUser)) {
                messages.add(localize("PAYMENT_REQUIRED", locale));
            }
            validateRequiredUserDocuments(deliveryMode, filesMap, existingUser, messages, locale);
        }

        if (step >= 4 && requiresVehicleInfo(deliveryMode)) {
            if (!hasText(vehicleDTO.getRegistrationNumber())) {
                messages.add(localize("VEHICLE_REGISTRATION_REQUIRED", locale));
            }
            if (!hasText(vehicleDTO.getBrand())) {
                messages.add(localize("VEHICLE_BRAND_REQUIRED", locale));
            }
            if (!hasText(vehicleDTO.getModel())) {
                messages.add(localize("VEHICLE_MODEL_REQUIRED", locale));
            }
            if (!hasText(vehicleDTO.getEnergyType())) {
                messages.add(localize("VEHICLE_ENERGY_REQUIRED", locale));
            }
            validateRequiredVehicleDocuments(deliveryMode, filesMap, existingUser, messages, locale);
        }

        return new ValidationResult(messages.isEmpty(), messages);
    }

    private ValidationResult validate(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, User existingUser, Locale locale) {
        List<String> messages = new ArrayList<>();
        boolean courier = "DELIVERY_PERSON".equalsIgnoreCase(user.getType());

        validateCommonProfile(user, messages, locale);
        if (courier) {
            validateCourierProfile(user, vehicleDTO, filesMap, existingUser, messages, locale);
        }
        return new ValidationResult(messages.isEmpty(), messages);
    }

    private void validateCommonProfile(UserDTO user, List<String> messages, Locale locale) {
        if (!hasText(user.getFirstName())) {
            messages.add(localize("FIRST_NAME_REQUIRED", locale));
        }
        if (!hasText(user.getLastName())) {
            messages.add(localize("LAST_NAME_REQUIRED", locale));
        }
        if (!hasText(user.getEmailAddress()) || !EMAIL_PATTERN.matcher(user.getEmailAddress()).matches()) {
            messages.add(localize("EMAIL_INVALID", locale));
        }
        if (!hasText(user.getPhone()) || !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            messages.add(localize("PHONE_INVALID", locale));
        }
    }

    private void validateCredentials(UserDTO user, List<String> messages, Locale locale) {
        if (!hasText(user.getPassword()) || user.getPassword().length() < 8) {
            messages.add(localize("PASSWORD_INVALID", locale));
        }
        if (!Objects.equals(user.getPassword(), user.getPasswordConfirmation())) {
            messages.add(localize("PASSWORD_CONFIRMATION_INVALID", locale));
        }
        if (!Objects.equals(user.getEmailAddress(), user.getEmailAddressConfirmation())) {
            messages.add(localize("EMAIL_CONFIRMATION_INVALID", locale));
        }
        if (!Objects.equals(user.getPhone(), user.getPhoneConfirmation())) {
            messages.add(localize("PHONE_CONFIRMATION_INVALID", locale));
        }
    }

    private void validateCourierProfile(UserDTO user,
                                        VehicleDTO vehicleDTO,
                                        MultiValueMap<String, MultipartFile> filesMap,
                                        User existingUser,
                                        List<String> messages,
                                        Locale locale) {
        if (!hasValidAdultBirthDate(user.getBirthDate())) {
            messages.add(localize("COURIER_MINIMUM_AGE", locale));
        }
        if (!hasResidenceAddress(user.getPersonalAddress(), user.getAddressAuto())) {
            messages.add(localize("ADDRESS_REQUIRED", locale));
        }

        String deliveryMode = Objects.toString(user.getDeliveryMode(), "").trim().toUpperCase(Locale.ROOT);
        if (!DELIVERY_MODES.contains(deliveryMode)) {
            messages.add(localize("DELIVERY_MODE_INVALID", locale));
            return;
        }

        if (!hasValidPaymentMethod(user.getPaymentModes(), existingUser)) {
            messages.add(localize("PAYMENT_REQUIRED", locale));
        }

        validateRequiredUserDocuments(deliveryMode, filesMap, existingUser, messages, locale);
        validateRequiredVehicleDocuments(deliveryMode, filesMap, existingUser, messages, locale);

        if (requiresVehicleInfo(deliveryMode)) {
            if (!hasVehicleField(vehicleDTO == null ? null : vehicleDTO.getRegistrationNumber(), existingUser, ExistingVehicleField.REGISTRATION_NUMBER)) {
                messages.add(localize("VEHICLE_REGISTRATION_REQUIRED", locale));
            }
            if (!hasVehicleField(vehicleDTO == null ? null : vehicleDTO.getBrand(), existingUser, ExistingVehicleField.BRAND)) {
                messages.add(localize("VEHICLE_BRAND_REQUIRED", locale));
            }
            if (!hasVehicleField(vehicleDTO == null ? null : vehicleDTO.getModel(), existingUser, ExistingVehicleField.MODEL)) {
                messages.add(localize("VEHICLE_MODEL_REQUIRED", locale));
            }
            if (!hasVehicleField(vehicleDTO == null ? null : vehicleDTO.getEnergyType(), existingUser, ExistingVehicleField.ENERGY_TYPE)) {
                messages.add(localize("VEHICLE_ENERGY_REQUIRED", locale));
            }
        }
    }

    private void validateRequiredUserDocuments(String deliveryMode,
                                               MultiValueMap<String, MultipartFile> filesMap,
                                               User existingUser,
                                               List<String> messages,
                                               Locale locale) {
        Set<DOCUMENT_TYPE> availableDocuments = availableDocuments(filesMap, existingUser);
        requiredUserDocuments(deliveryMode).stream()
                .filter(documentType -> !availableDocuments.contains(documentType))
                .forEach(documentType -> messages.add(localizeMissingDocument(documentType, locale)));
    }

    private void validateRequiredVehicleDocuments(String deliveryMode,
                                                  MultiValueMap<String, MultipartFile> filesMap,
                                                  User existingUser,
                                                  List<String> messages,
                                                  Locale locale) {
        Set<DOCUMENT_TYPE> availableDocuments = availableDocuments(filesMap, existingUser);
        requiredVehicleDocuments(deliveryMode).stream()
                .filter(documentType -> !availableDocuments.contains(documentType))
                .forEach(documentType -> messages.add(localizeMissingDocument(documentType, locale)));
    }

    private Set<DOCUMENT_TYPE> availableDocuments(MultiValueMap<String, MultipartFile> filesMap, User existingUser) {
        Set<DOCUMENT_TYPE> availableDocuments = new HashSet<>();
        if (existingUser != null) {
            existingUser.getDocument().stream()
                    .filter(document -> document.getDocumentStatus() != DOCUMENT_STATUS.REJECTED)
                    .map(Document::getType)
                    .forEach(availableDocuments::add);
        }

        if (filesMap != null) {
            filesMap.keySet().forEach(key -> {
                try {
                    availableDocuments.add(DOCUMENT_TYPE.valueOf(key));
                } catch (IllegalArgumentException ignored) {
                }
            });
        }
        return availableDocuments;
    }

    private boolean hasResidenceAddress(List<AddressDTO> addresses, String addressAuto) {
        if (hasText(addressAuto)) {
            return true;
        }
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }
        return addresses.stream().anyMatch(address ->
                hasText(address.getLine1()) && hasText(address.getTown()) && hasText(address.getZipCode()) && hasText(address.getCountry()));
    }

    private boolean hasValidAdultBirthDate(Date birthDate) {
        if (birthDate == null) {
            return false;
        }
        LocalDate birthLocalDate = birthDate.toLocalDate();
        return Period.between(birthLocalDate, LocalDate.now()).getYears() >= MINIMUM_COURIER_AGE;
    }

    private boolean hasValidPaymentMethod(Map<PAYMENT_TYPE, PaymentDTO> paymentModes, User existingUser) {
        if (paymentModes == null || paymentModes.isEmpty()) {
            return hasExistingPaymentMethod(existingUser);
        }
        PaymentDTO card = paymentModes.get(PAYMENT_TYPE.CREDIT_CARD);
        boolean cardValid = card != null
                && hasText(card.getHolderNam())
                && hasText(card.getCardNumber())
                && hasText(card.getExpiryDate())
                && hasText(card.getCvv());
        PaymentDTO iban = paymentModes.get(PAYMENT_TYPE.IBAN);
        boolean ibanValid = iban != null
                && hasText(iban.getIban())
                && hasText(iban.getBic());
        if (cardValid || ibanValid) {
            return true;
        }
        return hasExistingPaymentMethod(existingUser);
    }

    private boolean hasExistingPaymentMethod(User existingUser) {
        return existingUser != null
                && existingUser.getPayments() != null
                && Hibernate.isInitialized(existingUser.getPayments())
                && !existingUser.getPayments().isEmpty();
    }

    private boolean requiresVehicleInfo(String deliveryMode) {
        return "CAR".equals(deliveryMode) || "SCOOTER".equals(deliveryMode);
    }

    private Set<DOCUMENT_TYPE> requiredUserDocuments(String deliveryMode) {
        if ("CAR".equals(deliveryMode) || "SCOOTER".equals(deliveryMode)) {
            return EnumSet.of(
                    DOCUMENT_TYPE.ID,
                    DOCUMENT_TYPE.PICTURE,
                    DOCUMENT_TYPE.DRIVER_LICENCE,
                    DOCUMENT_TYPE.USER_COMPANY_EXTRACT,
                    DOCUMENT_TYPE.USER_COMPANY_INSURANCE
            );
        }
        return EnumSet.of(
                DOCUMENT_TYPE.ID,
                DOCUMENT_TYPE.PICTURE,
                DOCUMENT_TYPE.USER_COMPANY_EXTRACT,
                DOCUMENT_TYPE.USER_COMPANY_INSURANCE
        );
    }

    private Set<DOCUMENT_TYPE> requiredVehicleDocuments(String deliveryMode) {
        if (!requiresVehicleInfo(deliveryMode)) {
            return Set.of();
        }
        return EnumSet.of(DOCUMENT_TYPE.GRAY_CARD, DOCUMENT_TYPE.INSURANCE);
    }

    private String localizeMissingDocument(DOCUMENT_TYPE documentType, Locale locale) {
        boolean french = locale != null && "fr".equalsIgnoreCase(locale.getLanguage());
        return switch (documentType) {
            case ID -> french ? "Document requis manquant : piece d'identite." : "Missing required document: ID.";
            case PICTURE -> french ? "Document requis manquant : photo de profil." : "Missing required document: profile picture.";
            case DRIVER_LICENCE -> french ? "Document requis manquant : permis de conduire." : "Missing required document: driver licence.";
            case USER_COMPANY_EXTRACT -> french ? "Document requis manquant : extrait d'entreprise." : "Missing required document: company extract.";
            case USER_COMPANY_INSURANCE -> french ? "Document requis manquant : assurance d'entreprise." : "Missing required document: company insurance.";
            case GRAY_CARD -> french ? "Document requis manquant : carte grise." : "Missing required document: vehicle registration.";
            case INSURANCE -> french ? "Document requis manquant : assurance vehicule." : "Missing required document: vehicle insurance.";
            case RIB -> french ? "Document requis manquant : RIB." : "Missing required document: bank identity statement.";
            default -> french ? "Document requis manquant." : "Missing required document.";
        };
    }

    private String localize(String code, Locale locale) {
        boolean french = locale != null && "fr".equalsIgnoreCase(locale.getLanguage());
        return switch (code) {
            case "FIRST_NAME_REQUIRED" -> french ? "Le prenom est obligatoire." : "First name is required.";
            case "LAST_NAME_REQUIRED" -> french ? "Le nom est obligatoire." : "Last name is required.";
            case "EMAIL_INVALID" -> french ? "L'adresse e-mail est invalide." : "Email address is invalid.";
            case "PHONE_INVALID" -> french ? "Le numero de telephone est invalide." : "Phone number is invalid.";
            case "COURIER_MINIMUM_AGE" -> french ? "Le livreur doit avoir au moins 18 ans." : "Courier must be at least 18 years old.";
            case "ADDRESS_REQUIRED" -> french ? "L'adresse de residence est obligatoire." : "Residence address is required.";
            case "DELIVERY_MODE_INVALID" -> french ? "Le mode de livraison selectionne est invalide." : "Selected delivery mode is invalid.";
            case "PAYMENT_REQUIRED" -> french ? "Un moyen de paiement complet est obligatoire." : "A complete payment method is required.";
            case "VEHICLE_REGISTRATION_REQUIRED" -> french ? "Le numero d'immatriculation est obligatoire." : "Vehicle registration number is required.";
            case "VEHICLE_BRAND_REQUIRED" -> french ? "La marque du vehicule est obligatoire." : "Vehicle brand is required.";
            case "VEHICLE_MODEL_REQUIRED" -> french ? "Le modele du vehicule est obligatoire." : "Vehicle model is required.";
            case "VEHICLE_ENERGY_REQUIRED" -> french ? "Le type d'energie du vehicule est obligatoire." : "Vehicle energy type is required.";
            case "PASSWORD_INVALID" -> french ? "Le mot de passe doit contenir au moins 8 caracteres." : "Password must contain at least 8 characters.";
            case "PASSWORD_CONFIRMATION_INVALID" -> french ? "La confirmation du mot de passe est invalide." : "Password confirmation is invalid.";
            case "EMAIL_CONFIRMATION_INVALID" -> french ? "La confirmation de l'adresse e-mail est invalide." : "Email confirmation is invalid.";
            case "PHONE_CONFIRMATION_INVALID" -> french ? "La confirmation du numero de telephone est invalide." : "Phone confirmation is invalid.";
            default -> french ? "Le dossier n'est pas complet." : "The onboarding file is incomplete.";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasVehicleField(String incomingValue, User existingUser, ExistingVehicleField field) {
        if (hasText(incomingValue)) {
            return true;
        }
        if (existingUser == null || existingUser.getVehicles() == null) {
            return false;
        }
        return existingUser.getVehicles().stream().anyMatch(vehicle -> switch (field) {
            case REGISTRATION_NUMBER -> hasText(vehicle.getRegistrationNumber());
            case BRAND -> hasText(vehicle.getBrand());
            case MODEL -> hasText(vehicle.getModel());
            case ENERGY_TYPE -> hasText(vehicle.getEnergyType());
        });
    }

    private enum ExistingVehicleField {
        REGISTRATION_NUMBER,
        BRAND,
        MODEL,
        ENERGY_TYPE
    }

    public record ValidationResult(boolean valid, List<String> messages) {
    }
}
