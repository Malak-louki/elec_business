package com.hb.cda.elec_business.mapper;

import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.Booking;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BookingMapper {

    public static BookingResponseDto toResponseDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingResponseDto.BookingResponseDtoBuilder builder = BookingResponseDto.builder()
                .id(booking.getId())
                .startDateTime(booking.getStartDateTime())
                .endDateTime(booking.getEndDateTime())
                .totalAmount(booking.getTotalAmount()) // CORRECTION
                .bookingStatus(booking.getBookingStatus())
                .invoicePath(booking.getInvoicePath())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .expiresAt(booking.getExpiresAt());

        // User info
        if (booking.getUser() != null) {
            builder.user(BookingResponseDto.UserInfoDto.builder()
                    .id(booking.getUser().getId())
                    .firstName(booking.getUser().getFirstName())
                    .lastName(booking.getUser().getLastName())
                    .email(booking.getUser().getEmail())
                    .phone(booking.getUser().getPhone())
                    .build());
        }

        // Station info
        if (booking.getChargingStation() != null) {
            String address = "";
            String city = "";

            if (booking.getChargingStation().getChargingLocation() != null &&
                    booking.getChargingStation().getChargingLocation().getAddress() != null) {
                var addr = booking.getChargingStation().getChargingLocation().getAddress();
                address = String.format("%s %s",
                        addr.getNumber() != null ? addr.getNumber() : "",
                        addr.getStreet()).trim();
                city = addr.getCity();
            }

            builder.chargingStation(BookingResponseDto.StationInfoDto.builder()
                    .id(booking.getChargingStation().getId())
                    .name(booking.getChargingStation().getName())
                    .hourlyPrice(booking.getChargingStation().getHourlyPrice())
                    .chargingPower(booking.getChargingStation().getChargingPower())
                    .address(address)
                    .city(city)
                    .build());
        }

        // Payment info
        if (booking.getPayment() != null) {
            builder.payment(BookingResponseDto.PaymentInfoDto.builder()
                    .id(booking.getPayment().getId())
                    .stripePaymentIntentId(booking.getPayment().getStripePaymentIntentId())
                    .paymentStatus(booking.getPayment().getPaymentStatus().name())
                    .build());
        }

        return builder.build();
    }

    public static List<BookingResponseDto> toResponseDtoList(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return Collections.emptyList();
        }

        return bookings.stream()
                .map(BookingMapper::toResponseDto)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}