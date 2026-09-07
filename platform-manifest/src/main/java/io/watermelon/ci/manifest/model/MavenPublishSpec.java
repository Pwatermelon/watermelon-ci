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
