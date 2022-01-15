package cat.nyaa.playtimetracker.task;

import cat.nyaa.playtimetracker.PlayTimeTracker;

public class MissionCheckTask implements Runnable {
    @Override
    public void run() {
        if (PlayTimeTracker.getInstance() != null) {
            if (PlayTimeTracker.getInstance().getMissionManager() != null) {
                PlayTimeTracker.getInstance().getMissionManager().missionCheckTick();
            }
        }
    }
}
