package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.playtimetracker.config.ISerializableExt;
import cat.nyaa.playtimetracker.config.IValidationContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EcoRewardData implements ISerializableExt {

    public static final String SYSTEM_VAULT = "$system";

    @Serializable
    public RewardType type = RewardType.TRANSFER;
    @Serializable(name = "ref-vault")
    public String refVault = SYSTEM_VAULT;
    @Serializable
    public double ratio = 0.01;
    @Serializable
    public double min = 0.0;
    @Serializable
    public double max = 1024.0;
    @Serializable
    public String vault = SYSTEM_VAULT;
    @Serializable
    public double amount = 0.0;
    @Serializable(name = "sync-ref-cache-time")
    public long syncRefCacheTime = 256 * 50; // sync ref vault by cache its value, keeps $value milliseconds; default set to 4 * 64 gt

    private @Nullable UUID refVaultUUID = null;
    private @Nullable UUID vaultUUID = null;

    public EcoRewardData() {
    }

    @Override
    public void validate(IValidationContext context) throws Exception {
        if(!isRefVaultSystemVault()) {
            refVaultUUID = UUID.fromString(refVault);
        }
        if(ratio < 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("Invalid ratio (should be in [0.0, 1.0]): " + ratio);
        }
        if(min < 0.0 || max < 0.0 || min > max) {
            throw new IllegalArgumentException("Invalid min/max (should be non-negative and min <= max): " + min + "/" + max);
        }
        if(amount < 0.0) {
            throw new IllegalArgumentException("Invalid amount (should be non-negative): " + amount);
        }
        if(!isVaultSystemVault()) {
            vaultUUID = UUID.fromString(vault);
        }
    }

    public boolean isRefVaultSystemVault() {
        return SYSTEM_VAULT.equals(refVault);
    }

    public UUID getRefVaultAsUUID() {
        return refVaultUUID;
    }

    public boolean isVaultSystemVault() {
        return SYSTEM_VAULT.equals(vault);
    }

    public UUID getVaultAsUUID() {
        return vaultUUID;
    }

    public enum RewardType {
        TRANSFER,
        ADD,
        // REMOVE,
        // SET,
    }
}

