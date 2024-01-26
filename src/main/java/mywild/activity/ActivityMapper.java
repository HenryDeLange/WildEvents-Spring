package mywild.activity;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ActivityMapper {
    ActivityMapper INSTANCE = Mappers.getMapper(ActivityMapper.class);
    
    public ActivityEntity dtoToEntity(Activity dto);

    public ActivityEntity dtoToExistingEntity(@MappingTarget ActivityEntity entity, ActivityBase dto);

    public Activity entityToDto(ActivityEntity entity);

    public Activity baseDtoToFullDto(ActivityBase superDto);

    public Activity createDtoToFullDto(ActivityCreate superDto);

}
