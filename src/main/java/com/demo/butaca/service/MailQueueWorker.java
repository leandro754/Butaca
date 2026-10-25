package com.demo.butaca.service;

import com.demo.butaca.config.MailSettings;
import com.demo.butaca.model.EstadoEnvioNotificacion;
import com.demo.butaca.repository.NotificacionRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class MailQueueWorker {
    private final MailSettings settings;
    private final NotificacionRepository notifications;
    private final MailDeliveryService delivery;
    public MailQueueWorker(MailSettings settings, NotificacionRepository notifications, MailDeliveryService delivery) {
        this.settings = settings; this.notifications = notifications; this.delivery = delivery;
    }
    @Scheduled(fixedDelayString = "${butaca.mail.poll-ms:5000}", initialDelayString = "${butaca.mail.initial-delay-ms:10000}")
    public void processPending() {
        if (!settings.isEnabled()) return;
        for (var notification : notifications.findTop20ByEstadoEnvioOrderByCreatedAtAsc(EstadoEnvioNotificacion.PENDIENTE)) {
            delivery.deliver(notification.getIdNotificacion());
        }
    }
}
