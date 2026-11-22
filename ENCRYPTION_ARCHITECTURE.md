# KRYPT End-to-End Encryption (E2EE) Architecture & Limitations

## 1. Cryptographic Design Overview
KRYPT implements client-side end-to-end encryption (`KryptCryptoEngine.kt`) for both **1:1 Direct Conversations** and **Community Groups** so message payloads are encrypted on the sender's device before reaching Cloud Firestore.

### Primitives
- **Asymmetric Key Exchange**: Elliptic-Curve Diffie-Hellman (**ECDH**) over **NIST P-256 (`secp256r1`)**.
- **Key Derivation Function**: **HKDF-SHA256** (HMAC-SHA256 Extract-and-Expand) with domain-separation context labels (`KRYPT-DM-WRAP`, `KRYPT-GRP-WRAP`).
- **Authenticated Content Encryption**: **AES-256-GCM** with unique per-message 96-bit cryptographically random IVs (`SecureRandom`), 128-bit GCM authentication tags, and conversation/group IDs bound as Additional Authenticated Data (AAD).
- **At-Rest Private Key Storage**: Identity private keys (`id_<uid>`), signed prekeys (`spk_<uid>`), and group sender keys (`grp_sk_*` / `grp_rx_*`) are encrypted locally before persistence using a 256-bit **AES-256-GCM master key inside Android Keystore** (`krypt_e2ee_master_key_v1`). Raw private keys are never uploaded to Firestore.

---

## 2. Public Key Distribution & Identity Verification
1. When a member authenticates and completes onboarding, `KryptCryptoEngine.ensureLocalIdentityKeys(uid)` generates:
   - An EC P-256 **Identity Keypair**
   - A **Signed PreKey** (`signedPreKeyPub`, `signedPreKeyId`)
   - Five **One-Time PreKeys** (`oneTimePreKeys`)
   - A 12-byte SHA-256 **Key Fingerprint** displayed on the member's Profile screen (`AA:BB:...`) for out-of-band verification.
2. Only **public keys** and the public fingerprint are published to `/key_bundles/{userId}`.

---

## 3. One-to-One Messaging Protocol
For every 1:1 message sent in `conversations/{conversationId}/messages`:
1. The sender generates a fresh 256-bit random **per-message content key** (`contentKeyBytes`) and random 96-bit IV, encrypting the plaintext with `AES-256-GCM` (`AAD = conversationId`).
2. The sender generates an **ephemeral ECDH P-256 keypair** (`ephemeralPair`).
3. Using ECDH + HKDF-SHA256:
   - The sender wraps `contentKeyBytes` for the **recipient** using `ECDH(ephemeralPriv, recipientIdentityPub)`.
   - The sender also wraps `contentKeyBytes` for **themselves** using `ECDH(ephemeralPriv, senderIdentityPub)` so sent messages remain readable in sender history.
4. Both wrapped key envelopes and the encrypted ciphertext are stored in a `v2|...` payload along with `ephemeralPubKey` and `iv`.
5. On the receiving device, the recipient derives the exact same HKDF-SHA256 wrapping key via `ECDH(recipientIdentityPriv, ephemeralPubKey)`, unwraps `contentKeyBytes`, and decrypts the message.

---

## 4. Group Messaging Protocol (Sender Keys)
To avoid encrypting large group messages N times per payload:
1. Each sender generates a 256-bit **Group Sender Key** per `(groupId, epoch)` stored locally under Android Keystore encryption.
2. `distributeSenderKey()` wraps that Group Sender Key individually for each authorized member's public identity key (`ECDH + HKDF-SHA256 + AES-256-GCM`) and stores the wrapped distribution envelopes in `/groups/{groupId}/sender_keys/{senderUid}_{recipientUid}_{epoch}`.
3. Firestore security rules restrict reads of each `sender_keys` document strictly to `senderUid`, `recipientUid`, or `isOwner()`.
4. Subsequent group messages in that epoch are encrypted once with `AES-256-GCM` (`AAD = groupId`) under the sender's group key.

---

## 5. Transparent Limitations & Moderation Disclosure
KRYPT documents its cryptographic boundaries honestly:
1. **No Forward Secrecy Ratchet Across Key Loss / Double Ratchet**: KRYPT uses per-message ephemeral ECDH for 1:1 messages and epoch-based Sender Keys for groups, rather than a full Signal Double Ratchet state machine. If a device is wiped or uninstalled, keys protected inside hardware Android Keystore cannot be migrated, and historical messages encrypted exclusively to that device key will display `[E2EE Key Rotation Pending]`.
2. **Routing Metadata Is Not Encrypted**: Sender UID, recipient/participant UIDs, timestamps, typing indicator presence (suppressed during Ghost Mode), and message counts are stored as structured Firestore fields so queries, security rules, and unread counters operate.
3. **Media Binary Attachments**: Inline images/documents/voice notes are encoded and size-bounded (`<= 450 KB` base64) inside message documents; message text and captions are E2EE encrypted while attachments are stored inside the participant-isolated document.
4. **Authorized Moderation & Safety Reports**: Because un-reported E2EE messages cannot be decrypted by the server or Platform Owner without participant private keys, **Authorized Message Review** operates transparently:
   - When a member files a **Safety Report** on an abusive message, the reporting member's client attaches an explicit `evidenceSnapshot` from their decrypted view so the Platform Owner / Safety Admins can inspect the reported incident.
   - Owner inspection of group message logs records an immutable entry in `/admin_audit_logs` requiring a mandatory justification (`>= 5` chars) and can remove harmful messages (`deleted = true`, wiping ciphertext/media).
5. **Calls**: `/calls/{callId}` handles WebRTC-style SDP/ICE signaling over authenticated Firestore channels; actual real-time audio/video RTP media streams require a native WebRTC peer connection / TURN relay for production NAT traversal.
