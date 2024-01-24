package mywild.event;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString(callSuper = true)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Event extends EventBase {

    @NotBlank
    private String id;

    @NotNull
    @Size(min = 1)
    private List<String> admins; // WildEvents users

    private List<String> participants; // iNaturalist users

    // TODO: Add support for groups
    // private List<String> groups; // WildPlaces groups containing iNaturalist users

}
