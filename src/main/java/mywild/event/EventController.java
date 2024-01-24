package mywild.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.core.rest.Paged;
import mywild.core.security.jwt.Utils;

@Tag(name = "Events", description = "Manage Events.")
@RestController
@RequestMapping("events")
public class EventController {

    @Autowired
    private EventService service;

    @Operation(summary = "Find all Events associated with the User (admin, participant or public).")
    @GetMapping("")
    public Paged<Event> findEvents(@RequestParam(required = false) Integer page, JwtAuthenticationToken jwtToken) {
        return service.findEvents(Utils.getUserIdFromJwt(jwtToken), Utils.getINatNameFromJwt(jwtToken), page);
    }

    @Operation(summary = "Find an Event associated with the User (admin, participant or public).")
    @GetMapping("{id}")
    public Event findEvent(@PathVariable String id, JwtAuthenticationToken jwtToken) {
        return service.findEvent(Utils.getUserIdFromJwt(jwtToken), Utils.getINatNameFromJwt(jwtToken), id);
    }

    @Operation(summary = "Create an Event.")
    @PostMapping("")
    public Event createEvent(@RequestBody EventBase dto, JwtAuthenticationToken jwtToken) {
        return service.createEvent(Utils.getUserIdFromJwt(jwtToken), dto);
    }

    @Operation(summary = "Update an Event.")
    @PutMapping("{id}")
    public Event updateEvent(@PathVariable String id, @RequestBody EventBase dto, JwtAuthenticationToken jwtToken) {
        return service.updateEvent(Utils.getUserIdFromJwt(jwtToken), Utils.getINatNameFromJwt(jwtToken), id, dto);
    }

    @Operation(summary = "Delete an Event.")
    @DeleteMapping("{id}")
    public void deleteEvent(@PathVariable String id, JwtAuthenticationToken jwtToken) {
        service.deleteEvent(Utils.getUserIdFromJwt(jwtToken), id);
    }

    @Operation(summary = "Calculate all of the Activities in the Event.")
    @PostMapping("{id}/calculate")
    public void calculateEvent(@PathVariable String id, JwtAuthenticationToken jwtToken) {
        service.calculateEvent(Utils.getUserIdFromJwt(jwtToken), id);
    }

    @Operation(summary = "Join the Event as an Admin.")
    @PostMapping("{id}/admins/{adminId}")
    public Event adminJoinEvent(@PathVariable String id, @PathVariable String adminId,
            JwtAuthenticationToken jwtToken) {
        return service.adminJoinEvent(Utils.getUserIdFromJwt(jwtToken), id, adminId);
    }

    @Operation(summary = "Leave the Event as an Admin.")
    @DeleteMapping("{id}/admins/{adminId}")
    public Event adminLeaveEvent(@PathVariable String id, @PathVariable String adminId,
            JwtAuthenticationToken jwtToken) {
        return service.adminLeaveEvent(Utils.getUserIdFromJwt(jwtToken), id, adminId);
    }

    @Operation(summary = "Join the Event as a Participant.")
    @PostMapping("{id}/participants/{iNatId}")
    public Event participantJoinEvent(@PathVariable String id, @PathVariable String iNatId,
            JwtAuthenticationToken jwtToken) {
        return service.participantJoinEvent(Utils.getUserIdFromJwt(jwtToken), id, iNatId);
    }

    @Operation(summary = "Leave the Event as a Participant.")
    @DeleteMapping("{id}/participants/{iNatId}")
    public Event participantLeaveEvent(@PathVariable String id, @PathVariable String iNatId,
            JwtAuthenticationToken jwtToken) {
        return service.participantLeaveEvent(Utils.getUserIdFromJwt(jwtToken), id, iNatId);
    }

}
