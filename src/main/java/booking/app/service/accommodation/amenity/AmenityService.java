package booking.app.service.accommodation.amenity;

import booking.app.model.accommodation.Amenity;
import java.util.Set;

public interface AmenityService {
    Amenity createAmenity(String amenityName);

    Amenity getAmenityByName(String amenityName);

    Set<Amenity> getSetAmenitiesByAmenitiesNames(Set<String> amenityNames);
}
