package cat.nyaa.playtimetracker;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Reward {
    private String description;
    private String command;

    public Reward(ConfigurationSection s) {
        description=s.getString("description");
        command = s.getString("command");
    }

    public void applyTo(Player p) {
        p.sendMessage(description);
        Bukkit.getConsoleSender().sendMessage(command.replace("{playerName}", p.getName()));
    }
}
