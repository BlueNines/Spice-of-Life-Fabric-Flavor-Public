package com.sol2f.mixin;

import com.sol2f.server.FoodUseHandler;
import com.sol2f.SpiceOfLifeFabricFlavor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
            if (consumer == null) return;
            if (world == null) return;
            // log basic state for debugging timing issues
            SpiceOfLifeFabricFlavor.LOGGER.debug("ItemFinishMixin: finishUsing called - world.isClient={}, consumerClass={}, stack={}", world.isClient, consumer.getClass().getName(), stack == null ? "<null>" : stack.getItem().toString());
            if (world.isClient) return;
            if (!(consumer instanceof ServerPlayerEntity)) return;

            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) consumer;
            FoodUseHandler.onFoodEaten(serverPlayer, stack);
        } catch (Throwable t) {
            SpiceOfLifeFabricFlavor.LOGGER.error("ItemFinishMixin error", t);
        }
    }
}
