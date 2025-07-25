package cat.nyaa.playtimetracker.reward;

import cat.nyaa.ecore.EconomyCore;
import org.jetbrains.annotations.Nullable;

public interface IEconomyCoreProvider {

    @Nullable EconomyCore getEconomyCore();
}
