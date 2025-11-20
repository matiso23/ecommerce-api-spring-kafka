package dev.williamnogueira.ecommerce.domain.payment.kafka;

import dev.williamnogueira.ecommerce.domain.order.OrderService;
import dev.williamnogueira.ecommerce.domain.order.OrderStatusEnum;
import dev.williamnogueira.ecommerce.domain.order.OrderEntity;
import dev.williamnogueira.ecommerce.domain.payment.dto.PaymentProducerResponseDTO;
import dev.williamnogueira.ecommerce.domain.payment.exceptions.PaymentStatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static dev.williamnogueira.ecommerce.infrastructure.constants.ErrorMessages.ORDER_STATUS_MUST_BE_PENDING;
import static dev.williamnogueira.ecommerce.infrastructure.constants.KafkaConstants.PAYMENT_REQUEST_IS_NOW_BEING_PROCESSED;
import static dev.williamnogueira.ecommerce.infrastructure.constants.KafkaConstants.PAYMENT_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentProducer paymentProducer;

    @Test
    void sendPaymentRequest_shouldSendKafkaMessage_whenOrderStatusIsPending() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        var order = new OrderEntity();
        order.setStatus(OrderStatusEnum.PENDING);

        when(orderService.getEntity(UUID.fromString(orderId))).thenReturn(order);

        // Act
        PaymentProducerResponseDTO response = paymentProducer.sendPaymentRequest(orderId);

        // Assert
        assertThat(response.message()).isEqualTo(PAYMENT_REQUEST_IS_NOW_BEING_PROCESSED);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(PAYMENT_TOPIC);
        assertThat(messageCaptor.getValue()).isEqualTo(orderId);
    }

    @Test
    void sendPaymentRequest_shouldThrowException_whenOrderStatusIsNotPending() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        var order = new OrderEntity();

        // any status ≠ PENDING
        order.setStatus(OrderStatusEnum.DELIVERED);

        when(orderService.getEntity(UUID.fromString(orderId))).thenReturn(order);

        // Assert
        assertThatThrownBy(() -> paymentProducer.sendPaymentRequest(orderId))
                .isInstanceOf(PaymentStatusException.class)
                .hasMessage(HttpStatus.BAD_REQUEST + " \"" + ORDER_STATUS_MUST_BE_PENDING + "\"");
    }
}
