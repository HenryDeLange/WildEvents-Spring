package mywild.activity.calculate;

import java.util.HashMap;
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
 * Calculate the results of an Activity that is a "quiz" by answering steps with the correct observation.
 * 
 * Recommended iNat Query Params:
 *   - (REQUIRED) taxon_id
 *   - captive
 *   - introduced
 *   - verifiable
 *   - quality_grade
 *   - without_taxon_id
 * 
 * Unsupported iNat Query Params:
 *   - taxon_name
 */
@Service
public class CalculateQuiz extends CalculateAbstract {

    @Value("${mywild.wildevents.max-activity-steps}")
    private int maxSteps;

    @Override
    protected void doValidation(ActivityEntity activity) {
        for (Map<String, String> criteria : activity.getCriteria()) {
            Set<String> queryParamKeys = criteria.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
            if (!queryParamKeys.contains("taxon_id"))
                throw new BadRequestException("The Quiz Activity requires the 'taxon_id' to be specified.");
            if (queryParamKeys.contains("taxon_name"))
                throw new BadRequestException("The Quiz Activity does not support the use of the 'taxon_name', use the 'taxon_id' instead.");
        }
    }

    @Override
    protected Map<String, ActivityCalculation> doCalculation(
            List<String> participants, Map<String, String> criteria, List<Observation> observations) {
        Map<String, ActivityCalculation> calculationResults = new HashMap<>(participants.size());
        for (Observation observation : observations) {
            String obsParticipant = observation.user().login().toLowerCase();
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
