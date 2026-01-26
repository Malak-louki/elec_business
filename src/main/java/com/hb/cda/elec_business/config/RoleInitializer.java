package com.hb.cda.elec_business.config;

import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        log.info("🔄 Initializing roles...");

        // Créer USER si n'existe pas
        if (roleRepository.findByName(RoleName.USER).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(RoleName.USER);
            roleRepository.save(userRole);
            log.info("✅ Created USER role");
        } else {
            log.info("✓ USER role already exists");
        }

        // Créer OWNER si n'existe pas
        if (roleRepository.findByName(RoleName.OWNER).isEmpty()) {
            Role ownerRole = new Role();
            ownerRole.setName(RoleName.OWNER);
            roleRepository.save(ownerRole);
            log.info("✅ Created OWNER role");
        } else {
            log.info("✓ OWNER role already exists");
        }

        // Créer ADMIN si n'existe pas
        if (roleRepository.findByName(RoleName.ADMIN).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(RoleName.ADMIN);
            roleRepository.save(adminRole);
            log.info("✅ Created ADMIN role");
        } else {
            log.info("✓ ADMIN role already exists");
        }

        log.info("✅ Roles initialization complete");
    }
}