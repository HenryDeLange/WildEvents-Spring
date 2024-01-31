package mywild.activity.calculate;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.azure.cosmos.implementation.NotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import mywild.activity.ActivityEntity;
import mywild.activity.ActivityRepository;
import mywild.core.error.BadRequestException;
import mywild.event.EventEntity;
import mywild.event.EventRepository;

@Slf4j
@Validated
@Service
public class CalculateService {

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private CalculateRace calculateRace;

    @Autowired
    private CalculateHunt calculateHunt;

    @Autowired
    private CalculateQuiz calculateQuiz;

    @Autowired
    private CalculateExplore calculateExplore;

    // TODO: Schedule/throttle this to keep to the iNat recommendations (one per second should be fine, update a status to show pending/busy/done)

    public void calculateActivity(@NotNull ActivityEntity activity) {
        // TODO: Catch any errors and update status to show that it crashed, don't rerun unless user makes changes
        if (activity.getDisableReason() == null) {
            Optional<EventEntity> foundEvent = eventRepo.findById(activity.getEventId());
            if (!foundEvent.isPresent())
                throw new NotFoundException("The Event associated with this Activity cannot be found!");
            EventEntity event = foundEvent.get();
            switch (activity.getType()) {
                case RACE:
                    activity = calculateRace.calculate(event, activity);
                    break;
                case HUNT:
                    activity = calculateHunt.calculate(event, activity);
                    break;
                case QUIZ:
                    activity = calculateQuiz.calculate(event, activity);
                    break;
                case EXPLORE:
                    activity = calculateExplore.calculate(event, activity);
                    break;
                default:
                    throw new BadRequestException("Could not calculate the Activity!");
            }
            activity.setCalculated(ZonedDateTime.now());
            activityRepo.save(activity);
        }
        else {
            log.info("Skipped calculating of disabled ({}) Activity ({}).", activity.getDisableReason(), activity.getId());
        }
    }

}
