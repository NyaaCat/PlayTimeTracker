package cat.nyaa.playtimetracker;

import be.seeseemelk.mockbukkit.MockPlugin;
import cat.nyaa.ecore.EconomyCore;
import cat.nyaa.ecore.ServiceFeePreference;
import cat.nyaa.ecore.TransactionResult;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.reward.IBalanceCache;
import cat.nyaa.playtimetracker.reward.IEconomyCoreProvider;
import it.unimi.dsi.fastutil.longs.LongDoubleMutablePair;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DemoPTT extends MockPlugin implements IEconomyCoreProvider, IBalanceCache {

    FakeEcore ecore = new FakeEcore();
    private PTTConfiguration pttConfiguration;
    
    // Balance cache fields
    private LongDoubleMutablePair cachedSystemBalance = new LongDoubleMutablePair(0L, 0.0);
    private Map<UUID, LongDoubleMutablePair> cachedPlayerBalances = new HashMap<>();

    @Override
    public EconomyCore getEconomyCore() {
        return ecore;
    }
    
    public PTTConfiguration getPttConfiguration() {
        if(pttConfiguration == null) {
            pttConfiguration = new PTTConfiguration(this);
        }
        return pttConfiguration;
    }
    
    @Override
    public double getBalance(@Nullable UUID uuid, long timestamp, EconomyCore ecore) {
        PTTConfiguration config = getPttConfiguration();
        long cacheKeep = config.missionConfig.syncRefCacheTime;
        return PlayTimeTracker.getBalanceWithCache(uuid, timestamp, cacheKeep, ecore, cachedSystemBalance, cachedPlayerBalances);
    }


    public static class FakeEcore implements EconomyCore {

        public Object2DoubleMap<UUID> balances = new Object2DoubleAVLTreeMap<>();

        public static final double SYSTEM_BALANCE = 10000;

        public double systemBalance = SYSTEM_BALANCE;

        public void init() {
            balances.clear();
            systemBalance = SYSTEM_BALANCE;
        }

        @Override
        public TransactionResult playerTransfer(UUID uuid, UUID uuid1, double v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTransferToMultiple(UUID uuid, List<UUID> list, double v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTransferToMultiple(UUID uuid, List<UUID> list, double v, ServiceFeePreference serviceFeePreference) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTrade(UUID uuid, UUID uuid1, double v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTrade(UUID uuid, UUID uuid1, double v, ServiceFeePreference serviceFeePreference) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTrade(UUID uuid, UUID uuid1, double v, double v1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTrade(UUID uuid, UUID uuid1, double v, double v1, ServiceFeePreference serviceFeePreference) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTrade(UUID uuid, UUID uuid1, double v, double v1, double v2, double v3) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionResult playerTrade(UUID uuid, UUID uuid1, double v, double v1, double v2, double v3, ServiceFeePreference serviceFeePreference) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean depositPlayer(UUID uuid, double v) {
            double s = balances.getOrDefault(uuid, 0.0);
            boolean success = s + v < 1000;
            if (success) {
                s += v;
            }
            balances.put(uuid, s);
            return success;
        }

        @Override
        public boolean withdrawPlayer(UUID uuid, double v) {
            double s = balances.getOrDefault(uuid, 0.0);
            boolean success = s - v >= 0;
            if (success) {
                s -= v;
            }
            balances.put(uuid, s);
            return success;
        }

        @Override
        public boolean setPlayerBalance(UUID uuid, double v) {
            balances.put(uuid, v);
            return true;
        }

        @Override
        public boolean withdrawSystemVault(double v) {
            boolean success = systemBalance - v >= 0;
            if (success) {
                systemBalance -= v;
            }
            return success;
        }

        @Override
        public boolean depositSystemVault(double v) {
            systemBalance += v;
            return true;
        }

        @Override
        public double getPlayerBalance(UUID uuid) {
            return balances.getOrDefault(uuid, 0.0);
        }

        @Override
        public boolean setSystemBalance(double v) {
            systemBalance = v;
            return true;
        }

        @Override
        public double getSystemBalance() {
            return systemBalance;
        }

        @Override
        public double getTransferFeeRate() {
            return 0.01;
        }

        @Override
        public double getTradeFeeRate() {
            return 0.02;
        }

        @Override
        public String currencyNameSingular() {
            return "m";
        }

        @Override
        public String currencyNamePlural() {
            return "m";
        }

        @Override
        public String systemVaultName() {
            return "$system";
        }
    }
}
