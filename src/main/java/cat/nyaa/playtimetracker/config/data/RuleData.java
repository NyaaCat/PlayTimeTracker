package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.nyaacore.configuration.ISerializable;

import java.util.List;

public class RuleData implements ISerializable {
    @Serializable
    public String period;
    @Serializable
    public long require;
    @Serializable
    public boolean autoGive;
    @Serializable
    public long timeout = -1;
    @Serializable
    public String reward;
    @Serializable(name = "eligible-group")
    public List<String> group;

    public RuleData() {
    }

    public RuleData(String period, long require, boolean autoGive, long timeout, String reward, List<String> group) {

        this.period = period;
        this.require = require;
        this.autoGive = autoGive;
        this.timeout = timeout;
        this.reward = reward;
        this.group = group;
    }
}
