package mywild.activity.calculate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import mywild.activity.ActivityCalculation;
import mywild.activity.ActivityEntity;
import mywild.activity.ActivityStep;
import mywild.activity.ActivityStepResult;
import mywild.activity.calculate.inaturalist.Observation;
import mywild.core.error.BadRequestException;

/**
 * Calculate the results of an Activity that is a "race" to observe a taxon before other users.
 * 
 * Recommended iNat Query Params:
 *   - (REQUIRED) taxon_id
 *   - place_id
 *   - project_id
 *   - captive
 *   - introduced
 *   - threatened
 *   - verifiable
 *   - quality_grade
 *   - without_taxon_id
 * 
 * Unsupported iNat Query Params:
 *   - taxon_name
 */
@Service
public class CalculateRace extends CalculateAbstract {

    private static final int POINT_POSITIONS = 3;

    @Override
    protected void doValidation(ActivityEntity activity) {
        Set<String> queryParamKeys = activity.getSteps().get(0).getCriteria().keySet();
        if (!queryParamKeys.contains("taxon_id"))
            throw new BadRequestException("The Race Activity requires the 'taxon_id' to be specified.");
        if (queryParamKeys.contains("taxon_name"))
            throw new BadRequestException("The Race Activity does not support the use of the 'taxon_name', use the 'taxon_id' instead.");
    }

    @Override
    protected ActivityStepResult doCalculation(List<String> participants, ActivityStep step, List<Observation> observations) {
        List<Observation> scoringObservations = new ArrayList<>(POINT_POSITIONS);
        Set<String> scoringParticipants = new HashSet<>(POINT_POSITIONS);
        for (Observation observation : observations) {
            String obsParticipant = observation.user().login().toLowerCase();
            if (!scoringParticipants.contains(obsParticipant)) {
                scoringParticipants.add(obsParticipant);
                scoringObservations.add(observation);
            }
            if (scoringObservations.size() >= POINT_POSITIONS)
                break;
        }
        Map<String, ActivityCalculation> calculationResults = new HashMap<>(participants.size());
        for (String participant : participants) {
            calculationResults.put(participant, new ActivityCalculation(0, null));
        }
        for (int i = 0; i < scoringObservations.size(); i++) {
            Observation observation = scoringObservations.get(i);
            String obsParticipant = observation.user().login().toLowerCase();
            ActivityCalculation calculation = calculationResults.get(obsParticipant);
            calculation.setScore(calculation.getScore() + (POINT_POSITIONS - i));
            if (calculation.getObservations() == null)
                calculation.setObservations(new ArrayList<>(POINT_POSITIONS));
            calculation.getObservations().add(observation.id());
        }
        return new ActivityStepResult(step.getId(), calculationResults);
    }

}
