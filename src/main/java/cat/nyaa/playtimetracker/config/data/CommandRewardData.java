package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.playtimetracker.config.ISerializableExt;

import java.util.List;

public class CommandRewardData implements ISerializableExt {

    @Serializable(name = "pre-command")
    public String preCommand = "";

    @Serializable
    public String command = "";

    {
        command = "/tell %%player_name%% hello, world!";
    }

    public CommandRewardData() {
    }

    @Override
    public boolean validate(List<String> outputError) {
        return !command.isBlank();
    }
}
