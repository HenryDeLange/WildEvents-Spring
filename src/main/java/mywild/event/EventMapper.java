package mywild.event;

import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EventMapper {
    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    @Mapping(target = "admins", qualifiedByName = "listToString")
    @Mapping(target = "participants", qualifiedByName = "listToString")
    public EventEntity dtoToEntity(Event dto);

    public EventEntity dtoToExistingEntity(@MappingTarget EventEntity entity, EventBase dto);

    @Mapping(target = "admins", qualifiedByName = "stringToList")
    @Mapping(target = "participants", qualifiedByName = "stringToList")
    public Event entityToDto(EventEntity entity);

    public Event baseDtoToFullDto(EventBase superDto);

    @Named("listToString")
    default String listToString(List<String> list) {
        if (list.isEmpty())
            return "";
        return "#" + String.join("#,#", list) + "#";
    }

    @Named("stringToList")
    default List<String> stringToList(String string) {
        return Arrays.asList(string.replace("#", "").split(","));
    }

}
