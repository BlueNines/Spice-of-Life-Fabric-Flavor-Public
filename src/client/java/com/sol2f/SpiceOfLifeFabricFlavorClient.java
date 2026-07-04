package com.sol2f;

import com.sol2f.gui.FoodBookScreen;
import com.sol2f.key.KeyBindings;
import com.sol2f.network.payload.*;
import com.sol2f.config.SOL2FClientConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpiceOfLifeFabricFlavorClient implements ClientModInitializer {

    private static final Set<String> consumed = Collections.synchronizedSet(new HashSet<>()); // 记录已食用的食物ID
    private static final Set<String> allFoods = Collections.synchronizedSet(new HashSet<>());
    private static int CurrentHealth = 0;
    private static int MaxHealth = 240;
    private static volatile boolean allFoodsReceived = false;
    private static volatile boolean whitelistEnabled = false;

    @Override
    public void onInitializeClient() {

        // 注册客户端配置
        AutoConfig.register(SOL2FClientConfig.class, GsonConfigSerializer::new);

        // 注册网络包接收器
        ClientPlayNetworking.registerGlobalReceiver(S2CEatenFoodListPayload.PACKET_ID, (payload, context) -> {// 接收已食用食物列表
            List<String> list = payload.foods();
            synchronized (consumed) {
                consumed.clear();
                consumed.addAll(list);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(S2CAllFoodListPayload.PACKET_ID, (payload, context) -> {
            List<String> list = payload.allFoods();
            synchronized (allFoods) {
                allFoods.clear();
                allFoods.addAll(list);
            }
            allFoodsReceived = true;
        });

        ClientPlayNetworking.registerGlobalReceiver(S2CHealthPayload.PACKET_ID, (payload, context) -> {
            CurrentHealth = payload.health();
        });

        ClientPlayNetworking.registerGlobalReceiver(S2CHealthMaxPayload.PACKET_ID, (payload, context) -> {
            MaxHealth = payload.maxHealth();
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenFoodBookPayload.PACKET_ID, (payload, context) -> {
            MinecraftClient.getInstance().execute(() ->
                MinecraftClient.getInstance().setScreen(new FoodBookScreen())
            );
        });

        // Tooltip
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (!stack.contains(net.minecraft.component.DataComponentTypes.FOOD)) return;
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            // 基于服务端的allFoods列表进行判断，如果不在allFoods列表中且未发现则不显示tooltip
            synchronized (allFoods) {
                if (!allFoods.contains(id) && !consumed.contains(id)) return;
            }
            if (!consumed.contains(id) && AutoConfig.getConfigHolder(SOL2FClientConfig.class).getConfig().GUISetting.ShowUnconsumedTooltips) {
                lines.add(Text.translatable("sol2f.tooltip.unconsumed")
                    .formatted(Formatting.AQUA)
                    .formatted(Formatting.ITALIC));
            } else if (consumed.contains(id) && AutoConfig.getConfigHolder(SOL2FClientConfig.class).getConfig().GUISetting.ShowConsumedTooltips) {
                lines.add(Text.translatable("sol2f.tooltip.consumed")
                    .formatted(Formatting.GOLD)
                    .formatted(Formatting.ITALIC));
            }
        });

        // 注册快捷键
        KeyBindings.register();

        // 监听按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.OPEN_FOOD_BOOK.wasPressed()) {
                client.setScreen(new FoodBookScreen());
            }
        });

        // 连接时请求全部食物列表
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!allFoodsReceived) {
                ClientPlayNetworking.send(new C2SRequestAllFoodListPayload());
            }
        });
    }

    // 公共访问方法（不变）
    public static int getCurrentHealth() {
        return CurrentHealth;
    }

    public static int getMaxHealth() {
        return MaxHealth;
    }

    public static int getConsumedCount() {
        synchronized (consumed) {
            return consumed.size();
        }
    }

    public static int getAllCount() {
        synchronized (allFoods) {
            return allFoods.size();
        }
    }

    public static boolean isConsumed(String id) {
        return consumed.contains(id);
    }

    public static List<String> getConsumedSnapshot() {
        synchronized (consumed) {
            return new ArrayList<>(consumed);
        }
    }
}