package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.DemoPTT;
import cat.nyaa.playtimetracker.PlayTimeTrackerController;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import it.unimi.dsi.fastutil.longs.LongObjectImmutablePair;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/// Regression tests for the midnight "empty batch" crash.
///
/// At midnight `RepeatedlyTask` fires `PlayTimeTrackerController.doLoopUpdate`, which hops to the
/// sync thread and calls `updateAll()`. `updateAll()` sizes its arrays by the number of online
/// players but skips AFK ones, so the batch size it passes down is zero whenever nobody is online
/// or everybody is AFK. `pushWorkflowTaskBatch` used to reject `size <= 0` outright, so the loop
/// update blew up with `IllegalArgumentException: Invalid arguments for pushWorkflowTaskBatch`.
///
/// These tests drive the real controller with a recording executor; no task bodies are executed,
/// so no database is required.
public class TestEmptyBatchUpdate {

    private ServerMock server;
    private DemoPTT plugin;
    private RecordingTaskExecutor executor;
    private Set<UUID> afkPlayers;
    private PlayTimeTrackerController controller;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        var description = new PluginDescriptionFile("PlayTimeTracker", "1.0.0", DemoPTT.class.getName());
        this.plugin = MockBukkit.loadWith(DemoPTT.class, description, new Object[0]);
        this.executor = new RecordingTaskExecutor();
        this.afkPlayers = new HashSet<>();
        var context = new Context(this.plugin, this.executor, null, null);
        var timeBuilder = new PiecewiseTimeInfo.Builder(ZoneId.systemDefault(), DayOfWeek.MONDAY);
        this.controller = new PlayTimeTrackerController(context, timeBuilder, this.afkPlayers::contains);
        // the constructor arms RepeatedlyTask; forget that scheduling so the assertions below only
        // see what the batch itself does
        this.executor.reset();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (this.controller != null) {
            this.controller.close();
            this.controller = null;
        }
        MockBukkit.unmock();
    }

    /// Reproduces the production stack: TaskExecutor.runSync -> doLoopUpdate -> updateAll ->
    /// pushWorkflowTaskBatch, with an empty server.
    @Test
    void testMidnightLoopUpdateWithNoOnlinePlayers() throws Exception {
        Assertions.assertEquals(0, this.server.getOnlinePlayers().size());

        var doLoopUpdate = PlayTimeTrackerController.class.getDeclaredMethod("doLoopUpdate", Long.class);
        doLoopUpdate.setAccessible(true);
        // tick == null: the loop callback arrives on the timer thread and defers to the sync thread
        doLoopUpdate.invoke(this.controller, (Long) null);
        Assertions.assertEquals(1, this.executor.syncQueue.size(), "loop update should defer to the sync thread");

        // drains exactly like TaskExecutor.runSync, but without swallowing exceptions
        Assertions.assertEquals(1, this.executor.drainSync());

        Assertions.assertTrue(this.executor.asyncTasks.isEmpty(), "no workflow should be dispatched");
        Assertions.assertTrue(playerTasks(this.controller).isEmpty(), "no chain should be created");
    }

    @Test
    void testUpdateAllWithNoOnlinePlayers() throws Exception {
        Assertions.assertEquals(0, this.server.getOnlinePlayers().size());

        Assertions.assertEquals(0, this.controller.updateAll());

        assertNoExecutorInteraction();
        Assertions.assertTrue(playerTasks(this.controller).isEmpty());
    }

    @Test
    void testUpdateAllWithAllPlayersAfk() throws Exception {
        for (int i = 0; i < 3; i++) {
            var player = this.server.addPlayer("afk" + i);
            this.afkPlayers.add(player.getUniqueId());
        }
        Assertions.assertEquals(3, this.server.getOnlinePlayers().size());

        Assertions.assertEquals(0, this.controller.updateAll());

        assertNoExecutorInteraction();
        Assertions.assertTrue(playerTasks(this.controller).isEmpty());
    }

    @Test
    void testUpdateAllWithSomePlayersAfk() throws Exception {
        var active = new HashSet<UUID>();
        for (int i = 0; i < 3; i++) {
            var player = this.server.addPlayer("active" + i);
            active.add(player.getUniqueId());
        }
        var afk = this.server.addPlayer("afk");
        this.afkPlayers.add(afk.getUniqueId());

        Assertions.assertEquals(3, this.controller.updateAll());

        Assertions.assertEquals(3, this.executor.asyncTasks.size(), "one workflow per non-AFK player");
        Assertions.assertEquals(active, playerTasks(this.controller).keySet());
        Assertions.assertTrue(this.executor.cancelled.isEmpty());
        Assertions.assertTrue(this.executor.scheduled.isEmpty());
    }

    @Test
    void testPushWorkflowTaskBatchZeroSizeIsNoOp() throws Exception {
        // over-sized backing arrays with a zero fill count: exactly what updateAll() hands over
        // when every online player is AFK
        var uuids = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
        var tasks = new ITask[]{tick -> {}, tick -> {}};

        Assertions.assertEquals(0, this.controller.pushWorkflowTaskBatch(uuids, tasks, 0));
        Assertions.assertEquals(0, this.controller.pushWorkflowTaskBatch(new UUID[0], new ITask[0], 0));

        assertNoExecutorInteraction();
        Assertions.assertTrue(playerTasks(this.controller).isEmpty(), "a zero-length batch must not touch the chain map");
    }

    @Test
    void testPushWorkflowTaskBatchNonEmpty() throws Exception {
        var uuids = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
        var tasks = new ITask[]{tick -> {}, tick -> {}};

        Assertions.assertEquals(2, this.controller.pushWorkflowTaskBatch(uuids, tasks, 2));

        Assertions.assertEquals(2, this.executor.asyncTasks.size());
        Assertions.assertEquals(Set.of(uuids), playerTasks(this.controller).keySet());
    }

    @Test
    void testPushWorkflowTaskBatchRejectsInvalidArguments() throws Exception {
        var uuids = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
        var tasks = new ITask[]{tick -> {}, tick -> {}};

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.controller.pushWorkflowTaskBatch(uuids, tasks, -1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.controller.pushWorkflowTaskBatch(uuids, tasks, 3),
                "size larger than both arrays");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.controller.pushWorkflowTaskBatch(new UUID[1], tasks, 2),
                "undersized uuid array");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.controller.pushWorkflowTaskBatch(uuids, new ITask[1], 2),
                "undersized task array");

        assertNoExecutorInteraction();
        Assertions.assertTrue(playerTasks(this.controller).isEmpty());
    }

    private void assertNoExecutorInteraction() {
        Assertions.assertTrue(this.executor.asyncTasks.isEmpty(), "no workflow should be dispatched");
        Assertions.assertTrue(this.executor.syncQueue.isEmpty(), "nothing should be queued on the sync thread");
        Assertions.assertTrue(this.executor.scheduled.isEmpty(), "nothing should be scheduled");
        Assertions.assertTrue(this.executor.cancelled.isEmpty(), "nothing should be cancelled");
    }

    private static Map<UUID, ?> playerTasks(TaskChainController controller) throws Exception {
        Field field = TaskChainController.class.getDeclaredField("playerTasks");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var map = (Map<UUID, ?>) field.get(controller);
        return map;
    }

    /// Records what the controller asks of the executor instead of running it, so the workflow
    /// bodies (and therefore the database layer) are never touched.
    private static final class RecordingTaskExecutor implements ITaskExecutor {

        final List<ITask> asyncTasks = new ArrayList<>();
        final Deque<ITask> syncQueue = new ArrayDeque<>();
        final List<Object> scheduled = new ArrayList<>();
        final List<Object> cancelled = new ArrayList<>();

        private long tick;

        @Override
        public void sync(ITask task) {
            this.syncQueue.add(task);
        }

        @Override
        public void async(ITask task) {
            this.asyncTasks.add(task);
        }

        @Override
        public Object scheduleSync(ITask task, long delay, TimeUnit unit) {
            var handle = new Object();
            this.scheduled.add(handle);
            return handle;
        }

        @Override
        public Object scheduleAsync(ITask task, long delay, TimeUnit unit) {
            var handle = new Object();
            this.scheduled.add(handle);
            return handle;
        }

        @Override
        public boolean cancelTask(Object handle) {
            this.cancelled.add(handle);
            return true;
        }

        @Override
        public LongObjectImmutablePair<TimeUnit> getTimerPrecision() {
            return LongObjectImmutablePair.of(100L, TimeUnit.MILLISECONDS);
        }

        void reset() {
            this.asyncTasks.clear();
            this.syncQueue.clear();
            this.scheduled.clear();
            this.cancelled.clear();
        }

        /// Mirrors TaskExecutor.runSync, minus the catch-and-log, so failures surface in the test.
        int drainSync() {
            int count = 0;
            var syncTick = this.tick++;
            for (var task = this.syncQueue.poll(); task != null; task = this.syncQueue.poll()) {
                task.execute(syncTick);
                count++;
            }
            return count;
        }
    }
}
