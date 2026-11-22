const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const crypto = require("node:crypto");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_uid_1";
const OWNER_EMAIL = "anuragsinghgjp@gmail.com";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const CHARLIE_UID = "charlie_uid_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sha256(str) {
  return crypto.createHash("sha256").update(str).digest("hex");
}

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function futureDate(offsetMs = 86400000) {
  return new Date(Date.now() + offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function ownerDb(emailVerified = true) {
  return testEnv
    .authenticatedContext(OWNER_UID, {
      email: OWNER_EMAIL,
      email_verified: emailVerified,
    })
    .firestore();
}

function userDb(uid, email = `${uid}@krypt.test`) {
  return testEnv
    .authenticatedContext(uid, { email, email_verified: true })
    .firestore();
}

test("Unauthenticated access: blocked across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").get());
  await assertFails(unauth.collection("conversations").get());
  await assertFails(unauth.collection("groups").get());
  await assertFails(unauth.collection("access_passes").get());
  await assertFails(unauth.collection("admin_audit_logs").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      displayName: "Alice",
      accountStatus: "ACTIVE",
      role: "MEMBER",
      onlineVisible: true,
      ghostModeActive: false,
      readReceiptsEnabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Email spoofing & privilege escalation: unverified owner email and self-assigned OWNER role rejected", async () => {
  const spoofedOwner = ownerDb(false);
  await assertFails(spoofedOwner.collection("admin_audit_logs").get());
  await assertFails(
    spoofedOwner.collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      username: "spoof",
      displayName: "Spoof",
      accountStatus: "ACTIVE",
      role: "OWNER",
      onlineVisible: true,
      ghostModeActive: false,
      readReceiptsEnabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const alice = userDb(ALICE_UID);
  await assertFails(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      displayName: "Alice",
      accountStatus: "ACTIVE",
      role: "OWNER",
      onlineVisible: true,
      ghostModeActive: false,
      readReceiptsEnabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("PII Isolation & Ghost Mode profile updates", async () => {
  const alice = userDb(ALICE_UID, "alice@krypt.test");
  const bob = userDb(BOB_UID, "bob@krypt.test");
  const now = pastDate();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_krypt",
      displayName: "Alice",
      bio: "Private member",
      accountStatus: "ACTIVE",
      role: "MEMBER",
      onlineVisible: true,
      ghostModeActive: false,
      ghostModeUntilEpochMs: 0,
      readReceiptsEnabled: true,
      lastSeenAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Private PII doc
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@krypt.test",
      passHashUsed: sha256("KRYPT-PASS-1"),
      blockedUids: [],
      notifyMessages: true,
      notifyGroups: true,
      notifyCalls: true,
      notifyAnnouncements: true,
      hideLockScreenPreview: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's private PII doc
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );

  // Alice can toggle Ghost Mode, but cannot change role or accountStatus
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      ghostModeActive: true,
      ghostModeUntilEpochMs: Date.now() + 3600000,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      role: "OWNER",
      updatedAt: pastDate(400),
    })
  );
});

test("Special Access Pass: owner creation, non-owner enumeration blocked, atomic redemption enforced", async () => {
  const owner = ownerDb(true);
  const alice = userDb(ALICE_UID);
  const passHash = sha256("KRYPT-ALPHA-001");
  const now = pastDate(5000);

  await assertSucceeds(
    owner.collection("access_passes").doc(passHash).set({
      passHash,
      label: "Founding Cohort",
      passType: "SINGLE_USE",
      maxUses: 1,
      usedCount: 0,
      revoked: false,
      expiresAt: futureDate(),
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Regular user cannot list all passes
  await assertFails(alice.collection("access_passes").get());

  // Cannot update usedCount without atomic redemption doc
  await assertFails(
    alice.collection("access_passes").doc(passHash).update({
      usedCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Atomic batch redemption succeeds
  const batch = alice.batch();
  batch.update(alice.collection("access_passes").doc(passHash), {
    usedCount: 1,
    updatedAt: pastDate(1000),
  });
  batch.set(alice.collection("access_pass_redemptions").doc(`${passHash}_${ALICE_UID}`), {
    passHash,
    userId: ALICE_UID,
    username: "alice_krypt",
    redeemedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Duplicate redemption by Bob fails because maxUses == 1
  const bob = userDb(BOB_UID);
  const bobBatch = bob.batch();
  bobBatch.update(bob.collection("access_passes").doc(passHash), {
    usedCount: 2,
    updatedAt: pastDate(500),
  });
  bobBatch.set(bob.collection("access_pass_redemptions").doc(`${passHash}_${BOB_UID}`), {
    passHash,
    userId: BOB_UID,
    username: "bob_krypt",
    redeemedAt: pastDate(500),
  });
  await assertFails(bobBatch.commit());
});

test("1:1 Conversations & E2EE Messages: participant isolation and filtered queries", async () => {
  const alice = userDb(ALICE_UID);
  const charlie = userDb(CHARLIE_UID);
  const now = pastDate(5000);
  const convId = "conv_alice_bob";

  await assertSucceeds(
    alice.collection("conversations").doc(convId).set({
      participantUids: [ALICE_UID, BOB_UID],
      lastMessageCiphertext: "c2VjcmV0",
      lastMessageIv: "aXZ2dnZ2dnY=",
      lastMessageEphemeralPub: "cHVi",
      lastMessageSenderId: ALICE_UID,
      lastMessagePreviewHint: "Encrypted message",
      lastMessageAt: now,
      pinnedByUids: [],
      archivedByUids: [],
      mutedByUids: [],
      typingUid: "",
      unreadCounts: { [BOB_UID]: 1 },
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      conversationId: convId,
      participantUids: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      ciphertext: "YWJjZGVm",
      iv: "MTIzNDU2Nzg=",
      ephemeralPubKey: "cHVia2V5MTIzNDU2Nzg5MA==",
      keyVersion: 1,
      messageType: "TEXT",
      reactions: {},
      edited: false,
      deleted: false,
      pinned: false,
      deliveredToUids: [ALICE_UID],
      readByUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds for Alice
  await assertSucceeds(
    alice.collection("conversations").where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Unfiltered query fails for Charlie
  await assertFails(charlie.collection("conversations").get());
  await assertFails(charlie.collection("conversations").doc(convId).get());
  await assertFails(
    charlie.collection("conversations").doc(convId).collection("messages").doc("msg_1").get()
  );
});

test("Immutable Audit Logs & Owner Controls", async () => {
  const owner = ownerDb(true);
  const alice = userDb(ALICE_UID);
  const now = pastDate(2000);

  await assertSucceeds(
    owner.collection("admin_audit_logs").doc("log_1").set({
      adminId: OWNER_UID,
      adminEmail: OWNER_EMAIL,
      action: "GENERATE_PASS",
      targetType: "ACCESS_PASS",
      targetId: "hash_123",
      reason: "Onboarding VIP member",
      metadata: "maxUses=1",
      createdAt: now,
    })
  );

  // Non-owner cannot read or write audit logs
  await assertFails(alice.collection("admin_audit_logs").get());
  await assertFails(
    alice.collection("admin_audit_logs").doc("log_2").set({
      adminId: ALICE_UID,
      adminEmail: "alice@krypt.test",
      action: "SPOOF",
      targetType: "USER",
      targetId: BOB_UID,
      reason: "test",
      createdAt: now,
    })
  );

  // Even Owner cannot update or delete audit logs
  await assertFails(
    owner.collection("admin_audit_logs").doc("log_1").update({ reason: "Tampered" })
  );
  await assertFails(owner.collection("admin_audit_logs").doc("log_1").delete());
});

test("Private group invite lookup: non-discoverable group join via group_invites without enumeration", async () => {
  const alice = userDb(ALICE_UID);
  const bob = userDb(BOB_UID);
  const now = pastDate(3000);
  const groupId = "grp_private_vault";
  const inviteCode = "KRP-9988AA";

  const batch = alice.batch();
  batch.set(alice.collection("groups").doc(groupId), {
    name: "Zero Trust Vault",
    description: "Private team",
    iconBase64: "",
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    adminUids: [ALICE_UID],
    moderatorUids: [],
    discoverable: false,
    locked: false,
    suspended: false,
    slowModeSeconds: 0,
    inviteCode,
    inviteExpiresAt: futureDate(),
    announcementText: "",
    pinnedMessageId: "",
    mutedByUids: [],
    lastMessagePreview: "Group vault created",
    lastMessageSenderId: ALICE_UID,
    lastMessageAt: now,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(alice.collection("group_invites").doc(inviteCode), {
    inviteCode,
    groupId,
    expiresAt: futureDate(),
    createdBy: ALICE_UID,
    createdAt: now,
  });
  await assertSucceeds(batch.commit());

  // Non-owner cannot list all invites
  await assertFails(bob.collection("group_invites").get());

  // Bob can get the specific invite doc and join the private group
  const inviteDoc = await assertSucceeds(bob.collection("group_invites").doc(inviteCode).get());
  const targetGroupId = inviteDoc.data().groupId;
  await assertSucceeds(
    bob.collection("groups").doc(targetGroupId).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(500),
    })
  );
});

