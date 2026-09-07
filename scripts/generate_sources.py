#!/usr/bin/env python3
"""Generate remaining Watermelon CI Java sources. Run from repo root."""
from pathlib import Path


def write(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    if not content.endswith("\n"):
        content += "\n"
    p.write_text(content, encoding="utf-8")
    print("wrote", path)


def main() -> None:
    # ---------- manifest model ----------
    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/WhenPolicy.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WhenPolicy {
    ON_SUCCESS,
    MANUAL,
    ALWAYS;

    @JsonCreator
    public static WhenPolicy from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ON_SUCCESS;
        }
        return WhenPolicy.valueOf(raw.trim().toUpperCase());
    }
}
""".lstrip(),
    )

    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/DeployRuntime.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DeployRuntime {
    DOCKER,
    KUBERNETES,
    COMPOSE;

    @JsonCreator
    public static DeployRuntime from(String raw) {
        return DeployRuntime.valueOf(raw.trim().toUpperCase());
    }
}
""".lstrip(),
    )

    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/DeploySpec.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploySpec {
    private DeployRuntime runtime = DeployRuntime.DOCKER;
    private boolean track = true;
    private String release;
    private String image;
    private List<String> ports = new ArrayList<>();

    public DeployRuntime getRuntime() { return runtime; }
    public void setRuntime(DeployRuntime runtime) { this.runtime = runtime; }
    public boolean isTrack() { return track; }
    public void setTrack(boolean track) { this.track = track; }
    public String getRelease() { return release; }
    public void setRelease(String release) { this.release = release; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public List<String> getPorts() { return ports; }
    public void setPorts(List<String> ports) { this.ports = ports != null ? ports : new ArrayList<>(); }
}
""".lstrip(),
    )

    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/JobSpec.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobSpec {
    private String stage;
    private String image;
    private List<String> script = new ArrayList<>();
    private WhenPolicy when = WhenPolicy.ON_SUCCESS;
    private String environment;
    @JsonProperty("allow_failure")
    private boolean allowFailure;
    private List<String> needs = new ArrayList<>();
    private DeploySpec deploy;

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public List<String> getScript() { return script; }
    public void setScript(List<String> script) { this.script = script != null ? script : new ArrayList<>(); }
    public WhenPolicy getWhen() { return when; }
    public void setWhen(WhenPolicy when) { this.when = when != null ? when : WhenPolicy.ON_SUCCESS; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public boolean isAllowFailure() { return allowFailure; }
    public void setAllowFailure(boolean allowFailure) { this.allowFailure = allowFailure; }
    public List<String> getNeeds() { return needs; }
    public void setNeeds(List<String> needs) { this.needs = needs != null ? needs : new ArrayList<>(); }
    public DeploySpec getDeploy() { return deploy; }
    public void setDeploy(DeploySpec deploy) { this.deploy = deploy; }
}
""".lstrip(),
    )

    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/DockerPublishSpec.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerPublishSpec {
    private String image;
    private String context = ".";
    private String dockerfile = "Dockerfile";
    private List<String> tags = new ArrayList<>();

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context != null ? context : "."; }
    public String getDockerfile() { return dockerfile; }
    public void setDockerfile(String dockerfile) { this.dockerfile = dockerfile != null ? dockerfile : "Dockerfile"; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }
}
""".lstrip(),
    )

    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/MavenPublishSpec.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MavenPublishSpec {
    private boolean deploy = true;
    private List<String> goals = new ArrayList<>(List.of("deploy"));

    public boolean isDeploy() { return deploy; }
    public void setDeploy(boolean deploy) { this.deploy = deploy; }
    public List<String> getGoals() { return goals; }
    public void setGoals(List<String> goals) { this.goals = goals != null ? goals : new ArrayList<>(); }
}
""".lstrip(),
    )

    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/PublishSpec.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishSpec {
    private List<DockerPublishSpec> docker = new ArrayList<>();
    private MavenPublishSpec maven;

    public List<DockerPublishSpec> getDocker() { return docker; }
    public void setDocker(List<DockerPublishSpec> docker) { this.docker = docker != null ? docker : new ArrayList<>(); }
    public MavenPublishSpec getMaven() { return maven; }
    public void setMaven(MavenPublishSpec maven) { this.maven = maven; }
}
""".lstrip(),
    )

    write(
        "platform-manifest/src/main/java/io/watermelon/ci/manifest/model/PipelineManifest.java",
        """
package io.watermelon.ci.manifest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineManifest {
    private String name;
    private Map<String, String> variables = new LinkedHashMap<>();
    private List<String> stages = new ArrayList<>();
    private Map<String, JobSpec> jobs = new LinkedHashMap<>();
    private PublishSpec publish;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) {
        this.variables = variables != null ? variables : new LinkedHashMap<>();
    }
    public List<String> getStages() { return stages; }
    public void setStages(List<String> stages) { this.stages = stages != null ? stages : new ArrayList<>(); }
    public Map<String, JobSpec> getJobs() { return jobs; }
    public void setJobs(Map<String, JobSpec> jobs) { this.jobs = jobs != null ? jobs : new LinkedHashMap<>(); }
    public PublishSpec getPublish() { return publish; }
    public void setPublish(PublishSpec publish) { this.publish = publish; }
}
""".lstrip(),
    )

    print("model done")


if __name__ == "__main__":
    main()
