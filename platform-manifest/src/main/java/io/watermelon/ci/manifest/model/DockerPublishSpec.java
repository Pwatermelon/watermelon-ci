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
