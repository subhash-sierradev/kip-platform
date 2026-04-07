package com.integration.management.controller;

import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.integration.management.constants.ManagementSecurityConstants.X_TENANT_ID;

@Slf4j
@RestController
@RequestMapping("/api/management/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getUsers(
            @RequestAttribute(X_TENANT_ID) String tenantId) {
        log.info("Fetching all users for tenant: {}", tenantId);
        return ResponseEntity.ok(userProfileService.getAllUsersByTenant(tenantId));
    }
}
