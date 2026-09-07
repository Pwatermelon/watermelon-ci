
package io.watermelon.ci.domain.deployment;

public enum DeploymentStatus {
    PENDING,
    DEPLOYING,
    HEALTHY,
    DEGRADED,
    FAILED,
    STOPPED,
    UNKNOWN
}
