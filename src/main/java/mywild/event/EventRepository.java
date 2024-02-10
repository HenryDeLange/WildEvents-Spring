package mywild.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;

@Repository
public interface EventRepository extends CosmosRepository<EventEntity, String> {

    // TODO: The contains admin part (and ordering?) doesn't work... maybe easiest to just turn this into a string when storing it?

    Page<EventEntity> findAllByVisibilityOrAdminsContainsIgnoreCaseOrParticipantsContainsIgnoreCaseOrderByStartDescNameAsc(
        EventVisibilityType visibility, String adminId, String iNatId, Pageable pageable);

}
