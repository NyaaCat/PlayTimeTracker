create table time2
(
    player      VARCHAR not null
        primary key,
    lastUpdate  BIGINT  not null,
    dailyTime   BIGINT  not null,
    weeklyTime  BIGINT  not null,
    monthlyTime BIGINT  not null,
    totalTime   BIGINT  not null,
    lastSeen    BIGINT  not null
);