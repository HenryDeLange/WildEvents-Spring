package mywild.activity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.CosmosUniqueKey;
import com.azure.spring.data.cosmos.core.mapping.CosmosUniqueKeyPolicy;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import mywild.core.data.BaseEntity;

@ToString(callSuper = true)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "Activities")
@CosmosUniqueKeyPolicy(uniqueKeys = {
    @CosmosUniqueKey(paths = { "/eventId", "/name" })
})
public class ActivityEntity extends BaseEntity {

    @PartitionKey
    @NotBlank
    private String eventId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private ActivityType type;

    private ActivityDisableReason disableReason;

    private ZonedDateTime calculated;

    private List<Map<String, String>> criteria;

    private List<Map<String, ActivityCalculation>> results;

}
