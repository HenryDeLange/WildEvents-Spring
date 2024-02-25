package mywild.event;

import java.util.Arrays;
import java.util.List;
import mywild.core.error.BadRequestException;

public class EventUtils {

    private EventUtils() {
    }

    public static String getValidName(String name) {
        if (name == null || name.contains(",") || name.contains("#"))
            throw new BadRequestException("Invalid name!");
        return "#" + name.trim() + "#";
    }

    public static boolean containsName(String stringList, String name) {
        List<String> list = Arrays.asList(stringList.split(","));
        return list.contains(getValidName(name));
    }

}
