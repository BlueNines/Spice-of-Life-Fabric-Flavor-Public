package com.sol2f.mixin;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.module.FoodModule;

import me.shedaniel.autoconfig.AutoConfig;

@Mixin(PlayerEntity.class)
public abstract class PlayerEatMixin {

    @Redirect(
        method = "eatFood(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/component/type/FoodComponent;)Lnet/minecraft/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/HungerManager;eat(Lnet/minecraft/component/type/FoodComponent;)V"
        )
    )
    private void redirectEat(HungerManager hungerManager, FoodComponent originalComponent, World world, ItemStack stack, FoodComponent foodComponent) {
        try {
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().food.EnableNutritionModification){
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().food.Blacklist.contains(stack.getName().getString())) {
                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | Food is in blacklist, using original component");
                    }
                    hungerManager.eat(originalComponent);
                } else {
                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                    // 记录每次调用
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | redirectEat CALLED");
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | World is client: {}, Thread: {}", world.isClient(), Thread.currentThread().getName());
                    }

                    if (world.isClient()) {
                        if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | Client side, using original component");
                        }
                        hungerManager.eat(originalComponent);
                        return;
                    }

                    PlayerEntity player = (PlayerEntity)(Object)this;
                    if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                        if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | Not ServerPlayerEntity, using original");
                        }
                        hungerManager.eat(originalComponent);
                        return;
                    }

                    // 服务端逻辑
                    String itemName = stack.getName().getString();
                    String playerName = serverPlayer.getName().getString();
                    int originalNutrition = originalComponent.nutrition();
                    float originalSaturation = originalComponent.saturation();

                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | Server - Player: {}, Item: {}, Original: {}/{}",  playerName, itemName, originalNutrition, originalSaturation);
                    }

                    double newNutritionDouble = FoodModule.FoodCalculate(serverPlayer, stack, originalComponent);
                    int newNutrition = (int)Math.round(newNutritionDouble);

                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | FoodModule returned: {}, rounded: {}", newNutritionDouble, newNutrition);
                    }

                    if (newNutrition == originalNutrition) {
                        if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | No change, using original");
                        }
                        hungerManager.eat(originalComponent);
                        return;
                    }

                    float newSaturation = newNutrition * (originalSaturation / originalNutrition);

                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | New: {}/{}", newNutrition, newSaturation);
                    }

                    FoodComponent modifiedComponent = new FoodComponent(
                        newNutrition,
                        newSaturation,
                        originalComponent.canAlwaysEat(),
                        originalComponent.eatSeconds(),
                        originalComponent.usingConvertsTo(),
                        originalComponent.effects()
                    );

                    // 记录 HungerManager 当前状态
                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode){
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | Before eat - FoodLevel: {}, Saturation: {}",  hungerManager.getFoodLevel(), hungerManager.getSaturationLevel());
                    }
                    hungerManager.eat(modifiedComponent);

                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().dev.DeveloperMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f.PlayerEatMixin | After eat - FoodLevel: {}, Saturation: {}", hungerManager.getFoodLevel(), hungerManager.getSaturationLevel());
                    }
                }
            }
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f.PlayerEatMixin | Error in redirectEat: {}", e.getMessage());
        }
    }
}