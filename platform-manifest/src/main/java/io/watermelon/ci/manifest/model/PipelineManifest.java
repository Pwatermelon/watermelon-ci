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
