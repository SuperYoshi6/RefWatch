const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

// Set global options for all v2 functions in this file
setGlobalOptions({maxInstances: 10});

admin.initializeApp();

/**
 * Generates a localized, branded password reset link for the given email
 * and language. The link is returned to the caller, which can use it for
 * testing, custom email delivery, or analytics.
 *
 * Firebase Auth's built-in `sendPasswordResetEmail` (called from the
 * client) only sends the default Firebase template. If you want a fully
 * custom HTML email, pair this function with a transactional email
 * service such as SendGrid, Mailgun, or the "Trigger Email" Firebase
 * extension – then send the returned `link` from that service.
 *
 * The `continueUrl` is the RefWatch website so the reset link looks
 * legitimate to spam filters (branded domain + recognizable path).
 */
const PASSWORD_RESET_CONTINUE_URL =
  "https://superyoshi6.github.io/RefWatch/auth/action?mode=resetPassword";

exports.requestPasswordReset = onCall(async (request) => {
  const email = request.data?.email;
  const languageCode = request.data?.languageCode || "en";

  if (typeof email !== "string" || email.trim() === "") {
    throw new HttpsError(
        "invalid-argument",
        "A valid email address is required.",
    );
  }

  const actionCodeSettings = {
    url: PASSWORD_RESET_CONTINUE_URL,
    handleCodeInApp: false,
  };

  try {
    const link = await admin
        .auth()
        .generatePasswordResetLink(email, actionCodeSettings, languageCode);
    console.log(
        `Password reset link generated for ${email} (${languageCode})`,
    );
    return {success: true, link: link};
  } catch (error) {
    console.error(
        `Failed to generate password reset link for ${email} (${languageCode})`,
        error,
    );
    if (error.code === "auth/user-not-found") {
      // Don't leak which emails exist; return success anyway so the client
      // doesn't reveal account presence.
      return {success: true, link: null};
    }
    if (error.code === "auth/invalid-email") {
      throw new HttpsError("invalid-argument", "The email address is invalid.");
    }
    throw new HttpsError("internal", error.message);
  }
});

exports.linkWatch = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }

  const code = request.data?.code;
  if (typeof code !== "string" || code.length !== 6) {
    throw new HttpsError(
        "invalid-argument",
        "A valid 6-digit code is required.",
    );
  }

  const uid = request.auth.uid;
  const pairingDocRef = admin.firestore().collection("pairing_codes").doc(code);
  const pairingDoc = await pairingDocRef.get();

  if (!pairingDoc.exists) {
    throw new HttpsError("not-found", "Code ungültig oder abgelaufen.");
  }

  const {watchId, status} = pairingDoc.data();
  if (status !== "pending") {
    throw new HttpsError("failed-precondition", "Code bereits verwendet.");
  }

  try {
    // 1. Link watch to user
    await admin.firestore().collection("users").doc(uid)
        .collection("devices").doc(watchId).set({
          linkedAt: admin.firestore.FieldValue.serverTimestamp(),
          type: "wear_os_watch",
          active: true,
        });

    // 2. Generate custom token for the watch
    const customToken = await admin.auth().createCustomToken(uid);

    // 3. Send token to watch via the pairing document
    await pairingDocRef.update({
      status: "success",
      customToken: customToken,
      linkedTo: uid,
    });

    return {success: true};
  } catch (error) {
    console.error(`Error linking watch for UID: ${uid}`, error);
    throw new HttpsError("internal", "Verknüpfung fehlgeschlagen.");
  }
});

exports.generateCustomToken = onCall(async (request) => {
  if (!request.auth) {
    // Use the imported HttpsError directly
    throw new HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }
  const uid = request.auth.uid;
  //  admin.auth()
  //      .createCustomToken(uid)
  //      .then((customToken) => {
  //        console.log("Custom token with claims:", customToken);
  //        return {"customToken": customToken}; // !!! this returns null
  //      })
  //      .catch((error) => {
  //        console.log("Error creating custom token:", error);
  //      });
  try {
    const customToken = await admin.auth().createCustomToken(uid);
    console.log(`Successfully created custom token for UID: ${uid}`);
    return {customToken: customToken};
  } catch (error) {
    console.error(`Error creating custom token for UID: ${uid}`, error);
    // Use the imported HttpsError directly
    throw new HttpsError(
        "internal",
        "Unable to create custom token.",
        error.message,
    );
  }
});
