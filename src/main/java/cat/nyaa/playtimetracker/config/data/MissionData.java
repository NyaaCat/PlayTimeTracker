package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.nyaacore.configuration.ISerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MissionData implements ISerializable {
    @Serializable
    public String missionName = UUID.randomUUID().toString();
    @Serializable
    public List<String> group = new ArrayList<>();
    @Serializable
    public String expression = "lastSeen>1&&dailyTime>1&&weeklyTime>1&&monthlyTime>1&&totalTime>1&&1==2";
    @Serializable
    public int timeoutMS = -1;
    @Serializable
    public boolean resetDaily = false;
    @Serializable
    public boolean resetWeekly = false;
    @Serializable
    public boolean resetMonthly = false;
    @Serializable
    public List<String> rewardCommandList = new ArrayList<>();
    @Serializable
    public String rewardItemSteakBase64 = "";
    @Serializable
    public boolean notify = true;
    @Serializable
    public boolean autoGive = false;
}
