package booking.app.model.accommodation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Data
@SQLDelete(sql = "UPDATE accommodations SET is_deleted = TRUE WHERE id = ?")
@Where(clause = "is_deleted = FALSE")
@DynamicUpdate
@Table(name = "accommodations")
public class Accommodation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // тип приміщення
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private Type type;
    // адреса
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;
    // розмір скільки кімнат чи студія тощо...
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private Size size;
    // зручності
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "accommodations_amenities",
            joinColumns = @JoinColumn(name = "accommodation_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities;
    // щоденна ставка
    @Column(name = "daily_rate", nullable = false)
    private BigDecimal dailyRate;
    // кількість наявних апартаментів
    private Integer availability;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public enum Type {
        HOUSE,
        APARTMENT,
        CONDO,
        VACATION_HOME,
        VACATION_APARTMENT,
        COTTAGE,
        TOWNHOUSE
    }

    public enum Size {
        STUDIO,
        ONE_BEDROOM,
        TWO_BEDROOM,
        THREE_BEDROOM,
        FOUR_BEDROOM
    }

}
