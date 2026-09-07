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
