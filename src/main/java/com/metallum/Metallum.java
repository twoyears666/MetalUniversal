package com.metallum;

import com.metallum.client.metal.render.bridge.MetalNativeBridge;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metallum implements ModInitializer, PreLaunchEntrypoint {
    public static final String MOD_ID = "metallum";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onPreLaunch() {
        // PreLaunch 是 Fabric Loader 提供的最早入口点，在游戏启动之前调用。
        // iOS 上的 native dylib 只有在已嵌入并签名，或设备启用了 JIT 时才能加载。
        // 因此 native bridge 不可用不能阻止普通后端启动；真正选择 Metal 时，
        // Metal backend 会在使用 native bridge 时给出明确错误。
        try {
            // 必须尽早执行：LWJGL 的 Spvc.SPVC 会在类初始化时缓存库配置。
            MetalNativeBridge.ensureSpvcLibraryConfigured();
        } catch (Throwable throwable) {
            LOGGER.warn(
                    "Metal native bridge is unavailable; continuing without the Metal backend. "
                            + "On iOS, embed a signed libmetallum.dylib in the launcher Frameworks "
                            + "directory or enable JIT before selecting Metal.",
                    throwable
            );
        }
    }

    @Override
    public void onInitialize() {
    }
}