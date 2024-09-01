package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.nyaacore.configuration.ISerializable;
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

    private @Nullable UUID refVaultUUID = null;
    private @Nullable UUID vaultUUID = null;

    public EcoRewardData() {
    }

    @Override
    public boolean validate(List<String> outputError) {
        if(!isRefVaultSystemVault()) {
            try {
                refVaultUUID = UUID.fromString(refVault);
            } catch (IllegalArgumentException e) {
                outputError.add("Invalid ref-vault (should be UUID or \"$system\"): " + refVault);
                return false;
            }
        }
        if(ratio < 0.0 || ratio > 1.0) {
            outputError.add("Invalid ratio (should be in [0.0, 1.0]): " + ratio);
            return false;
        }
        if(min < 0.0 || max < 0.0 || min > max) {
            outputError.add("Invalid min/max: " + min + "/" + max);
            return false;
        }
        if(amount < 0.0) {
            outputError.add("Invalid amount (should be non-negative): " + amount);
            return false;
        }
        if(!isVaultSystemVault()) {
            try {
                vaultUUID = UUID.fromString(vault);
            } catch (IllegalArgumentException e) {
                outputError.add("Invalid vault (should be UUID or \"$system\"): " + vault);
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

