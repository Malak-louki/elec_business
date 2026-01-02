package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class ChargingStation extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Column(precision = 5, scale = 2)
    private BigDecimal hourlyPrice;

    /**
     * Puissance de charge numérique en kW (pour les calculs et recherches)
     * Exemples : 7.4, 22, 150
     */
    @Column(name = "charging_power_kw", precision = 6, scale = 2, nullable = false)
    @DecimalMin(value = "0.1", message = "Charging power must be at least 0.1 kW")
    @DecimalMax(value = "350.0", message = "Charging power must be less than 350 kW")
    private BigDecimal chargingPowerKw;

    /**
     * Puissance de charge avec unité (pour l'affichage)
     * Exemples : "7.4 kW", "22 kW", "150 kW"
     * Format validé par regex : "X.X kW" ou "XX kW"
     */
    @Pattern(regexp = "^[0-9]{1,3}(\\.[0-9]{1,2})?\\s?kW$")
    @Size(max = 15)
    @Column(name = "charging_power", length = 15)
    private String chargingPower;

    private String instruction;
    private Boolean hasStand;
    private String media;
    private Boolean available;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "charging_station_availability",
            joinColumns = @JoinColumn(name = "charging_station_id"),
            inverseJoinColumns = @JoinColumn(name = "availability_id")
    )
    private Set<Availability> availabilities  = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charging_location_id")
    private ChargingLocation chargingLocation;

    public ChargingStation() {

    }

    public ChargingStation(String name, BigDecimal hourlyPrice, BigDecimal chargingPowerKw, String chargingPower, String instruction, Boolean hasStand, String media, Boolean available) {
        this.name = name;
        this.hourlyPrice = hourlyPrice;
        this.chargingPowerKw = chargingPowerKw;
        this.chargingPower = chargingPower;
        this.instruction = instruction;
        this.hasStand = hasStand;
        this.media = media;
        this.available = available;
    }

    public ChargingStation(String id, String name, BigDecimal hourlyPrice, BigDecimal chargingPowerKw, String chargingPower, String instruction, Boolean hasStand, String media, Boolean available) {
        this.id = id;
        this.name = name;
        this.hourlyPrice = hourlyPrice;
        this.chargingPowerKw = chargingPowerKw;
        this.chargingPower = chargingPower;
        this.instruction = instruction;
        this.hasStand = hasStand;
        this.media = media;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getHourlyPrice() {
        return hourlyPrice;
    }

    public void setHourlyPrice(BigDecimal hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
    }

    public BigDecimal getChargingPowerKw() {
        return chargingPowerKw;
    }

    public void setChargingPowerKw(BigDecimal chargingPowerKw) {
        this.chargingPowerKw = chargingPowerKw;
    }

    public String getChargingPower() {
        return chargingPower;
    }

    public void setChargingPower(String chargingPower) {
        this.chargingPower = chargingPower;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public Boolean getHasStand() {
        return hasStand;
    }

    public void setHasStand(Boolean hasStand) {
        this.hasStand = hasStand;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Availability> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(Set<Availability> availabilities) {
        this.availabilities = availabilities;
    }

    public Boolean isAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public ChargingLocation getChargingLocation() {
        return chargingLocation;
    }

    public void setChargingLocation(ChargingLocation chargingLocation) {
        this.chargingLocation = chargingLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChargingStation that = (ChargingStation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}