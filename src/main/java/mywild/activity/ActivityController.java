package mywild.activity;

import java.util.List;
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
import mywild.core.security.jwt.Utils;

@Tag(name = "Activities", description = "Manage Activities.")
@RestController
public class ActivityController {

    @Autowired
    private ActivityService service;

    @Operation(summary = "Find all Activities associated with the Event.")
    @GetMapping("/activities")
    public List<Activity> findActivities(@RequestParam String eventId, JwtAuthenticationToken jwtToken) {
        return service.findActivities(Utils.getUserIdFromJwt(jwtToken), eventId);
    }

    @Operation(summary = "Find an Activity associated with the Event.")
    @GetMapping("/activities/{activityId}")
    public Activity findActivity(@PathVariable String activityId, JwtAuthenticationToken jwtToken) {
        return service.findActivity(Utils.getUserIdFromJwt(jwtToken), activityId);
    }

    @Operation(summary = "Create an Activity.")
    @PostMapping("/activities")
    public Activity createActivity(@RequestBody ActivityCreate dto, JwtAuthenticationToken jwtToken) {
        return service.createActivity(Utils.getUserIdFromJwt(jwtToken), dto);
    }

    @Operation(summary = "Update an Activity.")
    @PutMapping("/activities/{activityId}")
    public Activity updateActivity(@PathVariable String activityId, @RequestBody ActivityBase dto, JwtAuthenticationToken jwtToken) {
        return service.updateActivity(Utils.getUserIdFromJwt(jwtToken), activityId, dto);
    }

    @Operation(summary = "Delete an Activity.")
    @DeleteMapping("/activities/{activityId}")
    public void deleteActivity(@PathVariable String activityId, JwtAuthenticationToken jwtToken) {
        service.deleteActivity(Utils.getUserIdFromJwt(jwtToken), activityId);
    }

    @Operation(summary = "Calculate the Activity.")
    @PostMapping("/activities/{activityId}/calculate")
    public Activity calculateActivity(@PathVariable String activityId, JwtAuthenticationToken jwtToken) {
        return service.calculateActivity(Utils.getUserIdFromJwt(jwtToken), activityId);
    }

    @Operation(summary = "Enable an Activity.")
    @PutMapping("/activities/{activityId}/enable")
    public void enableActivity(@PathVariable String activityId, JwtAuthenticationToken jwtToken) {
        service.enableActivity(Utils.getUserIdFromJwt(jwtToken), activityId);
    }

    @Operation(summary = "Disable the Activity.")
    @PutMapping("/activities/{activityId}/disable")
    public Activity disableActivity(@PathVariable String activityId, JwtAuthenticationToken jwtToken) {
        return service.disableActivity(Utils.getUserIdFromJwt(jwtToken), activityId);
    }

}
