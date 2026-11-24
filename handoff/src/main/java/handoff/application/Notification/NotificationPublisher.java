package handoff.application.Notification;

import handoff.application.Config.NotificationProperties;
import handoff.application.Config.RabbitConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailRequested(EmailRequestedEvent event) {
        publish(event.message());
    }

    public void publish(EmailMessage message) {
        if (!properties.enabled() || message.to().isEmpty()) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitConfiguration.EXCHANGE, RabbitConfiguration.EMAIL_ROUTING_KEY, message);
        } catch (AmqpException e) {
            log.error("Não foi possível publicar o e-mail '{}' na fila", message.subject(), e);
        }
    }
}
