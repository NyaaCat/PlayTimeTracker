package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.data.RewardData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Reward {
    private final String description;
    private final String command;

    public Reward(RewardData rewardData) {
        description = ChatColor.translateAlternateColorCodes('&', rewardData.description);
        command = rewardData.command;
    }

    public String getDescription() {
        return description;
    }

    public void applyTo(Player p) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("{playerName}", p.getName()));
    }
}
