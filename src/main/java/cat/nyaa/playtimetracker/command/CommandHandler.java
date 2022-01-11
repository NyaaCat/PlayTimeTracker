package cat.nyaa.playtimetracker.command;

import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.PlayTimeTracker;

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
