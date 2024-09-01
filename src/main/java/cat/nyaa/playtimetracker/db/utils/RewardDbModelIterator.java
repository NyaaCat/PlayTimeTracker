package cat.nyaa.playtimetracker.db.utils;

import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.reward.IReward;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;

public class RewardDbModelIterator implements Iterator<RewardDbModel>, Iterable<RewardDbModel> {
    final UUID playerId;
    final String rewardName;
    final long completedTime;
    final Iterator<IReward> iterator;

    public RewardDbModelIterator(UUID playerId, String rewardName, long completedTime, Iterator<IReward> iterator) {
        this.playerId = playerId;
        this.rewardName = rewardName;
        this.completedTime = completedTime;
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public RewardDbModel next() {
        return new RewardDbModel(0, this.completedTime, this.playerId, this.rewardName, this.iterator.next());
    }

    @NotNull
    @Override
    public Iterator<RewardDbModel> iterator() {
        return this;
    }

    @Override
    public void forEach(Consumer<? super RewardDbModel> action) {
        while (this.iterator.hasNext()) {
            action.accept(new RewardDbModel(0, this.completedTime, this.playerId, this.rewardName, this.iterator.next()));
        }
    }
}
