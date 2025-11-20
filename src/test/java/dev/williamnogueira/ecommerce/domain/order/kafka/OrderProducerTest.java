package dev.williamnogueira.ecommerce.domain.order.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static dev.williamnogueira.ecommerce.infrastructure.constants.KafkaConstants.CLEAR_CART_TOPIC;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OrderProducer orderProducer;

    @Test
    void clearShoppingCart_ShouldSendMessageToKafka() {
        // arrange
        String customerId = "12345";

        // act
        orderProducer.clearShoppingCart(customerId);

        // assert
        verify(kafkaTemplate).send(CLEAR_CART_TOPIC, customerId);
    }
}
