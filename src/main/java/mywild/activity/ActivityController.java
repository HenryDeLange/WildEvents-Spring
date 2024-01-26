package mywild.activity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.core.rest.Paged;
import mywild.core.security.jwt.Utils;

@Tag(name = "Activities", description = "Manage Activities.")
@RestController
public class ActivityController {

    @Autowired
    private ActivityService service;

    @Operation(summary = "Find all Activities associated with the Event.")
    @GetMapping("/activities")
    public Paged<Activity> findActivities(@RequestParam String eventId, @RequestParam(defaultValue = "0") int page, @RequestParam(required = false) String requestContinuation, JwtAuthenticationToken jwtToken) {
        return service.findActivities(Utils.getUserIdFromJwt(jwtToken), eventId, page, requestContinuation);
    }

    @Operation(summary = "Find an Activity associated with the Event.")
    @GetMapping("/activities/{id}")
    public Activity findActivity(@PathVariable String id, JwtAuthenticationToken jwtToken) {
        return service.findActivity(Utils.getUserIdFromJwt(jwtToken), id);
    }

    @Operation(summary = "Create an Activity.")
    @PostMapping("/activities")
    public Activity createActivity(@RequestBody ActivityCreate dto, JwtAuthenticationToken jwtToken) {
        return service.createActivity(Utils.getUserIdFromJwt(jwtToken), dto);
    }

    @Operation(summary = "Update an Activity.")
    @PutMapping("/activities/{id}")
    public Activity updateActivity(@PathVariable String id, @RequestBody ActivityBase dto, JwtAuthenticationToken jwtToken) {
        return service.updateActivity(Utils.getUserIdFromJwt(jwtToken), id, dto);
    }

    @Operation(summary = "Delete an Activity.")
    @DeleteMapping("/activities/{id}")
    public void deleteActivity(@PathVariable String id, JwtAuthenticationToken jwtToken) {
        service.deleteActivity(Utils.getUserIdFromJwt(jwtToken), id);
    }

    @Operation(summary = "Calculate the Activity.")
    @PostMapping("/activities/{id}/calculate")
    public Activity calculateActivity(@PathVariable String id, JwtAuthenticationToken jwtToken) {
        return service.calculateActivity(Utils.getUserIdFromJwt(jwtToken), id);
    }

}
