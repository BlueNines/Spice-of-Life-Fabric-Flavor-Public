package com.food_advancement.mixin;

import com.food_advancement.data.PlayerFoodData;
import com.food_advancement.data.ClientFoodData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;


@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void addFoodTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        Item item = this.getItem();
        
        // 只有食物才添加提示
    if (player != null && item.isFood() && !ClientFoodData.hasEaten(item.getTranslationKey())) {
            List<Text> tooltip = cir.getReturnValue();
            // 在第二行（索引1）添加提示文本，如果tooltip为空则添加到第一行
            int insertIndex = tooltip.size() >= 1 ? 1 : 0;
            tooltip.add(insertIndex, Text.translatable("tooltip.food_advancement.unknown_food").setStyle(Style.EMPTY.withColor(Formatting.AQUA)));
        }
    }

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void onFinishUsing(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        // 在方法开始时就获取item，此时它一定是食物本身
        Item item = this.getItem();

        if (!world.isClient() && user instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) user;

            if (item.isFood()) {
                PlayerFoodData.addEatenFood(player, item);
            }
        }
    }
}
