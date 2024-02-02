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
import mywild.activity.ActivityCalculation;
import mywild.activity.ActivityEntity;
import mywild.activity.calculate.inaturalist.Observation;
import mywild.core.error.BadRequestException;

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
@Service
public class CalculateExplore extends CalculateAbstract {

    @Value("${mywild.wildevents.max-activity-steps}")
    private int maxSteps;

    @Override
    protected void doValidation(ActivityEntity activity) {
        for (Map<String, String> criteria : activity.getCriteria()) {
            Set<String> queryParamKeys = criteria.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
            if (!queryParamKeys.contains("nelat") || !criteria.keySet().contains("nelng")
                    || !criteria.keySet().contains("swlat") || !criteria.keySet().contains("swlng"))
                throw new BadRequestException("The Explore Activity requires the 'nelat', 'nelng', 'swlat' and 'swlng' to be specified.");
            if (queryParamKeys.contains("lat") || criteria.keySet().contains("lng") || criteria.keySet().contains("radius"))
                throw new BadRequestException("The Explore Activity does not support the use of 'lat', 'lng' and 'radius', use 'nelat', 'nelng', 'swlat' and 'swlng' instead.");
        }
    }

    @Override
    protected Map<String, ActivityCalculation> doCalculation(
            List<String> participants, Map<String, String> criteria, List<Observation> observations) {
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
        for (Observation observation : observations) {
            String participant = observation.user().login().toLowerCase();
            ActivityCalculation activityCalculation = calculationResults.get(participant);
            if (activityCalculation == null) {
                activityCalculation = new ActivityCalculation(0, new ArrayList<>(4));
                calculationResults.put(participant, activityCalculation);
            }
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
        return calculationResults;
    }

}
