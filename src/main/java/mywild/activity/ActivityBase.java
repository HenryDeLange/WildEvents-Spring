package mywild.activity;

import java.util.List;
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
public class ActivityBase {

    @NotBlank
    private String name;

    private String description;

    private ActivityStatus status;

    private ActivityDisableReason disableReason;

    private List<ActivityStep> steps;

}
