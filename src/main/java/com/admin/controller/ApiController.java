package com.admin.controller;

import com.admin.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private LicenseService licenseService;

    /**
     * Login endpoint for Android app.
     * Accepts: { "licenseKey": "...", "androidId": "...", "userName": "..." }
     * Returns: { "success": true/false, "message": "...", "expiryDate": "...",
     * "userName": "..." }
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String licenseKey = request.get("licenseKey");
        String androidId = request.get("androidId");
        String userName = request.get("userName");
        return licenseService.validateLicense(licenseKey, androidId, userName);
    }

    /**
     * Quick validation endpoint — app calls this on startup.
     * Returns: { "success": true/false, "message": "..." }
     */
    @GetMapping("/validate/{licenseKey}")
    public Map<String, Object> validate(@PathVariable String licenseKey) {
        return licenseService.checkValidity(licenseKey);
    }
}
