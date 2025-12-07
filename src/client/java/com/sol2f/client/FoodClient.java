package com.sol2f.client;

import com.sol2f.Gui.FoodBookScreen;
import com.sol2f.Key.KeyBindings;
import com.sol2f.network.FoodPackets;
import com.sol2f.network.S2CFoodListSync;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
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
    private static volatile boolean allFoodsReceived = false;// 是否已接收所有食物列表
    @Override
    public void onInitializeClient() {
        // 1. 网络包：接收食物列表
        ClientPlayNetworking.registerGlobalReceiver(FoodPackets.S2C_FOOD_LIST, (client, handler, buf, responseSender) -> {// 接收已食用食物列表
            List<String> list = S2CFoodListSync.readList(buf);
            synchronized (consumed) {
                consumed.clear();
                consumed.addAll(list);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(FoodPackets.S2C_ALL_FOOD_LIST, (client, handler, buf, responseSender) -> {// 获取所有食物
            List<String> list = S2CFoodListSync.readList(buf);
            synchronized (allFoods) {
                allFoods.clear();
                allFoods.addAll(list);
            }
            allFoodsReceived = true;// 标记所有食物列表已接收
        });

        ClientPlayNetworking.registerGlobalReceiver(FoodPackets.S2C_HEALTH, (client, handler, buf, responseSender) -> {// 获取当前生命值
            CurrentHealth = buf.readInt();
        });

        ClientPlayNetworking.registerGlobalReceiver(FoodPackets.S2C_HEALTH_MAX, (client, handler, buf, responseSender) -> {// 获取最大生命值
            MaxHealth = buf.readInt();
        });

        // 2. Tooltip：标记未食用食物
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (stack.getItem().getFoodComponent() == null) return;
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (!consumed.contains(id)) {
                lines.add(Text.translatable("sol2f.tooltip.unconsumed")
                    .formatted(Formatting.AQUA)
                    .formatted(Formatting.ITALIC));
            }
        });

        // 3. 注册快捷键（必须在初始化时调用）
        KeyBindings.register();

        // 4. 监听按键事件（每帧末尾检查）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.OPEN_FOOD_BOOK.wasPressed()) {
                client.setScreen(new FoodBookScreen());
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
        if (!allFoodsReceived) {
            ClientPlayNetworking.send(FoodPackets.C2S_REQUEST_ALL_FOOD_LIST, new PacketByteBuf(Unpooled.buffer()));
        }
    });

    }

    // 以下为公共访问方法

    public static int getCurrentHealth() {// 获取当前生命值（给GUI用）
        return CurrentHealth;
    }
    public static int getMaxHealth() {// 获取最大生命值（给GUI用）
        return MaxHealth;
    }

    public static int getConsumedCount() {// 获取已食用食物数量（给GUI用）
        synchronized (consumed) {
            return consumed.size();
        }
    }

    public static int getAllCount() {// 获取所有食物数量（给GUI用）
        synchronized (allFoods) {
            return allFoods.size();
        }
    }

    public static boolean isConsumed(String id) {// 判断某食物是否已食用
        return consumed.contains(id);
    }

    public static List<String> getConsumedSnapshot() {// 获取已食用食物列表缓存快照
        synchronized (consumed) {
            return new ArrayList<>(consumed);
        }
    }
}