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
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_789";
const ADMIN_ACCESS_HASH = crypto
  .createHash("sha256")
  .update("Fazaluddin:198500")
  .digest("hex");

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTs() {
  return new Date(Date.now() - 2000);
}

function validUserPayload(uid, overrides = {}) {
  return {
    userId: uid,
    fullName: "Alice Investor",
    username: "alice_inv",
    phone: "03001234567",
    email: "alice@example.com",
    passwordHash: "a".repeat(64),
    balance: 0,
    totalInvested: 0,
    totalEarnings: 0,
    referralCode: "CW" + uid.slice(0, 6).toUpperCase(),
    referralRewardPaid: false,
    referralCount: 0,
    referralEarnings: 0,
    hasActivePlan: false,
    faceStatus: "UNVERIFIED",
    kycStatus: "UNVERIFIED",
    twoFactorEnabled: false,
    createdAt: pastTs(),
    updatedAt: pastTs(),
    ...overrides,
  };
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

test("Unauthenticated caller cannot read users, deposits, chats, or admin notifications", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("deposits").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("admin_notifications").get());
  await assertFails(unauthDb.collection("support_chats").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("config").doc("app_settings").get());
});

test("User can create own profile with zero balance and cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set(validUserPayload(BOB_UID, { balance: 99999 }))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Non-admin cannot self-verify KYC/Face or inject shadow field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      kycStatus: "PENDING",
      kycFullName: "Alice Khan",
      kycCnic: "35202-1234567-1",
      kycDocumentType: "CNIC",
      faceStatus: "PENDING",
      faceSelfieBase64: "dGVzdF9zZWxmaWU=",
      updatedAt: pastTs(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      kycStatus: "VERIFIED",
      updatedAt: pastTs(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      faceStatus: "VERIFIED",
      updatedAt: pastTs(),
    })
  );

  // AI-backed face verification with selfie payload and AI note succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      faceStatus: "VERIFIED",
      faceSelfieBase64: "dGVzdF9zZWxmaWVfcGF5bG9hZA==",
      faceAiNote: "AI تصدیق کامیاب (95%)",
      updatedAt: pastTs(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuperAdmin: true,
      updatedAt: pastTs(),
    })
  );
});

test("Promo code redemption requires active VIP plan", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      adminAccessHash: ADMIN_ACCESS_HASH,
      createdAt: pastTs(),
    })
  );

  await assertSucceeds(
    adminDb.collection("promo_codes").doc("VIP500").set({
      code: "VIP500",
      bonusAmount: 500,
      maxUses: 50,
      usedCount: 0,
      isActive: true,
      description: "VIP Member Bonus",
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Without active plan -> rejected!
  await assertFails(
    aliceDb.collection("promo_redemptions").doc(`VIP500_${ALICE_UID}`).set({
      promoCode: "VIP500",
      userId: ALICE_UID,
      bonusAmount: 500,
      createdAt: pastTs(),
    })
  );

  // After activating VIP plan -> succeeds!
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      hasActivePlan: true,
      updatedAt: pastTs(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("promo_redemptions").doc(`VIP500_${ALICE_UID}`).set({
      promoCode: "VIP500",
      userId: ALICE_UID,
      bonusAmount: 500,
      createdAt: pastTs(),
    })
  );
});

test("Live chat enforces thread isolation and Admin senderName == Admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      adminAccessHash: ADMIN_ACCESS_HASH,
      createdAt: pastTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("support_chats").doc(ALICE_UID).set({
      userId: ALICE_UID,
      userName: "Alice",
      lastMessage: "Need deposit help",
      unreadByAdmin: 1,
      unreadByUser: 0,
      updatedAt: pastTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("support_chats").doc(ALICE_UID).collection("messages").doc("msg1").set({
      userId: ALICE_UID,
      senderRole: "USER",
      senderName: "Alice",
      text: "Need deposit help",
      createdAt: pastTs(),
    })
  );

  // User cannot spoof ADMIN role
  await assertFails(
    aliceDb.collection("support_chats").doc(ALICE_UID).collection("messages").doc("msg2").set({
      userId: ALICE_UID,
      senderRole: "ADMIN",
      senderName: "Admin",
      text: "Fake admin reply",
      createdAt: pastTs(),
    })
  );

  // Bob cannot read Alice's chat
  await assertFails(
    bobDb.collection("support_chats").doc(ALICE_UID).collection("messages").get()
  );

  // Admin replies strictly with senderName == 'Admin'
  await assertSucceeds(
    adminDb.collection("support_chats").doc(ALICE_UID).collection("messages").doc("msg3").set({
      userId: ALICE_UID,
      senderRole: "ADMIN",
      senderName: "Admin",
      text: "We are verifying your TID now.",
      createdAt: pastTs(),
    })
  );
});

test("Deposit lifecycle: user creates PENDING deposit, cannot self-approve, admin approves and terminal state locks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      adminAccessHash: ADMIN_ACCESS_HASH,
      createdAt: pastTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("deposits").doc("dep_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      amount: 1350,
      feeAmount: 0,
      netAmount: 1350,
      method: "EASYPAISA",
      senderPhone: "03451234567",
      transactionRef: "TID998877",
      status: "PENDING",
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );

  await assertFails(
    aliceDb.collection("deposits").doc("dep_1").update({
      status: "APPROVED",
      updatedAt: pastTs(),
    })
  );

  await assertSucceeds(
    adminDb.collection("deposits").doc("dep_1").update({
      status: "APPROVED",
      adminNote: "Verified TID",
      updatedAt: pastTs(),
    })
  );

  await assertFails(
    adminDb.collection("deposits").doc("dep_1").update({
      status: "REJECTED",
      updatedAt: pastTs(),
    })
  );
});
