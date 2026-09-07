package io.watermelon.ci.manifest.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.watermelon.ci.manifest.model.JobSpec;
import io.watermelon.ci.manifest.model.PipelineManifest;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ManifestValidator {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final JsonSchema schema;

    public ManifestValidator() {
        try (InputStream in = getClass().getResourceAsStream("/schemas/watermelon-ci.schema.json")) {
            if (in == null) {
                throw new IllegalStateException("schema resource missing");
            }
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            this.schema = factory.getSchema(in);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to load manifest schema", ex);
        }
    }

    public void validateYaml(String yaml) {
        try {
            JsonNode tree = yamlMapper.readTree(yaml);
            Set<ValidationMessage> errors = schema.validate(tree);
            if (!errors.isEmpty()) {
                List<String> messages = errors.stream().map(ValidationMessage::getMessage).toList();
                throw new ManifestValidationException(messages);
            }
        } catch (ManifestValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ManifestValidationException(List.of("schema validation error: " + ex.getMessage()));
        }
    }

    public void validateSemantics(PipelineManifest manifest) {
        List<String> violations = new ArrayList<>();
        if (manifest.getName() == null || manifest.getName().isBlank()) {
            violations.add("name is required");
        }
        if (manifest.getStages() == null || manifest.getStages().isEmpty()) {
            violations.add("stages must not be empty");
        }
        if (manifest.getJobs() == null || manifest.getJobs().isEmpty()) {
            violations.add("jobs must not be empty");
        } else {
            for (var entry : manifest.getJobs().entrySet()) {
                String jobName = entry.getKey();
                JobSpec job = entry.getValue();
                if (job.getStage() == null || !manifest.getStages().contains(job.getStage())) {
                    violations.add("job '" + jobName + "' references unknown stage '" + job.getStage() + "'");
                }
                if (job.getScript() == null || job.getScript().isEmpty()) {
                    violations.add("job '" + jobName + "' must have script");
                }
                if (job.getNeeds() != null) {
                    for (String need : job.getNeeds()) {
                        if (!manifest.getJobs().containsKey(need)) {
                            violations.add("job '" + jobName + "' needs unknown job '" + need + "'");
                        }
                    }
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new ManifestValidationException(violations);
        }
    }
}
