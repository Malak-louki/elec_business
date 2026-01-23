package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.user.UpgradeResponseDto;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // À adapter selon vos besoins
public class UserController {

    private final UserService userService;

    /**
     * Permet à un USER de devenir OWNER
     * POST /api/users/upgrade-to-owner
     */
    @PostMapping("/upgrade-to-owner")
    public ResponseEntity<UpgradeResponseDto> upgradeToOwner(
            @AuthenticationPrincipal User user
    ) {
        log.info("📤 User {} requesting upgrade to OWNER", user.getEmail());

        try {
            UpgradeResponseDto response = userService.upgradeToOwner(user);
            log.info("✅ User {} successfully upgraded to OWNER", user.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("⚠️ Upgrade failed for {}: {}", user.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during upgrade for {}", user.getEmail(), e);
            throw e;
        }
    }
}