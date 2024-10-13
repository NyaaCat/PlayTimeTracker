INSERT INTO time2 (
    player,
    lastUpdate,
    dailyTime,
    weeklyTime,
    monthlyTime,
    totalTime,
    lastSeen
)
SELECT
    player,
    lastSeen AS lastUpdate,
    dailyTime,
    weeklyTime,
    monthlyTime,
    totalTime,
    lastSeen
FROM time;