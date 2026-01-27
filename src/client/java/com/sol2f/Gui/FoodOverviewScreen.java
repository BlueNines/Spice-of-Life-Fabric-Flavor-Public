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