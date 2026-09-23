create table users(
    id uuid not null default uuidv7(),
    email varchar(254) not null,
    password_hash varchar(255) not null,
    first_name varchar(20) not null,
    last_name varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz null,
    constraint pk_users_id primary key (id),
    constraint ck_users_email_not_empty check (email = btrim(email) and email <> ''),
    constraint ck_users_first_name_not_empty check (first_name = btrim(first_name) and first_name <> ''),
    constraint ck_users_last_name_not_empty check (last_name = btrim(last_name) and last_name <> '')
);

create unique index uix_users_email on users(lower(email));