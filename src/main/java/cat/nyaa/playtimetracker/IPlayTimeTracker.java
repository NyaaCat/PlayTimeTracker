package cat.nyaa.playtimetracker;

import org.jetbrains.annotations.Nullable;

import java.io.File;

public interface IPlayTimeTracker {

    @Nullable
    PlayTimeTrackerController getController();

    @Nullable
    PlayerAFKManager getPlayerAFKManager();

    File getFileInDataFolder(String fileName);
}
