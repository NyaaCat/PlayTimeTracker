create table rewards
(
    id            INTEGER
        primary key autoincrement,
    completedTime BIGINT  not null,
    player        VARCHAR not null,
    rewardName    VARCHAR not null,
    rewardData    BLOB    not null
);