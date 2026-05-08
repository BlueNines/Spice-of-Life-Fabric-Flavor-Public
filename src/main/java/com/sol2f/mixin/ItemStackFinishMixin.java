package com.sol2f.mixin;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.module.HealthModule;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackFinishMixin {
    @Inject(method = "finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;", 
            at = @At("HEAD"))
    private void onFinishUsing(net.minecraft.world.World world, LivingEntity consumer, CallbackInfoReturnable<ItemStack> cir) {
        try {
            // 基础安全检查链
            if (consumer == null || world == null || world.isClient) return;

            // 获取当前ItemStack实例（Mixin目标为实例方法，this即ItemStack）
            ItemStack stack = (ItemStack)(Object)this;
            
            // 使用组件系统检查食物
            FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
            if (foodComponent == null) return;

            // 玩家类型检查
            if (!(consumer instanceof ServerPlayerEntity)) return;

            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) consumer;

            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                SpiceOfLifeFabricFlavor.LOGGER.debug(
                    "ItemStackFinishMixin.onFinishUsing | Player {} ate modded food item {}", 
                    serverPlayer.getName().getString(), 
                    stack.getItem()
                );
            }

            // 调用逻辑
            HealthModule.onFoodEaten(serverPlayer, stack);
        } catch (Throwable t) {
            ItemStack stack = (ItemStack)(Object)this;
            SpiceOfLifeFabricFlavor.LOGGER.error(
                "ItemStackFinishMixin.onFinishUsing | error to process food item {}", 
                stack.getItem(), 
                t
            );
        }
    }
}