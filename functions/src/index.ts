import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

const METHODOLOGY_VERSION = "Skitefi-v1.0";

/**
 * Atomic reservation of unique user nickname
 */
export const reserveNickname = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Требуется авторизация");
  }

  const uid = context.auth.uid;
  const rawNickname = data?.nickname;

  if (typeof rawNickname !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "Неверный формат никнейма");
  }

  const nickname = rawNickname.trim();
  const normalized = nickname.toLowerCase();

  if (normalized.length < 3 || normalized.length > 20) {
    throw new functions.https.HttpsError("invalid-argument", "Длина никнейма должна быть от 3 до 20 символов");
  }

  const regex = /^[a-zA-Z0-9_\u0400-\u04FF]+$/;
  if (!regex.test(nickname)) {
    throw new functions.https.HttpsError("invalid-argument", "Никнейм содержит недопустимые символы");
  }

  const reservationRef = db.collection("nicknameReservations").document(normalized);
  const profileRef = db.collection("publicProfiles").document(uid);

  return await db.runTransaction(async (transaction) => {
    const reservationDoc = await transaction.get(reservationRef);
    if (reservationDoc.exists) {
      const existingOwner = reservationDoc.data()?.uid;
      if (existingOwner !== uid) {
        throw new functions.https.HttpsError("already-exists", `Никнейм '${nickname}' уже занят`);
      }
    }

    transaction.set(reservationRef, {
      uid: uid,
      nickname: nickname,
      reservedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    transaction.set(profileRef, {
      uid: uid,
      nickname: nickname,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    return { success: true, nickname };
  });
});

/**
 * Server verification and publication of benchmark runs
 */
export const submitBenchmarkRun = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Требуется авторизация");
  }

  const uid = context.auth.uid;
  const runId = data?.runId;
  const category = data?.category;
  const score = Number(data?.score);
  const methodology = data?.methodologyVersion;
  const deviceModel = data?.deviceModel || "Android Device";

  if (!runId || !category || isNaN(score) || score <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Некорректные параметры запуска");
  }

  if (methodology !== METHODOLOGY_VERSION) {
    throw new functions.https.HttpsError("failed-precondition", `Несовместимая методика: ожидалась ${METHODOLOGY_VERSION}`);
  }

  // Rate limiting check: max 1 submission every 10 seconds per user
  const recentRuns = await db.collection("benchmarkRuns")
    .where("uid", "==", uid)
    .orderBy("timestamp", "desc")
    .limit(1)
    .get();

  if (!recentRuns.empty) {
    const lastTimestamp = recentRuns.docs[0].data()?.timestamp;
    if (lastTimestamp && Date.now() - lastTimestamp < 5000) {
      throw new functions.https.HttpsError("resource-exhausted", "Слишком частые отправки. Подождите несколько секунд.");
    }
  }

  // Fetch nickname
  const profileDoc = await db.collection("publicProfiles").document(uid).get();
  const nickname = profileDoc.data()?.nickname || "User";

  // Plausibility check
  const isPlausible = score < 50000;

  const runRef = db.collection("benchmarkRuns").document(runId);
  await runRef.set({
    runId,
    uid,
    nickname,
    category,
    score,
    deviceModel,
    methodologyVersion: methodology,
    timestamp: Date.now(),
    serverVerified: isPlausible,
  });

  if (isPlausible) {
    const leaderboardRef = db.collection("leaderboardEntries").document(runId);
    await leaderboardRef.set({
      id: runId,
      uid,
      nickname,
      category,
      score,
      deviceModel,
      methodologyVersion: methodology,
      timestamp: Date.now(),
      verified: true,
    });
  }

  return { success: true, verified: isPlausible };
});
