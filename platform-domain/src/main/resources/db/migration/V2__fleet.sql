-- Fleet control plane: clusters, worker nodes (in-platform admin console)

create table fleet_clusters (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    slug varchar(64) not null,
    name varchar(200) not null,
    kind varchar(32) not null,
    description varchar(2000),
    created_at timestamp with time zone not null,
    unique (organization_id, slug)
);

create table worker_nodes (
    id uuid primary key,
    cluster_id uuid not null references fleet_clusters(id),
    name varchar(64) not null,
    role varchar(16) not null,
    status varchar(16) not null,
    engine_url varchar(500) not null,
    hostname varchar(200),
    architecture varchar(64),
    cpu_cores integer,
    memory_bytes bigint,
    agent_version varchar(128),
    join_token_hash varchar(128) not null,
    created_at timestamp with time zone not null,
    last_heartbeat_at timestamp with time zone,
    unique (cluster_id, name)
);

create index idx_worker_nodes_cluster on worker_nodes(cluster_id);
create index idx_worker_nodes_status on worker_nodes(status);

alter table deployments add column cluster_id uuid;
alter table deployments add column node_id uuid;
