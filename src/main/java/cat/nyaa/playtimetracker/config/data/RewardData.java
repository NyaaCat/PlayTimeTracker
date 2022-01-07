package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.nyaacore.configuration.ISerializable;

public class RewardData implements ISerializable {
    @Serializable
    public String description;
    @Serializable
    public String command;

    public RewardData() {
    }
    public RewardData(String description, String command) {
        this.description = description;
        this.command = command;
    }
}
