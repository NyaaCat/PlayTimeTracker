create table time
(
    dailyTime   BIGINT  not null,
    lastSeen    BIGINT  not null,
    monthlyTime BIGINT  not null,
    player      VARCHAR not null
        primary key,
    totalTime   BIGINT  not null,
    weeklyTime  BIGINT  not null
);