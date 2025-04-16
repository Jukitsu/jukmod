package net.jukitsumc.jukmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.nio.file.Path;

public class Jukmod implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("jukmod");
    public static final String MOD_ID = "jukmod";
    private static Jukmod instance;



    public static Jukmod getInstance() { return instance; }
    public Logger getLogger() {
        return LOGGER;
    }


    @Override
    public void onInitialize() {
        instance = this;

        LOGGER.info("0.24 (\"1.8\") Miss Penalty Removed ! :D");
        LOGGER.info("Old hit color initialized, old corpse animation loading...");
        LOGGER.info("Fixed MC-69655, MC-120955 and MC-147694");

        FabricLoader loader = FabricLoader.getInstance();



    }
}