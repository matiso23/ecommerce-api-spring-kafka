package dev.williamnogueira.ecommerce.domain.payment;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static dev.williamnogueira.ecommerce.infrastructure.constants.KafkaConstants.PAYMENT_RESPONSE_TOPIC;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private MockWebServer mockWebServer;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        paymentService = new PaymentService(baseUrl, kafkaTemplate);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testProcessPaymentSuccess() {
        // Arrange
        String orderId = "123";
        mockWebServer.enqueue(new MockResponse()
                .setBody("OK")
                .setResponseCode(200));

        // Act
        paymentService.processPayment(orderId);

        // Assert (async check)
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(kafkaTemplate).send(PAYMENT_RESPONSE_TOPIC, orderId));
    }

    @Test
    void testProcessPaymentError() {
        // Arrange
        String orderId = "123";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400));

        // Act
        paymentService.processPayment(orderId);

        // Assert: Kafka call must never happen
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(kafkaTemplate, never())
                                .send(PAYMENT_RESPONSE_TOPIC, orderId));
    }
}
