package com.sol2f.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.function.BiConsumer;
    
public class SOL2FRightClickItem extends Item {// 创建物品一个类that可以在右键点击时执行特定操作，以添加功能书

    private final BiConsumer<ServerPlayerEntity, ItemStack> onRightClick;

    public SOL2FRightClickItem(Settings settings, BiConsumer<ServerPlayerEntity, ItemStack> onRightClick) {
        super(settings);
        this.onRightClick = onRightClick;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            onRightClick.accept(serverPlayer, user.getStackInHand(hand));
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}