package com.sol2f.command;

// ...existing code...
import com.sol2f.SpiceOfLifeFabricFlavor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class FoodCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("sol2f").then(literal("clearhealthy").executes(ctx -> {
                ServerCommandSource src = ctx.getSource();
                if (!src.hasPermissionLevel(2)) {
                    src.sendError(Text.translatable("commands.generic.permission"));
                    return 0;
                }

                ServerPlayerEntity player = src.getPlayer();
                if (player == null) {
                    src.sendError(Text.translatable("commands.generic.must_be_player"));
                    return 0;
                }

                // 使用中央清除逻辑确保与重生重置相同的行为
                try {
                    com.sol2f.server.FoodUseHandler.clearEatenFoods(player);
                    src.sendFeedback(() -> Text.translatable("commands.sol2f.clearhealthy.success"), false);
                } catch (Exception e) {
                    SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to clear healthy", e);
                    src.sendError(Text.translatable("commands.sol2f.clearhealthy.failed"));
                    return 0;
                }

                return 1;
            })));
        });
    }
}