package mywild.core.data;

import org.springframework.data.annotation.Id;

import com.azure.spring.data.cosmos.core.mapping.GeneratedValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private String id;

    // TODO: Add support for optimistic locking
    //       https://learn.microsoft.com/en-us/java/api/overview/azure/spring-data-cosmos-readme?view=azure-java-stable#optimistic-locking
    // @Version
    // private String _etag;

}
