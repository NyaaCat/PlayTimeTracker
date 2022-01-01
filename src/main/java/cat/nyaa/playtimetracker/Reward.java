package cat.nyaa.playtimetracker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Reward {
    private final String description;
    private final String command;

    public Reward(ConfigurationSection s) {
        description = ChatColor.translateAlternateColorCodes('&', s.getString("description"));
        command = s.getString("command");
    }

    public String getDescription() {
        return description;
    }

    public void applyTo(Player p) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("{playerName}", p.getName()));
    }
}
