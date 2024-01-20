package booking.app.service.accommodation.address;

import booking.app.exception.EntityNotFoundException;
import booking.app.model.accommodation.Address;
import booking.app.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;

    @Override
    public Address createAddress(final String address) {
        Address newAddress = new Address();
        newAddress.setAddress(address);
        return addressRepository.save(newAddress);
    }

    @Override
    public Address getAddressByAddressArgument(final String address) {
        return addressRepository.findByAddress(address).orElseThrow(
                () -> new EntityNotFoundException("Can't find address by address name: " + address)
        );
    }

    @Override
    public Address getAddressIfExistingOrSaveAndGet(final String address) {
        if (!addressRepository.existsByAddress(address)) {
            return createAddress(address);
        }
        return getAddressByAddressArgument(address);
    }

    @Override
    public boolean checkExistingAddress(final String address) {
        return addressRepository.existsByAddress(address);
    }

    @Override
    public void deleteById(final Long id) {
        if (!addressRepository.existsById(id)) {
            throw new EntityNotFoundException("Can't find address by id: " + id);
        }
        addressRepository.deleteById(id);
    }
}
