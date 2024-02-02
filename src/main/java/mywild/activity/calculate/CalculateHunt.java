package mywild.activity.calculate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import mywild.activity.ActivityCalculation;
import mywild.activity.ActivityEntity;
import mywild.activity.calculate.inaturalist.Observation;
import mywild.core.error.BadRequestException;

/**
 * Calculate the results of an Activity that is a "treasure hunt" by solving steps leading towards the final treasure.
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
 *   - without_taxon_id
 * 
 * Unsupported iNat Query Params:
 *   - taxon_name
 *   - nelat
 *   - nelng
 *   - swlat
 *   - swlng
 */
@Service
public class CalculateHunt extends CalculateAbstract {

    @Override
    protected void doValidation(ActivityEntity activity) {
        for (Map<String, String> criteria : activity.getCriteria()) {
            Set<String> queryParamKeys = criteria.keySet();
            if (!queryParamKeys.contains("taxon_id"))
                throw new BadRequestException("The Hunt Activity requires the 'taxon_id' to be specified.");
            if (!queryParamKeys.contains("lat") || !criteria.keySet().contains("lng") || !criteria.keySet().contains("radius"))
                throw new BadRequestException("The Hunt Activity requires the 'lat', 'lng' and 'radius' to be specified.");
            if (queryParamKeys.contains("taxon_name"))
                throw new BadRequestException("The Hunt Activity does not support the use of the 'taxon_name', use the 'taxon_id' instead.");
            if (queryParamKeys.contains("nelat") || criteria.keySet().contains("nelng")
                    || criteria.keySet().contains("swlat") || criteria.keySet().contains("swlng"))
                throw new BadRequestException("The Hunt Activity does not support the use of 'nelat', 'nelng', 'swlat' or 'swlng', use 'lat', 'lng' and 'radius' instead.");
        }
    }

    @Override
    protected Map<String, ActivityCalculation> doCalculation(
            List<String> participants, Map<String, String> criteria, List<Observation> observations) {
        Map<String, ActivityCalculation> calculationResults = new HashMap<>(participants.size());
        for (Observation observation : observations) {
            String obsParticipant = observation.user().login().toLowerCase();
                // TODO: Maybe only gain half the points if the previous steps weren't completed?
            calculationResults.putIfAbsent(obsParticipant, new ActivityCalculation(1, List.of(observation.id())));
        }
        if (calculationResults.size() < participants.size()) {
            for (String participant : participants) {
                calculationResults.putIfAbsent(participant, new ActivityCalculation(0, null));
            }
        }
        return calculationResults;
    }

}
