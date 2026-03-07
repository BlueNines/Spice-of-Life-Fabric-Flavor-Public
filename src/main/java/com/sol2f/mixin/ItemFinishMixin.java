package com.sol2f.mixin;

import com.sol2f.handler.HealthHandler;
import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemFinishMixin {
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void onFinishUsing(ItemStack stack, net.minecraft.world.World world, LivingEntity consumer, CallbackInfoReturnable<ItemStack> cir) {
        try {
            // 基础安全检查链
            if (consumer == null || world == null || world.isClient) return;
            
            // 使用组件系统检查食物
            FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
            if (foodComponent == null) return;  // 不是食物，直接返回
            
            // 黑名单检查wall，过滤掉黑名单物品
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().health.blacklist.contains(itemId)) {
                if (SpiceOfLifeFabricFlavor.LOGGER.isDebugEnabled()) {
                    SpiceOfLifeFabricFlavor.LOGGER.debug(
                        "Item {} is blacklisted, skipping processing", itemId
                    );
                }
                return;
            }
            
            // 玩家类型检查
            if (!(consumer instanceof ServerPlayerEntity)) return;
            
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) consumer;
            
            // 调试日志
            if (SpiceOfLifeFabricFlavor.LOGGER.isDebugEnabled()) {
                SpiceOfLifeFabricFlavor.LOGGER.debug(
                    "Player {} ate modded food item {}", 
                    serverPlayer.getName().getString(), stack.getItem()
                );
            }
            
            // 调用你的处理逻辑
            HealthHandler.onFoodEaten(serverPlayer, stack);
            
        } catch (Throwable t) {
            SpiceOfLifeFabricFlavor.LOGGER.error("ItemFinishMixin error", t);
        }
    }
}