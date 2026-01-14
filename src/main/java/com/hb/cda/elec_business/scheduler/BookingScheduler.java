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

/**
 * Scheduler pour gérer automatiquement les états des réservations
 *
 * Tâches planifiées :
 * 1. Expirer les réservations PENDING non payées après 15 minutes
 * 2. Marquer comme COMPLETED les réservations CONFIRMED terminées
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingRepository bookingRepository;

    /**
     * Expire les réservations PENDING non payées après le délai d'expiration
     * Exécuté toutes les 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300 000 ms
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
                    log.info("Booking {} expired (was pending since {})",
                            booking.getId(),
                            booking.getCreatedAt());
                }

                log.info("Successfully expired {} bookings", expiredBookings.size());
            } else {
                log.debug("No expired PENDING bookings found");
            }

        } catch (Exception e) {
            log.error("Error while expiring PENDING bookings", e);
            // Ne pas propager l'exception pour ne pas bloquer les prochaines exécutions
        }
    }

    /**
     * Marque comme COMPLETED les réservations CONFIRMED dont la fin est passée
     * Exécuté toutes les heures
     */
    @Scheduled(fixedRate = 3600000) // 1 heure = 3 600 000 ms
    @Transactional
    public void completeFinishedBookings() {
        log.debug("Running scheduled task: completeFinishedBookings");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now();
            List<Booking> finishedBookings = bookingRepository.findBookingsToComplete(cutoffTime);

            if (!finishedBookings.isEmpty()) {
                log.info("Found {} finished CONFIRMED bookings to mark as COMPLETED", finishedBookings.size());

                for (Booking booking : finishedBookings) {
                    booking.setBookingStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(booking);
                    log.info("Booking {} marked as COMPLETED (ended at {})",
                            booking.getId(),
                            booking.getEndDateTime());
                }

                log.info("Successfully completed {} bookings", finishedBookings.size());
            } else {
                log.debug("No finished CONFIRMED bookings found");
            }

        } catch (Exception e) {
            log.error("Error while completing finished bookings", e);
            // Ne pas propager l'exception pour ne pas bloquer les prochaines exécutions
        }
    }

    /**
     * Méthode utilitaire pour forcer l'exécution manuelle (pour les tests ou admin)
     */
    public void forceExpirePendingBookings() {
        log.info("Manual trigger: expirePendingBookings");
        expirePendingBookings();
    }

    /**
     * Méthode utilitaire pour forcer l'exécution manuelle (pour les tests ou admin)
     */
    public void forceCompleteFinishedBookings() {
        log.info("Manual trigger: completeFinishedBookings");
        completeFinishedBookings();
    }
}