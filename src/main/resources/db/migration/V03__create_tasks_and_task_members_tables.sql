create table tasks(
    id uuid not null default uuidv7(),
    name varchar(100) not null,
    description varchar(500) null,
    status varchar(20) not null,
    importance varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    project_member_id uuid not null,
    constraint pk_tasks_id primary key (id),
    constraint fk_tasks_project_members foreign key (project_member_id) references project_members(id),
    constraint ck_tasks_name_not_empty check (name = btrim(name) and name <> ''),
    constraint ck_tasks_description_not_empty check (description = btrim(description) and description <> ''),
    constraint ck_tasks_status_valid check (status in ('ONGOING', 'COMPLETED', 'ON_HOLD', 'CANCELLED')),
    constraint ck_tasks_importance_valid check (importance in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

create table task_members(
    task_id uuid not null,
    project_member_id uuid not null,
    constraint pk_task_members_task_id_project_member_id primary key (task_id, project_member_id),
    constraint fk_task_members_tasks foreign key (task_id) references tasks(id) on delete cascade,
    constraint fk_task_members_project_members foreign key (project_member_id) references project_members(id) on delete cascade
);

create index ix_task_members_project_member_id on task_members(project_member_id);