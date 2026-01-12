package cat.nyaa.playtimetracker.reward;

import cat.nyaa.ecore.EconomyCore;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Interface for caching economy balances to avoid frequent lookups.
 * This allows keeping system and player balances consistent within a time period.
 */
public interface IBalanceCache {

    /**
     * Get the cached balance for a system vault or player vault.
     * The cache duration is managed by the implementation itself.
     * 
     * @param uuid the UUID of the player vault, or null for system vault
     * @param timestamp the current timestamp in milliseconds
     * @param ecore the EconomyCore instance to fetch balance from if cache is expired
     * @return the cached or fresh balance
     */
    double getBalance(@Nullable UUID uuid, long timestamp, EconomyCore ecore);
}
