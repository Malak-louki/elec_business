package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<User, String> {
}
