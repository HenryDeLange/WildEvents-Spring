package mywild.activity.calculate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     *   - taxon_id (required)
     *   - without_taxon_id
     *   - place_id
     *   - project_id
     *   - captive
     *   - introduced
     *   - threatened
     *   - verifiable
     *   - quality_grade
     *   - preferred_place_id
     */
    @Override
    public ActivityEntity calculate(EventEntity event, ActivityEntity activity) {
        if (!activity.getCriteria().keySet().contains("taxon_id"))
            throw new BadRequestException("The Race Activity requires the 'taxon_id' to be specified.");
        // Fetch the data
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("observations");
        activity.getCriteria().entrySet()
            .forEach((entry) -> builder.queryParam(entry.getKey(), entry.getValue()));
        Observations results = restClient
                .get()
                .uri(configureBaseQueryParams(builder, event).toUriString())
                .retrieve()
                .body(Observations.class); // To see the full JSON use: body(String.class)
        if (checkMaxResults(activity, builder, results.total_results())) {
            // Do the calculation
            List<String> activityTaxa = new ArrayList<>(Arrays.asList(activity.getCriteria().get("taxon_id").split(",")));
            Map<String, List<Observation>> qualifiedObservations = new HashMap<>(activityTaxa.size());
            for (Observation observation : results.results()) {
                for (String ancestor : observation.taxon().min_species_ancestry().split(",")) {
                    if (activityTaxa.contains(ancestor)) {
                        List<Observation> tempObs = qualifiedObservations.computeIfAbsent(ancestor, key -> new ArrayList<>(POINT_POSITIONS));
                        if (tempObs.size() < POINT_POSITIONS)
                            tempObs.add(observation);
                        else
                            activityTaxa.remove(ancestor);
                    }
                    if (activityTaxa.isEmpty())
                        break;
                }
            }
            activity.setResults(new HashMap<>(event.getParticipants().size()));
            for (String participant : event.getParticipants()) {
                activity.getResults().put(participant, new ActivityCalculation(0, null));
            }
            for (List<Observation> observations : qualifiedObservations.values()) {
                for (int i = 0; i < observations.size(); i++) {
                    Observation observation = observations.get(i);
                    ActivityCalculation calculation = activity.getResults().get(observation.user().login());
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
