-- Project secrets metadata (values in Vault) + ArgoCD GitOps applications

create table project_secrets (
    id uuid primary key,
    project_id uuid not null references projects(id),
    environment varchar(64) not null,
    name varchar(128) not null,
    vault_path varchar(500) not null,
    description varchar(500),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    unique (project_id, environment, name)
);

create index idx_project_secrets_project on project_secrets(project_id);

create table gitops_applications (
    id uuid primary key,
    project_id uuid not null references projects(id),
    environment varchar(64) not null,
    release_name varchar(200) not null,
    argo_app_name varchar(200) not null unique,
    destination_namespace varchar(100) not null,
    repo_path varchar(500) not null,
    image_ref varchar(500),
    sync_status varchar(32) not null,
    health_status varchar(32) not null,
    last_rendered_yaml text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    unique (project_id, environment, release_name)
);

create index idx_gitops_apps_project on gitops_applications(project_id);
