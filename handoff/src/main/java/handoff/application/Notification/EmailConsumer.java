package handoff.application.Notification;

import handoff.application.Config.NotificationProperties;
import handoff.application.Config.RabbitConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @RabbitListener(queues = RabbitConfiguration.EMAIL_QUEUE)
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.from());
        mail.setTo(message.to().toArray(String[]::new));
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
    }
}
