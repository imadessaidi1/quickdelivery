# QuickDelivery Mobile Push Setup

This project now supports real mobile push notifications with Firebase Cloud Messaging for Android.

## What is already implemented

- Backend notification persistence and per-user realtime dispatch.
- Backend Firebase push provider with safe fallback when Firebase is not configured.
- Mobile device registration with `deviceId`, `pushToken`, `platform`, and `locale`.
- Capacitor Android push integration in the mobile app.
- Notification texts localized from backend in French and English.

## Files that must stay private

- Front Android Firebase config:
  - `quickdelivery-googlemaps-front/android/app/google-services.json`
- Backend Firebase Admin credentials:
  - recommended local path: `quickdelivery-packages/firebase-service-account.json`

These files are ignored by git.

## Android app setup

1. In Firebase Console, create or reuse the Android app.
2. Use the Android application id that matches the built app.
3. Download `google-services.json`.
4. Place it at:

```text
quickdelivery-googlemaps-front/android/app/google-services.json
```

5. Run:

```powershell
cd quickdelivery-googlemaps-front
npm run cap:sync:android
```

## Backend setup

Set these environment variables for `quickdelivery-packages`:

```text
PACKAGES_PUSH_FIREBASE_ENABLED=true
PACKAGES_PUSH_FIREBASE_CREDENTIALS_PATH=C:\absolute\path\to\firebase-service-account.json
PACKAGES_PUSH_FIREBASE_PROJECT_ID=your-firebase-project-id
```

Alternative:

```text
GOOGLE_APPLICATION_CREDENTIALS=C:\absolute\path\to\firebase-service-account.json
PACKAGES_PUSH_FIREBASE_ENABLED=true
PACKAGES_PUSH_FIREBASE_PROJECT_ID=your-firebase-project-id
```

## Runtime behavior

- Web desktop/mobile browser:
  - websocket + browser notifications
- Mobile app foreground:
  - websocket in-app updates
  - FCM token registered
  - received push mirrored to a local notification
- Mobile app background:
  - Firebase push notification shown by Android

## Debug checklist

1. Log in on the mobile app.
2. Confirm `/packages/v1/devices/register` stores a non-empty `pushToken`.
3. Confirm backend starts with Firebase enabled and no initialization warning.
4. Trigger a business notification such as:
   - package created
   - package reserved
   - package picked up
   - package delivered
5. Check:
   - app foreground: local notification appears
   - app background: Android system notification appears

## Useful commands

Build frontend:

```powershell
cd quickdelivery-googlemaps-front
npm run build
```

Sync Android:

```powershell
cd quickdelivery-googlemaps-front
npm run cap:sync:android
```

Build debug APK:

```powershell
cd quickdelivery-googlemaps-front/android
.\gradlew.bat assembleDebug
```
