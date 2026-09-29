# Architecture

## Control plane

Spring Boot application (`platform-app`) owns product workflows. External systems are adapters:

- **Forgejo** — Git hosting / file API for the editor
- **Jenkins** — execution engine only
- **Docker Registry / Harbor** — container images
- **Nexus** — Maven packages
- **Docker Engine / Kubernetes** — live deployed workload observation

## Artifact + runtime loop

```
pipeline publish ──► push image/maven ──► ArtifactCatalogService (DB index)
pipeline deploy   ──► docker run + labels ──► heartbeat ──► Deployment row
runtime reconcile ──► Docker Engine API ──► update Deployment status in UI
```

Labels used for discovery:

- `watermelon.ci/pipeline-run`
- `watermelon.ci/project`
- `watermelon.ci/environment`
- `watermelon.ci/track=true`

## Manifest contract

Source of truth: `platform-manifest/src/main/resources/schemas/watermelon-ci.schema.json`

Compile path: YAML **или** Карбыз (.kbz) → единая модель пайплайна → Declarative Jenkinsfile.
Карбыз — альтернативный синтаксис того же манифеста, не генератор YAML.


## Fleet admin console

In-platform control plane (`platform-fleet` + `console-ui`):

- Clusters and worker node registry with join tokens
- Per-node Docker Engine gateway (start/stop/restart/remove/logs/stats)
- Drain / activate nodes
- React console served from Spring Boot static resources at `/`

This replaces jumping out to Portainer / Lens / kubectl dashboard for day-2 ops on CI-deployed workloads.


## Secrets + GitOps

- **Vault/OpenBao** (`platform-secrets`): project-scoped KV at `watermelon/{org}/{project}/{env}`
- **ArgoCD overlay** (`platform-gitops`): Watermelon deploy block → plain K8s YAML + Application CR
- Users never author Helm charts for standard services; platform owns the template (`SimpleWorkloadRenderer`)
- Pipeline `runtime: argocd` calls `/api/v1/projects/{id}/gitops/deploy`
