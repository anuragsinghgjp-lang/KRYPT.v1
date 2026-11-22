# KRYPT Setup, Security, Backup & Deployment Guide

## 1. Architecture Summary
- **Client**: Android (Kotlin + Jetpack Compose + Material 3, `minSdk 24`, `targetSdk 36`).
- **Authentication**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` / `GetGoogleIdOption`) + Firebase Auth (`signInWithCredential`).
- **Backend**: Cloud Firestore (`ai-studio-android-6ba29168-7ca8-41f5-a20b-df8ce5bcc850`), hardened by `firestore.rules` and verified against `firestore.test.js`.
- **Local Cache & Preferences**: Room (`KryptDatabase`) for offline message caching/queueing, DataStore (`KryptPreferences`) for Ghost Mode timers and brute-force pass cooldown state, and Android Keystore AES-256-GCM for E2EE private key wrapping.

## 2. Bootstrapped Owner Account & Special Access Passes
- **Primary Owner Email**: `anuragsinghgjp@gmail.com` (requires `email_verified == true` in Firebase Auth token).
- Upon signing in with the Owner Google account for the first time, KRYPT automatically displays the **Owner Initialization** flow, which creates `/users/{uid}`, `/admins/{uid}`, and `/platform_settings/global`.
- Regular members must redeem a valid **Special Access Pass** generated in **Owner Control Center -> Access Passes** (`KRYPT-XXXX-XXXX-XXXX`).
- Pass redemption executes inside a Firestore transaction (`redeemAccessPassAndCreateProfile`) paired with an `existsAfter` security rule check on `/access_pass_redemptions/{passHash}_{uid}` to prevent duplicate redemption and race conditions.

## 3. Environment & Configuration
- `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` store the Firebase project and Firestore database ID (`ai-studio-android-6ba29168-7ca8-41f5-a20b-df8ce5bcc850`).
- **Do NOT** store API secrets in `local.properties` or commit private upload keystores to git. Configure release signing environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) in CI/CD or AI Studio Secrets.

## 4. Backup & Disaster Recovery
- **Firestore**: Enable scheduled Cloud Firestore daily exports to a locked Cloud Storage bucket (`gcloud firestore export`) for disaster recovery of conversations, passes, and immutable audit logs.
- **Security Rules Versioning**: Keep `firestore.rules` and `firestore.test.js` in version control and re-run the rules test suite before every deployment.
- **Device Loss**: E2EE private keys remain hardware-bound to Android Keystore per device. On a new device, `ensureLocalIdentityKeys()` automatically generates and publishes a new ECDH P-256 key bundle.

## 5. Running Verification Tests
### Firestore Security Rules Suite (Local Emulator)
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

### Android Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Android APK Build
```bash
gradle :app:assembleDebug
```
