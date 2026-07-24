package com.p99soft.deskflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Role Verification", description = "Endpoints for testing role-based access controls")
public class DemoController {

    @GetMapping("/admin/dashboard")
    @Operation(summary = "Admin dashboard", description = "Access restricted to users with the ADMIN role.")
    public ResponseEntity<String> getAdminDashboard() {
        return ResponseEntity.ok("Welcome to the Admin Dashboard. Accessible only by ADMIN.");
    }

    @GetMapping("/agent/dashboard")
    @Operation(summary = "Agent dashboard", description = "Access restricted to users with the AGENT or ADMIN roles.")
    public ResponseEntity<String> getAgentDashboard() {
        return ResponseEntity.ok("Welcome to the Agent Dashboard. Accessible only by AGENT or ADMIN.");
    }

    @GetMapping("/employee/dashboard")
    @Operation(summary = "Employee dashboard", description = "Access restricted to users with the EMPLOYEE or ADMIN roles.")
    public ResponseEntity<String> getEmployeeDashboard() {
        return ResponseEntity.ok("Welcome to the Employee Dashboard. Accessible only by EMPLOYEE or ADMIN.");
    }
}
