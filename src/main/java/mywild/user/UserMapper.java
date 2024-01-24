package mywild.user;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    public UserEntity dtoToEntity(User dto);

    public User entityToDto(UserEntity entity);

}
