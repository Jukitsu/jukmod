package net.jukitsumc.jukmod.implementation;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.jukitsumc.jukmod.Jukmod;
import net.jukitsumc.jukmod.client.config.ModConfig;
import net.jukitsumc.jukmod.client.config.impl.category.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuApiImpl::buildConfig;
    }

    public static Screen buildConfig(Screen prevScreen) {
        ModConfig config = Jukmod.getInstance().config;

        ConfigBuilder builder = ConfigBuilder.create()
                .setTitle(Component.translatable(ModConfig.TRANSLATION_KEY))
                .setTransparentBackground(Minecraft.getInstance().level != null)
                .setSavingRunnable(() -> {
                    try {
                        config.saveConfig();
                    } catch (IOException e) {
                        Jukmod.getInstance().getLogger().error("Failed to save Jukmod config file", e);
                    }
                });
        if (prevScreen != null) builder.setParentScreen(prevScreen);

        for (String id : config.getCategories().keySet()) {
            Category category = config.getCategory(id);
            ConfigCategory configCategory = builder.getOrCreateCategory(Component.translatable(category.getTranslationKey()));

            for (AbstractConfigListEntry<?> entry : category.getConfigEntries()) {
                configCategory.addEntry(entry);
            }
        }

        return builder.build();
    }
}