package mywild.event;

import java.time.ZonedDateTime;
import com.azure.cosmos.models.CompositePathSortOrder;
import com.azure.spring.data.cosmos.core.mapping.CompositeIndex;
import com.azure.spring.data.cosmos.core.mapping.CompositeIndexPath;
import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.CosmosIndexingPolicy;
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
@Container(containerName = "Events")
@CosmosUniqueKeyPolicy(uniqueKeys = {
    @CosmosUniqueKey(paths = { "/name" })
})
@CosmosIndexingPolicy(compositeIndexes = {
    @CompositeIndex(paths = {
        @CompositeIndexPath(path = "/start", order = CompositePathSortOrder.ASCENDING),
        @CompositeIndexPath(path = "/name", order = CompositePathSortOrder.ASCENDING)
    })
}, overwritePolicy = true)
public class EventEntity extends BaseEntity {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private ZonedDateTime start;

    @NotNull
    private ZonedDateTime stop;

    @NotNull
    private ZonedDateTime close;

    @PartitionKey
    @NotNull
    private EventVisibilityType visibility;

    @NotBlank
    private String admins;

    private String participants;

    // private List<String> groups;

}
