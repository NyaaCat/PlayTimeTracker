package cat.nyaa.playtimetracker.command;

import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.PlayerAFKManager;
import cat.nyaa.playtimetracker.Utils.TimeUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler extends CommandReceiver {
    private final I18n i18n;
    private final PlayTimeTracker plugin;

    /**
     * @param plugin for logging purpose only
     * @param _i18n  i18n
     */
    public CommandHandler(PlayTimeTracker plugin, I18n _i18n) {
        super(plugin, _i18n);
        this.i18n = _i18n;
        this.plugin = plugin;
    }

    @SubCommand(value = "afkstat", permission = "ptt.command.afkstat")
    public void afkStatus(CommandSender sender, Arguments args) {
        Player player = args.nextPlayer();

        if (!PlayerAFKManager.isAFK(player.getUniqueId())) {
            I18n.send(sender,"command.afkstst.no_afk", player.getName());
            return;
        }
        if (PlayTimeTracker.getInstance() == null || PlayTimeTracker.getInstance().getAfkManager() == null) {
            I18n.send(sender,"command.afkstst.not_found", player.getName());
            return;
        }
        long afkTime = PlayTimeTracker.getInstance().getAfkManager().getAfkTime(player.getUniqueId());
        long lastActivity = PlayTimeTracker.getInstance().getAfkManager().getlastActivity(player.getUniqueId());
        I18n.send(sender,"command.afkstst.info", player.getName(), TimeUtils.dateFormat(lastActivity),TimeUtils.timeFormat(afkTime));
    }
    @SubCommand(value = "reload", permission = "ptt.command.reload")
    public void reload(CommandSender sender, Arguments args) {
        I18n.send(sender,"command.reload.start");
        if (PlayTimeTracker.getInstance() != null) {
            try {
                PlayTimeTracker.getInstance().onReload();
            }catch (Exception e){
                e.printStackTrace();
                PlayTimeTracker.getInstance().getPluginLoader().disablePlugin(PlayTimeTracker.getInstance());
                I18n.send(sender,"command.reload.err");
            }
        }else{
            I18n.send(sender,"command.reload.err");
        }
        I18n.send(sender,"command.reload.finish");
    }
    public I18n getI18n() {
        return i18n;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }


}
