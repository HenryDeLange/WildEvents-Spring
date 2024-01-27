package mywild.activity.calculate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;
import mywild.activity.ActivityDisableReason;
import mywild.activity.ActivityEntity;
import mywild.event.EventEntity;

@Slf4j
@Service
public abstract class CalculateAbstract {

    protected RestClient restClient = RestClient.create("https://api.inaturalist.org/v1/");

    @Value("${mywild.wildevents.max-inat-results-per-activity}")
    protected int maxResults;

    public abstract ActivityEntity calculate(EventEntity event, ActivityEntity activity);

    protected UriComponentsBuilder configureBaseQueryParams(UriComponentsBuilder builder, EventEntity event) {
        return builder
            .queryParam("order", "desc")
            .queryParam("order_by", "observed_on")
            .queryParam("d1", event.getStart())
            .queryParam("d2", event.getStop())
            .queryParam("user_id", event.getParticipants().toString().replace("[", "").replace("]", "").replace(" ", ""));
    }

    protected boolean checkMaxResults(ActivityEntity activity, UriComponentsBuilder builder, int totalResults) {
        if (totalResults > maxResults) {
            log.error("This Activity ({}) needs to fetch {} results, but only {} are allowed. Query: {}", 
            activity.getId(), totalResults, maxResults, builder.toUriString());
            activity.setDisableReason(ActivityDisableReason.TOO_MANY_RESULTS);
            return false;
        }
        return true;
    }
    
}
