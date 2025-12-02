package dev.williamnogueira.ecommerce.domain.address;

import dev.williamnogueira.ecommerce.domain.address.dto.AddressRequestDTO;
import dev.williamnogueira.ecommerce.domain.address.dto.AddressResponseDTO;
import dev.williamnogueira.ecommerce.domain.address.exceptions.AddressNotFoundException;
import dev.williamnogueira.ecommerce.domain.customer.CustomerService;
import dev.williamnogueira.ecommerce.domain.customer.CustomerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static dev.williamnogueira.ecommerce.utils.AddressTestUtils.*;
import static dev.williamnogueira.ecommerce.utils.CustomerTestUtils.createCustomerEntity;
import static dev.williamnogueira.ecommerce.infrastructure.constants.ErrorMessages.ADDRESS_NOT_FOUND_WITH_ID;
import static dev.williamnogueira.ecommerce.utils.TestConstants.ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private AddressService addressService;

    @Captor
    private ArgumentCaptor<AddressEntity> addressCaptor;

    private AddressEntity addressEntity;
    private AddressResponseDTO addressResponseDTO;
    private AddressRequestDTO addressRequestDTO;
    private CustomerEntity customerEntity;

    @BeforeEach
    void setUp() {
        addressEntity = createAddressEntity();
        addressRequestDTO = createAddressRequestDTO();
        addressResponseDTO = createAddressResponseDTO();
        customerEntity = createCustomerEntity();
    }

    @Test
    void testCreate() {
        when(customerService.getEntity(addressRequestDTO.customer())).thenReturn(customerEntity);

        AddressEntity newAddressEntity = createAddressEntity();
        when(addressMapper.toEntity(addressRequestDTO)).thenReturn(newAddressEntity);

        when(addressRepository.save(any(AddressEntity.class))).thenReturn(newAddressEntity);
        when(addressMapper.toResponseDTO(newAddressEntity)).thenReturn(addressResponseDTO);

        var response = addressService.create(addressRequestDTO);

        assertThat(response).isNotNull().isEqualTo(addressResponseDTO);

        verify(addressRepository).save(addressCaptor.capture());
        AddressEntity savedEntity = addressCaptor.getValue();
        assertThat(savedEntity.getCustomer()).isEqualTo(customerEntity);

        verify(customerService).getEntity(addressRequestDTO.customer());
        verify(addressMapper).toEntity(addressRequestDTO);
        verify(addressMapper).toResponseDTO(newAddressEntity);
    }

    @Test
    void testUpdateById() {
        AddressEntity existingEntity = createAddressEntity();

        when(addressRepository.findById(ID)).thenReturn(Optional.of(existingEntity));
        when(customerService.getEntity(addressRequestDTO.customer())).thenReturn(customerEntity);

        AddressEntity updatedEntity = createAddressEntity();
        updatedEntity.setCustomer(customerEntity);
        updatedEntity.setStreet(addressRequestDTO.street());
        updatedEntity.setNumber(addressRequestDTO.number());
        updatedEntity.setZipCode(addressRequestDTO.zipCode());
        updatedEntity.setCity(addressRequestDTO.city());
        updatedEntity.setCountry(addressRequestDTO.country());
        updatedEntity.setNeighborhood(addressRequestDTO.neighborhood());
        updatedEntity.setType(addressEntity.getType());
        updatedEntity.setAdditionalInfo(addressRequestDTO.additionalInfo());

        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedEntity);
        when(addressMapper.toResponseDTO(updatedEntity)).thenReturn(addressResponseDTO);

        var response = addressService.updateById(ID, addressRequestDTO);

        assertThat(response).isNotNull().isEqualTo(addressResponseDTO);

        verify(addressRepository).save(addressCaptor.capture());
        AddressEntity savedEntity = addressCaptor.getValue();

        assertThat(savedEntity.getCustomer()).isEqualTo(customerEntity);
        assertThat(savedEntity.getStreet()).isEqualTo(addressRequestDTO.street());
        assertThat(savedEntity.getNumber()).isEqualTo(addressRequestDTO.number());
        assertThat(savedEntity.getNeighborhood()).isEqualTo(addressRequestDTO.neighborhood());
        assertThat(savedEntity.getCity()).isEqualTo(addressRequestDTO.city());
        assertThat(savedEntity.getState()).isEqualTo(addressRequestDTO.state());
        assertThat(savedEntity.getCountry()).isEqualTo(addressRequestDTO.country());
        assertThat(savedEntity.getZipCode()).isEqualTo(addressRequestDTO.zipCode());
        assertThat(savedEntity.getType()).isEqualTo(AddressTypeEnum.valueOf(addressRequestDTO.type()));
        assertThat(savedEntity.getAdditionalInfo()).isEqualTo(addressRequestDTO.additionalInfo());

        verify(addressRepository).findById(ID);
        verify(customerService).getEntity(addressRequestDTO.customer());
        verify(addressRepository).save(any(AddressEntity.class));
        verify(addressMapper).toResponseDTO(updatedEntity);
    }

    @Test
    void testUpdateById_ShouldFailIfAnyFieldNotSet() {
        AddressEntity existingEntity = createAddressEntity();

        when(addressRepository.findById(ID)).thenReturn(Optional.of(existingEntity));
        when(customerService.getEntity(addressRequestDTO.customer())).thenReturn(customerEntity);
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(existingEntity);
        when(addressMapper.toResponseDTO(existingEntity)).thenReturn(addressResponseDTO);

        var response = addressService.updateById(ID, addressRequestDTO);

        assertThat(response).isNotNull();

        verify(addressRepository).save(addressCaptor.capture());
        AddressEntity savedEntity = addressCaptor.getValue();

        assertThat(savedEntity.getCustomer()).isEqualTo(customerEntity);
        assertThat(savedEntity.getStreet()).isEqualTo(addressRequestDTO.street());
        assertThat(savedEntity.getNumber()).isEqualTo(addressRequestDTO.number());
        assertThat(savedEntity.getNeighborhood()).isEqualTo(addressRequestDTO.neighborhood());
        assertThat(savedEntity.getCity()).isEqualTo(addressRequestDTO.city());
        assertThat(savedEntity.getState()).isEqualTo(addressRequestDTO.state());
        assertThat(savedEntity.getCountry()).isEqualTo(addressRequestDTO.country());
        assertThat(savedEntity.getZipCode()).isEqualTo(addressRequestDTO.zipCode());
        assertThat(savedEntity.getType()).isEqualTo(AddressTypeEnum.valueOf(addressRequestDTO.type()));
        assertThat(savedEntity.getAdditionalInfo()).isEqualTo(addressRequestDTO.additionalInfo());
    }

    @Test
    void testGetEntity() {
        when(addressRepository.findById(ID)).thenReturn(Optional.of(addressEntity));

        var response = addressService.getEntity(ID);

        assertThat(response).isNotNull().isEqualTo(addressEntity);
        verify(addressRepository).findById(ID);
    }

    @Test
    void testGetEntityDoesNotFindEntity() {
        when(addressRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatException()
                .isThrownBy(() -> addressService.getEntity(ID))
                .isInstanceOf(AddressNotFoundException.class)
                .withMessageContaining(String.format(ADDRESS_NOT_FOUND_WITH_ID, ID));
        verify(addressRepository).findById(ID);
    }
}
