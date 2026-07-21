package com.sol2f.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import com.sol2f.SpiceOfLifeFabricFlavorClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class FoodOverviewScreen extends Screen {

    /**
     * 创建食物和生命增益概览页面。
     */
    public FoodOverviewScreen() {
        super(Text.translatable("sol2f.gui.food_overview.title"));
    }

    private static final int GUI_WIDTH = 276;
    private static final int GUI_HEIGHT = 166;
    private static final int MAIN_PANEL_WIDTH = 248;
    private final Identifier GUI_TEXTURE = new Identifier("sol2f", "textures/gui/food_book.png");

    /**
     * 概览页面不暂停世界。
     */
    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * 绘制食物数量、生命增益和导航按钮。
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

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
        Text OverviewFoodCount = Text.literal(SpiceOfLifeFabricFlavorClient.getConsumedCount() + " / " + SpiceOfLifeFabricFlavorClient.getAllCount());
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

        Text OverviewHealthyCount = Text.literal(SpiceOfLifeFabricFlavorClient.getCurrentHealth() + " / " + SpiceOfLifeFabricFlavorClient.getMaxHealth());// 当前增益血量 / 最大增益血量
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

    /**
     * 处理概览页面导航按钮点击。
     */
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
