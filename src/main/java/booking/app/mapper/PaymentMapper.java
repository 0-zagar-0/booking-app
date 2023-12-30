package booking.app.mapper;

import booking.app.config.MapperConfig;
import booking.app.dto.payment.PaymentResponseDto;
import booking.app.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface PaymentMapper {
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "paymentMethodType", source = "paymentMethodType")
    PaymentResponseDto toDto(Payment payment, String paymentMethodType, String currency);
}
