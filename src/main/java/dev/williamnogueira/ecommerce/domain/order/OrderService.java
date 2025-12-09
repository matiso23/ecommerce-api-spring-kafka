package dev.williamnogueira.ecommerce.domain.order;

import dev.williamnogueira.ecommerce.domain.address.AddressEntity;
import dev.williamnogueira.ecommerce.domain.address.AddressTypeEnum;
import dev.williamnogueira.ecommerce.domain.address.exceptions.AddressNotFoundException;
import dev.williamnogueira.ecommerce.domain.order.dto.OrderResponseDTO;
import dev.williamnogueira.ecommerce.domain.order.exceptions.EmptyShoppingCartException;
import dev.williamnogueira.ecommerce.domain.order.exceptions.OrderNotFoundException;
import dev.williamnogueira.ecommerce.domain.order.kafka.OrderProducer;
import dev.williamnogueira.ecommerce.domain.order.orderaddress.OrderAddressEntity;
import dev.williamnogueira.ecommerce.domain.order.orderitem.OrderItemEntity;
import dev.williamnogueira.ecommerce.domain.shoppingcart.ShoppingCartEntity;
import dev.williamnogueira.ecommerce.domain.shoppingcart.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static dev.williamnogueira.ecommerce.infrastructure.constants.ErrorMessages.ADDRESS_NOT_FOUND;
import static dev.williamnogueira.ecommerce.infrastructure.constants.ErrorMessages.ORDER_NOT_FOUND_WITH_ID;
import static dev.williamnogueira.ecommerce.infrastructure.constants.ErrorMessages.SHOPPING_CART_IS_EMPTY;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingCartService shoppingCartService;
    private final OrderMapper orderMapper;
    private final OrderProducer orderProducer;

    /*@
      @ requires id != null;
      @ ensures \result != null;
      @*/
    @Transactional(readOnly = true)
    public OrderResponseDTO findById(UUID id) {
        return orderMapper.toResponseDTO(getEntity(id));
    }

    /*@
      @ requires customerId != null;
      @ // If execution completes normally, the result is not null
      @ ensures \result != null;
      @*/
    @Transactional
    public OrderResponseDTO purchaseShoppingCart(UUID customerId) {
        var shoppingCart = shoppingCartService.findByCustomerId(customerId);

        if (shoppingCart.getItems().isEmpty()) {
            throw new EmptyShoppingCartException(SHOPPING_CART_IS_EMPTY);
        }

        var shippingAddress = getShippingAddress(shoppingCart);
        var orderItems = orderMapper.toOrderItemsEntity(shoppingCart.getItems());
        var orderAddress = orderMapper.toOrderAddressEntity(shippingAddress);
        var order = buildOrder(shoppingCart, orderItems, orderAddress);

        order.getOrderItems().forEach(item -> item.setOrder(order));
        orderRepository.save(order);

        orderProducer.clearShoppingCart(String.valueOf(customerId));
        return orderMapper.toResponseDTO(order);
    }

    /*@
      @ requires id != null;
      @ requires status != null;
      @*/
    @Transactional
    public void updateStatus(String id, OrderStatusEnum status) {
        var order = getEntity(UUID.fromString(id));
        order.setStatus(status);
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> findAllByCustomerId(UUID customerId, Pageable pageable) {
        return orderRepository.findAllByCustomerId(customerId, pageable)
                .map(orderMapper::toResponseDTO);
    }

    /*@
      @ requires id != null;
      @ // If the method returns normally, the result is not null
      @ ensures \result != null;
      @ // If the method returns normally, the ID matches the requested ID
      @ ensures \result.getId() != null && \result.getId().equals(id);
      @ // Throw exception if order is not found (simulated by signals)
      @ signals (OrderNotFoundException e) true;
      @*/
    public OrderEntity getEntity(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(String.format(ORDER_NOT_FOUND_WITH_ID, id)));
    }

    /*@
      @ requires shoppingCart != null;
      @ requires shoppingCart.getCustomer() != null;
      @ requires shoppingCart.getCustomer().getAddress() != null;
      @
      @ // The returned address must be either SHIPPING or BILLING type
      @ ensures \result.getType().equals(AddressTypeEnum.SHIPPING) ||
      @         \result.getType().equals(AddressTypeEnum.BILLING);
      @
      @ // If we return normally, result is not null
      @ ensures \result != null;
      @
      @ // If we throw an exception, it implies no suitable address was found
      @ signals (AddressNotFoundException e) true;
      @*/
    private AddressEntity getShippingAddress(ShoppingCartEntity shoppingCart) {
        return shoppingCart.getCustomer().getAddress().stream()
                .filter(a -> a.getType().equals(AddressTypeEnum.SHIPPING))
                .findFirst()
                .orElseGet(() -> shoppingCart.getCustomer().getAddress().stream()
                        .filter(a -> a.getType().equals(AddressTypeEnum.BILLING))
                        .findFirst()
                        .orElseThrow(() -> new AddressNotFoundException(ADDRESS_NOT_FOUND)));
    }

    /*@
      @ requires shoppingCart != null;
      @ requires orderItems != null;
      @ requires orderAddress != null;
      @
      @ // Ensure the built order has the correct status
      @ ensures \result.getStatus() == OrderStatusEnum.PENDING;
      @
      @ // Ensure the Total Price is mapped correctly from the Cart
      @ ensures \result.getTotalPrice() == shoppingCart.getTotalPrice();
      @
      @ // Ensure the Customer is mapped correctly
      @ ensures \result.getCustomer() == shoppingCart.getCustomer();
      @
      @ // Ensure the Shipping Address is mapped correctly
      @ ensures \result.getShippingAddress() == orderAddress;
      @
      @ // Ensure the Order Items are mapped correctly
      @ ensures \result.getOrderItems() == orderItems;
      @*/
    private OrderEntity buildOrder(ShoppingCartEntity shoppingCart, List<OrderItemEntity> orderItems, OrderAddressEntity orderAddress) {
        return OrderEntity.builder()
                .customer(shoppingCart.getCustomer())
                .orderItems(orderItems)
                .shippingAddress(orderAddress)
                .totalPrice(shoppingCart.getTotalPrice())
                .status(OrderStatusEnum.PENDING)
                .build();
    }
}
