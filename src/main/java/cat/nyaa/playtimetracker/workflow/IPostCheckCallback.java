package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import it.unimi.dsi.fastutil.Pair;
import java.time.Duration;
import java.util.List;

public interface IPostCheckCallback {

    void handle(final PlayerContext playerContext, final TimeTrackerDbModel model, final List<Pair<String, Duration>> records);
}

