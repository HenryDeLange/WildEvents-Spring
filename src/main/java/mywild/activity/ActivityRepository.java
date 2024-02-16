package mywild.activity;

import java.util.List;
import org.springframework.stereotype.Repository;
import com.azure.spring.data.cosmos.repository.CosmosRepository;

@Repository
public interface ActivityRepository extends CosmosRepository<ActivityEntity, String> {

    List<ActivityEntity> findAllByEventIdOrderByNameAsc(String eventId);

    int countByEventId(String eventId);

}
