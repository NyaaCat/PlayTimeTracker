package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.reward.IReward;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class DemoInvalidReward implements IReward {

    public boolean allowSerialize = false;

    public boolean allowDeserialize = false;

    public DemoInvalidReward(boolean allowSerialize, boolean allowDeserialize) {
        this.allowSerialize = allowSerialize;
        this.allowDeserialize = allowDeserialize;
    }


    @Override
    public boolean prepare(String rewardName, long completedTime, Player player, Plugin plugin) {
        return true;
    }

    @Override
    public Boolean distribute(Player player, Plugin plugin, List<Component> outputMessages) {
        return null;
    }

    @Override
    public void serialize(OutputStream outputStream) throws Exception {
        if(!allowSerialize) {
            throw new Exception("Not allowed to serialize");
        }
    }

    @Override
    public void deserialize(InputStream inputStream) throws Exception {
        if(!allowDeserialize) {
            throw new Exception("Not allowed to deserialize");
        }
    }
}
