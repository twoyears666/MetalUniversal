package com.metallum.mixin.render;

import com.metallum.client.metal.fx.MetalFxConfig;
import com.metallum.client.metal.fx.MetalFxWarningScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the MetalFX entry point to the vanilla Video Settings options list.
 *
 * <p>The button is inserted into the same scrollable list as the video
 * quality options, so it never occupies the fixed position above Done.</p>
 */
@Mixin(OptionsSubScreen.class)
abstract class VideoSettingsScreenMixin {
    @Shadow
    protected OptionsList list;

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
                .build();
        button.active = supported;
        this.list.addBig(button);
    }
}
