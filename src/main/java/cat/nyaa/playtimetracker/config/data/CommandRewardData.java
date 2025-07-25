package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.playtimetracker.config.ISerializableExt;
import cat.nyaa.playtimetracker.config.IValidationContext;

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
    public void validate(IValidationContext context) throws Exception {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
    }
}
