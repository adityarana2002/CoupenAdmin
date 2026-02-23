package com.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String adminLoginPage() {
        return "login";
    }

    @GetMapping("/user/login")
    public String userLoginPage() {
        return "user-login";
    }
}
