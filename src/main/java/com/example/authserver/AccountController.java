package com.example.authserver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {
    @PostMapping("/transfer")
    public String transferMoney(@RequestParam String account, @RequestParam double amount, HttpServletRequest request) {
        return "Successfully transferred $" + amount + " to " + account;
    }
}