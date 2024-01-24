package mywild.user;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;

@Repository
public interface UserRepository extends CosmosRepository<UserEntity, String> {

    Optional<UserEntity> findByUsernameAndPassword(String username, String password);

}
