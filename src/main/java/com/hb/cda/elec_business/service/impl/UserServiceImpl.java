package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.auth.UserResponseDto;
import com.hb.cda.elec_business.dto.user.UpgradeResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.mapper.UserMapper;
import com.hb.cda.elec_business.repository.RoleRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UpgradeResponseDto upgradeToOwner(User currentUser) {
        log.info("🔄 Starting upgrade process for user: {}", currentUser.getEmail());

        // Vérifier que l'utilisateur est ACTIVE
        if (currentUser.getUserStatus() != UserStatus.ACTIVE) {
            log.error("❌ Cannot upgrade inactive user: {}", currentUser.getEmail());
            throw new IllegalStateException("Le compte doit être activé pour devenir propriétaire");
        }

        // Récupérer le rôle OWNER
        log.info("🔍 Fetching OWNER role...");
        Role ownerRole = roleRepository.findByName(RoleName.OWNER)
                .orElseThrow(() -> new IllegalStateException("OWNER role not found - RoleInitializer may have failed"));
        log.info("✅ OWNER role found");

        // Vérifier si l'utilisateur a déjà le rôle OWNER
        boolean alreadyOwner = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.OWNER);

        if (alreadyOwner) {
            log.warn("⚠️ User {} already has OWNER role", currentUser.getEmail());
            throw new IllegalStateException("L'utilisateur a déjà le rôle de propriétaire");
        }

        // Ajouter le rôle OWNER
        log.info("➕ Adding OWNER role to user...");
        currentUser.getRoles().add(ownerRole);
        User updatedUser = userRepository.save(currentUser);

        log.info("✅ User {} successfully upgraded to OWNER", currentUser.getEmail());

        // Construire la réponse
        List<String> roleNames = updatedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return UpgradeResponseDto.builder()
                .message("Vous avez été promu au rôle de propriétaire avec succès !")
                .roles(roleNames)
                .build();
    }
}