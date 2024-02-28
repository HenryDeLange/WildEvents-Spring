package mywild.activity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import mywild.activity.calculate.CalculateService;
import mywild.core.error.BadRequestException;
import mywild.core.error.ForbiddenException;
import mywild.core.error.NotFoundException;
import mywild.event.EventEntity;
import mywild.event.EventRepository;
import mywild.event.EventUtils;
import mywild.event.EventVisibilityType;
import mywild.user.UserEntity;
import mywild.user.UserRepository;

@Validated
@Service
public class ActivityService {

    @Value("${mywild.wildevents.page-size}")
    private int pageSize;

    @Value("${mywild.wildevents.max-activities-per-event}")
    private int maxActivitiesPerEvent;

    @Autowired
    private ActivityRepository repo;

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CalculateService calculateService;

    public @Valid List<Activity> findActivities(@NotNull String userId, @NotNull String eventId) {
        UserEntity validUser = getValidUser(userId);
        EventEntity validEvent = getValidEvent(validUser, eventId, true); // Validate event
        List<ActivityEntity> entities = repo.findAllByEventIdOrderByNameAsc(validEvent.getId());
        return entities.stream().map(ActivityMapper.INSTANCE::entityToDto).toList();
    }

    public @Valid Activity findActivity(@NotNull String userId, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<ActivityEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Activity not found!");
        ActivityEntity entity = foundEntity.get();
        getValidEvent(validUser, entity.getEventId(), true); // Validate event
        return ActivityMapper.INSTANCE.entityToDto(entity);
    }

    public @Valid Activity createActivity(@NotNull String userId, @Valid ActivityCreate activityCreate) {
        UserEntity validUser = getValidUser(userId);
        EventEntity validEvent = getValidEvent(validUser, activityCreate.getEventId(), false);
        checkThatEventCanBeModified(validUser, validEvent);
        if (repo.countByEventId(validEvent.getId()) >= maxActivitiesPerEvent)
            throw new BadRequestException("No more Activities can be added to this Event!");
        lowercaseCriteria(activityCreate);
        return ActivityMapper.INSTANCE.entityToDto(repo
                .save(ActivityMapper.INSTANCE.dtoToEntity(ActivityMapper.INSTANCE.createDtoToFullDto(activityCreate))));
    }

    public @Valid Activity updateActivity(@NotNull String userId, @NotNull String id,
            @Valid ActivityBase activityBase) {
        UserEntity validUser = getValidUser(userId);
        Optional<ActivityEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Activity to update!");
        ActivityEntity entity = foundEntity.get();
        EventEntity validEvent = getValidEvent(validUser, entity.getEventId(), false);
        checkThatEventCanBeModified(validUser, validEvent);
        lowercaseCriteria(activityBase);
        return ActivityMapper.INSTANCE
                .entityToDto(repo.save(ActivityMapper.INSTANCE.dtoToExistingEntity(entity, activityBase)));
    }

    public void deleteActivity(@NotNull String userId, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<ActivityEntity> foundEntity = repo.findById(id);
        if (foundEntity.isPresent()) {
            ActivityEntity entity = foundEntity.get();
            EventEntity validEvent = getValidEvent(validUser, entity.getEventId(), false);
            checkThatEventCanBeModified(validUser, validEvent);
            repo.delete(entity);
        }
    }

    public @Valid Activity calculateActivity(@NotNull String userId, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<ActivityEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Activity to calculate!");
        ActivityEntity entity = foundEntity.get();
        EventEntity validEvent = getValidEvent(validUser, entity.getEventId(), false);
        checkThatEventCanBeModified(validUser, validEvent);
        entity = calculateService.calculateActivity(entity);
        return ActivityMapper.INSTANCE.entityToDto(entity);
    }

    public @Valid Activity enableActivity(@NotNull String userId, @NotNull String id) {
        return setDisabledReasonForActivity(userId, id, null);
    }

    public @Valid Activity disableActivity(@NotNull String userId, @NotNull String id) {
        return setDisabledReasonForActivity(userId, id, ActivityDisableReason.ADMIN_DISABLED);
    }

    private Activity setDisabledReasonForActivity(@NotNull String userId, @NotNull String id, ActivityDisableReason reason) {
        UserEntity validUser = getValidUser(userId);
        Optional<ActivityEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Activity to update!");
        ActivityEntity entity = foundEntity.get();
        EventEntity validEvent = getValidEvent(validUser, entity.getEventId(), false);
        checkThatEventCanBeModified(validUser, validEvent);
        entity.setDisableReason(reason);
        return ActivityMapper.INSTANCE.entityToDto(repo.save(entity));
    }

    private void checkThatEventCanBeModified(UserEntity user, EventEntity event) {
        if (!EventUtils.containsName(event.getAdmins(), user.getUsername()))
            throw new ForbiddenException("Activity cannot be modified by this User!");
        if (event.getClose().isBefore(LocalDate.now()))
            throw new BadRequestException("This Event is already finished, it cannot be modified anymore!");
    }

    private EventEntity getValidEvent(UserEntity user, String eventId, boolean canBeParticipant) {
        Optional<EventEntity> foundEntity = eventRepo.findById(eventId);
        if (!foundEntity.isPresent())
            throw new NotFoundException("The Event associated with this Activity cannot be found!");
        EventEntity entity = foundEntity.get();
        if (entity.getVisibility() == EventVisibilityType.PRIVATE && !entity.getAdmins().contains(user.getUsername())
                && !(canBeParticipant && EventUtils.containsName(entity.getParticipants(), user.getInaturalist())))
            throw new ForbiddenException("Event not accessible by this User!");
        return entity;
    }

    private UserEntity getValidUser(String userId) {
        Optional<UserEntity> userEntity = userRepo.findById(userId);
        if (!userEntity.isPresent())
            throw new ForbiddenException("Incorrect User ID!");
        return userEntity.get();
    }

    private void lowercaseCriteria(ActivityBase activityBase) {
        // TODO: Also validate that the criteria has the correct required key-value entries
        activityBase.getSteps().forEach(criteriaMap -> criteriaMap.getCriteria().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue)));
    }

}
