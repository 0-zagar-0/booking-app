package booking.app.service.accommodation.address;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import booking.app.exception.EntityNotFoundException;
import booking.app.model.accommodation.Address;
import booking.app.repository.AddressRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {
    private Validator validator;
    @Mock
    private AddressRepository addressRepository;
    @InjectMocks
    private AddressServiceImpl addressService;

    @Test
    @DisplayName("Create address with valid format, should return address")
    void createAddress_WithValidFormat_ShouldReturnAddress() {
        //Given
        String request = "City, Street, 12";

        Address address = new Address();
        address.setAddress(request);

        Address expected = new Address();
        expected.setId(1L);
        expected.setAddress(request);

        //When
        when(addressRepository.save(any(Address.class))).thenReturn(expected);

        //Then
        Address actual = addressService.createAddress(request);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Create address with invalid format, should violate constraint")
    void createAddress_WithInvalidFormat_ShouldViolateConstraint() {
        // Given
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        Address addressObject = new Address();
        addressObject.setAddress("Invalid Address Format");

        // When
        Set<ConstraintViolation<Address>> violations = validator.validate(addressObject);
        // Then
        assertFalse(violations.isEmpty(), "Violations expected for an invalid address format");
        assertEquals(1, violations.size(), "Exactly one violation expected");

        ConstraintViolation<Address> violation = violations.iterator().next();
        assertEquals("is incorrect must be: City, Street, number house", violation.getMessage());
    }

    @Test
    @DisplayName("Get address by address string, should return address")
    void getAddress_ByAddressString_ShouldReturnAddress() {
        //Given
        Address expected = new Address();
        expected.setId(1L);
        expected.setAddress("City, Street, 22");
        String requestAddress = "City, Street, 22";

        //When
        when(addressRepository.findByAddress(requestAddress)).thenReturn(Optional.of(expected));

        //Then
        Address actual = addressService.getAddressByAddressArgument(requestAddress);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(addressRepository, times(1)).findByAddress(requestAddress);
    }

    @Test
    @DisplayName("Get address by non exists address, should return exception")
    void getAddress_ByNonExistsAddress_ShouldReturnException() {
        //Given
        String requestAddress = "City, street, 500";

        //When
        when(addressRepository.findByAddress(requestAddress)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> addressService.getAddressByAddressArgument(requestAddress)
        );

        //Then
        String expected = "Can't find address by address name: " + requestAddress;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(addressRepository, times(1)).findByAddress(requestAddress);
    }

    @Test
    @DisplayName("Get address if exists or save and get by exists address, "
            + "should use get method and return address")
    void getAddressIfExistsOrSaveAndGet_ByExistsAddress_ShouldUseGetMethodAndReturnAddress() {
        //Given
        String address = "City, Street, 66";

        Address expected = new Address();
        expected.setId(2L);
        expected.setAddress("City, Street, 66");

        //When
        when(addressRepository.existsByAddress(address)).thenReturn(true);
        when(addressRepository.findByAddress(address)).thenReturn(Optional.of(expected));

        //Then
        Address actual = addressService.getAddressIfExistingOrSaveAndGet(address);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
        verify(addressRepository, times(1)).existsByAddress(address);
        verify(addressRepository, times(1)).findByAddress(address);
    }

    @Test
    @DisplayName("Get address if exists or save and get by exists address, "
            + "should use create method and return address")
    void getAddressIfExistsOrSaveAndGet_ByNonExistsAddress_ShouldUseCreateMethodAndReturnAddress() {
        //Given
        String address = "City, Street, 66";

        Address expected = new Address();
        expected.setId(2L);
        expected.setAddress("City, Street, 66");

        Address request = new Address();
        request.setAddress(address);

        //When
        when(addressRepository.existsByAddress(address)).thenReturn(false);
        when(addressRepository.save(any(Address.class))).thenReturn(expected);

        //Then
        Address actual = addressService.getAddressIfExistingOrSaveAndGet(address);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));

        verify(addressRepository, times(1)).existsByAddress(address);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Check existing address by exists address, should return true")
    void checkExistingAddress_ByExistsAddress_ShouldReturnTrue() {
        //Given
        String address = "City, street, 11";

        //When
        when(addressRepository.existsByAddress(address)).thenReturn(true);

        //Then
        assertTrue(addressService.checkExistingAddress(address));
        verify(addressRepository, times(1)).existsByAddress(address);
    }

    @Test
    @DisplayName("Delete by id by valid id, should dont throw exception")
    void deleteById_ByValidId_ShouldDontThrowException() {
        //Given
        Long id = 1L;

        //When
        when(addressRepository.existsById(id)).thenReturn(true);

        //Then
        assertDoesNotThrow(() -> addressService.deleteById(id));
        verify(addressRepository, times(1)).existsById(id);
        verify(addressRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("Delete by id by invalid id, should return exception")
    void deleteById_ByInvalidId_ShouldReturnException() {
        //Given
        Long id = 999L;

        //When
        when(addressRepository.existsById(id)).thenReturn(false);
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> addressService.deleteById(id)
        );

        //Then
        String expected = "Can't find address by id: " + id;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(addressRepository, times(1)).existsById(id);
    }
}
