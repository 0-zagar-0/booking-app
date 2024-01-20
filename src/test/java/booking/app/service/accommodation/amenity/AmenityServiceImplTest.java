package booking.app.service.accommodation.amenity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import booking.app.exception.EntityNotFoundException;
import booking.app.model.accommodation.Amenity;
import booking.app.repository.AmenityRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmenityServiceImplTest {
    @Mock
    private AmenityRepository amenityRepository;
    @InjectMocks
    private AmenityServiceImpl amenityService;

    @Test
    @DisplayName("Get set amenities by amenities names, by non exists amenities,"
            + " should user create method")
    void getSetAmenitiesByAmenitiesNames_ByNonExistsAmenities_ShouldUseCreateMethod() {
        //Given
        Set<String> request = new HashSet<>();
        request.add("WIFI");
        request.add("PARKING");
        Set<Amenity> expected = new HashSet<>();

        //When
        Long index = 1L;

        for (String name : request) {
            when(amenityRepository.existsByName(name)).thenReturn(false);
            Long finalIndex = index;
            Mockito.lenient().when(amenityRepository.save(any(Amenity.class))).thenAnswer(
                    invocation -> {
                        Amenity savedAmenity = invocation.getArgument(0);
                        savedAmenity.setId(finalIndex);
                        savedAmenity.setName(name);
                        expected.add(savedAmenity);
                        return savedAmenity;
                    });
            index++;
        }

        //Then
        Set<Amenity> actual = amenityService.getSetAmenitiesByAmenitiesNames(request);
        assertTrue(actual.containsAll(expected));
        verify(amenityRepository, times(request.size())).existsByName(any(String.class));
        verify(amenityRepository, times(request.size())).save(any(Amenity.class));
    }

    @Test
    @DisplayName("Get set amenities by amenities name by exists amenities, should use get method")
    void getSetAmenitiesByAmenitiesNames_ByExistsAmenities_ShouldUseGetMethod() {
        //Given
        Set<String> request = new HashSet<>();
        request.add("WIFI");
        request.add("PARKING");
        Set<Amenity> expected = new HashSet<>();

        //When
        Long index = 1L;

        for (String name : request) {
            when(amenityRepository.existsByName(name)).thenReturn(true);
            Amenity savedAmenity = new Amenity();
            savedAmenity.setId(index);
            savedAmenity.setName(name);

            when(amenityRepository.findByName(name)).thenReturn(Optional.of(savedAmenity));
            expected.add(savedAmenity);
            index++;
        }

        //Then
        Set<Amenity> actual = amenityService.getSetAmenitiesByAmenitiesNames(request);
        assertTrue(actual.containsAll(expected));
        verify(amenityRepository, times(request.size())).existsByName(any(String.class));
        verify(amenityRepository, times(request.size())).findByName(any(String.class));
    }
    
    @Test
    @DisplayName("Get amenity by name with invalid name, should return exception")
    void getAmenityByName_WithInvalidName_ShouldReturnException() {
        //Given
        String name = "Invalid name";

        //When
        when(amenityRepository.findByName(name)).thenReturn(Optional.empty());
        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> amenityService.getAmenityByName(name)
        );

        //Then
        String expected = "Can't find amenity by name: " + name;
        String actual = exception.getMessage();
        assertEquals(expected, actual);
        verify(amenityRepository, times(1)).findByName(name);
    }
}
