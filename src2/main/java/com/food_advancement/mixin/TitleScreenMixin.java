package com.food_advancement.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 教学用示例：向 `TitleScreen` 的初始化方法注入一个自定义按钮。
 * 说明点：
 * - 使用 @Mixin 指定要修改的目标类
 * - 使用 @Inject 指定注入点（init 方法 RETURN 表示在原方法执行完后运行）
 * - 注入方法可以访问原类的 protected/public 字段和方法（如果可见）
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

	/**
	 * 在标题界面初始化完成后加入一个按钮。
	 * 参数说明：
	 * - screen：被混入的方法所在的实例（这里是 TitleScreen）
	 * - ci：回调信息，可用于取消方法（ci.cancel()）或查看调用状态
	 */
	@Inject(method = "init", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		// 这里我们通过强转把 Screen 转为 TitleScreen（实际类型就是 TitleScreen）
		TitleScreen screen = (TitleScreen) (Object) this;

		// 创建一个简单的按钮：点击时打印日志（教学时可替换为实际逻辑）
	// ButtonWidget 的构造函数在不同 MC 版本会变化，
	// 在 1.20.x 中通常使用 (int x, int y, int width, int height, Text message, ButtonWidget.PressAction onPress)
	ButtonWidget demoButton = new ButtonWidget(
		screen.width / 2 - 100,
		screen.height / 4 + 48,
		200,
		20,
		Text.literal("示例按钮"),
		(button) -> System.out.println("示例按钮被点击")
	);

	// addDrawableChild 在 Screen 中可能为 protected，使用强转调用公共方法 addDrawableChild
	((Screen) screen).addDrawableChild(demoButton);
	}

}
