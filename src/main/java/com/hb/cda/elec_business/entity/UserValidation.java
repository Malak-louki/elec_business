package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "user_validation")
public class UserValidation {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Date createdAt;
    private String confirmationCode;
    private Date validatedAt;

    @ManyToOne
    @JoinColumn(name = "Id_User")
    private User user;

}
