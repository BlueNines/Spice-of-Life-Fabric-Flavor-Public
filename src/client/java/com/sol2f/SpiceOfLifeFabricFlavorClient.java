package com.sol2f;

import com.sol2f.gui.FoodBookScreen;
import com.sol2f.key.KeyBindings;
import com.sol2f.network.NetworkChannels;
import com.sol2f.config.SOL2FClientConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
    private static double CurrentHealth = 0.0D;
    private static double MaxHealth = 240.0D;
    private static volatile boolean allFoodsReceived = false;

    /**
     * 注册客户端配置、网络接收器、Tooltip、快捷键和连接事件。
     */
    @Override
    public void onInitializeClient() {

        // 注册客户端配置
        AutoConfig.register(SOL2FClientConfig.class, GsonConfigSerializer::new);

        // 注册网络包接收器
        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.S2C_EATEN_FOOD_LIST,
                (client, handler, buffer, responseSender) -> {
            List<String> list = NetworkChannels.readFoodList(buffer);
            client.execute(() -> replaceSet(consumed, list));
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.S2C_ALL_FOOD_LIST,
                (client, handler, buffer, responseSender) -> {
            List<String> list = NetworkChannels.readFoodList(buffer);
            client.execute(() -> {
                replaceSet(allFoods, list);
                allFoodsReceived = true;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.S2C_HEALTH,
                (client, handler, buffer, responseSender) -> {
            double health = buffer.readDouble();
            client.execute(() -> CurrentHealth = health);
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.S2C_HEALTH_MAX,
                (client, handler, buffer, responseSender) -> {
            double maxHealth = buffer.readDouble();
            client.execute(() -> MaxHealth = maxHealth);
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkChannels.S2C_OPEN_FOOD_BOOK,
                (client, handler, buffer, responseSender) -> {
            if (buffer.readableBytes() != 0) {
                return;
            }
            client.execute(() -> client.setScreen(new FoodBookScreen()));
        });

        // Tooltip
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, lines) -> {
            if (stack.getItem().getFoodComponent() == null) return;
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
            allFoodsReceived = false;
            ClientPlayNetworking.send(NetworkChannels.C2S_REQUEST_ALL_FOOD_LIST, PacketByteBufs.empty());
            ClientPlayNetworking.send(NetworkChannels.C2S_REQUEST_FOOD_LIST, PacketByteBufs.empty());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            replaceSet(consumed, Collections.emptyList());
            replaceSet(allFoods, Collections.emptyList());
            allFoodsReceived = false;
        });
    }

    /**
     * 在同步块中替换客户端集合内容。
     */
    private static void replaceSet(Set<String> target, List<String> values) {
        synchronized (target) {
            target.clear();
            target.addAll(values);
        }
    }

    /**
     * 返回当前服务端同步的生命增益。
     */
    public static double getCurrentHealth() {
        return CurrentHealth;
    }

    /**
     * 返回当前服务端同步的理论最大生命增益。
     */
    public static double getMaxHealth() {
        return MaxHealth;
    }

    /**
     * 返回客户端已发现食物数量。
     */
    public static int getConsumedCount() {
        synchronized (consumed) {
            return consumed.size();
        }
    }

    /**
     * 返回本服可发现食物数量。
     */
    public static int getAllCount() {
        synchronized (allFoods) {
            return allFoods.size();
        }
    }

    /**
     * 判断指定食物是否已经发现。
     */
    public static boolean isConsumed(String id) {
        return consumed.contains(id);
    }

    /**
     * 返回已发现食物的独立快照。
     */
    public static List<String> getConsumedSnapshot() {
        synchronized (consumed) {
            return new ArrayList<>(consumed);
        }
    }
}
