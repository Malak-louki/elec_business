package com.hb.cda.elec_business.scheduler;

import com.hb.cda.elec_business.entity.Booking;
import com.hb.cda.elec_business.entity.BookingStatus;
import com.hb.cda.elec_business.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduler pour gérer automatiquement les réservations - VERSION CORRIGÉE
 *
 * CORRECTION : Utilise la timezone centralisée
 *
 * 2 TÂCHES AUTOMATIQUES :
 * 1. Marquer EXPIRED les réservations PENDING non payées
 * 2. Marquer COMPLETED les réservations CONFIRMED terminées
 *
 * @author Votre Nom - CDA 2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        value = "app.booking.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BookingScheduler {

    private final BookingRepository bookingRepository;

    // CORRECTION : Timezone centralisée
    @Value("${app.timezone:Europe/Paris}")
    private String timezone;

    /**
     * TÂCHE 1 : Marquer les réservations PENDING expirées
     *
     * Exécution : Toutes les 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expirePendingBookings() {
        log.debug("Début vérification réservations PENDING expirées");

        try {
            List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(Instant.now());

            if (expiredBookings.isEmpty()) {
                log.debug("Aucune réservation PENDING expirée trouvée");
                return;
            }

            log.info("Traitement de {} réservation(s) PENDING expirée(s)", expiredBookings.size());

            for (Booking booking : expiredBookings) {
                booking.setBookingStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                log.info("Réservation {} marquée EXPIRED (délai de paiement dépassé)",
                        booking.getId());
            }

            log.info("{} réservation(s) marquée(s) EXPIRED avec succès", expiredBookings.size());

        } catch (Exception e) {
            log.error("Erreur lors du traitement des réservations expirées", e);
        }
    }

    /**
     * TÂCHE 2 : Marquer les réservations CONFIRMED comme COMPLETED
     *
     * Exécution : Toutes les heures
     * cron = "0 0 * * * *"
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void completeFinishedBookings() {
        log.debug("Début vérification réservations CONFIRMED à finaliser");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now(ZoneId.of(timezone)).minusMinutes(30);

            List<Booking> bookingsToComplete = bookingRepository.findBookingsToComplete(cutoffTime);

            if (bookingsToComplete.isEmpty()) {
                log.debug("Aucune réservation CONFIRMED à finaliser");
                return;
            }

            log.info("Traitement de {} réservation(s) CONFIRMED à finaliser",
                    bookingsToComplete.size());

            for (Booking booking : bookingsToComplete) {
                booking.setBookingStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);

                log.info("Réservation {} marquée COMPLETED (période terminée)",
                        booking.getId());
            }

            log.info("{} réservation(s) marquée(s) COMPLETED avec succès",
                    bookingsToComplete.size());

        } catch (Exception e) {
            log.error("Erreur lors de la finalisation des réservations", e);
        }
    }

    /**
     * TÂCHE 3 (OPTIONNELLE) : Nettoyage des anciennes données
     *
     * Exécution : Tous les jours à 3h du matin
     * cron = "0 0 3 * * *"
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldData() {
        log.debug("Nettoyage des données obsolètes");

        // Ici on pourrait :
        // - Archiver les vieilles réservations COMPLETED (> 1 an)
        // - Générer des statistiques
        // - Nettoyer les logs

        log.debug("Nettoyage terminé");
    }
}