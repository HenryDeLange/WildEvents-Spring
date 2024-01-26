package mywild.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.core.security.jwt.Utils;

@Tag(name = "User Authentication", description = "Manage Users.")
@RestController
@RequestMapping("users")
public class UserController {

    @Autowired
    private UserService service;

    @Operation(summary = "Register (create) a new User.")
    @PostMapping("register")
    public Tokens register(@RequestBody User user) {
        return service.register(user);
    }

    @Operation(summary = "Login as an existing User in order to get a pair of access and refresh tokens.")
    @PostMapping("login")
    public Tokens login(@RequestBody UserLogin login) {
        return service.login(login);
    }

    @Operation(summary = "Request a new pair of access and refresh tokens.")
    @PostMapping("refresh")
    public Tokens refresh(JwtAuthenticationToken jwtToken) {
        return service.refresh(Utils.getUserIdFromJwt(jwtToken));
    }

    // TODO: logout - for this I'll need to keep track of active tokens and revoke ones that are logged out

}
