package io.watermelon.ci.manifest.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.manifest.model.PipelineManifest;
import org.springframework.stereotype.Component;

@Component
public class ManifestParser {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public PipelineManifest parse(String yaml) {
        try {
            PipelineManifest manifest = yamlMapper.readValue(yaml, PipelineManifest.class);
            if (manifest == null) {
                throw new PlatformException(ErrorCode.MANIFEST_INVALID, "manifest is empty");
            }
            return manifest;
        } catch (PlatformException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PlatformException(ErrorCode.MANIFEST_INVALID, "failed to parse YAML: " + ex.getMessage(), ex);
        }
    }
}
