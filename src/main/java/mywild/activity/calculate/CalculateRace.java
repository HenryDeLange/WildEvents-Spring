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
        if (activity.getSteps().size() != 1)
            throw new BadRequestException("The Race Activity must have 1 step (only).");
        Set<String> queryParamKeys = activity.getSteps().get(0).getCriteria().keySet();
        if (!queryParamKeys.contains("taxon_id"))
            throw new BadRequestException("The Race Activity requires the 'taxon_id' to be specified.");
        if (queryParamKeys.contains("taxon_name"))
            throw new BadRequestException("The Race Activity does not support the use of the 'taxon_name', use the 'taxon_id' instead.");
    }

    @Override
    protected ActivityStepResult doCalculation(List<String> participants, ActivityStep step, List<Observation> observations) {
        List<String> activityTaxa = new ArrayList<>(Arrays.asList(step.getCriteria().get("taxon_id").split(",")));
        Map<String, List<Observation>> qualifiedObservations = new HashMap<>(activityTaxa.size());
        for (Observation observation : observations) {
            for (String ancestor : observation.taxon().min_species_ancestry().split(",")) {
                if (activityTaxa.contains(ancestor)) {
                    List<Observation> tempObservations = qualifiedObservations.computeIfAbsent(ancestor, key -> new ArrayList<>(POINT_POSITIONS));
                    Set<String> users = new HashSet<>(tempObservations.size());
                    for (Observation tempObservation : tempObservations) {
                        users.add(tempObservation.user().login().toLowerCase());
                    }
                    if (!users.contains(observation.user().login().toLowerCase())) {
                        tempObservations.add(observation);
                    }
                    if (tempObservations.size() >= POINT_POSITIONS)
                        activityTaxa.remove(ancestor);
                }
                if (activityTaxa.isEmpty())
                    break;
            }
        }
        Map<String, ActivityCalculation> calculationResults = new HashMap<>(participants.size());
        for (String participant : participants) {
            calculationResults.put(participant, new ActivityCalculation(0, null));
        }
        for (List<Observation> qualifiedObs : qualifiedObservations.values()) {
            for (int i = 0; i < qualifiedObs.size(); i++) {
                Observation observation = qualifiedObs.get(i);
                ActivityCalculation calculation = calculationResults.get(observation.user().login());
                if (calculation.getObservations() == null)
                    calculation.setObservations(new ArrayList<>(POINT_POSITIONS));
                calculation.setScore(calculation.getScore() + (POINT_POSITIONS - i));
                calculation.getObservations().add(observation.id());
            }
        }
        return new ActivityStepResult(step.getId(), calculationResults);
    }

}
