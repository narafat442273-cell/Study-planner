const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

function validProfile(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "Arafat Rahman",
    email: "arafat@example.com",
    educationLevel: "university",
    categoryDetail: "CSE 3rd Year",
    photoUri: "",
    dailyGoalMinutes: 180,
    weeklyGoalMinutes: 1260,
    personalGoalNote: "Complete Data Structures & Algorithms",
    language: "bn",
    themeMode: "system",
    accentTheme: "indigo",
    createdAt: now,
    updatedAt: now,
  };
}

function validSubject(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Mathematics",
    colorHex: 0xFF2563EB,
    priority: "high",
    chapters: ["Calculus", "Linear Algebra"],
    completedChapters: ["Calculus"],
    examDateMillis: 1799000000000,
    progressPercent: 50,
    createdAt: now,
    updatedAt: now,
  };
}

function validRoutine(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    subjectId: "sub_1",
    subjectName: "Physics",
    colorHex: 0xFF10B981,
    dateMillis: 1794600000000,
    startTimeMinutes: 480,
    endTimeMinutes: 540,
    durationMinutes: 60,
    breakMinutes: 10,
    repeatMode: "daily",
    repeatDays: [1, 2, 3],
    notes: "Review Optics chapters",
    sortOrder: 0,
    isCompleted: false,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write user profile or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("subjects").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("routines").doc("r1").set(validRoutine(ALICE_UID)));
});

test("Authenticated owner can create and update own profile and subjects", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));

  const subjectRef = profileRef.collection("subjects").doc("sub_1");
  await assertSucceeds(subjectRef.set(validSubject(ALICE_UID)));
  await assertSucceeds(subjectRef.update({ progressPercent: 75, updatedAt: new Date(Date.now() - 1000) }));
  await assertSucceeds(profileRef.collection("subjects").get());
});

test("Cross-user access is strictly denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("routines").doc("r1").set(validRoutine(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("routines").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("routines").doc("r1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("routines").doc("r2").set(validRoutine(BOB_UID)));
});

test("Shadow update with ghost field or immutable createdAt tamper is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));

  await assertFails(profileRef.update({ isAdmin: true, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(profileRef.update({ createdAt: new Date(Date.now() - 500), updatedAt: new Date(Date.now() - 500) }));
});

test("Invalid routine time range or subject name fails validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badSub = validSubject(ALICE_UID);
  badSub.name = "";
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("bad_sub").set(badSub));

  const badRoutine = validRoutine(ALICE_UID);
  badRoutine.durationMinutes = 1;
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("routines").doc("bad_r").set(badRoutine));
});
