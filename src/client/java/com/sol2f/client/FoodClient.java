package com.sol2f.client;

import com.sol2f.Gui.FoodBookScreen;
import com.sol2f.Key.KeyBindings;
import com.sol2f.network.payload.*;

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

public class FoodClient implements ClientModInitializer {

    private static final Set<String> consumed = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> allFoods = Collections.synchronizedSet(new HashSet<>());
    private static int CurrentHealth = 0;
    private static int MaxHealth = 240;
    private static volatile boolean allFoodsReceived = false;

    @Override
    public void onInitializeClient() {
        // 注册网络包接收器
        ClientPlayNetworking.registerGlobalReceiver(S2CFoodListPayload.PACKET_ID, (payload, context) -> {
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
            if (!consumed.contains(id)) {
                lines.add(Text.translatable("sol2f.tooltip.unconsumed")
                    .formatted(Formatting.AQUA)
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