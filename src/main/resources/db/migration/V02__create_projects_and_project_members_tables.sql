create table projects(
    id uuid not null default uuidv7(),
    name varchar(50) not null,
    description varchar(500) null,
    status varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    user_id uuid not null,
    constraint pk_projects_id primary key (id),
    constraint fk_projects_users foreign key (user_id) references users(id) on delete restrict,
    constraint uq_projects_user_id_name unique (user_id, name),
    constraint ck_projects_name_not_empty check (name = btrim(name) and name <> ''),
    constraint ck_projects_description_not_empty check (description = btrim(description) and description <> ''),
    constraint ck_projects_status_valid check (status in ('COMPLETED', 'ONGOING'))
);

create table project_members(
    id uuid not null default uuidv7(),
    role varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    project_id uuid not null,
    user_id uuid not null,
    constraint pk_project_members_id primary key (id),
    constraint fk_project_members_projects foreign key (project_id) references projects(id) on delete cascade,
    constraint fk_project_members_users foreign key (user_id) references users(id) on delete cascade,
    constraint uq_project_members_project_id_user_id unique (project_id, user_id),
    constraint ck_project_members_role_valid check (role in ('ADMIN', 'MANAGER', 'MEMBER'))
);

create unique index uix_project_members_role_admin on project_members(project_id) where role = 'ADMIN';
create index ix_project_members_user_id on project_members(user_id);