package booking.app.service.accommodation.address;

import booking.app.model.accommodation.Address;

public interface AddressService {
    Address createAddress(String address);

    Address getAddressByAddressArgument(String address);

    Address getAddressIfExistingOrSaveAndGet(String address);

    boolean checkExistingAddress(String address);

    void deleteById(Long id);
}
