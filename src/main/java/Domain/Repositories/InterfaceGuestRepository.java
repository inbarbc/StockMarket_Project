package Domain.Repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import Domain.Entities.Guest;

public interface InterfaceGuestRepository extends JpaRepository<Guest, Integer> {
    boolean existsByGuestId(String guestId);
    void deleteByGuestId(String guestId);
    Guest findByGuestId(String id);
}