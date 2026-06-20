package com.veritas.backend.auth.controller;

import com.veritas.backend.config.annotations.IsAdministrator;
import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.config.annotations.IsProcurementOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-security")
public class DummySecurityController {

    @GetMapping("/admin")
    @IsAdministrator
    public String adminOnly() {
        return "OK";
    }

    @GetMapping("/finance")
    @IsFinanceOfficer
    public String financeOnly() {
        return "OK";
    }

    @GetMapping("/procurement")
    @IsProcurementOfficer
    public String procurementOnly() {
        return "OK";
    }

    @GetMapping("/requester")
    @IsRequester
    public String requesterOnly() {
        return "OK";
    }
}
