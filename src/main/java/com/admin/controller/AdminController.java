package com.admin.controller;

import com.admin.entity.License;
import com.admin.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private LicenseService licenseService;

    /**
     * Dashboard — shows all licenses in a table.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<License> licenses = licenseService.getAllLicenses();
        model.addAttribute("licenses", licenses);

        // Compute stats in Java (SpEL doesn't support lambdas)
        long totalCount = licenses.size();
        long activeCount = 0;
        long inactiveCount = 0;
        long expiredCount = 0;
        LocalDate today = LocalDate.now();

        for (License l : licenses) {
            if (!l.isActive()) {
                inactiveCount++;
            } else if (l.getExpiryDate().isBefore(today)) {
                expiredCount++;
            } else {
                activeCount++;
            }
        }

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("today", today);

        return "dashboard";
    }

    /**
     * Show form to create a new license.
     */
    @GetMapping("/license/new")
    public String newLicenseForm(Model model) {
        model.addAttribute("license", new License());
        model.addAttribute("isNew", true);
        return "license-form";
    }

    /**
     * Save a new or updated license.
     */
    @PostMapping("/license/save")
    public String saveLicense(@RequestParam(required = false) Long id,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String androidId,
            @RequestParam(required = false, defaultValue = "true") boolean isActive) {
        LocalDate expiry = LocalDate.parse(expiryDate);

        if (id == null) {
            // Creating new license
            licenseService.createLicense(userName, expiry);
        } else {
            // Updating existing license
            licenseService.updateLicense(id, androidId, userName, expiry, isActive);
        }
        return "redirect:/admin/dashboard";
    }

    /**
     * Show edit form for an existing license.
     */
    @GetMapping("/license/edit/{id}")
    public String editLicenseForm(@PathVariable Long id, Model model) {
        License license = licenseService.getLicenseById(id)
                .orElseThrow(() -> new RuntimeException("License not found"));
        model.addAttribute("license", license);
        model.addAttribute("isNew", false);
        return "license-form";
    }

    /**
     * Toggle a license active/inactive.
     */
    @GetMapping("/license/toggle/{id}")
    public String toggleLicense(@PathVariable Long id) {
        License license = licenseService.getLicenseById(id)
                .orElseThrow(() -> new RuntimeException("License not found"));
        licenseService.updateLicense(id, license.getAndroidId(), license.getUserName(),
                license.getExpiryDate(), !license.isActive());
        return "redirect:/admin/dashboard";
    }

    /**
     * Delete a license.
     */
    @GetMapping("/license/delete/{id}")
    public String deleteLicense(@PathVariable Long id) {
        licenseService.deleteLicense(id);
        return "redirect:/admin/dashboard";
    }
}
