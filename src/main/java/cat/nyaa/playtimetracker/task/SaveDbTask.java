package cat.nyaa.playtimetracker.task;

import cat.nyaa.playtimetracker.PlayTimeTracker;

public class SaveDbTask implements Runnable{
    @Override
    public void run() {
        if (PlayTimeTracker.getInstance() != null) {
            if (PlayTimeTracker.getInstance().getTimeRecordManager() != null) {
                PlayTimeTracker.getInstance().getTimeRecordManager().getTimeTrackerConnection().doAsyncUpdate();
            }
        }
    }
}
