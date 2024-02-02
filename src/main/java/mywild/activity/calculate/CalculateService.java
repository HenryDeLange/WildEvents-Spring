package mywild.activity.calculate;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.azure.cosmos.implementation.NotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import mywild.activity.ActivityEntity;
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
    private CalculateRace calculateRace;

    @Autowired
    private CalculateHunt calculateHunt;

    @Autowired
    private CalculateQuiz calculateQuiz;

    @Autowired
    private CalculateExplore calculateExplore;

    // TODO: Implement better queuing than using synchronized
    //       add activities to a list/queue [LinkedBlockingQueue] and use schedular [@Scheduled(fixedRate = 1000)]
    //       but aso how to limit the 5 inat calls per activity?? (maybe just space activity schedules 5 seconds apart?) 

    public synchronized ActivityEntity calculateActivity(@NotNull ActivityEntity activity) {
        log.debug("Preparing to calculate activity ({})", activity.getId());
        if (activity.getDisableReason() == null) {
            Optional<EventEntity> foundEvent = eventRepo.findById(activity.getEventId());
            if (!foundEvent.isPresent())
                throw new NotFoundException("The Event associated with this Activity cannot be found!");
            EventEntity event = foundEvent.get();
            switch (activity.getType()) {
                case RACE:
                    activity = calculateRace.process(event, activity);
                    break;
                case HUNT:
                    activity = calculateHunt.process(event, activity);
                    break;
                case QUIZ:
                    activity = calculateQuiz.process(event, activity);
                    break;
                case EXPLORE:
                    activity = calculateExplore.process(event, activity);
                    break;
                default:
                    throw new BadRequestException("Could not calculate the Activity!");
            }
        }
        else {
            log.info("Skipped calculating of disabled ({}) Activity ({}).", activity.getDisableReason(), activity.getId());
        }
        log.debug("Finished calculating activity ({})", activity.getId());
        return activity;
    }

}
