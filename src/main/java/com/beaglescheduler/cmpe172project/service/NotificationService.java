package com.beaglescheduler.cmpe172project.service;

import com.beaglescheduler.cmpe172project.model.Notification;
import com.beaglescheduler.cmpe172project.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifRepo;
    private final RestTemplate restTemplate;

    public NotificationService(NotificationRepository notifRepo, RestTemplate restTemplate) {
        this.notifRepo = notifRepo;
        this.restTemplate = restTemplate;
    }

    /**
     * Writes a pending notification record to the DB.
     * Call this inside a @Transactional method so the record only commits
     * if the surrounding transaction succeeds (outbox pattern).
     */
    public void queue(long appointmentId, long userId, String channel,
                      String type, String payloadJson) {
        Notification n = new Notification();
        n.setAppointmentId(appointmentId);
        n.setUserId(userId);
        n.setChannel(channel);
        n.setNotificationType(type);
        n.setPayloadJson(payloadJson);
        notifRepo.save(n);
        log.info("Queued notification: type={}, appointmentId={}, userId={}", type, appointmentId, userId);
    }

    /**
     * Background dispatcher — runs every 10 seconds.
     * Picks up all pending records, forwards each to /mock/notify,
     * then marks sent or failed.
     */
    @Scheduled(fixedDelay = 10_000)
    public void dispatchPending() {
        List<Notification> pending = notifRepo.findPending();
        if (pending.isEmpty()) return;

        log.info("Dispatcher: {} pending notification(s) to process", pending.size());

        for (Notification n : pending) {
            try {
                restTemplate.postForEntity(
                    "http://localhost:8080/mock/notify",
                    Map.of(
                        "notificationId",   n.getNotificationId(),
                        "appointmentId",    n.getAppointmentId(),
                        "channel",          n.getChannel(),
                        "type",             n.getNotificationType(),
                        "payload",          n.getPayloadJson() != null ? n.getPayloadJson() : ""
                    ),
                    Map.class
                );
                notifRepo.markSent(n.getNotificationId());
                log.info("Dispatched notificationId={} type={}", n.getNotificationId(), n.getNotificationType());
            } catch (Exception e) {
                notifRepo.markFailed(n.getNotificationId());
                log.warn("Dispatch failed for notificationId={}: {}", n.getNotificationId(), e.getMessage());
            }
        }
    }

    public List<Notification> getAllNotifications() {
        return notifRepo.findAll();
    }

    public List<Notification> getNotificationsForUser(long userId) {
        return notifRepo.findByUser(userId);
    }
}
