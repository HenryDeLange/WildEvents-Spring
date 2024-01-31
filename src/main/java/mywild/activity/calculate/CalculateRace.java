package mywild.activity.calculate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
public class CalculateRace extends CalculateAbstract {

    private static final int POINT_POSITIONS = 3;

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
     *   - preferred_place_id
     *   - without_taxon_id
     * 
     * Unsupported iNat Query Params:
     *   - taxon_name
     */
    @Override
    public ActivityEntity calculate(EventEntity event, ActivityEntity activity) {
        // Validate
        if (activity.getCriteria().size() != 1)
            throw new BadRequestException("The Race Activity must have 1 step (only).");
        Map<String, String> criteria = activity.getCriteria().get(0);
        Set<String> queryParamKeys = criteria.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
        if (!queryParamKeys.contains("taxon_id"))
            throw new BadRequestException("The Race Activity requires the 'taxon_id' to be specified.");
        if (queryParamKeys.contains("taxon_name"))
            throw new BadRequestException("The Race Activity does not support the use of the 'taxon_name', use the 'taxon_id' instead.");
        // Process the step
        // Fetch the data
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("observations");
        criteria.entrySet().forEach(entry -> builder.queryParam(entry.getKey(), entry.getValue()));
        Observations fetchResults = restClient
                .get()
                .uri(configureBaseQueryParams(builder, event).toUriString())
                .retrieve()
                .body(Observations.class); // To see the full JSON use: body(String.class)
        if (checkMaxResults(activity, builder, fetchResults.total_results())) {
            // Do the calculation
            List<String> activityTaxa = new ArrayList<>(Arrays.asList(criteria.get("taxon_id").split(",")));
            Map<String, List<Observation>> qualifiedObservations = new HashMap<>(activityTaxa.size());
            for (Observation observation : fetchResults.results()) {
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
            Map<String, ActivityCalculation> calculationResults = new HashMap<>(event.getParticipants().size());
            activity.setResults(List.of(calculationResults));
            for (String participant : event.getParticipants()) {
                calculationResults.put(participant, new ActivityCalculation(0, null));
            }
            for (List<Observation> observations : qualifiedObservations.values()) {
                for (int i = 0; i < observations.size(); i++) {
                    Observation observation = observations.get(i);
                    ActivityCalculation calculation = calculationResults.get(observation.user().login());
                    if (calculation.getObservations() == null)
                        calculation.setObservations(new ArrayList<>(POINT_POSITIONS));
                    calculation.setScore(calculation.getScore() + (POINT_POSITIONS - i));
                    calculation.getObservations().add(observation.id());
                }
            }
        }
        return activity;
    }

}
