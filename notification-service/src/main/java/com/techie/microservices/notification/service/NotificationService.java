package com.techie.microservices.notification.service;

import com.techie.microservices.notification.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final JavaMailSender javaMailSender;

	@KafkaListener(topics = "order-placed")
	public void listen(OrderPlacedEvent orderPlacedEvent) {

		log.info("Got message from order-placed topic: {}", orderPlacedEvent);

		MimeMessagePreparator messagePreparator = mimeMessage -> {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
			helper.setFrom("springshop@email.com");
			helper.setTo(orderPlacedEvent.getEmail());
			helper.setSubject("Spring Shop: Order Confirmation");
			helper.setText("""
			               Hi,

			               Thank you for your order.
			               Your order number is %s.

			               Best regards,
			               Spring Shop Team
			               """.formatted(orderPlacedEvent.getOrderNumber()));
		};

	try {
		javaMailSender.send(messagePreparator);
			log.info("Order confirmation email sent!");
		} catch (Exception e) {
			log.error("Email sending failed: {}", e.getMessage(), e);
			throw new RuntimeException("Email sending failed, ignoring for now", e);
		}
	}
}