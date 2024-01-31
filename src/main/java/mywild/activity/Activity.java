package mywild.activity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
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
public class Activity extends ActivityCreate {

    @NotBlank
    private String id;

    private ZonedDateTime calculated;

    private List<Map<String, ActivityCalculation>> results;

}
