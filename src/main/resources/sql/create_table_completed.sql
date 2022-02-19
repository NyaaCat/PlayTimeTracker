create table completed
(
    id            INTEGER
        primary key autoincrement,
    lastCompleted BIGINT  not null,
    mission       VARCHAR not null,
    player        VARCHAR not null
);