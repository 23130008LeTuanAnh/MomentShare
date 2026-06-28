const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * Người 5 thực hiện: gửi push notification thật bằng Firebase Cloud Messaging.
 *
 * Hàm này chạy khi app tạo document mới trong collection notifications.
 * Luồng xử lý:
 * 1. Đọc notification vừa được tạo.
 * 2. Lấy userId người nhận.
 * 3. Đọc fcmToken trong users/{userId}.
 * 4. Gửi FCM message đến đúng thiết bị của người nhận.
 */
exports.sendPushNotificationOnNotificationCreate = onDocumentCreated(
  "notifications/{notificationId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      return null;
    }

    const notification = snapshot.data();
    const notificationId = event.params.notificationId;
    const userId = notification.userId;

    if (!userId) {
      console.log("Missing userId in notification", notificationId);
      return null;
    }

    const userSnapshot = await getFirestore().collection("users").doc(userId).get();
    if (!userSnapshot.exists) {
      console.log("User not found", userId);
      return null;
    }

    const fcmToken = userSnapshot.get("fcmToken");
    if (!fcmToken) {
      console.log("User has no FCM token", userId);
      return null;
    }

    const title = notification.title || "MomentShare";
    const body = notification.message || "Bạn có thông báo mới.";
    const type = notification.type || "general";

    const message = {
      token: fcmToken,
      notification: {
        title,
        body
      },
      data: {
        notificationId,
        type,
        targetUserId: userId
      }
    };

    return getMessaging().send(message);
  }
);