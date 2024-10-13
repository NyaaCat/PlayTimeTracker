package cat.nyaa.playtimetracker.reward;

import cat.nyaa.playtimetracker.config.data.CommandRewardData;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommandReward implements IReward {

    private final @Nullable CommandRewardData cfg;
    private @Nullable String command;

    public CommandReward() {
        this.cfg = null;
        this.command = null;
    }

    public CommandReward(@Nullable CommandRewardData cfg) {
        this.cfg = cfg;
        this.command = null;
    }

    @Override
    public boolean prepare(String rewardName, long completedTime, Player player, Plugin plugin) {
        if(this.cfg == null) {
            return false;
        }
        if(this.cfg.preCommand != null && !this.cfg.preCommand.isBlank()) {
            var server = plugin.getServer();
            var command = PlaceholderAPIUtils.setPlaceholders(player, this.cfg.preCommand);
            return server.dispatchCommand(server.getConsoleSender(), command);
        }
        return true;
    }

    @Override
    public Boolean distribute(Player player, Plugin plugin, List<Component> outputMessages) {
        if(this.command == null) {
            return false;
        }
        var server = plugin.getServer();
        var command = PlaceholderAPIUtils.setPlaceholders(player, this.command);
        return server.dispatchCommand(server.getConsoleSender(), command);
    }

    @Override
    public void serialize(OutputStream outputStream) throws Exception {
        if(this.cfg == null) {
            throw new Exception("CommandRewardData is null");
        }
        if(this.cfg.command == null || this.cfg.command.isBlank()) {
            throw new Exception("command is null or empty");
        }
        byte[] bytes = this.cfg.command.getBytes(StandardCharsets.UTF_8);
        DataOutputStream dos = IReward.wrapOutputStream(outputStream);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    @Override
    public void deserialize(InputStream inputStream) throws Exception {
        DataInputStream dis = IReward.wrapInputStream(inputStream);
        int length = dis.readInt();
        if(length <= 0) {
            throw new Exception("Invalid command length");
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        this.command = new String(bytes, StandardCharsets.UTF_8);
    }
}
