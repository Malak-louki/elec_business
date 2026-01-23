package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.user.UpgradeResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
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
    public UpgradeResponseDto upgradeToOwner(User user) {
        log.info("🔄 Starting upgrade process for user: {}", user.getEmail());

        // Vérifier si l'utilisateur a déjà le rôle OWNER
        boolean hasOwnerRole = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.OWNER);

        if (hasOwnerRole) {
            log.warn("⚠️ User {} already has OWNER role", user.getEmail());
            throw new IllegalStateException("Vous êtes déjà propriétaire");
        }

        // Récupérer le rôle OWNER depuis la base de données
        Role ownerRole = roleRepository.findByName(RoleName.OWNER)
                .orElseThrow(() -> {
                    log.error("❌ OWNER role not found in database");
                    return new IllegalStateException("Rôle OWNER introuvable dans la base de données");
                });

        // Ajouter le rôle OWNER (on garde aussi le rôle USER)
        user.getRoles().add(ownerRole);

        // Sauvegarder l'utilisateur
        User updatedUser = userRepository.save(user);

        log.info("✅ User {} successfully upgraded to OWNER. Current roles: {}",
                user.getEmail(),
                updatedUser.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.joining(", ")));

        // Récupérer les noms des rôles
        List<String> roleNames = updatedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return UpgradeResponseDto.builder()
                .message("Votre compte a été mis à niveau vers propriétaire avec succès")
                .roles(roleNames)
                .build();
    }
}






