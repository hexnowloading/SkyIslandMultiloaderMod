package dev.hexnowloading.dungeonnowloading.config;

import dev.hexnowloading.dungeonnowloading.platform.Services;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class DNLServerConfig {
    public static void register() {
        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

        GeneralConfig.registerServerConfig(SERVER_BUILDER);
        BossConfig.registerServerConfig(SERVER_BUILDER);
        MobConfig.registerServerConfig(SERVER_BUILDER);
        PvpConfig.registerServerConfig(SERVER_BUILDER);

        Services.CONFIG.registerConfig(ModConfig.Type.SERVER, SERVER_BUILDER.build());
    }
}
