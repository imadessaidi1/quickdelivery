# Onboarding V2 Design

## Goal

Reduce latency and failure rate for account creation by splitting onboarding into two phases:

1. `create-account`
2. `complete-onboarding`

This keeps identity creation and profile bootstrap fast, while moving documents, OCR and courier completeness checks to a second step.

## Why

The current `/users/v1/create` endpoint performs too much work synchronously:

- profile validation
- address geocoding
- vehicle creation
- document validation
- S3 uploads
- OCR trigger
- Keycloak provisioning

Under load, this creates long tail latency on:

- `customer-create`
- `courier-create`
- `courier-validate`

## Target API

### POST `/users/v1/create-account`

Minimal JSON request:

- `type`
- `firstName`
- `lastName`
- `birthDate`
- `sex`
- `emailAddress`
- `emailAddressConfirmation`
- `phone`
- `phoneConfirmation`
- `password`
- `passwordConfirmation`
- `deliveryMode`

Behavior:

- validates minimal identity fields
- creates the local user
- provisions Keycloak
- creates onboarding state
- for customers:
  - marks account active immediately
  - onboarding state is completed
- for couriers:
  - leaves account inactive
  - onboarding state is `ACCOUNT_CREATED`

### POST `/users/v1/complete-onboarding`

Multipart request:

- `user`
- `vehicle`
- `locale`
- optional document files

Behavior:

- loads the existing user by id/email
- updates address, payment modes, vehicle and documents
- uploads files to S3
- triggers OCR after commit
- advances onboarding state
- for couriers:
  - reaches `READY_FOR_VALIDATION`
- for customers:
  - stays completed

### GET `/users/v1/onboarding-status?email=...`

Returns:

- `userId`
- `userType`
- `status`
- `accountCreated`
- `profileCompleted`
- `documentsUploaded`
- `readyForValidation`
- `activeAccount`
- `emailAddressValidation`

## Data Model

New entity: `UserOnboarding`

- `id`
- `version`
- `user`
- `status`
- `accountCreatedAt`
- `profileCompletedAt`
- `documentsUploadedAt`
- `readyForValidationAt`
- `completedAt`
- `lastUpdatedAt`
- `lastErrorCode`
- `lastErrorMessage`

Enum: `USER_ONBOARDING_STATUS`

- `ACCOUNT_CREATED`
- `PROFILE_COMPLETED`
- `DOCUMENTS_UPLOADED`
- `READY_FOR_VALIDATION`
- `COMPLETED`
- `FAILED`

## Compatibility Strategy

The current endpoints remain available:

- `/users/v1/create`
- `/users/v1/update`
- `/users/v1/public-update`

They continue to work during migration.

The new front flow should prefer:

- `create-account`
- `complete-onboarding`

## Activation Rules

### Customer

- local user active immediately
- Keycloak user enabled immediately
- no admin validation required
- email verification remains separate

### Courier

- local user inactive until admin validation
- Keycloak user provisioned but disabled
- onboarding reaches `READY_FOR_VALIDATION`
- admin validation still uses `/users/v1/validateUser`

## Performance Impact Expected

- customer creation path becomes mostly DB + Keycloak
- courier creation path avoids document upload/OCR on the first request
- retryability is improved because phase 2 can be replayed safely

## Validation Plan

1. unit/compile validation on users service
2. UI registration flow validation
3. perf:
   - `regression`
   - `nominal`
   - `stress`

