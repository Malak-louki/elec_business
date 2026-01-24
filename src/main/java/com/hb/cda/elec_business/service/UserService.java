package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.dto.user.UpgradeResponseDto;
import com.hb.cda.elec_business.entity.User;

public interface UserService {
    UpgradeResponseDto upgradeToOwner(User user);
}
