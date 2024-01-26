package mywild.event;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EventMapper {
    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);
    
    public EventEntity dtoToEntity(Event dto);

    public EventEntity dtoToExistingEntity(@MappingTarget EventEntity entity, EventBase dto);

    public Event entityToDto(EventEntity entity);

    public Event baseDtoToFullDto(EventBase superDto);

}
