const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const RAHUL_UID = "rahul_123";
const PRIYA_UID = "priya_456";
const AMAN_UID = "aman_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read users or posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("posts").get());
});

test("Authenticated user: can create own profile, cannot spoof another user's profile", async () => {
  const rahulDb = testEnv.authenticatedContext(RAHUL_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    rahulDb.collection("users").doc(RAHUL_UID).set({
      userId: RAHUL_UID,
      name: "Rahul",
      username: "rahul_insta",
      bio: "Namaste",
      isOnline: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(
    rahulDb.collection("users").doc(PRIYA_UID).set({
      userId: PRIYA_UID,
      name: "Priya",
      username: "priya_insta",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Shadow update test: ghost field rejected on user profile", async () => {
  const past = new Date(Date.now() - 2000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(RAHUL_UID).set({
      userId: RAHUL_UID,
      name: "Rahul",
      username: "rahul_insta",
      createdAt: past,
      updatedAt: past,
    });
  });

  const rahulDb = testEnv.authenticatedContext(RAHUL_UID).firestore();
  await assertFails(
    rahulDb.collection("users").doc(RAHUL_UID).update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Notifications: only recipient can read notifications", async () => {
  const past = new Date(Date.now() - 2000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("notifications").doc("notif_1").set({
      recipientId: PRIYA_UID,
      actorId: RAHUL_UID,
      actorUsername: "rahul_insta",
      type: "follow",
      message: "Rahul ne tumhe Follow kiya",
      isRead: false,
      createdAt: past,
    });
  });

  const priyaDb = testEnv.authenticatedContext(PRIYA_UID).firestore();
  const amanDb = testEnv.authenticatedContext(AMAN_UID).firestore();

  await assertSucceeds(priyaDb.collection("notifications").where("recipientId", "==", PRIYA_UID).get());
  await assertFails(amanDb.collection("notifications").doc("notif_1").get());
  await assertFails(amanDb.collection("notifications").where("recipientId", "==", PRIYA_UID).get());
});

test("Chats: only participants can read or send messages", async () => {
  const past = new Date(Date.now() - 2000);
  const chatId = `${PRIYA_UID}_${RAHUL_UID}`;
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("chats").doc(chatId).set({
      participantIds: [PRIYA_UID, RAHUL_UID],
      lastMessage: "Namaste!",
      lastSenderId: RAHUL_UID,
      createdAt: past,
      updatedAt: past,
    });
  });

  const rahulDb = testEnv.authenticatedContext(RAHUL_UID).firestore();
  const amanDb = testEnv.authenticatedContext(AMAN_UID).firestore();

  await assertSucceeds(rahulDb.collection("chats").doc(chatId).get());
  await assertFails(amanDb.collection("chats").doc(chatId).get());
});
