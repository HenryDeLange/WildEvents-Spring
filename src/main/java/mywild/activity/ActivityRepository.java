package mywild.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.azure.spring.data.cosmos.repository.CosmosRepository;

@Repository
public interface ActivityRepository extends CosmosRepository<ActivityEntity, String> {

    Page<ActivityEntity> findAllByEventIdOrderByNameAsc(String eventId, Pageable pageable);

    int countByEventId(String eventId);

}
