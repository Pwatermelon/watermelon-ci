
create table organizations (
    id uuid primary key,
    slug varchar(64) not null unique,
    name varchar(200) not null,
    created_at timestamp with time zone not null
);

create table projects (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    slug varchar(64) not null,
    name varchar(200) not null,
    description varchar(2000),
    created_at timestamp with time zone not null,
    unique (organization_id, slug)
);

create table access_groups (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    slug varchar(64) not null,
    name varchar(200) not null,
    default_role varchar(32) not null,
    created_at timestamp with time zone not null,
    unique (organization_id, slug)
);

create table project_memberships (
    id uuid primary key,
    project_id uuid not null references projects(id),
    subject varchar(200) not null,
    role varchar(32) not null,
    created_at timestamp with time zone not null,
    unique (project_id, subject)
);

create table pipeline_runs (
    id uuid primary key,
    project_id uuid not null references projects(id),
    number bigint not null,
    ref varchar(200) not null,
    commit_sha varchar(64),
    status varchar(32) not null,
    jenkins_job_name varchar(500),
    jenkins_build_number integer,
    manifest_yaml text not null,
    compiled_jenkinsfile text,
    created_at timestamp with time zone not null,
    finished_at timestamp with time zone,
    unique (project_id, number)
);

create table issues (
    id uuid primary key,
    project_id uuid not null references projects(id),
    number bigint not null,
    title varchar(300) not null,
    body text,
    status varchar(32) not null,
    assignee_subject varchar(200),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    unique (project_id, number)
);

create table artifacts (
    id uuid primary key,
    project_id uuid not null references projects(id),
    pipeline_run_id uuid references pipeline_runs(id),
    kind varchar(32) not null,
    name varchar(500) not null,
    version varchar(200) not null,
    locator varchar(1000) not null,
    digest varchar(128),
    created_at timestamp with time zone not null
);

create index idx_artifacts_project on artifacts(project_id);

create table deployments (
    id uuid primary key,
    project_id uuid not null references projects(id),
    pipeline_run_id uuid references pipeline_runs(id),
    environment varchar(64) not null,
    runtime varchar(32) not null,
    image_ref varchar(500) not null,
    release_name varchar(200),
    external_id varchar(200),
    status varchar(32) not null,
    status_message varchar(2000),
    created_at timestamp with time zone not null,
    last_seen_at timestamp with time zone
);

create index idx_deployments_project on deployments(project_id);
create index idx_deployments_env on deployments(project_id, environment);
create index idx_deployments_external on deployments(external_id);

create table audit_events (
    id uuid primary key,
    organization_id uuid,
    project_id uuid,
    actor varchar(200) not null,
    action varchar(100) not null,
    details text,
    created_at timestamp with time zone not null
);
