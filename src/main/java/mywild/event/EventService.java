package mywild.event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import mywild.core.error.BadRequestException;
import mywild.core.error.ForbiddenException;
import mywild.core.error.NotFoundException;
import mywild.core.rest.Paged;
import mywild.user.UserEntity;
import mywild.user.UserRepository;

@Validated
@Service
public class EventService {
    private static final int PAGE_SIZE = 10;

    @Autowired
    private EventRepository repo;

    @Autowired
    private UserRepository userRepo;

    public @Valid Paged<Event> findEvents(@NotNull String userId, @NotNull String iNatName, Integer page) {
        if (page == null || page < 0)
            page = 0;
        UserEntity validUser = getValidUser(userId);
        Page<EventEntity> entities = repo.findAllByVisibilityOrAdminsIgnoreCaseContainsOrParticipantsIgnoreCaseContainsOrderByStartDescNameAsc(
            EventVisibilityType.PUBLIC, validUser.getId(), iNatName,
            Pageable.ofSize(PAGE_SIZE).withPage(page));
        return new Paged<>(page, entities.getTotalElements(),
            entities.getContent().stream().map(EventMapper.INSTANCE::entityToDto).toList());
    }

    public @Valid Event findEvent(@NotNull String userId, @NotNull String iNatName, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Event not found!");
        EventEntity entity = foundEntity.get();
        if (entity.getVisibility() == EventVisibilityType.PRIVATE
                && !entity.getAdmins().contains(validUser.getUsername())
                && !entity.getParticipants().contains(iNatName))
            throw new ForbiddenException("Event not accessible by this User!");
        return EventMapper.INSTANCE.entityToDto(entity);
    }

    public @Valid Event createEvent(@NotNull String userId, @Valid EventBase eventBase) {
        UserEntity validUser = getValidUser(userId);
        return EventMapper.INSTANCE.entityToDto(repo.save(EventMapper.INSTANCE
            .dtoToEntity(EventMapper.INSTANCE.superToChild(eventBase).toBuilder().admins(List.of(validUser.getUsername()))
                .participants(List.of(validUser.getInaturalist())).build())));
    }

    public @Valid Event updateEvent(@NotNull String userId, @NotNull String iNatName, @NotNull String id,  @Valid EventBase eventBase) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to update!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("Event cannot be updated by this User!");
        makeSureEventIsNotClosed(entity);
        return EventMapper.INSTANCE.entityToDto(repo.save(EventMapper.INSTANCE.dtoToExistingEntity(entity, eventBase)));
    }

    public void deleteEvent(@NotNull String userId, @NotNull String id) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to delete!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("Event cannot be deleted by this User!");
        repo.deleteById(id);
        // TODO: Also delete all associated activities
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
        // TODO: Calculate all associated activities
    }

    public @Valid Event adminJoinEvent(@NotNull String userId, @NotNull String id, @NotNull String adminId) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to add an Admin to!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("This User cannot add an Admin to this Event!");
        makeSureEventIsNotClosed(entity);
        if (!entity.getAdmins().contains(adminId))
            entity.getAdmins().add(adminId);
        // TODO: Only add existing users
        return EventMapper.INSTANCE.entityToDto(repo.save(entity));
    }

    public @Valid Event adminLeaveEvent(@NotNull String userId, @NotNull String id, @NotNull String adminId) {
        UserEntity validUser = getValidUser(userId);
        Optional<EventEntity> foundEntity = repo.findById(id);
        if (!foundEntity.isPresent())
            throw new NotFoundException("Could not find the Event to remove an Admin from!");
        EventEntity entity = foundEntity.get();
        if (!entity.getAdmins().contains(validUser.getUsername()))
            throw new ForbiddenException("This User cannot remove an Admin from this Event!");
        makeSureEventIsNotClosed(entity);
        if (entity.getAdmins().contains(adminId))
            entity.getAdmins().remove(adminId);
        // TODO: Don't allow removing the last admin
        return EventMapper.INSTANCE.entityToDto(repo.save(entity));
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
        return EventMapper.INSTANCE.entityToDto(repo.save(entity));
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
        return EventMapper.INSTANCE.entityToDto(repo.save(entity));
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