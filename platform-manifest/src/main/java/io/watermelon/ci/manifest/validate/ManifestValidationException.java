package io.watermelon.ci.manifest.validate;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import java.util.List;

public class ManifestValidationException extends PlatformException {
    private final List<String> violations;

    public ManifestValidationException(List<String> violations) {
        super(ErrorCode.MANIFEST_INVALID, "manifest validation failed: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
