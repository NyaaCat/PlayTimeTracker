package cat.nyaa.playtimetracker.reward;

import cat.nyaa.ecore.EconomyCore;
import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class EcoReward implements IReward {

    private static final Logger logger = LoggerFactory.getLogger(EcoReward.class);

    private final @Nullable EcoRewardData cfg;

    private @Nullable Double amount; // if null, the reward is not prepared
    private int rollbackFlag; // 0: no rollback; 1: rollback to system vault; 2: rollback to player vault with `rollbackPlayerUUID`
    private @Nullable UUID rollbackPlayerUUID;

    public EcoReward() {
        this(null);
    }

    public EcoReward(EcoRewardData cfg) {
        this.cfg = cfg;
        this.amount = null;
        this.rollbackFlag = 0;
        this.rollbackPlayerUUID = null;
    }

    @Override
    public boolean prepare(String rewardName, long completedTime, Player player, Plugin plugin) {
        EconomyCore ecore = null;
        if(plugin instanceof IEconomyCoreProvider provider) {
            ecore = provider.getEconomyCore();
        }
        if(cfg == null || ecore == null) {
            return false;
        }
        try {
            switch (cfg.type) {
                case TRANSFER -> {
                    double balance = 0.0;
                    if(cfg.isRefVaultSystemVault()) {
                        balance = ecore.getSystemBalance();
                    } else {
                        balance = ecore.getPlayerBalance(cfg.getRefVaultAsUUID());
                    }
                    double expect = balance * cfg.ratio;
                    amount = Math.max(Math.min(cfg.max, expect), cfg.min);
                    boolean success = false;
                    if(cfg.isVaultSystemVault()) {
                        success = ecore.withdrawSystemVault(amount);
                        rollbackFlag = 1;
                    } else {
                        success = ecore.withdrawPlayer(cfg.getVaultAsUUID(), amount);
                        rollbackFlag = 2;
                        rollbackPlayerUUID = cfg.getVaultAsUUID();
                    }
                    if(!success) {
                        logger.error("Failed to withdraw eco-reward ({}) from {}", amount, cfg.vault);
                        amount = null;
                    }
                }
                case ADD -> {
                    amount = cfg.amount;
                }
            };
            if(amount != null) {
                logger.debug("Prepared eco-reward ({}) for {}", amount, player.getName());
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to prepare eco-reward", e);
            amount = null;
            return false;
        }
    }

    @Override
    public Boolean distribute(Player player, Plugin plugin, List<Component> outputMessages) {
        EconomyCore ecore = null;
        if(plugin instanceof IEconomyCoreProvider provider) {
            ecore = provider.getEconomyCore();
        }
        if(ecore == null || amount == null) {
            return false;
        }
        try {
            boolean success = ecore.depositPlayer(player.getUniqueId(), amount);
            if(success) {
                // TODO: use adventure text api
                String text = I18n.format("message.reward.eco.success", amount, amount == 1.0 ? ecore.currencyNameSingular() : ecore.currencyNamePlural());
                TextComponent msg = LegacyComponentSerializer.legacySection().deserialize(text);
                outputMessages.add(msg);
            } else {
                logger.error("Failed to distribute eco-reward ({}) to {}", amount, player.getName());
                if(rollbackFlag == 1) {
                    if(!ecore.depositSystemVault(amount)) {
                        logger.error("Failed to rollback eco-reward ({}) to system vault", amount);
                    }
                } else if(rollbackFlag == 2 && rollbackPlayerUUID != null) {
                    if(!ecore.depositPlayer(rollbackPlayerUUID, amount)) {
                        logger.error("Failed to rollback eco-reward ({}) to {}", amount, rollbackPlayerUUID);
                    }
                }
            }
            return success;
        } catch (Exception e) {
            logger.error("Failed to distribute eco-reward", e);
            return false;
        }
    }

    @Override
    public void serialize(OutputStream outputStream) throws Exception {
        if(amount == null) {
            throw new IllegalStateException("prepare() must be called before serialize()");
        }
        DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeDouble(amount);
        dos.writeInt(rollbackFlag);
        if(rollbackFlag == 2 && rollbackPlayerUUID != null) {
            dos.writeLong(rollbackPlayerUUID.getMostSignificantBits());
            dos.writeLong(rollbackPlayerUUID.getLeastSignificantBits());
        }
    }

    @Override
    public void deserialize(InputStream inputStream) throws Exception {
        DataInputStream dis = new DataInputStream(inputStream);
        amount = dis.readDouble();
        rollbackFlag = dis.readInt();
        rollbackPlayerUUID = null;
        if(rollbackFlag == 2) {
            long mostSigBits = dis.readLong();
            long leastSigBits = dis.readLong();
            rollbackPlayerUUID = new UUID(mostSigBits, leastSigBits);
        }
    }

    public Double getAmount() {
        return amount;
    }
}
