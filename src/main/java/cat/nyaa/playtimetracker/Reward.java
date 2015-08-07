package cat.nyaa.playtimetracker;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Reward {
    private String description;
    private String command;

    public String getDescription() {
        return description;
    }

    public Reward(ConfigurationSection s) {
        description = s.getString("description");
        command = s.getString("command");
    }

    public void applyTo(Player p) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("{playerName}", p.getName()));
    }
}
