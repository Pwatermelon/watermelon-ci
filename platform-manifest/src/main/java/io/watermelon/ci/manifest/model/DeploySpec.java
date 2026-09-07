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
    private String namespace;
    private int replicas = 1;
    private List<String> ports = new ArrayList<>();
    /** Secret key names resolved from project Vault path for this environment. */
    private List<String> secrets = new ArrayList<>();

    public DeployRuntime getRuntime() { return runtime; }
    public void setRuntime(DeployRuntime runtime) { this.runtime = runtime; }
    public boolean isTrack() { return track; }
    public void setTrack(boolean track) { this.track = track; }
    public String getRelease() { return release; }
    public void setRelease(String release) { this.release = release; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public int getReplicas() { return replicas; }
    public void setReplicas(int replicas) { this.replicas = replicas; }
    public List<String> getPorts() { return ports; }
    public void setPorts(List<String> ports) { this.ports = ports != null ? ports : new ArrayList<>(); }
    public List<String> getSecrets() { return secrets; }
    public void setSecrets(List<String> secrets) { this.secrets = secrets != null ? secrets : new ArrayList<>(); }
}
