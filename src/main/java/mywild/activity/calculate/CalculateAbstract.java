package mywild.activity.calculate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;
import mywild.activity.ActivityDisableReason;
import mywild.activity.ActivityEntity;
import mywild.activity.ActivityRepository;
import mywild.activity.ActivityStatus;
import mywild.activity.ActivityStep;
import mywild.activity.ActivityStepResult;
import mywild.activity.calculate.inaturalist.Observation;
import mywild.activity.calculate.inaturalist.Observations;
import mywild.core.error.BadRequestException;
import mywild.event.EventEntity;

// TODO: Add unit tests for all the CalculateAbstract implementions

@Slf4j
@Service
public abstract class CalculateAbstract {

    protected RestClient restClient = RestClient.create("https://api.inaturalist.org/v1/");

    @Value("${mywild.wildevents.max-inat-results-per-activity}")
    protected int maxResults;

    @Value("${mywild.wildevents.max-activity-steps}")
    private int maxSteps;

    @Value("${mywild.wildevents.inat-results-per-page}")
    private int perPage;

    @Autowired
    private ActivityRepository activityRepo;

    public final ActivityEntity process(EventEntity event, ActivityEntity activity) {
        try {
            // Start
            activity.setResults(new ArrayList<>(maxSteps));
            activity.setCalculated(null);
            activity.setDisableReason(null);
            activity = saveStatus(activity, ActivityStatus.CALCULATING);
            // Validate Activity
            validate(activity);
            // Process each Criteria
            List<String> participants = Arrays.asList(event.getParticipants().replace("#", "").split(","));
            processing:
            for (ActivityStep step : activity.getSteps()) {
                // Fetch Data
                log.debug("Preparing to fetch all observations from iNat...");
                int totalResults = maxResults;
                int page = 1; // Starts at 1 not 0
                List<Observation> observations = new ArrayList<>();
                while (page * perPage < totalResults) {
                    Observations observationsPage = fetch(event, step, page++);
                    if (observationsPage.total_results() <= maxResults) {
                        observations.addAll(observationsPage.results());
                    }
                    else {
                        log.warn("This Activity ({}) needs to fetch {} results, but only {} are allowed.", 
                            activity.getId(), observationsPage.results().size(), maxResults);
                        activity.setCalculated(null);
                        activity.setDisableReason(ActivityDisableReason.TOO_MANY_RESULTS);
                        activity = saveStatus(activity, ActivityStatus.ERROR);
                        break processing;
                    }
                    totalResults = Math.min(observationsPage.total_results(), maxResults);
                }
                log.debug("Done fetching all observations from iNat");
                // Calculate Results
                log.debug("Preparing to calculate {} observations...", observations.size());
                activity.getResults().add(doCalculation(participants, step, observations));
                activity.setCalculated(ZonedDateTime.now());
                activity = saveStatus(activity, ActivityStatus.CALCULATED);
                log.debug("Calculated");
            }
        }
        catch (Throwable ex) {
            log.error("Failed to calculate the Activity ({})!", activity.getId());
            log.error("Calculation Error!", ex);
            activity.setCalculated(null);
            activity.setDisableReason(ActivityDisableReason.FAILED_TO_CALCULATE);
            activity = saveStatus(activity, ActivityStatus.ERROR);
        }
        return activity;
    }

    private ActivityEntity saveStatus(ActivityEntity activity, ActivityStatus status) {
        activity.setStatus(status);
        activityRepo.save(activity);
        return activityRepo.findById(activity.getId()).get();
    }

    private void validate(ActivityEntity activity) {
        if (activity.getSteps().isEmpty())
            throw new BadRequestException("The Activity must have at least 1 step.");
        if (activity.getSteps().size() > maxSteps)
            throw new BadRequestException("The Activity cannot have more than " + maxSteps + " steps.");
        doValidation(activity);
    }

    private Observations fetch(EventEntity event, ActivityStep step, int page) {
        // TODO: Implement better throttling than sleeping the thread
        try {
            log.debug("Sleep before fetching, in order to limit to the requests per minute sent to iNaturalist...");
            Thread.sleep(1500);
        }
        catch (InterruptedException ex) {
            log.warn(ex.getMessage(), ex);
        }
        log.debug("Fetching page {}...", page);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("observations");
        step.getCriteria().entrySet().forEach(entry -> builder.queryParam(entry.getKey(), entry.getValue()));
        try {
            return restClient
                .get()
                .uri(configureBaseQueryParams(builder, event, page))
                .retrieve()
                .body(Observations.class); // To see the full JSON use: body(String.class)
        }
        catch (Throwable ex) {
            log.error("Failed Query: {}", "https://api.inaturalist.org/v1/" + builder.toUriString());
            throw ex;
        }
    }

    private String configureBaseQueryParams(UriComponentsBuilder builder, EventEntity event, int page) {
        String inatUri = builder
            .queryParam("page", page)
            .queryParam("per_page", perPage)
            .queryParam("order", "asc")
            .queryParam("order_by", "observed_on")
            .queryParam("d1", event.getStart())
            .queryParam("d2", event.getStop())
            .queryParam("user_id", event.getParticipants().replace("#", ""))
            .toUriString();
        log.info("Fetching results from iNaturalist using URL: {}", inatUri);
        return inatUri;
    }

    protected abstract void doValidation(ActivityEntity activity);

    protected abstract ActivityStepResult doCalculation(List<String> participants, ActivityStep step, List<Observation> observations);
    
}
