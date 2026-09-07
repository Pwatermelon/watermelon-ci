
package io.watermelon.ci.domain.pipeline;

public enum PipelineStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELED,
    MANUAL_WAIT
}
