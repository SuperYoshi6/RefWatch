const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

// Set global options for all v2 functions in this file
setGlobalOptions({maxInstances: 10});

admin.initializeApp();

exports.generateCustomToken = onCall(async (request) => {
  if (!request.auth) {
    // Use the imported HttpsError directly
    throw new HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }
  const uid = request.auth.uid;
  admin.auth()
      .createCustomToken(uid)
      .then((customToken) => {
        // Send token back to client
      })
      .catch((error) => {
        console.log("Error creating custom token:", error);
      });
//  try {
//    const customToken = await admin.auth().createCustomToken(uid);
//    console.log(`Successfully created custom token for UID: ${uid}`);
//    return {customToken: customToken};
//  } catch (error) {
//    console.error(`Error creating custom token for UID: ${uid}`, error);
//    // Use the imported HttpsError directly
//    throw new HttpsError(
//        "internal",
//        "Unable to create custom token.",
//        error.message,
//    );
//  }
});
