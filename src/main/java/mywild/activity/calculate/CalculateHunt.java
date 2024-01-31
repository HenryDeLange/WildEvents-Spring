package mywild.activity.calculate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;
import mywild.activity.ActivityCalculation;
import mywild.activity.ActivityEntity;
import mywild.activity.calculate.inaturalist.Observation;
import mywild.activity.calculate.inaturalist.Observations;
import mywild.core.error.BadRequestException;
import mywild.event.EventEntity;

@Slf4j
@Service
public class CalculateHunt extends CalculateAbstract {

    @Value("${mywild.wildevents.max-activity-steps}")
    private int maxSteps;

    /**
     * Calculate the results of an Activity that is a "treasure hunt" to solve steps leading towards the final challenge.
     * 
     * Recommended iNat Query Params:
     *   - (REQUIRED) taxon_id
     *   - (REQUIRED) lat
     *   - (REQUIRED) lng
     *   - (REQUIRED) radius
     *   - captive
     *   - introduced
     *   - threatened
     *   - verifiable
     *   - quality_grade
     *   - preferred_place_id
     *   - without_taxon_id
     * 
     * Unsupported iNat Query Params:
     *   - taxon_name
     *   - nelat
     *   - nelng
     *   - swlat
     *   - swlng
     */
    @Override
    public ActivityEntity calculate(EventEntity event, ActivityEntity activity) {
        // Validate
        if (activity.getCriteria().size() > maxSteps)
            throw new BadRequestException("The Hunt Activity cannot have more than " + maxSteps + " steps.");
        for (Map<String, String> criteria : activity.getCriteria()) {
            Set<String> queryParamKeys = criteria.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
            if (!queryParamKeys.contains("taxon_id"))
                throw new BadRequestException("The Hunt Activity requires the 'taxon_id' to be specified.");
            if (!queryParamKeys.contains("lat") || !criteria.keySet().contains("lng") || !criteria.keySet().contains("radius"))
                throw new BadRequestException("The Hunt Activity requires the 'lat', 'lng' and 'radius' to be specified.");
            if (queryParamKeys.contains("taxon_name"))
                throw new BadRequestException("The Hunt Activity does not support the use of the 'taxon_name', use the 'taxon_id' instead.");
            if (queryParamKeys.contains("nelat") || criteria.keySet().contains("nelng")
                    || criteria.keySet().contains("swlat") || criteria.keySet().contains("swlng"))
                throw new BadRequestException("The Race Activity does not support the use of 'nelat', 'nelng', 'swlat' or 'swlng', use 'lat', 'lng' and 'radius' instead.");
        }
        // Process the steps
        Set<String> participants = event.getParticipants().stream().map(String::toLowerCase).collect(Collectors.toSet());
        activity.setResults(new ArrayList<>(activity.getCriteria().size()));
        for (Map<String, String> criteria : activity.getCriteria()) {
            // Fetch the data
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("observations");
            criteria.entrySet().forEach(entry -> builder.queryParam(entry.getKey(), entry.getValue()));
            // TODO: This should be throttled
            Observations fetchResults = restClient
                    .get()
                    .uri(configureBaseQueryParams(builder, event).toUriString())
                    .retrieve()
                    .body(Observations.class);
            // Do the calculation
            Map<String, ActivityCalculation> calculationResults = new HashMap<>(participants.size());
            if (checkMaxResults(activity, builder, fetchResults.total_results())) {
                for (Observation observation : fetchResults.results()) {
                    String obsParticipant = observation.user().login().toLowerCase();
                    if (participants.contains(obsParticipant)) {
                        calculationResults.putIfAbsent(obsParticipant, new ActivityCalculation(1, List.of(observation.id())));
                    }
                }
            }
            if (calculationResults.size() < participants.size()) {
                for (String participant : participants) {
                    calculationResults.putIfAbsent(participant, new ActivityCalculation(0, null));
                }
            }
            activity.getResults().add(calculationResults);
        }
        return activity;
    }

}
