package mywild.user;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import mywild.core.error.ForbiddenException;
import mywild.core.security.jwt.TokenService;
import mywild.core.security.jwt.TokenType;

@Validated
@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private TokenService tokenService;

    public Tokens register(@Valid User user) {
        user.setUsername(user.getUsername().trim());
        user.setInaturalist(user.getInaturalist().trim().toLowerCase());
        UserEntity userEntity = repo.save(UserMapper.INSTANCE.dtoToEntity(user));
        return new Tokens(
                userEntity.getUsername(),
                userEntity.getInaturalist(),
                tokenService.generateToken(TokenType.ACCESS, userEntity),
                tokenService.generateToken(TokenType.REFRESH, userEntity));
    }

    // TODO: keep track of login attempts and restrict it to X per hour/day, to prevent brute force attacks
    public Tokens login(@Valid UserLogin login) {
        Optional<UserEntity> foundEntity = repo.findByUsernameAndPassword(login.getUsername(), login.getPassword());
        if (!foundEntity.isPresent())
            throw new ForbiddenException("Incorrect User credentials!");
        UserEntity entity = foundEntity.get();
        return new Tokens(
                entity.getUsername(),
                entity.getInaturalist(),
                tokenService.generateToken(TokenType.ACCESS, entity),
                tokenService.generateToken(TokenType.REFRESH, entity));
    }

    public Tokens refresh(String userId) {
        UserEntity userEntity = getValidUser(userId);
        return new Tokens(
                userEntity.getUsername(),
                userEntity.getInaturalist(),
                tokenService.generateToken(TokenType.ACCESS, userEntity),
                tokenService.generateToken(TokenType.REFRESH, userEntity));
    }

    /**
     * Make sure the userId is valid.
     */
    private UserEntity getValidUser(String userId) {
        if (userId == null || userId.isEmpty())
            throw new ForbiddenException("Incorrect User ID!");
        Optional<UserEntity> userEntity = repo.findById(userId);
        if (!userEntity.isPresent())
            throw new ForbiddenException("Incorrect User ID!");
        return userEntity.get();
    }

}
