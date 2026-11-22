# KRYPT Privacy Policy & Community Governance Disclosure

**Effective Date:** 2026-11-22  
**Application:** KRYPT Private Messaging & Community Platform (`com.krypt.messenger`)

## 1. Invite-Only Membership & Access Passes
KRYPT is publicly downloadable but requires an authenticated Google Identity and a valid **Special Access Pass** issued by the Platform Owner. Raw pass codes are hashed client-side (`SHA-256`) prior to atomic transaction validation and are never stored in publicly enumerable database fields.

## 2. Information We Collect & Data Isolation
- **Public Member Directory (`/users/{uid}`):** Username, display name, short bio, optional profile avatar, account status, role, and presence preferences (`onlineVisible`, `ghostModeActive`, `readReceiptsEnabled`).
- **Strictly Isolated Private Account (`/users/{uid}/private/account`):** Email address, pass hash used at onboarding, blocked user IDs, and notification preferences. Readable only by the account holder and verified Platform Owner.
- **Device Sessions (`/users/{uid}/sessions/{sessionId}`):** Device manufacturer/model, Android version, and active/revoked session state so you can remotely revoke lost sessions.
- **Encrypted Messages:** 1:1 and group text payloads are end-to-end encrypted before leaving your device. Un-reported ciphertexts cannot be read by platform administrators.

## 3. Ghost Mode Privacy Guarantee
When **Ghost Mode** is enabled (either manually or for a configured timer such as 15 minutes, 1 hour, 8 hours, or 24 hours):
- Presence heartbeats (`lastSeenAt`) are suppressed.
- Typing indicators are not transmitted.
- Direct and group read receipts (`readByUids`) and unread counter resets are strictly suppressed so you can read messages silently.
- A persistent amber indicator banner allows quick verification or one-tap exit at any time.

## 4. Permissions & Google Play Compliance
KRYPT follows strict least-privilege permission practices:
- **No broad storage permissions** (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`) are requested. Media selection uses Android's zero-permission **Photo Picker** (`PickVisualMedia`) and **Storage Access Framework** (`OpenDocument`).
- **`RECORD_AUDIO`**: Requested at runtime only when recording voice notes or starting voice/video calls.
- **`CAMERA`**: Requested at runtime only when enabling video during calls.
- **`POST_NOTIFICATIONS`**: Requested cleanly after sign-in on Android 13+ for message and call alerts.

## 5. Safety Reporting, Moderation & Immutable Audit Logs
- There is **no hidden surveillance or undisclosed backdoor**.
- Members may report abusive users, messages, or groups. When reporting a message, the member explicitly submits an `evidenceSnapshot` for safety review.
- Every administrative action by the Platform Owner or Safety Administrators (pass issuance/revocation, account warning/suspension/ban, group lock/suspension, or authorized message inspection) writes a permanent, append-only record to `/admin_audit_logs` that cannot be edited or deleted even by the Owner.
- Suspended users can view moderation reasons and submit formal appeals directly inside the application.
