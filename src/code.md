# 目录结构

├── client/
    ├── client
    ├── java/
    │   ├── java
    │   └── com/
    │   │   └── com
    │   │   └── sol2f/
    │   │       └── sol2f
    │   │       ├── client/
    │   │           ├── client
    │   │           └── FoodClient.java
    │   │       ├── Gui/
    │   │           ├── Gui
    │   │           ├── FoodBookScreen.java
    │   │           ├── FoodOverviewScreen.java
    │   │           ├── ModMenuLink.java
    │   │           └── Sol2FConfigGUI.java
    │   │       ├── Key/
    │   │           ├── Key
    │   │           └── KeyBindings.java
    │   │       └── SpiceOfLifeFabricFlavorClient.java
    └── resources/
    │   └── resources
    │   └── sol2f.client.mixins.json
├── code.js
└── main/
    └── main
    ├── java/
        ├── java
        └── com/
        │   └── com
        │   └── sol2f/
        │       └── sol2f
        │       ├── command/
        │           ├── command
        │           └── FoodCommands.java
        │       ├── config/
        │           ├── config
        │           └── Sol2FConfig.java
        │       ├── item/
        │           ├── item
        │           └── SOL2FRightClickItem.java
        │       ├── mixin/
        │           ├── mixin
        │           ├── ItemFinishMixin.java
        │           └── PlayerEntityMixin.java
        │       ├── network/
        │           ├── network
        │           └── payload/
        │           │   └── payload
        │           │   ├── C2SRequestAllFoodListPayload.java
        │           │   ├── C2SRequestFoodListPayload.java
        │           │   ├── OpenFoodBookPayload.java
        │           │   ├── S2CAllFoodListPayload.java
        │           │   ├── S2CFoodListPayload.java
        │           │   ├── S2CHealthMaxPayload.java
        │           │   └── S2CHealthPayload.java
        │       ├── server/
        │           ├── server
        │           ├── FoodUseHandler.java
        │           └── ServerEventHandlers.java
        │       ├── SpiceOfLifeFabricFlavor.java
        │       └── util/
        │           └── util
        │           ├── FunctionCaculator.java
        │           └── IEntityDataSaver.java
    └── resources/
        └── resources
        ├── assets/
            ├── assets
            └── sol2f/
            │   └── sol2f
            │   ├── icon.png
            │   ├── lang/
            │       ├── lang
            │       ├── en_us.json
            │       └── zh_cn.json
            │   ├── models/
            │       ├── models
            │       └── item/
            │       │   └── item
            │       │   └── food_book.json
            │   └── textures/
            │       └── textures
            │       ├── gui/
            │           ├── gui
            │           └── food_book.png
            │       └── item/
            │           └── item
            │           └── food_book.png
        ├── data/
            ├── data
            └── sol2f/
            │   └── sol2f
            │   └── recipes/
            │       └── recipes
            │       └── food_book.json
        ├── fabric.mod.json
        └── sol2f.mixins.json

# 文件内容

```client/java/com/sol2f/Gui/FoodBookScreen.java
package com.sol2f.Gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.sol2f.client.FoodClient;

import java.util.List;
import java.util.Optional;

public class FoodBookScreen extends Screen {

    public FoodBookScreen() {
        super(Text.translatable("sol2f.gui.food_book.title"));
    }

    private static final int GUI_WIDTH = 276;
    private static final int GUI_HEIGHT = 166;
    private static final int MAIN_PANEL_WIDTH = 248;
    private final Identifier GUI_TEXTURE = Identifier.of("sol2f", "textures/gui/food_book.png");

    private int scrollPageOffset = 0;
    private static final int ITEMS_PER_PAGE = 96;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 首先渲染背景（必须是第一行！）
        this.renderBackground(context, mouseX, mouseY, delta);

        // 计算 GUI 位置
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // 重置悬停物品
        this.hoveredStack = ItemStack.EMPTY;

        // 计算分页
        int totalItem = FoodClient.getConsumedCount();
        int pageCount = Math.max(1, (totalItem + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int maxOffset = pageCount - 1;
        scrollPageOffset = Math.min(0, Math.max(-maxOffset, scrollPageOffset));

        // 绘制 GUI 纹理
        context.drawTexture(
            GUI_TEXTURE,
            guiLeft, guiTop,
            0, 0,
            GUI_WIDTH, GUI_HEIGHT,
            276, 342
        );

        // 绘制标题
        Text title = Text.translatable("sol2f.gui.food_book.title");
        context.drawText(textRenderer, title, guiLeft + 9, guiTop + 5, 0x000000, false);

        // 绘制滑块
        int currentSliderY = (pageCount <= 1) ? 17 : 17 + (133 * (-scrollPageOffset)) / (pageCount - 1);
        context.drawTexture(GUI_TEXTURE, guiLeft + 232, guiTop + currentSliderY, 0, 333, 7, 9, 276, 342);

        // 绘制物品格子
        renderItemGrid(context, guiLeft, guiTop, mouseX, mouseY);

        // 渲染 Tooltip
        if (!hoveredStack.isEmpty()) {
            List<Text> tooltip = Screen.getTooltipFromItem(MinecraftClient.getInstance(), hoveredStack);
            if (tooltip.isEmpty()) {
                tooltip.add(hoveredStack.getName());
                if (tooltip.isEmpty()) {
                    tooltip.add(Text.translatable("sol2f.gui.food_book.unknown_item_text"));
                }
            }
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }
    private void renderItemGrid(DrawContext context, int guiLeft, int guiTop, int mouseX, int mouseY) {
        int totalItem = FoodClient.getConsumedCount();
        List<String> consumedItems = FoodClient.getConsumedSnapshot();
        int startIndex = -scrollPageOffset * ITEMS_PER_PAGE;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 12; col++) {
                int index = row * 12 + col;
                int itemIndex = startIndex + index;
                int slotX = guiLeft + 9 + col * 18;
                int slotY = guiTop + 16 + row * 18;

                if (itemIndex < totalItem) {
                    String itemId = consumedItems.get(itemIndex);
                    try {
                        Identifier id = Identifier.of(itemId);
                        Optional<Item> itemOpt = Registries.ITEM.getOrEmpty(id);
                        if (itemOpt.isPresent()) {
                            ItemStack stack = new ItemStack(itemOpt.get());
                            context.drawItem(stack, slotX + 1, slotY + 1);
                            context.drawItemInSlot(textRenderer, stack, slotX + 1, slotY + 1);
                            if (isMouseOver(slotX, slotY, mouseX, mouseY)) {
                                hoveredStack = stack;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore invalid IDs (e.g., mod removed)
                    }
                }
            }
        }
}
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        int targetLeft = guiLeft + 248;
        int targetTop = guiTop + 44;
        int targetRight = guiLeft + 271;
        int targetBottom = guiTop + 70;

        if (mouseX >= targetLeft && mouseX < targetRight && mouseY >= targetTop && mouseY < targetBottom) {
            if (button == 0) {
                MinecraftClient.getInstance().setScreen(new FoodOverviewScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(int slotX, int slotY, int mouseX, int mouseY) {
        return mouseX >= slotX && mouseY >= slotY && mouseX < slotX + 18 && mouseY < slotY + 18;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            int totalItem = FoodClient.getConsumedCount();
        int maxOffset = (totalItem + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE - 1;

        if (verticalAmount > 0) {
            scrollPageOffset = Math.max(-maxOffset, scrollPageOffset + 1);
        } else if (verticalAmount < 0) {
            scrollPageOffset = Math.min(0, scrollPageOffset - 1);
        }
        return true;
    }
}
```

```client/java/com/sol2f/Gui/FoodOverviewScreen.java
package com.sol2f.Gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import com.sol2f.client.FoodClient;

public class FoodOverviewScreen extends Screen {

    public FoodOverviewScreen() {
        super(Text.translatable("sol2f.gui.food_overview.title"));
    }

    private static final int GUI_WIDTH = 276;
    private static final int GUI_HEIGHT = 166;
    private static final int MAIN_PANEL_WIDTH = 248;
    private final Identifier GUI_TEXTURE = Identifier.of("sol2f", "textures/gui/food_book.png");

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        context.drawTexture(
            GUI_TEXTURE,
            guiLeft, guiTop,
            0, 167,
            GUI_WIDTH, GUI_HEIGHT,
            276, 342
        );

        Text title = Text.translatable("sol2f.gui.food_overview.title");
        context.drawText(
            textRenderer,
            title,
            guiLeft + 23,
            guiTop + 16,
            0x000000,
            false
        );

        Text OverviewText1 = Text.translatable("sol2f.gui.food_overview.text_FoodCount").formatted(Formatting.BOLD);
        context.drawText(
            textRenderer,
            OverviewText1,
            guiLeft + 40,
            guiTop + 29,
            0x000000,
            false
        );

        Text OverviewText2 = Text.translatable("sol2f.gui.food_overview.text_HealthyCount").formatted(Formatting.BOLD);
        context.drawText(
            textRenderer,
            OverviewText2,
            guiLeft + 40,
            guiTop + 62,
            0x000000,
            false
        );

        int OverviewDrawWidth = 88;
        Text OverviewFoodCount = Text.literal(FoodClient.getConsumedCount() + " / " + FoodClient.getAllCount());
        int FCFontWidth = textRenderer.getWidth(OverviewFoodCount);
        int FCDrawX = guiLeft + 23 + (OverviewDrawWidth - FCFontWidth) / 2;
        context.drawText(
            textRenderer,
            OverviewFoodCount,
            FCDrawX,
            guiTop + 44,
            0x000000,
            false
        );

        Text OverviewHealthyCount = Text.literal(FoodClient.getCurrentHealth() + " / " + FoodClient.getMaxHealth());
        int HCFontWidth = textRenderer.getWidth(OverviewHealthyCount);
        int HCDrawX = guiLeft + 23 + (OverviewDrawWidth - HCFontWidth) / 2;
        context.drawText(
            textRenderer,
            OverviewHealthyCount,
            HCDrawX,
            guiTop + 76,
            0x000000,
            false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        int targetLeft = guiLeft + 248;
        int targetTop = guiTop + 16;
        int targetRight = guiLeft + 271;
        int targetBottom = guiTop + 42;

        if (mouseX >= targetLeft && mouseX < targetRight && mouseY >= targetTop && mouseY < targetBottom) {
            if (button == 0) {
                MinecraftClient.getInstance().setScreen(new FoodBookScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
```

```client/java/com/sol2f/Gui/ModMenuLink.java
package com.sol2f.Gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuLink implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return Sol2FConfigGUI::openConfigScreen;
    }
    
}

```

```client/java/com/sol2f/Gui/Sol2FConfigGUI.java
package com.sol2f.Gui;

import com.sol2f.config.Sol2FConfig;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class Sol2FConfigGUI {

    public static Screen openConfigScreen(Screen parent) {
        
        System.out.println("Sol2FConfigGUI: openConfigScreen called!");

        Sol2FConfig config = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig();

        System.out.println("Config loaded");

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("sol2f.gui.config.title"))
            .setSavingRunnable(() -> AutoConfig.getConfigHolder(Sol2FConfig.class).save());// 保存当前 config 实例（AutoConfig 自动写入文件）

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ===== 血量设置 =====
        ConfigCategory healthyCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.healthy")
        );

        healthyCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.healthy.maxHealthy"),
                config.healthy.maxHealthy
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.healthy.maxHealthy.@Tooltip")
            )
            .setMin(2)
            .setMax(240)
            .setSaveConsumer(newValue -> config.healthy.maxHealthy = newValue)
            .build()
        );

        // ===== 功能设置 =====
        ConfigCategory featureCat = builder.getOrCreateCategory(
            Text.translatable("sol2f.gui.config.option.features")
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.healthyGain"),
                config.features.healthyGain
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthyGain.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> config.features.healthyGain = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.Increasefrequency"),
                config.features.Increasefrequency
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.Increasefrequency.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> config.features.Increasefrequency = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.frequencyGain"),
                config.features.frequencyGain
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.frequencyGain.@Tooltip")
            )
            .setMin(1).setMax(30)  // 注意：语言文件说 1~30，但原注解是 min=2，按语言文件为准
            .setSaveConsumer(v -> config.features.frequencyGain = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.features.resetOnDeath"),
                config.features.resetOnDeath
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.resetOnDeath.@Tooltip")
            )
            .setSaveConsumer(v -> config.features.resetOnDeath = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startBooleanToggle(
                Text.translatable("sol2f.gui.config.option.features.healthToMaxOnIncrease"),
                config.features.healthToMaxOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthToMaxOnIncrease.@Tooltip")
            )
            .setSaveConsumer(v -> config.features.healthToMaxOnIncrease = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startIntField(
                Text.translatable("sol2f.gui.config.option.features.healthIncreaseOnIncrease"),
                config.features.healthIncreaseOnIncrease
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.healthIncreaseOnIncrease.@Tooltip")
            )
            .setMin(0).setMax(20)
            .setSaveConsumer(v -> config.features.healthIncreaseOnIncrease = v)
            .build()
        );

        featureCat.addEntry(
            entryBuilder.startStrField(
                Text.translatable("sol2f.gui.config.option.features.Expression"),
                config.features.Expression
            )
            .setTooltip(
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[1]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[2]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[3]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[4]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[5]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[6]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[7]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[8]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[9]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[10]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[11]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[12]"),
                Text.translatable("sol2f.gui.config.option.features.Expression.@Tooltip[13]")
            )
            .setSaveConsumer(v -> config.features.Expression = v)
            .build()
        );

        return builder.build();
    }
}
```

```client/java/com/sol2f/Key/KeyBindings.java
// com/sol2f/Key/KeyBindings.java
package com.sol2f.Key;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    // 必须是 public static
    public static KeyBinding OPEN_FOOD_BOOK;

    public static void register() {
        OPEN_FOOD_BOOK = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.sol2f.open_food_book",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.sol2f.main"
            )
        );
    }
}
```

```client/java/com/sol2f/SpiceOfLifeFabricFlavorClient.java
package com.sol2f;

import com.sol2f.client.FoodClient;
import net.fabricmc.api.ClientModInitializer;

public class SpiceOfLifeFabricFlavorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 委托给FoodClient进行客户端特定的初始化
		new FoodClient().onInitializeClient();
	}
}
```

```client/java/com/sol2f/client/FoodClient.java
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
```

```client/resources/sol2f.client.mixins.json
{
	"required": true,
	"package": "com.sol2f.mixin.client",
	"compatibilityLevel": "JAVA_17",
	"mixins": [],
	"injectors": {
		"defaultRequire": 1
	}
}
```

```main/java/com/sol2f/SpiceOfLifeFabricFlavor.java
package com.sol2f;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.item.ItemGroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.item.SOL2FRightClickItem;
import com.sol2f.server.FoodUseHandler;
import com.sol2f.server.ServerEventHandlers;
import com.sol2f.command.FoodCommands;
import com.sol2f.network.payload.*;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class SpiceOfLifeFabricFlavor implements ModInitializer {
	public static final String MOD_ID = "sol2f";

	// 这个日志记录器用于向控制台和日志文件写入文本
	// 最佳实践是使用你的模组ID作为日志记录器的名称
	// 这样，就可以清楚地知道是哪个模组写入了信息、警告和错误
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	// 当Logback不可用但developerMode=true时使用的备用写入器
	private static BufferedWriter DEV_FILE_WRITER = null;
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter STARTUP_FILE_FMT = DateTimeFormatter.ofPattern("yyyy-M-d-HH-mm-ss");

	public static final Item FOOD_BOOK_ITEM = new SOL2FRightClickItem(// 创建食物书物品
        new Item.Settings().maxCount(1),
        (player, stack) -> {
            // 发送打开食物书界面的数据包给客户端
            ServerPlayNetworking.send(player, new OpenFoodBookPayload());
        }
    );

	@Override
	public void onInitialize() {

		LOGGER.info("SpiceOfLife: initializing");
		AutoConfig.register(Sol2FConfig.class, GsonConfigSerializer::new);
		// 注册并加载配置（Cloth Config / Auto Config）
		// 如果启用了developerMode，则在run/logs/sol2f下添加专用的文件附加器
		// 通过反射启用开发者文件日志记录，以避免对Logback的编译时依赖
			try {
				if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
				Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
				Path logsDir = gameDir.resolve("logs").resolve("sol2f");
				if (!Files.exists(logsDir)) Files.createDirectories(logsDir);
				String startupTime = java.time.LocalDateTime.now().format(STARTUP_FILE_FMT);
				String logFile = logsDir.resolve("sol2f_" + startupTime + ".log").toString();
				Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");

				// 尝试动态配置Logback（如果可用）
				try {
					Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
					Class<?> patternEncoderClass = Class.forName("ch.qos.logback.classic.encoder.PatternLayoutEncoder");
					// 通过反射检查ILoggingEvent类型；不需要本地变量
					Class<?> fileAppenderClass = Class.forName("ch.qos.logback.core.FileAppender");

					Object ctx = loggerFactoryClass.getMethod("getILoggerFactory").invoke(null);
					java.lang.reflect.Constructor<?> encCtor = patternEncoderClass.getConstructor();
					Object ple = encCtor.newInstance();
					patternEncoderClass.getMethod("setPattern", String.class).invoke(ple, "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
					patternEncoderClass.getMethod("setContext", loggerContextClass).invoke(ple, ctx);
					patternEncoderClass.getMethod("start").invoke(ple);

					java.lang.reflect.Constructor<?> faCtor = fileAppenderClass.getConstructor();
					Object fa = faCtor.newInstance();
					fileAppenderClass.getMethod("setFile", String.class).invoke(fa, logFile);
					fileAppenderClass.getMethod("setEncoder", Class.forName("ch.qos.logback.core.encoder.Encoder")).invoke(fa, ple);
					fileAppenderClass.getMethod("setContext", loggerContextClass).invoke(fa, ctx);
					fileAppenderClass.getMethod("setName", String.class).invoke(fa, "SOL2F_FILE_APPENDER");
					fileAppenderClass.getMethod("start").invoke(fa);

					Class<?> classicLoggerClass = Class.forName("ch.qos.logback.classic.Logger");
					Object root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
					classicLoggerClass.getMethod("addAppender", Class.forName("ch.qos.logback.core.Appender")).invoke(root, fa);
					LOGGER.info("sol2f: developerMode enabled - logging to {}", logFile);
					writeDevLog("sol2f: developerMode enabled - logging to %s", logFile);
				} catch (Throwable t) {
					LOGGER.warn("sol2f: Logback not available for developerMode file logging, falling back to simple file writer", t);
					writeDevLog("sol2f: Logback not available for developerMode file logging, falling back to simple file writer: %s", t.toString());
					// 为开发者日志打开一个简单的备用写入器
					try {
						DEV_FILE_WRITER = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), java.nio.charset.StandardCharsets.UTF_8));
						DEV_FILE_WRITER.write("--- sol2f developer log started at " + java.time.LocalDateTime.now().format(TIME_FMT) + " ---\n");
						DEV_FILE_WRITER.flush();
					} catch (Throwable ioe) {
						LOGGER.warn("sol2f: failed to open fallback dev log file {}", logFile, ioe);
						DEV_FILE_WRITER = null;
					}
				}
			}
		} catch (Throwable t) {
			LOGGER.warn("sol2f: failed to enable developerMode file logger", t);
			writeDevLog("sol2f: failed to enable developerMode file logger: %s", t.toString());
		}

		// 注册payload
		PayloadTypeRegistry.playS2C().register(S2CFoodListPayload.PACKET_ID, S2CFoodListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CAllFoodListPayload.PACKET_ID, S2CAllFoodListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CHealthPayload.PACKET_ID, S2CHealthPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(S2CHealthMaxPayload.PACKET_ID, S2CHealthMaxPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenFoodBookPayload.PACKET_ID, OpenFoodBookPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SRequestAllFoodListPayload.PACKET_ID, C2SRequestAllFoodListPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(C2SRequestFoodListPayload.PACKET_ID, C2SRequestFoodListPayload.CODEC);

		// 注册服务端处理器
		FoodUseHandler.register();
		ServerEventHandlers.register();
		FoodCommands.register();

		Registry.register(Registries.ITEM, Identifier.of("sol2f", "food_book"), FOOD_BOOK_ITEM);// 注册食物书物品
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(FOOD_BOOK_ITEM);
		});

		LOGGER.info("SpiceOfLife: initialized");
	}

	public static void writeDevLog(String fmt, Object... args) {
		// 向后兼容：将格式化消息记录到控制台和结构化文件行（作为"message"字段）
		try {
			LOGGER.info(fmt.replace("{}", "%s"), args);
		} catch (Throwable t) {
			// 忽略
		}
		if (DEV_FILE_WRITER != null) {
			try {
				String msg = String.format(fmt.replace("{}", "%s"), args);
				java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
				m.put("message", msg);
				writeStructuredDevLog(m);
			} catch (Throwable t) {
				// 静默失败
			}
		}
	}

	/**
	 * 写入结构化开发日志行。
	 * 每行格式: [yyyy-MM-dd HH:mm:ss]{"key"="value";"k2"="v2";}
	 */
	public static void writeStructuredDevLog(java.util.Map<String, String> fields) {
		if (DEV_FILE_WRITER == null) return;
		try {
			StringBuilder sb = new StringBuilder();
			String ts = java.time.LocalDateTime.now().format(TIME_FMT);
			sb.append('[').append(ts).append(']');
			sb.append('{');
			boolean first = true;
			for (java.util.Map.Entry<String, String> e : fields.entrySet()) {
				if (!first) sb.append(';');
				first = false;
				String k = e.getKey();
				String v = e.getValue();
				if (v == null) v = "";
				// 转义值中的双引号和反斜杠
				v = v.replace("\\", "\\\\").replace("\"", "\\\"");
				sb.append('"').append(k).append('"').append("=").append('"').append(v).append('"');
			}
			sb.append('}');
			sb.append('\n');
			DEV_FILE_WRITER.write(sb.toString());
			DEV_FILE_WRITER.flush();
		} catch (Throwable t) {
			// 静默失败
		}
	}
}
```

```main/java/com/sol2f/command/FoodCommands.java
package com.sol2f.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.server.FoodUseHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FoodCommands {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(" ");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("sol2f")
                .then(literal("clearhealthy").executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    if (!src.hasPermissionLevel(2)) {
                        src.sendError(Text.translatable("sol2f.commands.clearhealthy.permission"));
                        return 0;
                    }

                    ServerPlayerEntity player = src.getPlayer();
                    if (player == null) {
                        src.sendError(Text.translatable("sol2f.commands.clearhealthy.must_be_player"));
                        return 0;
                    }

                    try {// 清除玩家已食用食物记录
                        com.sol2f.server.FoodUseHandler.clearEatenFoods(player);
                        src.sendFeedback(() -> Text.translatable("sol2f.commands.clearhealthy.success"), false);
                    } catch (Exception e) {
                        SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to clear healthy", e);
                        src.sendError(Text.translatable("sol2f.commands.clearhealthy.failed"));
                        return 0;
                    }
                    return 1;
                }))

                .then(literal("getlist")
                    .executes(ctx -> executeGetList(ctx.getSource(), null))
                    .then(argument("player", StringArgumentType.word())
                        .suggests((context, builder) ->
                            CommandSource.suggestMatching(
                                context.getSource().getServer().getPlayerNames(),
                                builder
                            )
                        )
                        .executes(ctx -> executeGetList(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "player")
                        ))
                    )
                )
            );
        });
    }

    private static int executeGetList(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target;

        if (playerName == null) {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                target = (ServerPlayerEntity) source.getEntity();
            } else {
                source.sendError(Text.translatable("sol2f.commands.getlist.must_choose_player"));
                return 0;
            }
        } else {
            target = source.getServer().getPlayerManager().getPlayer(playerName);
            if (target == null) {
                source.sendError(Text.translatable("sol2f.commands.getlist.player_not_found", playerName));
                return 0;
            }
        }

        // 获取已食用食物 ID 列表
        Set<String> eatenFoods = FoodUseHandler.getEatenFoods(target);
        String timeStr = LocalDateTime.now().format(TIME_FORMAT);

        // 构建输出
        String header = "[sol2f food list | player:" + target.getName().getString() + " | time:" + timeStr + "]";
        String foodList = "{" + String.join(", ", eatenFoods) + "}";

        source.sendFeedback(() -> Text.literal(header), false);
        source.sendFeedback(() -> Text.literal(foodList), false);

        return 1;
    }
}
```

```main/java/com/sol2f/config/Sol2FConfig.java
package com.sol2f.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "spice-of-life-fabric-flavor")
public class Sol2FConfig implements ConfigData {
    public HealthySetting healthy = new HealthySetting();
    public FeatureSetting features = new FeatureSetting();

    public static class HealthySetting {
        public int maxHealthy = 5000;
    }

    public static class FeatureSetting {
        public int healthyGain = 2;
        public int Increasefrequency = 0;
        public int frequencyGain = 2;
        public boolean resetOnDeath = false;
        public boolean developerMode = false;
        public boolean healthToMaxOnIncrease = false;
        public int healthIncreaseOnIncrease = 0;
        public String Expression = "0";
    }
}
```

```main/java/com/sol2f/item/SOL2FRightClickItem.java
package com.sol2f.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.function.BiConsumer;
    
public class SOL2FRightClickItem extends Item {// 创建物品一个类that可以在右键点击时执行特定操作，以添加功能书

    private final BiConsumer<ServerPlayerEntity, ItemStack> onRightClick;

    public SOL2FRightClickItem(Settings settings, BiConsumer<ServerPlayerEntity, ItemStack> onRightClick) {
        super(settings);
        this.onRightClick = onRightClick;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            onRightClick.accept(serverPlayer, user.getStackInHand(hand));
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
```

```main/java/com/sol2f/mixin/ItemFinishMixin.java
package com.sol2f.mixin;

import com.sol2f.server.FoodUseHandler;
import com.sol2f.SpiceOfLifeFabricFlavor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
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
            // 基础安全检查链
            if (consumer == null || world == null || world.isClient) return;
            
            // 使用组件系统检查食物
            FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
            if (foodComponent == null) return;  // 不是食物，直接返回
            
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
            FoodUseHandler.onFoodEaten(serverPlayer, stack);
            
        } catch (Throwable t) {
            SpiceOfLifeFabricFlavor.LOGGER.error("ItemFinishMixin error", t);
        }
    }
}
```

```main/java/com/sol2f/mixin/PlayerEntityMixin.java
package com.sol2f.mixin;

import com.sol2f.util.IEntityDataSaver;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerEntityMixin implements IEntityDataSaver {
    private NbtCompound persistentData;

    @Override
    public NbtCompound getPersistentData() {
        if (this.persistentData == null) {
            this.persistentData = new NbtCompound();
        }
        return persistentData;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    protected void injectWriteMethod(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> info) {
        if (persistentData != null) {
            nbt.put("sol2f", persistentData);
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    protected void injectReadMethod(NbtCompound nbt, CallbackInfo info) {
        if (nbt.contains("sol2f", 10)) {
            persistentData = nbt.getCompound("sol2f");
        }
    }
}

```

```main/java/com/sol2f/network/payload/C2SRequestAllFoodListPayload.java
package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record C2SRequestAllFoodListPayload() implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "c2s_request_all_food_list");
    public static final CustomPayload.Id<C2SRequestAllFoodListPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, C2SRequestAllFoodListPayload> CODEC = PacketCodec.unit(new C2SRequestAllFoodListPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

```

```main/java/com/sol2f/network/payload/C2SRequestFoodListPayload.java
package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record C2SRequestFoodListPayload() implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "c2s_request_food_list");
    public static final CustomPayload.Id<C2SRequestFoodListPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, C2SRequestFoodListPayload> CODEC = PacketCodec.unit(new C2SRequestFoodListPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

```

```main/java/com/sol2f/network/payload/OpenFoodBookPayload.java
package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record OpenFoodBookPayload() implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "open_food_book_screen");
    public static final CustomPayload.Id<OpenFoodBookPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, OpenFoodBookPayload> CODEC = PacketCodec.unit(new OpenFoodBookPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

```

```main/java/com/sol2f/network/payload/S2CAllFoodListPayload.java
package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;


public record S2CAllFoodListPayload(List<String> allFoods) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_all_food_list");
    public static final CustomPayload.Id<S2CAllFoodListPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CAllFoodListPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING.collect(PacketCodecs.toList()), S2CAllFoodListPayload::allFoods, S2CAllFoodListPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

```

```main/java/com/sol2f/network/payload/S2CFoodListPayload.java
package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;


public record S2CFoodListPayload(List<String> foods) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_food_list");
    public static final CustomPayload.Id<S2CFoodListPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CFoodListPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING.collect(PacketCodecs.toList()), S2CFoodListPayload::foods, S2CFoodListPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

```

```main/java/com/sol2f/network/payload/S2CHealthMaxPayload.java
package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record S2CHealthMaxPayload(int maxHealth) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_health_max");
    public static final CustomPayload.Id<S2CHealthMaxPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CHealthMaxPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, S2CHealthMaxPayload::maxHealth, S2CHealthMaxPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

```

```main/java/com/sol2f/network/payload/S2CHealthPayload.java
package com.sol2f.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record S2CHealthPayload(int health) implements CustomPayload {
    public static final Identifier ID = Identifier.of("sol2f", "s2c_health");
    public static final CustomPayload.Id<S2CHealthPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<PacketByteBuf, S2CHealthPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, S2CHealthPayload::health, S2CHealthPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

```

```main/java/com/sol2f/server/FoodUseHandler.java
package com.sol2f.server;

import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.payload.*;
import com.sol2f.util.FunctionCaculator;
import com.sol2f.SpiceOfLifeFabricFlavor;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.text.Text;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Identifier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.ArrayList;

public class FoodUseHandler {

    // 为初始化器保留注册；使用mixin时不需要运行时注册
    public static void register() {
    // 注册占位符（服务器事件处理程序位于ServerEventHandlers中）
    }

    public static boolean isFoodItem(ItemStack stack) {// 二次判断物品是否为食物
        return stack != null && stack.contains(DataComponentTypes.FOOD);
    }

    public static boolean isFoodItemType(Item item) {// 获取ALLFoods时使用
        return item != null && item.getComponents().contains(DataComponentTypes.FOOD);
    }

    public static void onFoodEaten(ServerPlayerEntity serverPlayer, ItemStack stack) {
        if (!isFoodItem(stack)) return;
        if (serverPlayer == null) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: onFoodEaten called with null player");
            return;
        }

    String id = Registries.ITEM.getId(stack.getItem()).toString();

        // 在玩家上使用同步块以避免竞争条件，其中两个事件
        // 都在写入之前读取NBT，因此都发送首次食用消息
        synchronized (serverPlayer) {
            // developerMode的入口日志：记录尝试
            try {
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat attempt player={} food={}", serverPlayer.getName().getString(), id);
                    java.util.Map<String, String> _f = new java.util.LinkedHashMap<>();
                    _f.put("event", "eat_attempt");
                    _f.put("player", serverPlayer.getName().getString());
                    _f.put("food", id);
                    SpiceOfLifeFabricFlavor.writeStructuredDevLog(_f);
                }
            } catch (Throwable t) {
                // 忽略日志记录失败
            }
            // 在锁内重新读取权威NBT
            Set<String> current = getEatenFoods(serverPlayer);
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: before change player={} eaten={}", serverPlayer.getName().getString(), current);
                java.util.Map<String, String> _b = new java.util.LinkedHashMap<>();
                _b.put("event", "before_change");
                _b.put("player", serverPlayer.getName().getString());
                _b.put("eaten", current.toString());
                SpiceOfLifeFabricFlavor.writeStructuredDevLog(_b);
            }
            if (!current.contains(id)) {
                // 更新并持久化，存到NBT里
                current.add(id);
                saveEatenFoods(serverPlayer, current);

                // 重新读取权威持久化集合以确保写入成功
                Set<String> authoritative = getEatenFoods(serverPlayer);
                if (!authoritative.contains(id)) {
                    SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: after save, authoritative eaten set does not contain {} for player {}", id, serverPlayer.getName().getString());
                } else {
                    // 使用权威更新集应用生命值修饰符
                    double prevMax = serverPlayer.getMaxHealth();
                    applyHealthModifier(serverPlayer, authoritative);
                    double newMax = serverPlayer.getMaxHealth();
                    syncToClient(serverPlayer, authoritative);

                    // 基于NBT状态在消息栏发送一次消息
                    Text displayName = stack.getName();
                    serverPlayer.sendMessage(Text.translatable("sol2f.msg.first_eaten", displayName), false);
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: Player {} first ate {}", serverPlayer.getName().getString(), id);
                    if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                        SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat success player={} food={} eaten={} prevMax={} newMax={}", serverPlayer.getName().getString(), id, authoritative, prevMax, newMax);
                        java.util.Map<String, String> _s = new java.util.LinkedHashMap<>();
                        _s.put("event", "eat_success");
                        _s.put("player", serverPlayer.getName().getString());
                        _s.put("food", id);
                        _s.put("eaten", authoritative.toString());
                        _s.put("prevMaxHealth", Double.toString(prevMax));
                        _s.put("newMaxHealth", Double.toString(newMax));
                        SpiceOfLifeFabricFlavor.writeStructuredDevLog(_s);
                    }
                }
            }
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
                    SpiceOfLifeFabricFlavor.LOGGER.info("sol2f-log: eat ignored (already eaten) player={} food={} eaten={}", serverPlayer.getName().getString(), id, current);
                    java.util.Map<String, String> _i = new java.util.LinkedHashMap<>();
                    _i.put("event", "eat_ignored");
                    _i.put("player", serverPlayer.getName().getString());
                    _i.put("food", id);
                    _i.put("eaten", current.toString());
                    SpiceOfLifeFabricFlavor.writeStructuredDevLog(_i);
                }
        }
    }

    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("sol2f", "health_bonus");
    private static final String CONSUMED_KEY = "consumed_foods";
    private static final String DATA_VERSION = "data_version";
    private static final int CURRENT_VERSION = 1;

    private static NbtCompound readPersistentCompound(ServerPlayerEntity player) {
        if (player instanceof com.sol2f.util.IEntityDataSaver saver) {
            return saver.getPersistentData();
        }

        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to read persistent compound");
        return new NbtCompound();
    }

    private static void writePersistentCompound(ServerPlayerEntity player, NbtCompound data) {
        if (player instanceof com.sol2f.util.IEntityDataSaver saver) {
            NbtCompound persistent = saver.getPersistentData();
            for (String key : data.getKeys()) {
                persistent.put(key, data.get(key));
            }
            return;
        }
        SpiceOfLifeFabricFlavor.LOGGER.error("Failed to write persistent compound. Player is not IEntityDataSaver!");
    }

    // 这里获取所有食物集合
    public static final Set<String> ALLFoods = Registries.ITEM.stream()
        .filter(item -> isFoodItemType(item))
        .map(item -> Registries.ITEM.getId(item).toString())
        .collect(Collectors.toSet());

    public static Set<String> getEatenFoods(ServerPlayerEntity player) {
        try {
            NbtCompound persistent = readPersistentCompound(player);
            NbtList consumed = persistent.contains(CONSUMED_KEY, 9) ? persistent.getList(CONSUMED_KEY, 8) : new NbtList();
            Set<String> set = new HashSet<>();
            for (int i = 0; i < consumed.size(); i++) set.add(consumed.getString(i));
            return set;
        } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to get eaten foods", e);
            return new HashSet<>();
        }
    }

    private static void saveEatenFoods(ServerPlayerEntity player, Set<String> eatenFoods) {
        try {
            NbtCompound persistent = readPersistentCompound(player);
            SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: before save for player {} persistent contains: {}", player.getName().getString(), persistent.contains(CONSUMED_KEY, 9) ? persistent.getList(CONSUMED_KEY, 8) : "<none>");
            NbtList newList = new NbtList();
            for (String food : eatenFoods) {
                newList.add(NbtString.of(food));
            }
            persistent.put(CONSUMED_KEY, newList);
            persistent.putInt(DATA_VERSION, CURRENT_VERSION);
            writePersistentCompound(player, persistent);
            // 读回并记录权威持久化内容
            try {
                NbtCompound after = readPersistentCompound(player);
                SpiceOfLifeFabricFlavor.LOGGER.info("sol2f: after save for player {} persistent contains: {}", player.getName().getString(), after.contains(CONSUMED_KEY, 9) ? after.getList(CONSUMED_KEY, 8) : "<none>");
            } catch (Throwable t) {
                SpiceOfLifeFabricFlavor.LOGGER.warn("sol2f: failed to read back persistent compound after save for player {}", player.getName().getString());
            }
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: failed to save eaten foods", e);
        }
    }

    public static void addEatenFood(ServerPlayerEntity player, ItemStack food) {
        Set<String> eatenFoods = getEatenFoods(player);
        String foodId = Registries.ITEM.getId(food.getItem()).toString();

        if (eatenFoods.add(foodId)) {
            saveEatenFoods(player, eatenFoods);
            applyHealthModifier(player, eatenFoods);
            syncToClient(player, eatenFoods);
        }
    }

    public static void syncToClient(ServerPlayerEntity player, Set<String> eatenFoods) {
        try {
            ServerPlayNetworking.send(player, new S2CFoodListPayload(new ArrayList<>(eatenFoods)));
            SpiceOfLifeFabricFlavor.LOGGER.info("Synced food list to client: {}", eatenFoods);
        } catch (Exception e) {
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to sync to client", e);
        }
    }

    public static void clearEatenFoods(ServerPlayerEntity player) {
        Set<String> empty = new HashSet<>();
        saveEatenFoods(player, empty);
        Set<String> eaten = getEatenFoods(player);// 确保数据一致性，方便测试发现错误
        applyHealthModifier(player, eaten);
        syncToClient(player, eaten);
    }

    public static void applyHealthModifier(ServerPlayerEntity player) {
        applyHealthModifier(player, getEatenFoods(player));
    }

    public static void applyHealthModifier(ServerPlayerEntity player, Set<String> eatenFoods) {// 和生命值有关的操作
        if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {
            player.sendMessage(Text.literal("run apply"), false);
        }
        try {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr == null) {// 玩家不存在最大生命值属性，不应该发生，但是防止空指针异常，人人有责
                SpiceOfLifeFabricFlavor.LOGGER.error("Health attribute instance is null for player {}", player.getName().getString());
                return;
            }

            // ---计算及判断生命值部分---
            attr.removeModifier(HEALTH_MODIFIER_ID);// 1\ 移除已有的生命值修饰符
            SpiceOfLifeFabricFlavor.LOGGER.info("Removed existing health modifier for player {}", player.getName().getString());

            double CurrentHealth = attr.getValue();// 2\ 获取当前最大生命值（不含修饰符，包含别的mod的修改）

            // 3\ 计算生命值增益
            // 3.1\ 这里计算默认增益
            int unique = eatenFoods.size();// 将healthyGain解释为每个独特食物（新食用）项目增加的生命值（以生命值单位）
            double perHp = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthyGain;
            double BaseBonus = unique * perHp; // 来自独特食物的总生命值奖励

            // 3.2\ 这里计算频率增益
            int FrequencyCount;// 达到的频率奖励次数
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.Increasefrequency > 0) {// 当频率增益频率大于0时（功能启用时）执行，不判断将除以0导致报错
                FrequencyCount = unique / AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.Increasefrequency;
            } else {
                FrequencyCount = 0;
            }
            double FrequencyBonus = FrequencyCount * AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.frequencyGain; // 来自频率奖励的总生命值奖励

            // 3.3\ 计算函数
            String formula = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.Expression;
            double result = FunctionCaculator.evaluate(formula, Map.of(
                "uniqueFoods", (double) unique,
                "currentHealth", CurrentHealth
            ));

            double HealthBonus = BaseBonus + FrequencyBonus + result;// 3.4\ 计算总生命值奖励

            // 4\ 这里判断总增益是否超过最大生命值
            double HealthyMaximum = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.maxHealthy;
            double newMaxHealth = Math.min(CurrentHealth + HealthBonus, HealthyMaximum);

            // 这里应用生命修饰符
            EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_MODIFIER_ID, HealthBonus, EntityAttributeModifier.Operation.ADD_VALUE);
            attr.addPersistentModifier(mod);
            SpiceOfLifeFabricFlavor.LOGGER.info("Added health modifier: {} for player {} (unique={}, perHp={}, bonus={})", mod, player.getName().getString(), unique, perHp, HealthBonus);
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.developerMode) {// 输出日志
                player.sendMessage(Text.literal("apply: " + HealthBonus + ", " + FrequencyBonus + ", " + BaseBonus + ", " + FrequencyCount + ", " + unique), false);// 总生值奖励、频率奖励、基础奖励、频率计数、独特食物计数
            }

            ServerPlayNetworking.send(player, new S2CHealthMaxPayload((int) newMaxHealth));// 发送生命值数据（仅用于GUI显示）

            // 这里是恢复生命逻辑
            if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthToMaxOnIncrease) { // 恢复最大生命
                player.setHealth(player.getMaxHealth());
                SpiceOfLifeFabricFlavor.LOGGER.info("Player {} healed to max health: {}", player.getName().getString(), player.getMaxHealth());
            } else if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthIncreaseOnIncrease > 0) { // 恢复指定生命 
                float currentHealth = player.getHealth();
                float increase = AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.healthIncreaseOnIncrease;
                float newHealth = Math.min(currentHealth + increase, (float)player.getMaxHealth());
                player.setHealth(newHealth);
                SpiceOfLifeFabricFlavor.LOGGER.info("Player {} healed to max health: {}", player.getName().getString(), player.getMaxHealth());
            } else {}
        } catch (Exception e) {
            player.sendMessage(Text.literal("[apply error]"), false);
            SpiceOfLifeFabricFlavor.LOGGER.error("Failed to apply health modifier for player {}", player.getName().getString(), e);
        }
    }
}
```

```main/java/com/sol2f/server/ServerEventHandlers.java
package com.sol2f.server;

import java.util.ArrayList;
import java.util.Set;

import com.sol2f.SpiceOfLifeFabricFlavor;
import com.sol2f.util.IEntityDataSaver;
import com.sol2f.config.Sol2FConfig;
import com.sol2f.network.payload.*;

import net.minecraft.server.network.ServerPlayerEntity;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public class ServerEventHandlers {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;// 获取玩家实例
            if (player == null) return;// 防止为null，避免空指针异常

            Set<String> eaten = FoodUseHandler.getEatenFoods(player);// 返回空集而不是null，避免空指针异常
            FoodUseHandler.applyHealthModifier(player, eaten);// 应用生命值修饰符，eaten不会为null，上面的方法保证了这一点
            FoodUseHandler.syncToClient(player, eaten);// 同步已消耗列表到客户端

            ServerPlayNetworking.send(player, new S2CFoodListPayload(new ArrayList<>(FoodUseHandler.ALLFoods)));// 发送所有食物的列表
            ServerPlayNetworking.send(player, new S2CHealthMaxPayload(AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().healthy.maxHealthy));
        });

        ServerPlayNetworking.registerGlobalReceiver(C2SRequestAllFoodListPayload.PACKET_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ServerPlayNetworking.send(player, new S2CAllFoodListPayload(new ArrayList<>(FoodUseHandler.ALLFoods)));
        });

        // 复制逻辑
        // ServerPlayerEvents.COPY_FROM 在两种场景会被调用：
        // 1) 玩家死亡并重生 (alive == false)
        // 2) 玩家在同一会话中切换维度/传送 (alive == true)
        // 需要在维度切换时也复制持久化数据（例如末地返回），否则会出现数据不同步的问题。
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            boolean shouldCopy = alive || (!alive && !AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.resetOnDeath);
            if (shouldCopy) {
                if (oldPlayer instanceof IEntityDataSaver olDataSaver && newPlayer instanceof IEntityDataSaver newDataSaver) {
                    // 复制持久化数据
                    newDataSaver.getPersistentData().copyFrom(olDataSaver.getPersistentData());
                }
            }
        });

        // 重置和应用逻辑
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if(!alive) {
                if (AutoConfig.getConfigHolder(Sol2FConfig.class).getConfig().features.resetOnDeath) {
                    // 重置已消耗的食物列表
                    FoodUseHandler.clearEatenFoods(newPlayer);
                } else {
                    // 复制旧玩家的已消耗食物列表到新玩家
                    Set<String> eaten = FoodUseHandler.getEatenFoods(newPlayer);
                    FoodUseHandler.applyHealthModifier(newPlayer, eaten);
                    FoodUseHandler.syncToClient(newPlayer, eaten);
                }
            }
        });

        // 响应客户端请求食物列表
        ServerPlayNetworking.registerGlobalReceiver(C2SRequestFoodListPayload.PACKET_ID, (payload, context) -> {
            try {
                ServerPlayerEntity player = context.player();
                Set<String> eaten = FoodUseHandler.getEatenFoods(player);
                FoodUseHandler.syncToClient(player, eaten);
            } catch (Exception e) {
                SpiceOfLifeFabricFlavor.LOGGER.error("sol2f: error responding to client list request", e);
            }
        });
    }
}
```

```main/java/com/sol2f/util/FunctionCaculator.java
package com.sol2f.util;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.HashMap;
import java.util.Map;

public class FunctionCaculator {
    public static final Function FLOOR = new Function("floor", 1) {// 定义向下取整函数
        @Override
        public double apply(double... args) {
            return Math.floor(args[0]);
        };
    };

    public static final Function CEIL = new Function("ceil", 1) {// 定义向上取整函数
        @Override
        public double apply(double... args) {
            return Math.ceil(args[0]);
        }
    };

    public static final Function ROUND = new Function("round", 1) {// 定义四舍五入函数
        @Override
        public double apply(double... args) {
            return Math.round(args[0]);
        }
    };

    public static final Function MIN = new Function("min", 2) {// 定义取最小值函数
        @Override
        public double apply(double... args) {
            return Math.min(args[0], args[1]);
        }
    };

    public static final Function MAX = new Function("max", 2) {// 定义取最大值函数
        @Override
        public double apply(double... args) {
            return Math.max(args[0], args[1]);
        }
    };

    public static final Function POW = new Function("pow", 2) {// 定义幂函数
        @Override
        public double apply(double... args) {
            return Math.pow(args[0], args[1]);
        }
    };

    public static final Function LOG = new Function("log", 2) {// 定义对数函数
        @Override
        public double apply(double... args) {
            return Math.log(args[1]) / Math.log(args[0]);
        }
    };

    public static final Function[] CUSTOM_FUNCTIONS = { FLOOR, CEIL, ROUND, MIN, MAX, POW };// 自定义函数数组

    public static Expression buildExpression(String exprStr, String... variables) {
        if (exprStr == null || exprStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression string cannot be null or empty");
        }

        try {
            ExpressionBuilder builder = new ExpressionBuilder(exprStr).variables(variables).functions(CUSTOM_FUNCTIONS);
            Expression expr = builder.build();
            return expr;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to build expression: " + e.getMessage(), e);
        }
    }

    public static double evaluate(String expressionStr, Map<String, Double> variables) {
        if (variables == null) {
            throw new IllegalArgumentException("Variables map cannot be null");
        }

        Map<String, Double> allVars = new HashMap<>(variables);
        allVars.put("e", Math.E);
        allVars.put("pi", Math.PI);
        String[] varNames = allVars.keySet().toArray(new String[0]);
        Expression expr = buildExpression(expressionStr, varNames);
        for (Map.Entry<String, Double> entry : allVars.entrySet()) {
            expr.setVariable(entry.getKey(), entry.getValue());
        }

        try {
            return expr.evaluate();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Error evaluating expression: " + e.getMessage(), e);
        }
    };
}

```

```main/java/com/sol2f/util/IEntityDataSaver.java
package com.sol2f.util;

import net.minecraft.nbt.NbtCompound;

public interface IEntityDataSaver {
    NbtCompound getPersistentData();
}

```

```main/resources/assets/sol2f/lang/en_us.json
{
  "sol2f.tooltip.unconsumed": "Not yet consumed",

  "sol2f.msg.first_eaten": "You discovered: %s",
  "sol2f.commands.clearhealthy.success": "Cleared all consumed food records for this player",
  "sol2f.commands.clearhealthy.permission": "You do not have permission to run this command",
  "sol2f.commands.clearhealthy.must_be_player": "This command must be executed by a player",
  "sol2f.commands.clearhealthy.failed": "Failed to reset food consumption data",
  "sol2f.commands.getlist.must_choose_player": "Player name must be specified when running from console",
  "sol2f.commands.getlist.player_not_found": "Player %s does not exist (or is offline)",

  "sol2f.gui.config.title": "Spice of Life: Fabric Flavor Settings",
  "sol2f.gui.config.option.healthy": "Health Settings",
  "sol2f.gui.config.option.features": "Feature Settings",
  "sol2f.gui.config.option.healthy.maxHealthy": "Maximum Health",
  "sol2f.gui.config.option.features.healthyGain": "Health Gain per New Food",
  "sol2f.gui.config.option.features.resetOnDeath": "Reset on Death",
  "sol2f.gui.config.option.features.healthToMaxOnIncrease": "Full Heal on Health Increase",
  "sol2f.gui.config.option.features.healthIncreaseOnIncrease": "Heal Amount on Health Increase",
  "sol2f.gui.config.option.features.Increasefrequency": "Special Reward Frequency",
  "sol2f.gui.config.option.features.Increasefrequency.@Tooltip": "Frequency of extra health increases (set to 0 to disable special rewards). Range: 0–20",
  "sol2f.gui.config.option.features.frequencyGain": "Frequency Reward Amount",
  "sol2f.gui.config.option.features.frequencyGain.@Tooltip": "Amount of health added to the maximum each time the frequency threshold is reached. Range: 2–30",
  "sol2f.gui.config.option.healthy.maxHealthy.@Tooltip": "Range: 2–240",
  "sol2f.gui.config.option.features.healthyGain.@Tooltip": "Maximum health gained per unique food eaten. Range: 0–20",
  "sol2f.gui.config.option.features.resetOnDeath.@Tooltip": "If enabled, the player’s health resets to default upon death.",
  "sol2f.gui.config.option.features.healthToMaxOnIncrease.@Tooltip": "Fully restores health whenever the health maximum increases.",
  "sol2f.gui.config.option.features.healthIncreaseOnIncrease.@Tooltip": "Restores this amount of health whenever the health maximum increases. Must be an even number.",
  "sol2f.gui.config.option.features.Expression": "Health Calculation Expression",
  "sol2f.gui.config.option.features.Expression.@Tooltip[1]": "Custom health calculation expression (setting to 0 disables it).",
  "sol2f.gui.config.option.features.Expression.@Tooltip[2]": "Tip: To use only the expression, set base health gain and frequency gain to 0.",
  "sol2f.gui.config.option.features.Expression.@Tooltip[3]": "This setting will not affect health if disabled, even if the expression is valid. Simple health settings still apply.",
  "sol2f.gui.config.option.features.Expression.@Tooltip[4]": "Available variables:",
  "sol2f.gui.config.option.features.Expression.@Tooltip[5]": "- uniqueFoods: Number of unique foods consumed",
  "sol2f.gui.config.option.features.Expression.@Tooltip[6]": "- currentHealth: Current health value",
  "sol2f.gui.config.option.features.Expression.@Tooltip[7]": "Supported operations:",
  "sol2f.gui.config.option.features.Expression.@Tooltip[8]": "- Basic arithmetic (+, −, *, /) with parentheses precedence",
  "sol2f.gui.config.option.features.Expression.@Tooltip[9]": "- Power: pow(base, exponent)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[10]": "- Floor: floor(num), Ceiling: ceil(num), Round: round(num)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[11]": "- Minimum: min(num1, num2), Maximum: max(num1, num2)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[12]": "- Absolute value: abs(num)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[13]": "- Constants: e, pi",

  "category.sol2f.main": "Spice of Life: Carrot Edition",
  "key.sol2f.open_food_book": "Open Food Book",
  "sol2f.gui.food_book.title": "Food Book",
  "sol2f.gui.food_overview.title": "Overview",
  "sol2f.gui.food_book.unknown_item_text": "[Error] Tooltip failed to load. Please check if the item's mod has been removed.",
  "sol2f.gui.food_overview.text_FoodCount": "Food Summary",
  "sol2f.gui.food_overview.text_HealthyCount": "Health Summary",

  "item.sol2f.food_book": "Food Book"
}
```

```main/resources/assets/sol2f/lang/zh_cn.json
{
  "sol2f.tooltip.unconsumed": "尚未食用",

  "sol2f.msg.first_eaten": "你发现了：%s",
  "sol2f.commands.clearhealthy.success": "已清除该玩家所有已食用食物记录",
  "sol2f.commands.clearhealthy.permission": "你没有权限运行此命令",
  "sol2f.commands.clearhealthy.must_be_player": "必须由玩家执行此命令",
  "sol2f.commands.clearhealthy.failed": "重置食物食用数据失败",
  "sol2f.commands.getlist.must_choose_player": "从控制台执行时必须指定玩家名称",
  "sol2f.commands.getlist.player_not_found": "玩家 %s 不存在（或不在线）",

  "sol2f.gui.config.title": "Spice of Life: Fabric Flavor 设置",
  "sol2f.gui.config.option.healthy": "血量设置",
  "sol2f.gui.config.option.features": "功能设置",
  "sol2f.gui.config.option.healthy.maxHealthy": "最大血量",
  "sol2f.gui.config.option.features.healthyGain": "血量增幅",
  "sol2f.gui.config.option.features.resetOnDeath": "死亡重置",
  "sol2f.gui.config.option.features.healthToMaxOnIncrease": "奖励时恢复满血",
  "sol2f.gui.config.option.features.healthIncreaseOnIncrease": "奖励时恢复血量",
  "sol2f.gui.config.option.features.Increasefrequency": "特殊奖励频率",
  "sol2f.gui.config.option.features.Increasefrequency.@Tooltip": "每次增加额外血量的频率（设置此为0则等于禁用特殊奖励）（通过将原有增幅设置为0并启用此达到多次食用再增加的效果）.范围 0~20",
  "sol2f.gui.config.option.features.frequencyGain": "频率奖励增幅",
  "sol2f.gui.config.option.features.frequencyGain.@Tooltip": "每达到一次额外增长频率血量数值，血量上限将增加的数值.范围 2~30",
  "sol2f.gui.config.option.healthy.maxHealthy.@Tooltip": "范围 2~240",
  "sol2f.gui.config.option.features.healthyGain.@Tooltip": "每种新食物增加的最大血量.范围 0~20",
  "sol2f.gui.config.option.features.resetOnDeath.@Tooltip": "启用后，玩家死亡时血量将重置为默认值。",
  "sol2f.gui.config.option.features.healthToMaxOnIncrease.@Tooltip": "每次增加玩家血量上限时时恢复满血",
  "sol2f.gui.config.option.features.healthIncreaseOnIncrease.@Tooltip": "每次增加血量上限时恢复血量数值，2的倍数",
  "sol2f.gui.config.option.features.Expression": "血量计算函数",
  "sol2f.gui.config.option.features.Expression.@Tooltip[1]": "自定义血量计算函数（若为0则相当于不启用）",
  "sol2f.gui.config.option.features.Expression.@Tooltip[2]": "提示：若需要只启用函数，则将原有血量增幅设置为0，并设置频率增益的频率为0。",
  "sol2f.gui.config.option.features.Expression.@Tooltip[3]":  "这代表此项配置将不影响血量，即使函数可用，原有的简单血量配置任然生效。",
  "sol2f.gui.config.option.features.Expression.@Tooltip[4]":  "可用变量：",
  "sol2f.gui.config.option.features.Expression.@Tooltip[5]":  "- uniqueFoods: 食用过的食物数",
  "sol2f.gui.config.option.features.Expression.@Tooltip[6]":  "- currentHealth: 当前血量",
  "sol2f.gui.config.option.features.Expression.@Tooltip[7]":  "支持的计算：",
  "sol2f.gui.config.option.features.Expression.@Tooltip[8]":  "- 四则运算 括号优先级",
  "sol2f.gui.config.option.features.Expression.@Tooltip[9]":  "- 幂运算 pow(base, exponent)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[10]":  "- 向下取整数:floor(num) 向上取整数:ceil(num) 四舍五入数:round(num)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[11]":  "- 取大值:min(num1, num2) 取大值:max(num1, num2)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[12]":  "- 绝对值:abs(num)",
  "sol2f.gui.config.option.features.Expression.@Tooltip[13]":  "- e:e pi:π",
  "category.sol2f.main": "生活调味料：胡罗贝版",
  "key.sol2f.open_food_book": "打开食物簿",
  "sol2f.gui.food_book.title": "食物簿",
  "sol2f.gui.food_overview.title": "概览",
  "sol2f.gui.food_book.unknown_item_text": "[错误]tooltip加载错误，请检查该物品的mod是否已卸载",
  "sol2f.gui.food_overview.text_FoodCount": "食物概述",
  "sol2f.gui.food_overview.text_HealthyCount": "血量概述",

  "item.sol2f.food_book": "食物簿"
}
```

```main/resources/assets/sol2f/models/item/food_book.json
{
    "parent": "item/generated",
    "textures": {
        "layer0": "sol2f:item/food_book"
    }
}
```

```main/resources/data/sol2f/recipes/food_book.json
{
    "type": "minecraft:crafting_shapeless",
    "ingredients": [
        {"item": "minecraft:carrot"},
        {"item": "minecraft:book"}
    ],
    "result": {
        "item": "sol2f:food_book",
        "count": 1
    }
}
```

```main/resources/fabric.mod.json
{
	"schemaVersion": 1,
	"id": "sol2f",
	"version": "3.0.0",
	"name": "Spice of Life:Fabric Flavor",
	"description": "一个fabric版本的食物奖励mod，基于Spice of Life重植。",
	"authors": [
		"xianfish"
	],
	"contact": {
		"homepage": "https://modrinth.com/project/mkvFT6R6",
		"modrinth": "https://modrinth.com/project/mkvFT6R6"
	},
	"license": "ARR",
	"icon": "assets/sol2f/icon.png",
	"environment": "*",
	"entrypoints": {
		"main": [
			"com.sol2f.SpiceOfLifeFabricFlavor"
		],
		"client": [
			"com.sol2f.SpiceOfLifeFabricFlavorClient"
		],
		"modmenu": ["com.sol2f.Gui.ModMenuLink"]
	},
	"mixins": [
		"sol2f.mixins.json",
		{
			"config": "sol2f.client.mixins.json",
			"environment": "client"
		}
	],
	"depends": {
		"fabricloader": ">=0.18.4",
		"minecraft": "~1.21.1",
		"java": ">=21",
		"fabric-api": "*"
	},
	"suggests": {
		"another-mod": "*"
	}
}
```

```main/resources/sol2f.mixins.json
{
	"required": true,
	"package": "com.sol2f.mixin",
	"compatibilityLevel": "JAVA_17",
	"mixins": [
		"ItemFinishMixin",
		"PlayerEntityMixin"
	],
	"injectors": {
		"defaultRequire": 1
	},
	"overwrites": {
		"requireAnnotations": true
	}
}
```
