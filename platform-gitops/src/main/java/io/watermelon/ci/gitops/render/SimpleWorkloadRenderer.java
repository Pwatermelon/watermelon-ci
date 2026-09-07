package io.watermelon.ci.gitops.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renders plain Kubernetes YAML from a Watermelon deploy block.
 * Users do NOT write Helm charts — the platform owns the template.
 */
@Component
public class SimpleWorkloadRenderer {

    public record WorkloadSpec(
            String releaseName,
            String namespace,
            String image,
            int replicas,
            List<String> containerPorts,
            List<String> secretKeys,
            String vaultPath,
            Map<String, String> labels
    ) {}

    public String render(WorkloadSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("apiVersion: v1\nkind: Namespace\nmetadata:\n  name: ").append(spec.namespace()).append("\n");

        if (spec.secretKeys() != null && !spec.secretKeys().isEmpty()) {
            sb.append("---\n");
            sb.append("apiVersion: v1\nkind: Secret\nmetadata:\n");
            sb.append("  name: ").append(spec.releaseName()).append("-secrets\n");
            sb.append("  namespace: ").append(spec.namespace()).append("\n");
            sb.append("  annotations:\n");
            sb.append("    watermelon.ci/vault-path: \"").append(escape(spec.vaultPath())).append("\"\n");
            sb.append("    watermelon.ci/managed-by: watermelon-ci\n");
            sb.append("type: Opaque\nstringData:\n");
            for (String key : spec.secretKeys()) {
                sb.append("  ").append(key).append(": \"${vault:").append(escape(spec.vaultPath())).append("#").append(key).append("}\"\n");
            }
            sb.append("# Values are injected by Watermelon secret sync / External Secrets Operator\n");
        }

        sb.append("---\n");
        sb.append("apiVersion: apps/v1\nkind: Deployment\nmetadata:\n");
        sb.append("  name: ").append(spec.releaseName()).append("\n");
        sb.append("  namespace: ").append(spec.namespace()).append("\n");
        sb.append("  labels:\n");
        appendLabels(sb, spec.labels(), 4);
        sb.append("spec:\n");
        sb.append("  replicas: ").append(Math.max(1, spec.replicas())).append("\n");
        sb.append("  selector:\n    matchLabels:\n      app: ").append(spec.releaseName()).append("\n");
        sb.append("  template:\n    metadata:\n      labels:\n");
        sb.append("        app: ").append(spec.releaseName()).append("\n");
        appendLabels(sb, spec.labels(), 8);
        sb.append("    spec:\n      containers:\n");
        sb.append("        - name: app\n");
        sb.append("          image: ").append(escape(spec.image())).append("\n");
        sb.append("          imagePullPolicy: IfNotPresent\n");
        if (spec.containerPorts() != null && !spec.containerPorts().isEmpty()) {
            sb.append("          ports:\n");
            for (String port : spec.containerPorts()) {
                int p = parsePort(port);
                sb.append("            - containerPort: ").append(p).append("\n");
            }
        }
        if (spec.secretKeys() != null && !spec.secretKeys().isEmpty()) {
            sb.append("          envFrom:\n");
            sb.append("            - secretRef:\n");
            sb.append("                name: ").append(spec.releaseName()).append("-secrets\n");
        }

        if (spec.containerPorts() != null && !spec.containerPorts().isEmpty()) {
            sb.append("---\n");
            sb.append("apiVersion: v1\nkind: Service\nmetadata:\n");
            sb.append("  name: ").append(spec.releaseName()).append("\n");
            sb.append("  namespace: ").append(spec.namespace()).append("\n");
            sb.append("spec:\n  selector:\n    app: ").append(spec.releaseName()).append("\n");
            sb.append("  ports:\n");
            for (String port : spec.containerPorts()) {
                int p = parsePort(port);
                sb.append("    - name: p").append(p).append("\n");
                sb.append("      port: ").append(p).append("\n");
                sb.append("      targetPort: ").append(p).append("\n");
            }
        }
        return sb.toString();
    }

    public String renderArgoApplication(
            String appName,
            String project,
            String repoUrl,
            String repoPath,
            String namespace,
            String cluster) {
        return """
                apiVersion: argoproj.io/v1alpha1
                kind: Application
                metadata:
                  name: %s
                  namespace: argocd
                  labels:
                    watermelon.ci/managed: "true"
                spec:
                  project: %s
                  source:
                    repoURL: %s
                    path: %s
                    targetRevision: HEAD
                  destination:
                    server: %s
                    namespace: %s
                  syncPolicy:
                    automated:
                      prune: true
                      selfHeal: true
                    syncOptions:
                      - CreateNamespace=true
                """.formatted(appName, project, repoUrl, repoPath, cluster, namespace);
    }

    private static void appendLabels(StringBuilder sb, Map<String, String> labels, int indent) {
        if (labels == null) {
            return;
        }
        String pad = " ".repeat(indent);
        labels.forEach((k, v) -> sb.append(pad).append(k).append(": \"").append(escape(v)).append("\"\n"));
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) {
            return 8080;
        }
        String left = raw.contains(":") ? raw.substring(raw.lastIndexOf(':') + 1) : raw;
        left = left.contains("/") ? left.substring(0, left.indexOf('/')) : left;
        try {
            return Integer.parseInt(left.trim());
        } catch (NumberFormatException ex) {
            return 8080;
        }
    }

    private static String escape(String raw) {
        return raw == null ? "" : raw.replace("\"", "\\\"");
    }

    public static String sanitizeName(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-").replaceAll("-{2,}", "-");
    }

    public static List<String> containerPortsOnly(List<String> ports) {
        if (ports == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String p : ports) {
            result.add(String.valueOf(parsePort(p)));
        }
        return result;
    }
}
