package com.sol2f.sync;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FDatabaseConfig;
import com.sol2f.module.HealthModule;
import com.sol2f.network.NetWorkHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 管理在线玩家食物进度、异步 MySQL 任务和主线程回调。
 */
public final class PlayerDataSyncService {
    private static volatile PlayerDataSyncService instance;

    private final MinecraftServer server;
    private final DatabaseSettings settings;
    private final ThreadPoolExecutor databaseExecutor;
    private final ScheduledThreadPoolExecutor scheduler;
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();
    private final AtomicBoolean initializeInFlight = new AtomicBoolean();

    private volatile MySqlStorage storage;
    private volatile boolean databaseReady;
    private volatile boolean stopping;

    /**
     * 创建同步服务及其有界线程池。
     */
    private PlayerDataSyncService(MinecraftServer server, DatabaseSettings settings) {
        this.server = server;
        this.settings = settings;
        this.databaseExecutor = new ThreadPoolExecutor(
                settings.executorThreads(),
                settings.executorThreads(),
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(settings.executorQueueSize()),
                namedThreadFactory("sol2f-db-"),
                new ThreadPoolExecutor.AbortPolicy());
        this.databaseExecutor.allowCoreThreadTimeOut(false);
        this.scheduler = new ScheduledThreadPoolExecutor(1, namedThreadFactory("sol2f-scheduler-"));
        this.scheduler.setRemoveOnCancelPolicy(true);
    }

    /**
     * 按专用服务器配置启动同步服务。
     */
    public static void start(MinecraftServer server, Sol2FDatabaseConfig config) {
        if (instance != null) {
            throw new IllegalStateException("Player data sync service is already running");
        }

        DatabaseSettings settings;
        try {
            settings = DatabaseSettings.from(config);
        } catch (IllegalArgumentException exception) {
            SpiceOfLifeFabricFlavor.LOGGER.error("MySQL synchronization config is invalid: {}", exception.getMessage());
            return;
        }
        if (!settings.enabled()) {
            SpiceOfLifeFabricFlavor.LOGGER.info("MySQL player synchronization is disabled");
            return;
        }

        PlayerDataSyncService service = new PlayerDataSyncService(server, settings);
        instance = service;
        SpiceOfLifeFabricFlavor.LOGGER.info("Starting MySQL player synchronization: {}", settings);
        service.initializeStorageAsync();
    }

    /**
     * 停止同步服务并有限等待已经提交的数据库任务。
     */
    public static void stop() {
        PlayerDataSyncService service = instance;
        instance = null;
        if (service != null) {
            service.shutdown();
        }
    }

    /**
     * 处理玩家登录；未启用数据库时返回 false。
     */
    public static boolean handleJoin(ServerPlayerEntity player) {
        PlayerDataSyncService service = instance;
        if (service == null) {
            return false;
        }
        service.join(player);
        return true;
    }

    /**
     * 移除玩家在线会话，不取消已经提交的不可变数据库写入。
     */
    public static void handleDisconnect(UUID playerUuid) {
        PlayerDataSyncService service = instance;
        if (service != null) {
            service.sessions.remove(playerUuid);
        }
    }

    /**
     * 返回不包含凭据的数据库同步状态文本。
     */
    public static String describeStatus() {
        PlayerDataSyncService service = instance;
        if (service == null) {
            return "disabled";
        }
        return (service.databaseReady ? "ready" : "fallback")
                + ", group=" + service.settings.syncGroup()
                + ", server=" + service.settings.serverId()
                + ", sessions=" + service.sessions.size()
                + ", queued=" + service.databaseExecutor.getQueue().size();
    }

    /**
     * 返回在线会话食物快照；没有会话时返回 null。
     */
    public static Set<String> getFoodsSnapshot(UUID playerUuid) {
        PlayerDataSyncService service = instance;
        if (service == null) {
            return null;
        }
        PlayerSession session = service.sessions.get(playerUuid);
        return session == null ? null : new HashSet<>(session.foods);
    }

    /**
     * 记录玩家首次食用的新食物并异步刷入数据库。
     */
    public static void recordFood(ServerPlayerEntity player, String foodId, Set<String> currentFoods) {
        PlayerDataSyncService service = instance;
        if (service == null) {
            return;
        }
        PlayerSession session = service.sessions.get(player.getUuid());
        if (session == null) {
            return;
        }

        session.foods.clear();
        session.foods.addAll(currentFoods);
        session.pendingFoods.add(foodId);
        session.dirty = true;
        if (session.state == SessionState.READY) {
            service.flushPending(session);
        }
    }

    /**
     * 异步递增 generation 并在成功后清空玩家进度。
     */
    public static boolean clearPlayer(ServerPlayerEntity player, Consumer<Boolean> completion) {
        PlayerDataSyncService service = instance;
        if (service == null) {
            return false;
        }
        service.clear(player, completion);
        return true;
    }

    /**
     * 创建登录会话并启动立即查询和五秒补偿查询。
     */
    private void join(ServerPlayerEntity player) {
        Set<String> localFoods = HealthModule.readLocalEatenFoods(player);
        long localGeneration = HealthModule.getLocalDatabaseGeneration(player);
        boolean localDirty = HealthModule.isLocalDatabaseDirty(player);
        PlayerSession session = new PlayerSession(
                player.getUuid(),
                UUID.randomUUID(),
                player.getName().getString(),
                localFoods,
                localGeneration,
                localDirty);
        sessions.put(player.getUuid(), session);

        HealthModule.applyHealthModifier(player, localFoods);
        NetWorkHandler.syncFoodDataToClient(player, localFoods);
        requestLoad(session);

        scheduler.schedule(
                () -> server.execute(() -> reconcileIfCurrent(session.playerUuid, session.sessionToken)),
                settings.loginReconcileDelaySeconds(),
                TimeUnit.SECONDS);
    }

    /**
     * 在五秒补偿点重新读取数据库当前状态。
     */
    private void reconcileIfCurrent(UUID playerUuid, UUID sessionToken) {
        PlayerSession session = sessions.get(playerUuid);
        if (session == null || !session.sessionToken.equals(sessionToken)) {
            return;
        }
        requestLoad(session);
    }

    /**
     * 异步加载玩家数据库快照。
     */
    private void requestLoad(PlayerSession session) {
        MySqlStorage currentStorage = storage;
        if (!databaseReady || currentStorage == null) {
            session.state = SessionState.FALLBACK;
            return;
        }

        UUID playerUuid = session.playerUuid;
        UUID sessionToken = session.sessionToken;
        String playerName = session.playerName;
        submitDatabase(
                () -> currentStorage.loadPlayer(playerUuid, playerName),
                snapshot -> applySnapshot(playerUuid, sessionToken, snapshot),
                error -> handleLoadFailure(playerUuid, sessionToken, error));
    }

    /**
     * 在服务端线程合并数据库、旧 NBT 和当前会话新增食物。
     */
    private void applySnapshot(UUID playerUuid, UUID sessionToken, PlayerFoodSnapshot snapshot) {
        PlayerSession session = sessions.get(playerUuid);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (session == null || player == null || !session.sessionToken.equals(sessionToken)) {
            return;
        }
        if (session.databaseLoaded && snapshot.generation() < session.generation) {
            SpiceOfLifeFabricFlavor.LOGGER.warn(
                    "Ignored stale MySQL snapshot for {}: snapshot generation {}, current generation {}",
                    playerUuid, snapshot.generation(), session.generation);
            return;
        }

        boolean firstSuccessfulLoad = !session.databaseLoaded;
        PlayerFoodMerge.MergeResult mergeResult = PlayerFoodMerge.merge(
                snapshot.foods(),
                snapshot.generation(),
                firstSuccessfulLoad,
                session.loginFoods,
                session.loginGeneration,
                session.loginDirty,
                session.pendingFoods);
        Set<String> merged = new HashSet<>(mergeResult.foods());
        if (session.generation != snapshot.generation()) {
            session.writeInFlight = false;
        }
        session.pendingFoods.addAll(mergeResult.missingFromDatabase());
        session.foods.clear();
        session.foods.addAll(merged);
        session.generation = snapshot.generation();
        session.databaseLoaded = true;
        session.state = SessionState.READY;
        session.dirty = !session.pendingFoods.isEmpty();

        HealthModule.saveLocalEatenFoods(player, merged, session.dirty);
        HealthModule.markLocalDatabaseState(player, session.generation, session.dirty);
        HealthModule.applyHealthModifier(player, merged);
        NetWorkHandler.syncFoodDataToClient(player, merged);
        flushPending(session);
    }

    /**
     * 处理加载失败并安排有界重试。
     */
    private void handleLoadFailure(UUID playerUuid, UUID sessionToken, Throwable error) {
        PlayerSession session = sessions.get(playerUuid);
        if (session == null || !session.sessionToken.equals(sessionToken)) {
            return;
        }
        session.state = SessionState.FALLBACK;
        SpiceOfLifeFabricFlavor.LOGGER.warn("Failed to load MySQL player data for {}: {}", playerUuid, error.getMessage());
        scheduler.schedule(
                () -> server.execute(() -> reconcileIfCurrent(playerUuid, sessionToken)),
                settings.retryDelaySeconds(),
                TimeUnit.SECONDS);
    }

    /**
     * 提交当前会话尚未确认的食物写入。
     */
    private void flushPending(PlayerSession session) {
        MySqlStorage currentStorage = storage;
        if (!databaseReady || currentStorage == null || session.state != SessionState.READY
                || session.writeInFlight || session.pendingFoods.isEmpty()) {
            return;
        }

        Set<String> batch = new HashSet<>(session.pendingFoods);
        UUID playerUuid = session.playerUuid;
        UUID sessionToken = session.sessionToken;
        long generation = session.generation;
        session.writeInFlight = true;
        submitDatabase(
                () -> {
                    currentStorage.insertFoods(playerUuid, generation, batch);
                    return Boolean.TRUE;
                },
                ignored -> finishWrite(playerUuid, sessionToken, generation, batch),
                error -> handleWriteFailure(playerUuid, sessionToken, error));
    }

    /**
     * 在主线程确认成功写入并继续处理后续脏数据。
     */
    private void finishWrite(UUID playerUuid, UUID sessionToken, long generation, Set<String> writtenFoods) {
        PlayerSession session = sessions.get(playerUuid);
        if (session == null || !session.sessionToken.equals(sessionToken) || session.generation != generation) {
            return;
        }
        session.writeInFlight = false;
        session.pendingFoods.removeAll(writtenFoods);
        session.dirty = !session.pendingFoods.isEmpty();

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (player != null) {
            HealthModule.saveLocalEatenFoods(player, session.foods, session.dirty);
            HealthModule.markLocalDatabaseState(player, generation, session.dirty);
        }
        flushPending(session);
    }

    /**
     * 保留失败批次并在延迟后重新加载数据库状态。
     */
    private void handleWriteFailure(UUID playerUuid, UUID sessionToken, Throwable error) {
        PlayerSession session = sessions.get(playerUuid);
        if (session == null || !session.sessionToken.equals(sessionToken)) {
            return;
        }
        session.writeInFlight = false;
        session.state = SessionState.FALLBACK;
        SpiceOfLifeFabricFlavor.LOGGER.warn("Failed to save MySQL player data for {}: {}", playerUuid, error.getMessage());
        scheduler.schedule(
                () -> server.execute(() -> reconcileIfCurrent(playerUuid, sessionToken)),
                settings.retryDelaySeconds(),
                TimeUnit.SECONDS);
    }

    /**
     * 提交 generation 递增事务并在成功后清空在线状态。
     */
    private void clear(ServerPlayerEntity player, Consumer<Boolean> completion) {
        PlayerSession session = sessions.get(player.getUuid());
        MySqlStorage currentStorage = storage;
        if (session == null || !databaseReady || currentStorage == null) {
            completion.accept(false);
            return;
        }

        UUID playerUuid = player.getUuid();
        UUID sessionToken = session.sessionToken;
        String playerName = player.getName().getString();
        submitDatabase(
                () -> currentStorage.advanceGeneration(playerUuid, playerName),
                generation -> finishClear(playerUuid, sessionToken, generation, completion),
                error -> {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("Failed to clear MySQL player data for {}: {}", playerUuid,
                            error.getMessage());
                    completion.accept(false);
                });
    }

    /**
     * 在主线程应用成功的 generation 清空结果。
     */
    private void finishClear(UUID playerUuid, UUID sessionToken, long generation, Consumer<Boolean> completion) {
        PlayerSession session = sessions.get(playerUuid);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (session == null || player == null || !session.sessionToken.equals(sessionToken)) {
            completion.accept(false);
            return;
        }

        session.generation = generation;
        session.foods.clear();
        session.pendingFoods.clear();
        session.writeInFlight = false;
        session.dirty = false;
        session.state = SessionState.READY;
        session.databaseLoaded = true;
        HealthModule.saveLocalEatenFoods(player, Collections.emptySet(), false);
        HealthModule.markLocalDatabaseState(player, generation, false);
        HealthModule.applyHealthModifier(player, Collections.emptySet());
        NetWorkHandler.syncFoodDataToClient(player, Collections.emptySet());
        completion.accept(true);
    }

    /**
     * 在数据库线程初始化连接池和业务表。
     */
    private void initializeStorageAsync() {
        if (stopping || !initializeInFlight.compareAndSet(false, true)) {
            return;
        }
        try {
            databaseExecutor.execute(() -> {
                MySqlStorage candidate = null;
                try {
                    candidate = new MySqlStorage(settings);
                    candidate.initializeSchema();
                    storage = candidate;
                    databaseReady = true;
                    server.execute(this::onStorageReady);
                } catch (Throwable error) {
                    if (candidate != null) {
                        candidate.close();
                    }
                    databaseReady = false;
                    SpiceOfLifeFabricFlavor.LOGGER.warn("MySQL synchronization initialization failed: {}", error.getMessage());
                    scheduleInitializationRetry();
                } finally {
                    initializeInFlight.set(false);
                }
            });
        } catch (RejectedExecutionException exception) {
            initializeInFlight.set(false);
            SpiceOfLifeFabricFlavor.LOGGER.error("MySQL initialization task was rejected because the queue is full");
        }
    }

    /**
     * 数据库恢复后重新加载所有仍在线的玩家会话。
     */
    private void onStorageReady() {
        SpiceOfLifeFabricFlavor.LOGGER.info("MySQL player synchronization is ready for group {} on server {}",
                settings.syncGroup(), settings.serverId());
        for (PlayerSession session : sessions.values()) {
            requestLoad(session);
        }
    }

    /**
     * 延迟重试数据库初始化，不阻塞服务器线程。
     */
    private void scheduleInitializationRetry() {
        if (!stopping) {
            scheduler.schedule(this::initializeStorageAsync, settings.retryDelaySeconds(), TimeUnit.SECONDS);
        }
    }

    /**
     * 向有界执行器提交数据库任务，并把结果派发回服务端线程。
     */
    private <T> void submitDatabase(CheckedSupplier<T> task, Consumer<T> success, Consumer<Throwable> failure) {
        try {
            databaseExecutor.execute(() -> {
                try {
                    T result = task.get();
                    server.execute(() -> success.accept(result));
                } catch (Throwable error) {
                    server.execute(() -> failure.accept(error));
                }
            });
        } catch (RejectedExecutionException exception) {
            SpiceOfLifeFabricFlavor.LOGGER.error("MySQL task queue is full; preserving local dirty data");
            server.execute(() -> failure.accept(exception));
        }
    }

    /**
     * 关闭调度器、数据库执行器和连接池。
     */
    private void shutdown() {
        stopping = true;
        scheduler.shutdownNow();
        databaseExecutor.shutdown();
        boolean terminated = false;
        try {
            terminated = databaseExecutor.awaitTermination(settings.shutdownWaitSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (!terminated) {
            int dropped = databaseExecutor.shutdownNow().size();
            SpiceOfLifeFabricFlavor.LOGGER.warn("MySQL synchronization stopped with {} queued tasks unfinished", dropped);
        }

        MySqlStorage currentStorage = storage;
        storage = null;
        databaseReady = false;
        if (currentStorage != null) {
            currentStorage.close();
        }
    }

    /**
     * 创建带清晰名称的守护线程工厂。
     */
    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * 表示数据库线程中允许抛出受检异常的任务。
     */
    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /**
     * 表示在线玩家的数据库准备状态。
     */
    private enum SessionState {
        LOADING,
        READY,
        FALLBACK
    }

    /**
     * 保存仅由服务端线程修改的在线玩家状态。
     */
    private static final class PlayerSession {
        private final UUID playerUuid;
        private final UUID sessionToken;
        private final String playerName;
        private final Set<String> loginFoods;
        private final long loginGeneration;
        private final boolean loginDirty;
        private final Set<String> foods;
        private final Set<String> pendingFoods = new HashSet<>();
        private SessionState state = SessionState.LOADING;
        private long generation;
        private boolean dirty;
        private boolean writeInFlight;
        private boolean databaseLoaded;

        /**
         * 创建一次登录对应的会话快照。
         */
        private PlayerSession(UUID playerUuid, UUID sessionToken, String playerName, Set<String> localFoods,
                long localGeneration, boolean localDirty) {
            this.playerUuid = playerUuid;
            this.sessionToken = sessionToken;
            this.playerName = playerName;
            this.loginFoods = Set.copyOf(localFoods);
            this.loginGeneration = localGeneration;
            this.loginDirty = localDirty;
            this.foods = new HashSet<>(localFoods);
            this.generation = Math.max(0L, localGeneration);
            this.dirty = localDirty;
        }
    }
}
