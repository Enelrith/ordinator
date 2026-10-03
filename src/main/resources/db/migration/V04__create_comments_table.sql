create table comments(
    id uuid not null default uuidv7(),
    content varchar(300) not null,
    attachment_name varchar(255) null,
    attachment_object_key varchar(500) null,
    created_at timestamptz not null,
    task_id uuid not null,
    project_member_id uuid null,
    constraint pk_comments_id primary key (id),
    constraint fk_comments_tasks foreign key (task_id) references tasks(id) on delete cascade,
    constraint fk_comments_project_members foreign key (project_member_id)
        references project_members(id) on delete set null,
    constraint uq_comments_attachment_object_key unique (attachment_object_key),
    constraint ck_comments_content_not_empty check (content = btrim(content) and content <> ''),
    constraint ck_comments_attachment_name_attachment_object_key_valid
        check (
        (attachment_name is null and attachment_object_key is null)
            or
        (attachment_name is not null and attachment_object_key is not null)
    )
);

create index ix_comments_task_id on comments(task_id);