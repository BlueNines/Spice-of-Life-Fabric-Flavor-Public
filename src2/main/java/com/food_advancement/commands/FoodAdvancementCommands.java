package com.food_advancement.commands;

import com.food_advancement.data.PlayerFoodData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

public class FoodAdvancementCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("foodadv")
            .requires(source -> source.hasPermissionLevel(2)) // 需要权限等级2（op）
            .then(literal("clearhealthy")
                .executes(FoodAdvancementCommands::clearHealthy)
            )
        );
    }

    private static int clearHealthy(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
    // 调用PlayerFoodData中的方法来清除数据、同步并更新生命值
    PlayerFoodData.clearEatenFoods(player);
        
        // 发送确认消息
        context.getSource().sendFeedback(
            () -> Text.translatable("commands.foodadv.clearhealthy.success"),
            true
        );
        return Command.SINGLE_SUCCESS;
    }
}
