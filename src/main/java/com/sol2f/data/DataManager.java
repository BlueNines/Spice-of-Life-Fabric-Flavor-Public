package com.sol2f.data;

import com.sol2f.SpiceOfLifeFabricFlavor;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class DataManager {
    static NbtCompound readPersistentCompound(ServerPlayerEntity player) {
        if (player instanceof com.sol2f.interfaces.IEntityDataSaver saver) {
            return saver.getPersistentData();
        }

        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to read persistent compound");
        return new NbtCompound();
    }

    static void writePersistentCompound(ServerPlayerEntity player, NbtCompound data) {
        if (player instanceof com.sol2f.interfaces.IEntityDataSaver saver) {
            NbtCompound persistent = saver.getPersistentData();
            for (String key : data.getKeys()) {
                persistent.put(key, data.get(key));
            }
            return;
        }
        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to write persistent compound. Player is not IEntityDataSaver!");
    }
}
