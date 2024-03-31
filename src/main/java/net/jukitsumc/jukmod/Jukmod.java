package net.jukitsumc.jukmod;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.jukitsumc.jukmod.entity.Human;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jukmod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("jukmod");
    public static final String MOD_ID = "jukmod";

	public static final EntityType<Human> HUMAN = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			new ResourceLocation(Jukmod.MOD_ID, "human"),
			FabricEntityTypeBuilder.create(MobCategory.CREATURE, Human::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).build()
	);



	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("1.8 Miss Penalty Removed ! :D");
		FabricDefaultAttributeRegistry.register(HUMAN, Human.createHumanAttributes());
	}
}