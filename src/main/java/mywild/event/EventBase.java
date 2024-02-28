package mywild.event;

import java.time.ZonedDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class EventBase {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private ZonedDateTime start; // iNaturalist observations start counting towards Achievements from this date

    @NotNull
    private ZonedDateTime stop; // iNaturalist observations stop counting towards Achievements by this date

    @NotNull
    private ZonedDateTime close; // Achievements can be (re)calculate up to this date (to cater for late identifications)

    @NotNull
    private EventVisibilityType visibility;

}
