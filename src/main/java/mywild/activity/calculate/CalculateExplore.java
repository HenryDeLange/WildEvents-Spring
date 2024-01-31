package mywild.activity.calculate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
public class CalculateExplore extends CalculateAbstract {

    @Value("${mywild.wildevents.max-activity-steps}")
    private int maxSteps;

    /**
     * Calculate the results of an Activity that is an "exploration" by making observations in grids.
     * 
     * Recommended iNat Query Params:
     *   - (REQUIRED) nelat
     *   - (REQUIRED) nelng
     *   - (REQUIRED) swlat
     *   - (REQUIRED) swlng
     *   - taxon_id
     *   - captive
     *   - introduced
     *   - threatened
     *   - verifiable
     *   - quality_grade
     *   - without_taxon_id
     * 
     * Unsupported iNat Query Params:
     *   - lat
     *   - lng
     *   - radius
     */
    @Override
    public ActivityEntity calculate(EventEntity event, ActivityEntity activity) {
        // Validate
        if (activity.getCriteria().size() > maxSteps)
            throw new BadRequestException("The Explore Activity cannot have more than " + maxSteps + " steps.");
        for (Map<String, String> criteria : activity.getCriteria()) {
            Set<String> queryParamKeys = criteria.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
            if (!queryParamKeys.contains("nelat") || !criteria.keySet().contains("nelng")
                    || !criteria.keySet().contains("swlat") || !criteria.keySet().contains("swlng"))
                throw new BadRequestException("The Explore Activity requires the 'nelat', 'nelng', 'swlat' and 'swlng' to be specified.");
            if (queryParamKeys.contains("lat") || criteria.keySet().contains("lng") || criteria.keySet().contains("radius"))
                throw new BadRequestException("The Explore Activity does not support the use of 'lat', 'lng' and 'radius', use 'nelat', 'nelng', 'swlat' and 'swlng' instead.");
        }
        // Process the steps
        Set<String> participants = event.getParticipants().stream().map(String::toLowerCase).collect(Collectors.toSet());
        activity.setResults(new ArrayList<>(activity.getCriteria().size()));
        for (Map<String, String> criteria : activity.getCriteria()) {
            // Fetch the data
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("observations");
            criteria.entrySet().forEach(entry -> builder.queryParam(entry.getKey(), entry.getValue()));
            Observations fetchResults = restClient
                    .get()
                    .uri(configureBaseQueryParams(builder, event).toUriString())
                    .retrieve()
                    .body(Observations.class);
            // Do the calculation
            if (checkMaxResults(activity, builder, fetchResults.total_results())) {
                Map<String, ActivityCalculation> calculationResults = new HashMap<>(participants.size());
                double neLat = Double.parseDouble(criteria.get("nelat"));
                double neLng = Double.parseDouble(criteria.get("nelng"));
                double swLat = Double.parseDouble(criteria.get("swlat"));
                double swLng = Double.parseDouble(criteria.get("swlng"));
                double deltaLat = (neLat - swLat) / 2.0;
                double deltaLng = (neLng - swLng) / 2.0;
                Set<String> q1 = new HashSet<>(participants.size());
                Set<String> q2 = new HashSet<>(participants.size());
                Set<String> q3 = new HashSet<>(participants.size());
                Set<String> q4 = new HashSet<>(participants.size());
                for (Observation observation : fetchResults.results()) {
                    String participant = observation.user().login().toLowerCase();
                    ActivityCalculation activityCalculation = calculationResults.getOrDefault(participant, new ActivityCalculation(0, new ArrayList<>(4)));
                    String[] location = observation.location().split(",");
                    double obsLat = Double.parseDouble(location[0]);
                    double obsLng = Double.parseDouble(location[1]);
                    if (!q1.contains(participant)
                            && obsLat <= neLat && obsLat > (neLat - deltaLat)
                            && obsLng <= neLng && obsLng >= (neLng - deltaLng)) {
                        q1.add(participant);
                        activityCalculation.setScore(activityCalculation.getScore() + 1);
                        activityCalculation.getObservations().add(observation.id());
                    }
                    if (!q2.contains(participant)
                            && obsLat <= (swLat + deltaLat) && obsLat >= swLat
                            && obsLng <= neLng && obsLng > (neLng - deltaLng)) {
                        q2.add(participant);
                        activityCalculation.setScore(activityCalculation.getScore() + 1);
                        activityCalculation.getObservations().add(observation.id());
                    }
                    if (!q3.contains(participant)
                            && obsLat < (swLat + deltaLat) && obsLat >= swLat
                            && obsLng <= (swLng + deltaLng) && obsLng >= swLng) {
                        q3.add(participant);
                        activityCalculation.setScore(activityCalculation.getScore() + 1);
                        activityCalculation.getObservations().add(observation.id());
                    }
                    if (!q4.contains(participant)
                            && obsLat <= neLat && obsLat >= (neLat - deltaLat)
                            && obsLng < (swLng + deltaLng) && obsLng >= swLng) {
                        q4.add(participant);
                        activityCalculation.setScore(activityCalculation.getScore() + 1);
                        activityCalculation.getObservations().add(observation.id());
                    }
                }
                if (calculationResults.size() < participants.size()) {
                    for (String participant : participants) {
                        calculationResults.putIfAbsent(participant, new ActivityCalculation(0, null));
                    }
                }
                activity.getResults().add(calculationResults);
            }
        }
        return activity;
    }

}
