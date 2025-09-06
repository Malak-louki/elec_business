package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_table")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String userStatus;
    private Date dateOfBirth;

    @ManyToOne
    @JoinColumn(name = "Id_Adress")
    private Address address;

    @OneToMany(mappedBy = "user")
    private Set<ChargingStation> chargingStations;

    @OneToMany(mappedBy = "user")
    private Set<UserValidation> validations;

    @OneToMany(mappedBy = "user")
    private Set<Booking> bookings;

    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "Id_User"),
            inverseJoinColumns = @JoinColumn(name = "Id_Role")
    )
    private Set<Role> roles;

}
