package cat.nyaa.playtimetracker.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TaskUtils {
    public static int get5BitId(@NotNull UUID uniqueId) {//MAX= 0001 1111 =31
        return getBitId(uniqueId, 5);
    }

    public static int getBitId(@NotNull UUID uniqueId, int bit) {
        long l = (uniqueId.getMostSignificantBits() >> 1) + (uniqueId.getLeastSignificantBits() >> 1);
        return (int) ((l >>> (64 - bit)));
    }

    public static void waitToRun(long tickCount, int bit, @NotNull UUID uniqueId, @NotNull Runnable runnable) {
        int mod = 1 << bit;
        int hashedId = getBitId(uniqueId, bit);
        if (hashedId == (tickCount % mod)) {
            runnable.run();
        }
    }

    public static void mod64TickToRun(long tickCount, @NotNull UUID uniqueId, @NotNull Runnable runnable) {
        waitToRun(tickCount, 6, uniqueId, runnable);
    }

    public static void mod32TickToRun(long tickCount, @NotNull UUID uniqueId, @NotNull Runnable runnable) {
        waitToRun(tickCount, 5, uniqueId, runnable);
    }

    public static void mod2TickToRun(long tickCount, @NotNull UUID uniqueId, @NotNull Runnable runnable) {
        waitToRun(tickCount, 1, uniqueId, runnable);
    }
}
