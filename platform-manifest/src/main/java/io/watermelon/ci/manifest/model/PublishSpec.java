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
