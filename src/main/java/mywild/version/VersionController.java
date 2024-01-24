package mywild.version;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "WildEvents Version", description = "Version information about the WildEvents server.")
@RestController
@RequestMapping("version")
public class VersionController {

    @Value("${mywild.app.version}")
    private String version;

    @Operation(summary = "Get server version.")
    @GetMapping("")
    public Version getVersion() {
        return Version.builder().appVersion(version).build();
    }

}
