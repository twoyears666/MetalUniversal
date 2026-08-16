package com.metallum.mixin.render;

import com.metallum.client.metal.fx.MetalFxConfig;
import com.metallum.client.metal.fx.MetalFxWarningScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the MetalFX entry point to vanilla Video Settings.
 *
 * <p>This targets {@link Screen} because the widget-registration method is
 * declared there and inherited by {@link VideoSettingsScreen}.</p>
 */
@Mixin(Screen.class)
abstract class VideoSettingsScreenMixin {
    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable> T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void metallum$addMetalFxButton(final CallbackInfo ci) {
        if (!((Object) this instanceof VideoSettingsScreen)) {
            return;
        }

        MetalFxConfig config = MetalFxConfig.get();
        boolean supported = config.spatialSupported()
                || config.temporalSupported()
                || config.interpolationSupported();

        VideoSettingsScreen screen = (VideoSettingsScreen) (Object) this;
        Button button = Button.builder(
                        Component.translatable("metallum.fx.button"),
                        ignored -> MetalFxWarningScreen.openIfNotAcknowledged(screen)
                )
                .pos(this.width / 2 - 100, this.height - 52)
                .size(200, 20)
                .build();
        button.active = supported;
        this.addRenderableWidget(button);
    }
}
