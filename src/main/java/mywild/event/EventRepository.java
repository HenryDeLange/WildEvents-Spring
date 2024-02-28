package mywild.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;

@Repository
public interface EventRepository extends CosmosRepository<EventEntity, String> {

    Page<EventEntity> findAllByVisibilityOrAdminsContainsIgnoreCaseOrParticipantsContainsIgnoreCaseOrderByStartAscNameAsc(
        EventVisibilityType visibility, String adminId, String iNatId, Pageable pageable);

}
