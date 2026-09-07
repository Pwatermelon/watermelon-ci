package io.watermelon.ci.manifest;

import io.watermelon.ci.manifest.compile.CompileContext;
import io.watermelon.ci.manifest.compile.CompiledPipeline;
import io.watermelon.ci.manifest.compile.JenkinsfileCompiler;
import io.watermelon.ci.manifest.model.PipelineManifest;
import io.watermelon.ci.manifest.parse.ManifestParser;
import io.watermelon.ci.manifest.validate.ManifestValidator;
import org.springframework.stereotype.Service;

@Service
public class ManifestService {

    private final ManifestParser parser;
    private final ManifestValidator validator;
    private final JenkinsfileCompiler compiler;

    public ManifestService(ManifestParser parser, ManifestValidator validator, JenkinsfileCompiler compiler) {
        this.parser = parser;
        this.validator = validator;
        this.compiler = compiler;
    }

    public PipelineManifest parseAndValidate(String yaml) {
        validator.validateYaml(yaml);
        PipelineManifest manifest = parser.parse(yaml);
        validator.validateSemantics(manifest);
        return manifest;
    }

    public CompiledPipeline compile(String yaml, CompileContext context) {
        PipelineManifest manifest = parseAndValidate(yaml);
        return compiler.compile(manifest, context);
    }
}
