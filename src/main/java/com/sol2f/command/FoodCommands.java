package com.sol2f.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.module.HealthModule;
import com.sol2f.network.NetWorkHandler;
import com.sol2f.sync.PlayerDataSyncService;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FoodCommands {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("sol2f")
                .then(literal("status").executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (!source.hasPermissionLevel(2)) {
                        source.sendError(Text.translatable("sol2f.commands.clearhealthy.permission"));
                        return 0;
                    }
                    source.sendFeedback(() -> Text.literal("sol2f mysql: " + PlayerDataSyncService.describeStatus()), false);
                    return 1;
                }))
                .then(literal("clearhealthy").executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    if (!src.hasPermissionLevel(2)) {
                        src.sendError(Text.translatable("sol2f.commands.clearhealthy.permission"));
                        return 0;
                    }

                    ServerPlayerEntity player = src.getPlayer();
                    if (player == null) {
                        src.sendError(Text.translatable("sol2f.commands.clearhealthy.must_be_player"));
                        return 0;
                    }

                    try {// 清除玩家已食用食物记录
                        boolean asynchronous = PlayerDataSyncService.clearPlayer(player, success -> {
                            if (success) {
                                src.sendFeedback(() -> Text.translatable("sol2f.commands.clearhealthy.success"), false);
                            } else {
                                src.sendError(Text.translatable("sol2f.commands.clearhealthy.failed"));
                            }
                        });
                        if (asynchronous) {
                            src.sendFeedback(() -> Text.translatable("sol2f.commands.clearhealthy.processing"), false);
                        } else {
                            HealthModule.cleanEatenFoods(player);
                            src.sendFeedback(() -> Text.translatable("sol2f.commands.clearhealthy.success"), false);
                        }
                    } catch (Exception e) {
                        SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to clear healthy", e);
                        src.sendError(Text.translatable("sol2f.commands.clearhealthy.failed"));
                        return 0;
                    }
                    return 1;
                }))

                .then(literal("getlist")
                    .executes(ctx -> executeGetList(ctx.getSource(), null))
                    .then(argument("player", StringArgumentType.word())
                        .suggests((context, builder) ->
                            CommandSource.suggestMatching(
                                context.getSource().getServer().getPlayerNames(),
                                builder
                            )
                        )
                        .executes(ctx -> executeGetList(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "player")
                        ))
                    )
                )
                .then(literal("sync")
                    .executes(ctx -> executeSync(ctx.getSource(), "both"))
                    .then(literal("AllFoodList").executes(ctx -> executeSync(ctx.getSource(), "allfoodlist")))
                    .then(literal("PlayerData").executes(ctx -> executeSync(ctx.getSource(), "playerdata")))
                )
            );
        });
    }

    private static int executeGetList(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target;

        if (playerName == null) {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                target = (ServerPlayerEntity) source.getEntity();
            } else {
                source.sendError(Text.translatable("sol2f.commands.getlist.must_choose_player"));
                return 0;
            }
        } else {
            target = source.getServer().getPlayerManager().getPlayer(playerName);
            if (target == null) {
                source.sendError(Text.translatable("sol2f.commands.getlist.player_not_found", playerName));
                return 0;
            }
        }

        // 获取已食用食物 ID 列表
        Set<String> eatenFoods = HealthModule.getEatenFoods(target);
        String timeStr = LocalDateTime.now().format(TIME_FORMAT);

        // 构建输出
        String header = "[sol2f food list | player:" + target.getName().getString() + " | time:" + timeStr + "]";
        String foodList = "{" + String.join(", ", eatenFoods) + "}";

        source.sendFeedback(() -> Text.literal(header), false);
        source.sendFeedback(() -> Text.literal(foodList), false);

        return 1;
    }
    
    private static int executeSync(ServerCommandSource source, String syncType) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.translatable("sol2f.commands.sync.must_be_player"));
            return 0;
        }
        
        try {
            switch (syncType.toLowerCase()) {
                case "allfoodlist":
                    // 获取并同步所有非黑名单食物列表
                    Set<String> allFoods = HealthModule.getAllFoods();
                    NetWorkHandler.syncAllFoodListToClient(player, allFoods);
                    
                    // 重新计算并发送理论最大增益值
                    int recalculatedMaxBonus = HealthModule.calculateTheoreticalMaxHealthBonus();
                    NetWorkHandler.syncHealthMaxToClient(player, recalculatedMaxBonus);
                    
                    source.sendFeedback(() -> Text.translatable("sol2f.commands.sync.allfoodlist.success"), false);
                    break;
                    
                case "playerdata":
                    // 同步玩家数据
                    Set<String> eatenFoods = HealthModule.getEatenFoods(player);
                    eatenFoods.retainAll(HealthModule.getAllFoods());
                    NetWorkHandler.syncConsumedFoodToClient(player, eatenFoods);

                    source.sendFeedback(() -> Text.translatable("sol2f.commands.sync.playerdata.success"), false);
                    break;
                    
                case "both":
                default:
                    // 同步所有数据
                    Set<String> bothAllFoods = HealthModule.getAllFoods();
                    NetWorkHandler.syncAllFoodListToClient(player, bothAllFoods);
                    Set<String> playerEaten = HealthModule.getEatenFoods(player);
                    playerEaten.retainAll(bothAllFoods);
                    NetWorkHandler.syncConsumedFoodToClient(player, playerEaten);
                    int currentBonus = Math.max(0, (int) Math.round(player.getMaxHealth() - player.getAttributeBaseValue(
                            net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH)));
                    NetWorkHandler.syncHealthBonusToClient(player, currentBonus);
                    int maxBonus = HealthModule.calculateTheoreticalMaxHealthBonus();
                    NetWorkHandler.syncHealthMaxToClient(player, maxBonus);
                    
                    source.sendFeedback(() -> Text.translatable("sol2f.commands.sync.both.success"), false);
                    break;
            }
            
            return 1;
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to sync data", e);
            source.sendError(Text.translatable("sol2f.commands.sync.failed"));
            return 0;
        }
    }
}
