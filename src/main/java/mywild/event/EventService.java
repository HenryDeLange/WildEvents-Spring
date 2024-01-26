package mywild.event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.azure.cosmos.models.PartitionKey;
import com.azure.spring.data.cosmos.core.query.CosmosPageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import mywild.activity.ActivityRepository;
import mywild.activity.calculate.CalculateService;
import mywild.core.error.BadRequestException;
import mywild.core.error.ForbiddenException;
import mywild.core.error.NotFoundException;
import mywild.core.rest.Paged;
import mywild.user.UserEntity;
import mywild.user.UserRepository;

@Validated
@Service
public class EventService {

    @Value("${mywild.wildevents.page-size}")
    private int pageSize;

    @Autowired
    private EventRepository repo;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CalculateService calculateService;

    public @Valid Paged<Event> findEvents(@NotNull String userId, int page, String requestContinuation) {
        UserEntity validUser = getValidUser(userId);
        Page<EventEntity> entities = repo.findAllByVisibilityOrAdminsIgnoreCaseContainsOrParticipantsIgnoreCaseContainsOrderByStartDescNameAsc(
            EventVisibilityType.PUBLIC, validUser.getUsername(), validUser.getInaturalist(),
            CosmosPageRequest.of(page, pageSize, requestContinuation, Sort.unsorted()));
        return new Paged<>(
            page, pageSize, entities.getTotalElements(),
            entities.getContent().stream().map(EventMapper.INSTANCE::entityToDto).toList(),
            entities.isFirst(), entities.isLast(),
            ((CosmosPageRequest) entities.getPageable()).getRequestContinuation());
    }

    public @Valid Event findEvent(@NotNull String userId, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Event not found!");
        EventEntity entity = foundEntity.get();
        if (entity.getVisibility() == EventVisibilityType.PRIVATE
                && !entity.getAdmins().contains(validUser.getUsername())
                && !entity.getParticipants().contains(validUser.getInaturalist()))
            throw new ForbiddenException("Event not accessible by this User!");
        return EventMapper.INSTANCE.entityToDto(entity);
    }

    public @Valid Event createEvent(@NotNull String userId, @Valid EventBase eventBase) {
        UserEntity validUser = getValidUser(userId);
        return EventMapper.INSTANCE.entityToDto(
            repo.save(EventMapper.INSTANCE.dtoToEntity(
                EventMapper.INSTANCE.baseDtoToFullDto(eventBase)
                    .toBuilder()
                    .admins(List.of(validUser.getUsername()))
                    .participants(List.of(validUser.getInaturalist()))
                    .build())));
    }

    public @Valid Event updateEvent(@NotNull String userId, @NotNull String id,  @Valid EventBase eventBase) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to update!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("Event cannot be updated by this User!");
        makeSureEventIsNotClosed(entity);
        return EventMapper.INSTANCE.entityToDto(
            repo.save(EventMapper.INSTANCE.dtoToExistingEntity(entity, eventBase)));
    }

    public void deleteEvent(@NotNull String userId, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (foundEntity.isPresent()) {
            EventEntity entity = foundEntity.get();
            if (!entity.getAdmins().contains(validUser.getUsername()))
                throw new ForbiddenException("Event cannot be deleted by this User!");
            repo.delete(entity);
        }
        // Also delete all associated activities
        activityRepo.findAll(new PartitionKey(id))
            .forEach(activity -> activityRepo.delete(activity));
    }

    public void calculateEvent(@NotNull String userId, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to calculate!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("Event cannot be calculated by this User!");
        makeSureEventIsNotClosed(entity);
        // Calculate all associated activities
        // TODO: Implement the calculation logic
        activityRepo.findAll(new PartitionKey(id))
            .forEach(activity -> {

                // TODO: Do calculation
                calculateService.calculateActivity(activity);
                activity.setCalculated(ZonedDateTime.now());
                activityRepo.save(activity);

            });
    }

    public @Valid Event adminJoinEvent(@NotNull String userId, @NotNull String id, @NotNull String adminUsername) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to add an Admin to!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("This User cannot add an Admin to this Event!");
        makeSureEventIsNotClosed(entity);
        Optional<UserEntity> adminUser = userRepo.findByUsername(adminUsername);
        if (!adminUser.isPresent())
            throw new BadRequestException("Cannot find the Admin User to add to this Event!");
        if (!entity.getAdmins().contains(adminUsername))
            entity.getAdmins().add(adminUsername);
        return EventMapper.INSTANCE.entityToDto(
            repo.save(entity));
    }

    public @Valid Event adminLeaveEvent(@NotNull String userId, @NotNull String id, @NotNull String adminUsername) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to remove an Admin from!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("This User cannot remove an Admin from this Event!");
        makeSureEventIsNotClosed(entity);
        if (entity.getAdmins().contains(adminUsername))
            entity.getAdmins().remove(adminUsername);
        if (entity.getAdmins().isEmpty())
            throw new BadRequestException("The event must have at least one Admin!");
        return EventMapper.INSTANCE.entityToDto(
            repo.save(entity));
    }

    public @Valid Event participantJoinEvent(@NotNull String userId, @NotNull String id, @NotNull String iNatName) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to add a Participant to!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("This User cannot add a Participant to this Event!");
        makeSureEventIsNotClosed(entity);
        if (!entity.getParticipants().contains(iNatName))
            entity.getParticipants().add(iNatName);
        return EventMapper.INSTANCE.entityToDto(
            repo.save(entity));
    }

    public @Valid Event participantLeaveEvent(@NotNull String userId, @NotNull String id, @NotNull String iNatName) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to remove a Participant from!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("This User cannot remove a Participant from this Event!");
        makeSureEventIsNotClosed(entity);
        if (entity.getParticipants().contains(iNatName))
            entity.getParticipants().remove(iNatName);
        return EventMapper.INSTANCE.entityToDto(
            repo.save(entity));
    }

    private void makeSureEventIsNotClosed(EventEntity entity) {
        if (entity.getClose().isBefore(ZonedDateTime.now()))
            throw new BadRequestException("This Event is already finished, it cannot be modified anymore!");
    }

    private UserEntity getValidUser(String userId) {
        Optional<UserEntity> userEntity = userRepo.findById(userId);
        if (!userEntity.isPresent())
            throw new ForbiddenException("Incorrect User ID!");
        return userEntity.get();
    }

}
