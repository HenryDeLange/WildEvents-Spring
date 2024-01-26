package mywild.activity;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import mywild.core.error.BadRequestException;
import mywild.core.error.ForbiddenException;
import mywild.core.error.NotFoundException;
import mywild.core.rest.Paged;
import mywild.event.EventEntity;
import mywild.event.EventRepository;
import mywild.event.EventVisibilityType;
import mywild.user.UserEntity;
import mywild.user.UserRepository;

@Validated
@Service
public class ActivityService {

    private static final int PAGE_SIZE = 10;

    @Value("${mywild.wildevents.max-activities-per-event}")
    private int maxActivitiesPErEvent;

    @Autowired
    private ActivityRepository repo;

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private UserRepository userRepo;

    public @Valid Paged<Activity> findActivities(@NotNull String userId, @NotNull String eventId, Integer page) {
        if (page == null || page < 0)
            page = 0;
        // UserEntity validUser = getValidUser(userId);
        // Page<ActivityEntity> entities = repo.findAll(new PartitionKey(eventId)
        // EventVisibilityType.PUBLIC, validUser.getUsername(), validUser.getInaturalist(),
        // Pageable.ofSize(PAGE_SIZE).withPage(page));
        // return new Paged<>(page, entities.getTotalElements(),
        // entities.getContent().stream().map(ActivityMapper.INSTANCE::entityToDto).toList());
        return null;
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
        if (repo.countByEventId(validEvent.getId()) >= maxActivitiesPErEvent)
            throw new BadRequestException("No more Activities can be added to this Event!");
        return ActivityMapper.INSTANCE.entityToDto(
            repo.save(ActivityMapper.INSTANCE.dtoToEntity(
                ActivityMapper.INSTANCE.createDtoToFullDto(activityCreate))));
    }

    public @Valid Activity updateActivity(@NotNull String userId, @NotNull String id, @Valid ActivityBase activityBase) {
        UserEntity validUser = getValidUser(userId);
        Optional<ActivityEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Activity to update!");
        ActivityEntity entity = foundEntity.get();
        EventEntity validEvent = getValidEvent(validUser, entity.getEventId(), false);
        checkThatEventCanBeModified(validUser, validEvent);
        return ActivityMapper.INSTANCE.entityToDto(
            repo.save(ActivityMapper.INSTANCE.dtoToExistingEntity(entity, activityBase)));
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

        // TODO: Do calculation
        entity.setCalculated(ZonedDateTime.now());

        return ActivityMapper.INSTANCE.entityToDto(
            repo.save(entity));
    }

    private void checkThatEventCanBeModified(UserEntity user, EventEntity event) {
        if (!event.getAdmins().contains(user.getUsername()))
            throw new ForbiddenException("Activity cannot be modified by this User!");
        if (event.getClose().isBefore(ZonedDateTime.now()))
            throw new BadRequestException("This Event is already finished, it cannot be modified anymore!");
    }

    private EventEntity getValidEvent(UserEntity user, String eventId, boolean canBeParticipant) {
        Optional<EventEntity> foundEntity = eventRepo.findById(eventId);
        if (!foundEntity.isPresent())
            throw new NotFoundException("The Event associated with this Activity cannot be found!");
        EventEntity entity = foundEntity.get();
        if (entity.getVisibility() == EventVisibilityType.PRIVATE
                && !entity.getAdmins().contains(user.getUsername())
                && !(canBeParticipant && entity.getParticipants().contains(user.getInaturalist())))
            throw new ForbiddenException("Event not accessible by this User!");
        
        return entity;
    }

    private UserEntity getValidUser(String userId) {
        Optional<UserEntity> userEntity = userRepo.findById(userId);
        if (!userEntity.isPresent())
            throw new ForbiddenException("Incorrect User ID!");
        return userEntity.get();
    }

}
