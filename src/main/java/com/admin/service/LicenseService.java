package com.admin.service;

import com.admin.entity.License;
import com.admin.repository.LicenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LicenseService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int KEY_LENGTH = 12;
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private LicenseRepository licenseRepository;

    /**
     * Generates a unique 12-character uppercase alphanumeric license key.
     */
    public String generateLicenseKey() {
        String key;
        do {
            StringBuilder sb = new StringBuilder(KEY_LENGTH);
            for (int i = 0; i < KEY_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            key = sb.toString();
        } while (licenseRepository.findByLicenseKey(key).isPresent());
        return key;
    }

    /**
     * Creates a new license with an auto-generated key.
     */
    public License createLicense(String userName, LocalDate expiryDate) {
        License license = new License();
        license.setLicenseKey(generateLicenseKey());
        license.setUserName(userName);
        license.setExpiryDate(expiryDate);
        license.setActive(true);
        return licenseRepository.save(license);
    }

    /**
     * Validates a license key from the Android app.
     * Binds androidId on first login. Returns detailed status messages.
     */
    public Map<String, Object> validateLicense(String licenseKey, String androidId, String userName) {
        Map<String, Object> response = new HashMap<>();
        Optional<License> optLicense = licenseRepository.findByLicenseKey(licenseKey);

        if (optLicense.isEmpty()) {
            response.put("success", false);
            response.put("status", "INVALID_KEY");
            response.put("message", "This license key does not exist. Please check the key and try again.");
            response.put("action", "Please purchase a valid license key to use the app.");
            return response;
        }

        License license = optLicense.get();

        // Check if deactivated by admin
        if (!license.isActive()) {
            response.put("success", false);
            response.put("status", "DEACTIVATED");
            response.put("message", "Your license has been deactivated by the administrator.");
            response.put("action", "Please contact support to reactivate your license.");
            return response;
        }

        // Check expiry
        if (license.getExpiryDate().isBefore(LocalDate.now())) {
            response.put("success", false);
            response.put("status", "EXPIRED");
            response.put("message", "Your license expired on " + license.getExpiryDate() + ".");
            response.put("action", "Please purchase a new license key to continue using the app.");
            response.put("expiredOn", license.getExpiryDate().toString());
            return response;
        }

        // Check if android ID is already bound to a different device
        if (license.getAndroidId() != null && !license.getAndroidId().isEmpty()
                && !license.getAndroidId().equals(androidId)) {
            response.put("success", false);
            response.put("status", "DEVICE_MISMATCH");
            response.put("message", "This license key is already registered on another device.");
            response.put("action",
                    "Each license can only be used on one device. Please buy a new key for this device.");
            return response;
        }

        // Bind android ID on first login
        if (license.getAndroidId() == null || license.getAndroidId().isEmpty()) {
            license.setAndroidId(androidId);
        }
        if (userName != null && !userName.isEmpty()) {
            license.setUserName(userName);
        }
        licenseRepository.save(license);

        response.put("success", true);
        response.put("status", "VALID");
        response.put("message", "Welcome! Your license is active.");
        response.put("expiryDate", license.getExpiryDate().toString());
        response.put("userName", license.getUserName());
        response.put("daysRemaining",
                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), license.getExpiryDate()));
        return response;
    }

    /**
     * Quick validation check (used by app on startup).
     */
    public Map<String, Object> checkValidity(String licenseKey) {
        Map<String, Object> response = new HashMap<>();
        Optional<License> optLicense = licenseRepository.findByLicenseKey(licenseKey);

        if (optLicense.isEmpty()) {
            response.put("success", false);
            response.put("status", "INVALID_KEY");
            response.put("message", "This license key does not exist.");
            response.put("action", "Please purchase a valid license key.");
            return response;
        }

        License license = optLicense.get();

        if (!license.isActive()) {
            response.put("success", false);
            response.put("status", "DEACTIVATED");
            response.put("message", "Your license has been deactivated.");
            response.put("action", "Please contact support to reactivate your license.");
            return response;
        }

        if (license.getExpiryDate().isBefore(LocalDate.now())) {
            response.put("success", false);
            response.put("status", "EXPIRED");
            response.put("message", "Your license expired on " + license.getExpiryDate() + ".");
            response.put("action", "Please purchase a new license key.");
            return response;
        }

        response.put("success", true);
        response.put("status", "VALID");
        response.put("message", "License is active and valid.");
        response.put("expiryDate", license.getExpiryDate().toString());
        response.put("userName", license.getUserName());
        response.put("daysRemaining",
                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), license.getExpiryDate()));
        return response;
    }

    /**
     * Secure validation with device binding check.
     * Verifies the androidId matches the one registered to this license.
     * Used by the new POST /api/validate endpoint.
     */
    public Map<String, Object> checkValidityWithDevice(String licenseKey, String androidId) {
        Map<String, Object> response = new HashMap<>();
        Optional<License> optLicense = licenseRepository.findByLicenseKey(licenseKey);

        if (optLicense.isEmpty()) {
            response.put("success", false);
            response.put("status", "INVALID_KEY");
            response.put("message", "This license key does not exist.");
            return response;
        }

        License license = optLicense.get();

        if (!license.isActive()) {
            response.put("success", false);
            response.put("status", "DEACTIVATED");
            response.put("message", "Your license has been deactivated.");
            return response;
        }

        if (license.getExpiryDate().isBefore(LocalDate.now())) {
            response.put("success", false);
            response.put("status", "EXPIRED");
            response.put("message", "Your license expired on " + license.getExpiryDate() + ".");
            return response;
        }

        // Verify device binding — the androidId must match
        if (license.getAndroidId() != null && !license.getAndroidId().isEmpty()
                && !license.getAndroidId().equals(androidId)) {
            response.put("success", false);
            response.put("status", "DEVICE_MISMATCH");
            response.put("message", "This license is registered to a different device.");
            return response;
        }

        response.put("success", true);
        response.put("status", "VALID");
        response.put("message", "License is active and valid.");
        response.put("expiryDate", license.getExpiryDate().toString());
        response.put("userName", license.getUserName());
        response.put("daysRemaining",
                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), license.getExpiryDate()));
        return response;
    }

    public List<License> getAllLicenses() {
        return licenseRepository.findAll();
    }

    public Optional<License> getLicenseById(Long id) {
        return licenseRepository.findById(id);
    }

    public License updateLicense(Long id, String androidId, String userName,
            LocalDate expiryDate, boolean isActive) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("License not found with id: " + id));

        if (androidId != null)
            license.setAndroidId(androidId);
        if (userName != null)
            license.setUserName(userName);
        if (expiryDate != null)
            license.setExpiryDate(expiryDate);
        license.setActive(isActive);

        return licenseRepository.save(license);
    }

    public void deleteLicense(Long id) {
        licenseRepository.deleteById(id);
    }
}
