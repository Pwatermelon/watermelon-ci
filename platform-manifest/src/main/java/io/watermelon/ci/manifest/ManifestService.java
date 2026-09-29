package io.watermelon.ci.manifest;

import io.watermelon.ci.manifest.compile.CompileContext;
import io.watermelon.ci.manifest.compile.CompiledPipeline;
import io.watermelon.ci.manifest.compile.JenkinsfileCompiler;
import io.watermelon.ci.manifest.model.PipelineManifest;
import io.watermelon.ci.manifest.parse.KarbyzManifestParser;
import io.watermelon.ci.manifest.parse.ManifestParser;
import io.watermelon.ci.manifest.validate.ManifestValidator;
import org.springframework.stereotype.Service;

@Service
public class ManifestService {

    private final ManifestParser yamlParser;
    private final KarbyzManifestParser karbyzParser;
    private final ManifestValidator validator;
    private final JenkinsfileCompiler compiler;

    public ManifestService(
            ManifestParser yamlParser,
            KarbyzManifestParser karbyzParser,
            ManifestValidator validator,
            JenkinsfileCompiler compiler) {
        this.yamlParser = yamlParser;
        this.karbyzParser = karbyzParser;
        this.validator = validator;
        this.compiler = compiler;
    }

    public PipelineManifest parseAndValidate(String source) {
        PipelineManifest manifest;
        if (karbyzParser.isKarbyz(source)) {
            manifest = karbyzParser.parse(source);
        } else {
            validator.validateYaml(source);
            manifest = yamlParser.parse(source);
        }
        validator.validateSemantics(manifest);
        return manifest;
    }

    public CompiledPipeline compile(String source, CompileContext context) {
        PipelineManifest manifest = parseAndValidate(source);
        return compiler.compile(manifest, context);
    }
}
