package cat.nyaa.playtimetracker.reward;

import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.time.Instant;

public class DemoReward implements IReward {

    private final static Gson gson = GsonComponentSerializer.gson().serializer();

    TextComponent message = null;

    @Override
    public boolean prepare(String rewardName, long completedTime, Player player, Plugin plugin) {
        Instant completedInstant = Instant.ofEpochMilli(completedTime);
        this.message = Component.text()
                .append(player.displayName())
                .append(Component.text(" has completed the task "))
                .append(
                        Component.text()
                                .content(rewardName)
                                .color(NamedTextColor.AQUA)
                )
                .append(Component.text(" at "))
                .append(
                        Component.text()
                                .content(completedInstant.toString())
                                .color(NamedTextColor.GREEN)
                )
                .build();
        return true;
    }

    @Override
    public Boolean distribute(Player player, Plugin plugin) {
        if(this.message == null) {
            return false;
        }
        player.sendMessage(this.message);
        return true;
    }

    @Override
    public void serialize(OutputStream outputStream) throws Exception {
        if(this.message == null) {
            throw new IllegalStateException("prepare() must be called before serialize()");
        }
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        gson.toJson(this.message, TextComponent.class, writer);
        writer.flush();
    }

    @Override
    public void deserialize(InputStream inputStream) throws Exception {
        InputStreamReader reader = new InputStreamReader(inputStream);
        this.message = gson.fromJson(reader, TextComponent.class);
    }
}
