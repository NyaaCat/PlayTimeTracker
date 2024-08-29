package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.jetbrains.annotations.Nullable;

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

    private @Nullable UUID refVaultUUID = null;
    private @Nullable UUID vaultUUID = null;

    public EcoRewardData() {
    }

    @Override
    public boolean validate() {
        if(!isRefVaultSystemVault()) {
            try {
                refVaultUUID = UUID.fromString(refVault);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        if(ratio < 0.0 || ratio > 1.0) {
            return false;
        }
        if(min < 0.0 || max < 0.0 || min > max) {
            return false;
        }
        if(amount < 0.0) {
            return false;
        }
        if(!isVaultSystemVault()) {
            try {
                vaultUUID = UUID.fromString(vault);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
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

