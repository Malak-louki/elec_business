package com.hb.cda.elec_business.mapper;

import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.Booking;
import com.hb.cda.elec_business.entity.ChargingStation;
import com.hb.cda.elec_business.entity.Payment;
import com.hb.cda.elec_business.entity.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir les entités Booking en DTOs
 */
public class BookingMapper {

    private BookingMapper() {
        // Classe utilitaire, pas d'instanciation
    }

    /**
     * Convertit une entité Booking en DTO complet
     */
    public static BookingResponseDto toResponseDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingResponseDto.builder()
                .id(booking.getId())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .startHour(booking.getStartHour())
                .endHour(booking.getEndHour())
                .paidAmount(booking.getPaidAmount())
                .bookingStatus(booking.getBookingStatus())
                .invoicePath(booking.getInvoicePath())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .chargingStation(mapChargingStationSummary(booking.getChargingStation()))
                .customer(mapUserSummary(booking.getUser()))
                .payment(mapPaymentSummary(booking.getPayment()))
                .build();
    }

    /**
     * Convertit une liste de Bookings en liste de DTOs
     */
    public static List<BookingResponseDto> toResponseDtoList(List<Booking> bookings) {
        if (bookings == null) {
            return List.of();
        }
        return bookings.stream()
                .map(BookingMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Mappe les informations essentielles d'une ChargingStation
     */
    private static BookingResponseDto.ChargingStationSummaryDto mapChargingStationSummary(ChargingStation station) {
        if (station == null) {
            return null;
        }

        String address = null;
        if (station.getChargingLocation() != null && station.getChargingLocation().getAddress() != null) {
            var addr = station.getChargingLocation().getAddress();
            address = String.format("%s %s, %s %s",
                    addr.getNumber() != null ? addr.getNumber() : "",
                    addr.getStreet(),
                    addr.getPostalCode(),
                    addr.getCity()
            );
        }

        return BookingResponseDto.ChargingStationSummaryDto.builder()
                .id(station.getId())
                .name(station.getName())
                .hourlyPrice(station.getHourlyPrice())
                .chargingPower(station.getChargingPower())
                .address(address)
                .build();
    }

    /**
     * Mappe les informations essentielles d'un User
     */
    private static BookingResponseDto.UserSummaryDto mapUserSummary(User user) {
        if (user == null) {
            return null;
        }

        return BookingResponseDto.UserSummaryDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    /**
     * Mappe les informations essentielles d'un Payment
     */
    private static BookingResponseDto.PaymentSummaryDto mapPaymentSummary(Payment payment) {
        if (payment == null) {
            return null;
        }

        return BookingResponseDto.PaymentSummaryDto.builder()
                .id(payment.getId())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .paymentStatus(payment.getPaymentStatus().toString())
                .build();
    }
}