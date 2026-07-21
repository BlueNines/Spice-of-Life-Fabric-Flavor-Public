package com.sol2f.network;

import java.util.Collection;
import java.util.Set;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.module.HealthModule;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NetWorkHandler {

    private NetWorkHandler() {
    }

    /**
     * 向客户端同步玩家已食用食物。
     */
    public static void syncConsumedFoodToClient(ServerPlayerEntity player, Collection<String> eatenFoods) {
        try {
            ServerPlayNetworking.send(player, NetworkChannels.S2C_EATEN_FOOD_LIST, NetworkChannels.writeFoodList(eatenFoods));
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync food list to client", e);
        }
    }

    /**
     * 向客户端同步当前服务器可用的全部食物。
     */
    public static void syncAllFoodListToClient(ServerPlayerEntity player, Collection<String> allFoods) {
        try {
            ServerPlayNetworking.send(player, NetworkChannels.S2C_ALL_FOOD_LIST, NetworkChannels.writeFoodList(allFoods));
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync all food list to client", e);
        }
    }

    /**
     * 向客户端同步当前生命值增益。
     */
    public static void syncHealthBonusToClient(ServerPlayerEntity player, int healthBonus) {
        try {
            ServerPlayNetworking.send(player, NetworkChannels.S2C_HEALTH, NetworkChannels.writeInt(healthBonus));
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync health bonus to client", e);
        }
    }

    /**
     * 向客户端同步理论最大生命值增益。
     */
    public static void syncHealthMaxToClient(ServerPlayerEntity player, int maxHealthBonus) {
        try {
            ServerPlayNetworking.send(player, NetworkChannels.S2C_HEALTH_MAX, NetworkChannels.writeInt(maxHealthBonus));
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync max health bonus to client", e);
        }
    }

    /**
     * 向客户端发送打开食物书界面的请求。
     */
    public static void openFoodBook(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, NetworkChannels.S2C_OPEN_FOOD_BOOK,
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
    }

    /**
     * 向客户端同步登录后需要的完整食物数据。
     */
    public static void syncFoodDataToClient(ServerPlayerEntity player, Set<String> eatenFoods) {
        syncConsumedFoodToClient(player, eatenFoods);
        syncAllFoodListToClient(player, HealthModule.getAllFoods());
        syncHealthMaxToClient(player, HealthModule.calculateTheoreticalMaxHealthBonus());
    }
}
