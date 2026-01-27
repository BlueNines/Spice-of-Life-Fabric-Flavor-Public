package com.sol2f.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.server.FoodUseHandler;
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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(" ");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("sol2f")
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
                        com.sol2f.server.FoodUseHandler.clearEatenFoods(player);
                        src.sendFeedback(() -> Text.translatable("sol2f.commands.clearhealthy.success"), false);
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
        Set<String> eatenFoods = FoodUseHandler.getEatenFoods(target);
        String timeStr = LocalDateTime.now().format(TIME_FORMAT);

        // 构建输出
        String header = "[sol2f food list | player:" + target.getName().getString() + " | time:" + timeStr + "]";
        String foodList = "{" + String.join(", ", eatenFoods) + "}";

        source.sendFeedback(() -> Text.literal(header), false);
        source.sendFeedback(() -> Text.literal(foodList), false);

        return 1;
    }
}