package mywild.activity;

import org.springframework.stereotype.Repository;
import com.azure.spring.data.cosmos.repository.CosmosRepository;

@Repository
public interface ActivityRepository extends CosmosRepository<ActivityEntity, String> {

    int countByEventId(String eventId);

}
