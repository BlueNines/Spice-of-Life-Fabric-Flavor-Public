package com.sol2f.item;

import com.sol2f.item.RightClickItem;
import com.sol2f.network.FoodPackets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.fabricmc.api.ModInitializer;

public class SOL2FItem implements ModInitializer {

    public static final Item FOOD_BOOK_ITEM = new RightClickItem(
        new Item.Settings().maxCount(1),
        (player, stack) -> {
            // 发送打开食物书界面的数据包给客户端
            ServerPlayNetworking.send(player, FoodPackets.OPEN_FOOD_BOOK_SCREEN, PacketByteBufs.empty());
        }
    );

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier("sol2f", "food_book"), FOOD_BOOK_ITEM);
    }
}