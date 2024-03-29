package mywild.activity.calculate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import mywild.activity.ActivityCalculation;
import mywild.activity.ActivityEntity;
import mywild.activity.ActivityStep;
import mywild.activity.ActivityStepResult;
import mywild.activity.calculate.inaturalist.Observation;
import mywild.core.error.BadRequestException;

/**
 * Calculate the results of an Activity that is a "quiz" by answering steps with the correct observation.
 * 
 * Recommended iNat Query Params:
 *   - (REQUIRED) taxon_name
 *   - captive
 *   - introduced
 *   - verifiable
 *   - quality_grade
 *   - without_taxon_name
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
        for (ActivityStep step : activity.getSteps()) {
            Set<String> queryParamKeys = step.getCriteria().keySet();
            if (!queryParamKeys.contains("taxon_name"))
                throw new BadRequestException("The Quiz Activity requires the 'taxon_name' to be specified.");
            if (queryParamKeys.contains("taxon_id"))
                throw new BadRequestException("The Quiz Activity does not support the use of the 'taxon_id', use the 'taxon_name' instead.");
        }
    }

    @Override
    protected ActivityStepResult doCalculation(List<String> participants, ActivityStep step, List<Observation> observations) {
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
        return new ActivityStepResult(step.getId(), calculationResults);
    }

}
