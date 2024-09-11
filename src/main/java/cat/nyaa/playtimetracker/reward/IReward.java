package cat.nyaa.playtimetracker.reward;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IReward {

    /**
     * prepare the reward for the player
     * @return true if the reward is ready to be distributed; false otherwise
     */
    boolean prepare(String rewardName, long completedTime, Player player, Plugin plugin);


    /**
     * distribute the reward to the player
     * @param player the player to distribute the reward to
     * @param plugin the plugin
     * @param outputMessages the messages to be sent to the player;
     *                       a component message of reward description can be added to the list on success;
     *                       a component message of error message can be added to the list otherwise;
     *                       in any cases, no message be put in the list are allowed
     * @return true if the reward is successfully distributed;
     *         false if the reward distribution has failed;
     *         null if the reward distribution is blocked and should be retried later
     */
    Boolean distribute(Player player, Plugin plugin, List<Component> outputMessages);


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


    static DataOutputStream fromOutputStream(OutputStream outputStream) {
        if(outputStream instanceof DataOutputStream dataOutputStream) {
            return dataOutputStream;
        }
        return new DataOutputStream(outputStream);
    }

    static DataInputStream fromInputStream(InputStream inputStream) {
        if(inputStream instanceof DataInputStream dataInputStream) {
            return dataInputStream;
        }
        return new DataInputStream(inputStream);
    }
}
