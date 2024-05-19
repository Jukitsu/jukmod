package net.jukitsumc.jukmod.client.config.impl.option;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.jukitsumc.jukmod.client.config.impl.category.Category;
import net.jukitsumc.jukmod.config.option.Option;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class OptionImpl<T, U extends AbstractConfigListEntry<?>> implements Option<T> {
    private final String id;
    private final Category category;
    private final T defaultValue;
    protected T value;
    private Supplier<U> entry;

    protected OptionImpl(String id, Category category, T defaultValue) {
        this.id = id;
        this.category = category;
        this.defaultValue = defaultValue;
        this.value = this.defaultValue;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return this.category.getTranslationKey() + "." + id;
    }

    public Category getCategory() {
        return category;
    }

    public T get() {
        return this.value;
    }

    public Object getInConfigFormat() {
        return this.value;
    }

    public void set(T value) {
        this.value = value;
    }

    public T getDefault() {
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public void load(Object object) {
        this.set((T) object);
    }

    public U getConfigEntry() {
        return entry.get();
    }

    public void setConfigEntry(Supplier<U> entry) {
        this.entry = entry;
    }

    public Optional<Component[]> getTooltip(String translationId) {
        List<Component> components = new ArrayList<>();
        String tooltipKey = translationId + ".tooltip.";

        int i = 0;
        while (i != -1) {
            if (Language.getInstance().has(tooltipKey + i)) {
                components.add(Component.translatable(tooltipKey + i));
                i++;
            } else {
                i = -1;
            }
        }
        Component[] array = components.toArray(new Component[0]);

        return Optional.ofNullable(array.length != 0 ? array : null);
    }
}