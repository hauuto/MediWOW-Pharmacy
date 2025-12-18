package com.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MoMo Payment Service for One-Time Payment integration
 * @author MediWOW Pharmacy
 */
public class MoMoPaymentService {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String PARTNER_CODE = dotenv.get("MOMO_PARTNER_CODE");
    private static final String ACCESS_KEY = dotenv.get("MOMO_ACCESS_KEY");
    private static final String SECRET_KEY = dotenv.get("MOMO_SECRET_KEY");
    private static final String API_ENDPOINT = dotenv.get("MOMO_API_ENDPOINT");
    private static final String QUERY_ENDPOINT = dotenv.get("MOMO_QUERY_ENDPOINT");
    private static final String REDIRECT_URL = "https://momo.vn/return";
    private static final String IPN_URL = "https://callback.url/notify";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
    private static final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * Response class for MoMo payment creation
     */
    public static class MoMoPaymentResponse {
        public String partnerCode;
        public String orderId;
        public String requestId;
        public long amount;
        public long responseTime;
        public String message;
        public int resultCode;
        public String payUrl;
        public String qrCodeUrl;
        public String deeplink;
        public String deeplinkMiniApp;
        public String shortLink;

        public boolean isSuccess() {
            return resultCode == 0;
        }

        /**
         * Get the best URL for QR code generation.
         * Priority: deeplink > shortLink > qrCodeUrl > payUrl
         * The deeplink format is what MoMo app expects to scan
         */
        public String getQrContent() {
            // MoMo deeplink is the proper format for QR scanning
            if (deeplink != null && !deeplink.isEmpty()) {
                return deeplink;
            }
            // shortLink is also scannable by MoMo app
            if (shortLink != null && !shortLink.isEmpty()) {
                return shortLink;
            }
            // qrCodeUrl is an image URL, not for QR content
            // payUrl is a web URL, not ideal for QR scanning
            // But we can try payUrl as last resort
            if (payUrl != null && !payUrl.isEmpty()) {
                return payUrl;
            }
            return null;
        }

        /**
         * Check if we have a direct QR code image URL from MoMo
         */
        public boolean hasQrCodeImage() {
            return qrCodeUrl != null && !qrCodeUrl.isEmpty();
        }
    }

    /**
     * Response class for MoMo payment status query
     */
    public static class MoMoQueryResponse {
        public String partnerCode;
        public String orderId;
        public String requestId;
        public String extraData;
        public long amount;
        public long transId;
        public String payType;
        public int resultCode;
        public String message;
        public long responseTime;

        public boolean isPaid() {
            return resultCode == 0;
        }

        public boolean isPending() {
            return resultCode == 1000;
        }
    }

    /**
     * Create a MoMo payment request
     * @param orderId Unique order ID
     * @param amount Payment amount in VND (minimum 1000)
     * @param orderInfo Description of the order
     * @return MoMoPaymentResponse containing QR code URL
     */
    public static MoMoPaymentResponse createPayment(String orderId, long amount, String orderInfo) throws Exception {
        // Ensure minimum amount
        if (amount < 1000) {
            amount = 1000;
        }

        String requestId = UUID.randomUUID().toString();
        String extraData = "";

        // Create raw signature string (parameters must be in alphabetical order)
        String rawSignature = String.format(
            "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
            ACCESS_KEY, amount, extraData, IPN_URL, orderId, orderInfo, PARTNER_CODE, REDIRECT_URL, requestId, "captureWallet"
        );

        System.out.println("=== MoMo Payment Request ===");
        System.out.println("Raw Signature: " + rawSignature);

        // Generate HMAC SHA256 signature
        String signature = hmacSHA256(rawSignature, SECRET_KEY);
        System.out.println("Signature: " + signature);

        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("partnerCode", PARTNER_CODE);
        requestBody.addProperty("partnerName", "MediWOW Pharmacy");
        requestBody.addProperty("storeId", "MediWOWStore");
        requestBody.addProperty("requestId", requestId);
        requestBody.addProperty("amount", amount);
        requestBody.addProperty("orderId", orderId);
        requestBody.addProperty("orderInfo", orderInfo);
        requestBody.addProperty("redirectUrl", REDIRECT_URL);
        requestBody.addProperty("ipnUrl", IPN_URL);
        requestBody.addProperty("lang", "vi");
        requestBody.addProperty("extraData", extraData);
        requestBody.addProperty("requestType", "captureWallet");
        requestBody.addProperty("signature", signature);

        System.out.println("Request Body: " + requestBody);
        System.out.println("API Endpoint: " + API_ENDPOINT);

        // Send HTTP request
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
            .url(API_ENDPOINT)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            System.out.println("Response Code: " + response.code());
            System.out.println("Response Body: " + responseBody);

            if (!response.isSuccessful()) {
                throw new RuntimeException("MoMo API HTTP Error: " + response.code() + " - " + responseBody);
            }

            MoMoPaymentResponse momoResponse = gson.fromJson(responseBody, MoMoPaymentResponse.class);

            System.out.println("=== MoMo Response Parsed ===");
            System.out.println("resultCode: " + momoResponse.resultCode);
            System.out.println("message: " + momoResponse.message);
            System.out.println("payUrl: " + momoResponse.payUrl);
            System.out.println("qrCodeUrl: " + momoResponse.qrCodeUrl);
            System.out.println("deeplink: " + momoResponse.deeplink);
            System.out.println("shortLink: " + momoResponse.shortLink);

            if (!momoResponse.isSuccess()) {
                throw new RuntimeException("MoMo API Error: " + momoResponse.resultCode + " - " + momoResponse.message);
            }

            // Check if we have valid content for QR
            String qrContent = momoResponse.getQrContent();
            if (qrContent == null || qrContent.isEmpty()) {
                throw new RuntimeException("MoMo không trả về thông tin thanh toán. Vui lòng thử lại sau.");
            }

            return momoResponse;
        }
    }

    /**
     * Query payment status
     * @param orderId The order ID to query
     * @return MoMoQueryResponse containing payment status
     */
    public static MoMoQueryResponse queryPaymentStatus(String orderId) throws Exception {
        String requestId = UUID.randomUUID().toString();

        // Create raw signature string for query
        String rawSignature = String.format(
            "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
            ACCESS_KEY, orderId, PARTNER_CODE, requestId
        );

        // Generate HMAC SHA256 signature
        String signature = hmacSHA256(rawSignature, SECRET_KEY);

        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("partnerCode", PARTNER_CODE);
        requestBody.addProperty("requestId", requestId);
        requestBody.addProperty("orderId", orderId);
        requestBody.addProperty("signature", signature);
        requestBody.addProperty("lang", "vi");

        // Send HTTP request
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
            .url(QUERY_ENDPOINT)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new RuntimeException("MoMo Query API HTTP Error: " + response.code() + " - " + responseBody);
            }

            return gson.fromJson(responseBody, MoMoQueryResponse.class);
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private static String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Generate a unique order ID
     */
    public static String generateOrderId() {
        return "MOMO" + System.currentTimeMillis();
    }
}
