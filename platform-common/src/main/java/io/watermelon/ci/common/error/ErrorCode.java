package io.watermelon.ci.common.error;

public enum ErrorCode {
    VALIDATION_FAILED,
    NOT_FOUND,
    CONFLICT,
    FORBIDDEN,
    UNAUTHORIZED,
    MANIFEST_INVALID,
    JENKINS_ERROR,
    GIT_ERROR,
    REGISTRY_ERROR,
    RUNTIME_ERROR,
    SECRETS_ERROR,
    GITOPS_ERROR,
    INTERNAL_ERROR
}
