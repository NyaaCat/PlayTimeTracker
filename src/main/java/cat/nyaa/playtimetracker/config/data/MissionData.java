package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MissionData implements ISerializable {
    public MissionData() {
    }

    @Serializable
    public List<String> group = new ArrayList<>();
    @Serializable
    public String expression = "lastSeen>1&&dailyTime>1&&weeklyTime>1&&monthlyTime>1&&totalTime>1&&1==2";
    @Serializable(name = "timeout-ms")
    public int timeoutMS = -1;
    @Serializable(name = "reset-daily")
    public boolean resetDaily = false;
    @Serializable(name = "reset-weekly")
    public boolean resetWeekly = false;
    @Serializable(name = "reset-monthly")
    public boolean resetMonthly = false;
    @Serializable(name = "reward-commands-list")
    public List<String> rewardCommandList = new ArrayList<>();

    {
        rewardCommandList.add("tell %player_name% nya!");
    }

    @Serializable(name = "reward-items-base64")
    public String rewardItemsBase64 = ItemStackUtils.itemsToBase64(
            Arrays.asList(
                    new ItemStack(Material.STONE),
                    new ItemStack(Material.OAK_WOOD)
            )
    );

    @Serializable
    public boolean notify = true;
    @Serializable(name = "auto-give")
    public boolean autoGive = false;
}
