package dev.williamnogueira.ecommerce.domain.payment.kafka;

import dev.williamnogueira.ecommerce.domain.order.OrderService;
import dev.williamnogueira.ecommerce.domain.order.OrderStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaymentConsumerTest {

    private OrderService orderService;
    private PaymentConsumer consumer;

    @BeforeEach
    void setUp() {
        orderService = Mockito.mock(OrderService.class);
        consumer = new PaymentConsumer(orderService);
    }

    @Test
    void testConsumePaymentResponse_updatesOrderStatusToPaid() {
        // Arrange
        String orderId = "12345";

        // Act
        consumer.consumePaymentResponse(orderId);

        // Assert
        Mockito.verify(orderService)
                .updateStatus(orderId, OrderStatusEnum.PAID);
    }
}
