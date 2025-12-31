package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface RoleRepository  extends JpaRepository<Role, String> {
    Optional<Role> findByName(RoleName name);

}
