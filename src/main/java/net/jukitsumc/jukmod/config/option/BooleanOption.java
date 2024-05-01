package net.jukitsumc.jukmod.config.option;


import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import net.jukitsumc.jukmod.config.category.Category;
import net.minecraft.network.chat.Component;


public class BooleanOption extends Option<Boolean, BooleanListEntry> {
    public BooleanOption(String id, Category category, Boolean defaultValue, boolean forceRestart) {
        super(id, category, defaultValue);
        this.setConfigEntry(() -> {
            BooleanToggleBuilder builder = ConfigEntryBuilder.create()
                    .startBooleanToggle(Component.translatable(this.getTranslationKey()), this.get())
                    .setDefaultValue(this.getDefault())
                    .setSaveConsumer(this::set);
            builder.requireRestart(forceRestart);
            builder.setTooltip(this.getTooltip(this.getTranslationKey()));
            return builder.build();
        });
    }

    public BooleanOption(String id, Category category, Boolean defaultValue) {
        this(id, category, defaultValue, false);
    }
}