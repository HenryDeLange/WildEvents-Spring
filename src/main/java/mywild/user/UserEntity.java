package mywild.user;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.CosmosUniqueKey;
import com.azure.spring.data.cosmos.core.mapping.CosmosUniqueKeyPolicy;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Container(containerName = "Users")
@CosmosUniqueKeyPolicy(uniqueKeys = {
        @CosmosUniqueKey(paths = { "/username" }),
        @CosmosUniqueKey(paths = { "/inaturalist" })
})
public class UserEntity extends BaseEntity {

    @PartitionKey
    @NotBlank
    @Size(min = 4)
    private String username;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String inaturalist;

}
