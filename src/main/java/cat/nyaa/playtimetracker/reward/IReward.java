package cat.nyaa.playtimetracker.reward;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.io.OutputStream;

public interface IReward {

    /**
     * prepare the reward for the player
     * @return true if the reward is ready to be distributed; false otherwise
     */
    boolean prepare(String rewardName, long completedTime, Player player, Plugin plugin);


    /**
     * distribute the reward to the player
     * @return true if the reward is successfully distributed;
     *         false if the reward distribution has failed;
     *         null if the reward distribution is blocked and should be retried later
     */
    Boolean distribute(Player player, Plugin plugin);


    /**
     * serialize the reward to a stream
     * @param outputStream the stream to serialize the reward to
     * @throws Exception if any error occurs;
     *         specially, IllegalStateException should be thrown if prepare() is not called before serialize()
     */
    void serialize(OutputStream outputStream) throws Exception;


    /**
     * deserialize the reward from a stream
     * @param inputStream the stream to deserialize the reward from
     * @throws Exception if any error occurs
     */
    void deserialize(InputStream inputStream) throws Exception;
}
