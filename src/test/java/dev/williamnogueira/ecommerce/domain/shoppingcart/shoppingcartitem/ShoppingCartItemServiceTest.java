package dev.williamnogueira.ecommerce.domain.shoppingcart.shoppingcartitem;

import dev.williamnogueira.ecommerce.domain.product.exceptions.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartItemServiceTest {

    @Mock
    private ShoppingCartItemRepository repository;

    @InjectMocks
    private ShoppingCartItemService service;

    @Test
    void save_shouldCallRepositorySave() {
        ShoppingCartItemEntity item = new ShoppingCartItemEntity();
        service.save(item);
        verify(repository).save(item);
    }

    @Test
    void delete_shouldCallRepositoryDelete() {
        ShoppingCartItemEntity item = new ShoppingCartItemEntity();
        service.delete(item);
        verify(repository).delete(item);
    }

    @Test
    void findByProductId_shouldReturnEntity() {
        UUID id = UUID.randomUUID();
        ShoppingCartItemEntity entity = new ShoppingCartItemEntity();

        when(repository.findByProductId(id)).thenReturn(Optional.of(entity));

        ShoppingCartItemEntity result = service.findByProductId(id);

        assertEquals(entity, result);
        verify(repository).findByProductId(id);
    }

    @Test
    void findByProductId_shouldThrowExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(repository.findByProductId(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.findByProductId(id));
        verify(repository).findByProductId(id);
    }
}
