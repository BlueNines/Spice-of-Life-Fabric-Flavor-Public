package com.sol2f.mixin.client;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {
    @Shadow public abstract Item getItem();

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void addFoodTooltip(net.minecraft.entity.player.PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
    Item item = this.getItem();
    if (player == null) return;
    if (!item.isFood()) return;
    // Tooltip handled centrally in FoodClient via ItemTooltipCallback; mixin no longer injects text.
    }
}
