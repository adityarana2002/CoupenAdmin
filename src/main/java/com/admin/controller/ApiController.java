package com.admin.controller;

import com.admin.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private LicenseService licenseService;

    @Value("${app.hmac-secret}")
    private String hmacSecret;

    // Maximum allowed time difference for signed requests (5 minutes)
    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L;

    /**
     * Login endpoint for Android app.
     * Accepts: { "licenseKey": "...", "androidId": "...", "userName": "..." }
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String licenseKey = request.get("licenseKey");
        String androidId = request.get("androidId");
        String userName = request.get("userName");
        return licenseService.validateLicense(licenseKey, androidId, userName);
    }

    /**
     * Secure validation endpoint — POST with HMAC signature.
     * Accepts: { "licenseKey": "...", "androidId": "...", "timestamp": "...", "signature": "..." }
     *
     * Validates:
     * 1. HMAC signature matches
     * 2. Timestamp is within 5 minutes (prevents replay attacks)
     * 3. License key is valid and belongs to the requesting device
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, String> request) {
        String licenseKey = request.get("licenseKey");
        String androidId = request.get("androidId");
        String timestamp = request.get("timestamp");
        String signature = request.get("signature");

        // Validate required fields
        if (licenseKey == null || androidId == null || timestamp == null || signature == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("status", "BAD_REQUEST");
            error.put("message", "Missing required fields.");
            return ResponseEntity.badRequest().body(error);
        }

        // ── Layer 3: HMAC Signature Verification ──
        try {
            // Check timestamp freshness (prevent replay attacks)
            long requestTimestamp = Long.parseLong(timestamp);
            long timeDiff = Math.abs(System.currentTimeMillis() - requestTimestamp);
            if (timeDiff > MAX_TIMESTAMP_DIFF_MS) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("status", "EXPIRED_REQUEST");
                error.put("message", "Request has expired. Please try again.");
                return ResponseEntity.status(403).body(error);
            }

            // Verify HMAC signature
            String dataToSign = licenseKey + androidId + timestamp;
            String expectedSignature = generateHmacSignature(dataToSign, hmacSecret);

            if (!expectedSignature.equals(signature)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("status", "INVALID_SIGNATURE");
                error.put("message", "Request authentication failed.");
                return ResponseEntity.status(403).body(error);
            }
        } catch (NumberFormatException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("status", "BAD_REQUEST");
            error.put("message", "Invalid timestamp format.");
            return ResponseEntity.badRequest().body(error);
        }

        // Signature valid — check the license with androidId verification
        Map<String, Object> result = licenseService.checkValidityWithDevice(licenseKey, androidId);
        return ResponseEntity.ok(result);
    }

    /**
     * Legacy GET validate endpoint — kept for backward compatibility but now
     * returns a deprecation notice. Remove after all clients are updated.
     */
    @GetMapping("/validate/{licenseKey}")
    public Map<String, Object> validateLegacy(@PathVariable String licenseKey) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("status", "DEPRECATED");
        response.put("message", "This endpoint is deprecated. Please update your app.");
        return response;
    }

    /**
     * Generate HMAC-SHA256 signature (server-side verification).
     */
    private String generateHmacSignature(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey =
                    new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }
}
