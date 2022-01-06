package cat.nyaa.playtimetracker.command;

import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.nyaacore.utils.OfflinePlayerUtils;
import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.PTT;
import cat.nyaa.playtimetracker.Rule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class CommandHandler extends CommandReceiver {
    private final PTT plugin;
    private final I18n i18n;

    /**
     * @param plugin for logging purpose only
     * @param _i18n  i18n
     */
    public CommandHandler(PTT plugin, I18n _i18n) {
        super(plugin, _i18n);
        this.i18n = _i18n;
        this.plugin = plugin;
    }

    @SubCommand(value = "view", permission = "ptt.view", isDefaultCommand = true)
    public void view(CommandSender sender, Arguments args) {
        if (!(sender instanceof Player)) {
            I18n.send(sender, "command.only-player-can-do");
            return;
        }
        plugin.getUpdater().updateSingle((Player) sender);
        plugin.printStatistic(sender, (Player) sender);

    }

    @SubCommand(value = "reload", permission = "ptt.reload")
    public void reload(CommandSender sender, Arguments args) {
        plugin.onReload();
        I18n.send(sender, "command.reload-finished");
    }

    @SubCommand(value = "reset", permission = "ptt.reset")
    public void reset(CommandSender sender, Arguments args) {
        String name = args.nextString();
        if ("all".equalsIgnoreCase(name)) {
            plugin.getUpdater().resetAllStatistic();
        } else {
            plugin.getUpdater().resetSingleStatistic(OfflinePlayerUtils.lookupPlayer(name).getUniqueId());
        }
        I18n.send(sender, "command.done");
    }

    @SubCommand(value = "acquire", alias = {"ac"}, permission = "ptt.acquire")
    public void acquire(CommandSender sender, Arguments args) {
        if (!(sender instanceof Player p)) {
            I18n.send(sender, "command.only-player-can-do");
            return;
        }
        Set<Rule> satisfiedRules = plugin.getSatisfiedRules(p.getUniqueId());
        if (satisfiedRules.size() == 0) {
            I18n.send(sender, "command.nothing-to-acquire");
            return;
        }
        // feature removed: acquire particular reward
        for (Rule r : satisfiedRules) {
            plugin.applyReward(r, p);
            plugin.getUpdater().markRuleAsApplied(p.getUniqueId(), r);
        }
    }

    @SubCommand(value = "recurrence", alias = {"recur"}, permission = "ptt.recurrence")
    public void recurrence(CommandSender sender, Arguments args) {
        String playerName = args.nextString();
        String ruleName = args.nextString();
        UUID id = OfflinePlayerUtils.lookupPlayer(playerName).getUniqueId();
        Rule rule = plugin.getRules().get(ruleName);
        if (rule != null && rule.period == Rule.PeriodType.DISPOSABLE) {
            plugin.getDatabase().setRecurrenceRule(ruleName, id);
            plugin.getDatabase().save();
        } else {
            I18n.send(sender, "invalid-rule");
        }
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }
}
