package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
public class ChargingLocation extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Boolean available;
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToMany(mappedBy = "chargingLocation")
    private Set<ChargingStation> chargingStations = new HashSet<>();


    public ChargingLocation() {
    }

    public ChargingLocation(Boolean available, BigDecimal latitude, BigDecimal longitude, Address address) {
        this.available = available;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public ChargingLocation(String id, Boolean available, BigDecimal latitude, BigDecimal longitude, Address address) {
        this.id = id;
        this.available = available;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Set<ChargingStation> getChargingStations() {
        return chargingStations;
    }

    public void setChargingStations(Set<ChargingStation> chargingStations) {
        this.chargingStations = chargingStations;
    }

}
