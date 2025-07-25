package cat.nyaa.playtimetracker.workflow;

import net.ess3.api.IEssentials;
import org.jetbrains.annotations.Nullable;

public interface IEssentialsAPIProvider {

    @Nullable IEssentials getEssentialsAPI();
}
