package dev.williamnogueira.ecommerce.domain.order.kafka;

import dev.williamnogueira.ecommerce.domain.shoppingcart.ShoppingCartEntity;
import dev.williamnogueira.ecommerce.domain.shoppingcart.ShoppingCartService;
import dev.williamnogueira.ecommerce.domain.shoppingcart.shoppingcartitem.ShoppingCartItemEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock
    private ShoppingCartService shoppingCartService;

    @InjectMocks
    private OrderConsumer orderConsumer;

    @Test
    void consumePaymentResponse_ShouldClearCartAndResetTotalPrice() {
        // arrange
        UUID id = UUID.randomUUID();
        String customerId = id.toString();

        ShoppingCartEntity cart = new ShoppingCartEntity();
        cart.setItems(new ArrayList<>());
        cart.getItems().add(new ShoppingCartItemEntity());
        cart.setTotalPrice(BigDecimal.TEN);

        when(shoppingCartService.findByCustomerId(id)).thenReturn(cart);

        // act
        orderConsumer.consumePaymentResponse(customerId);

        // assert
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalPrice()).isEqualTo(BigDecimal.ZERO);

        verify(shoppingCartService).findByCustomerId(id);
        verify(shoppingCartService).save(cart);
    }
}
