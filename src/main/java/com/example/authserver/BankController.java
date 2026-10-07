package com.example.authserver;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class BankController {

    @GetMapping("/transfer")
    public String showTransferPage() {
        return "transfer";
    }

    @PostMapping("/transfer")
    @ResponseBody
    public String processTransfer(@RequestParam String account, @RequestParam double amount) {
        return "<h3>Transaction Successful!</h3><p>Successfully transferred $" + amount + " to account " + account + "</p>";
    }
}
