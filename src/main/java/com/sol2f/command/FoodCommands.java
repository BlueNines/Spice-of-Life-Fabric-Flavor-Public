package com.sol2f.command;

import com.sol2f.network.S2CFoodListSync;
import com.sol2f.config.ModConfig;
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

                // clear NBT list
                try {
                    net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
                    player.writeNbt(nbt);
                    nbt.put("sol2f:consumed_foods", new net.minecraft.nbt.NbtList());
                    player.readNbt(nbt);

                    // reset max health
                    player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(ModConfig.getInstance().defaultHealthy);

                    // send empty sync
                    S2CFoodListSync.sendTo(player, java.util.Collections.emptyList());

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
