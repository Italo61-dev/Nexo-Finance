package com.nexofinance.backend.domain.admin.controller;

import com.nexofinance.backend.domain.admin.AdminUserService;
import com.nexofinance.backend.domain.admin.dto.AdminUserSummaryResponseDTO;
import com.nexofinance.backend.domain.admin.dto.PlatformMetricsResponseDTO;
import com.nexofinance.backend.domain.admin.dto.UpdateUserRoleRequestDTO;
import com.nexofinance.backend.domain.admin.dto.UpdateUserStatusRequestDTO;
import com.nexofinance.backend.domain.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserSummaryResponseDTO>> findAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AdminUserSummaryResponseDTO> users = adminUserService.findAllUsers(search, role, active, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/metrics")
    public ResponseEntity<PlatformMetricsResponseDTO> getPlatformMetrics() {
        PlatformMetricsResponseDTO metrics = adminUserService.getPlatformMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserSummaryResponseDTO> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String adminEmail = extractAdminEmail(principal);
        AdminUserSummaryResponseDTO updated = adminUserService.updateRole(id, requestDTO.role(), adminEmail);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<AdminUserSummaryResponseDTO> updateRolePut(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return updateRole(id, requestDTO, principal);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserSummaryResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String adminEmail = extractAdminEmail(principal);
        AdminUserSummaryResponseDTO updated = adminUserService.updateStatus(id, requestDTO.active(), adminEmail);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AdminUserSummaryResponseDTO> updateStatusPut(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequestDTO requestDTO,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return updateStatus(id, requestDTO, principal);
    }

    private String extractAdminEmail(UserDetails principal) {
        if (principal == null || principal.getUsername() == null || principal.getUsername().isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Identificação do administrador ausente");
        }
        return principal.getUsername();
    }
}
