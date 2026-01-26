package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.user.UpgradeResponseDto;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<UpgradeResponseDto> upgradeToOwner(Authentication authentication) {
        log.info("📤 User {} requesting upgrade to OWNER",
                ((User) authentication.getPrincipal()).getEmail());

        User currentUser = (User) authentication.getPrincipal();
        UpgradeResponseDto response = userService.upgradeToOwner(currentUser);

        log.info("✅ User {} successfully upgraded", currentUser.getEmail());
        return ResponseEntity.ok(response);
    }
}