package com.hb.cda.elec_business.scheduler;

import com.hb.cda.elec_business.entity.Booking;
import com.hb.cda.elec_business.entity.BookingStatus;
import com.hb.cda.elec_business.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 300000) // 5 min
    @Transactional
    public void expirePendingBookings() {
        log.debug("Running scheduled task: expirePendingBookings");

        try {
            Instant now = Instant.now();
            List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(now);

            if (!expiredBookings.isEmpty()) {
                log.info("Found {} expired PENDING bookings to process", expiredBookings.size());

                for (Booking booking : expiredBookings) {
                    booking.setBookingStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(booking);
                }
            } else {
                log.debug("No expired PENDING bookings found");
            }

        } catch (Exception e) {
            log.error("Error while expiring PENDING bookings", e);
        }
    }


    @Scheduled(fixedRate = 3600000) // 1h ms
    @Transactional
    public void completeFinishedBookings() {
        log.debug("Running scheduled task: completeFinishedBookings");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now();
            List<Booking> finishedBookings = bookingRepository.findBookingsToComplete(cutoffTime);

            if (!finishedBookings.isEmpty()) {

                for (Booking booking : finishedBookings) {
                    booking.setBookingStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(booking);
                    log.info("Booking {} marked as COMPLETED (ended at {})",
                            booking.getId(),
                            booking.getEndDateTime());
                }
            } else {
                log.debug("No finished CONFIRMED bookings found");
            }

        } catch (Exception e) {
            log.error("Error while completing finished bookings", e);
        }
    }

    public void forceExpirePendingBookings() {
        expirePendingBookings();
    }

    public void forceCompleteFinishedBookings() {
        completeFinishedBookings();
    }
}