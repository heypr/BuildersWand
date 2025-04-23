package dev.heypr.buildersWand;

import dev.heypr.buildersWand.managers.ConfigManager;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class Wand {
    private final int id;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final int maxSize;
    private final String maxSizeText;
    private final int maxRayTraceDistance;
    private final boolean consumesItems;
    private final int durabilityAmount;
    private final boolean durabilityEnabled;
    private final String durabilityText;
    private final float cooldown;

    public Wand(int id,
                String name,
                Material material,
                List<String> lore,
                int maxSize,
                String maxSizeText,
                int maxRayTraceDistance,
                boolean consumesItems,
                int durabilityAmount,
                boolean durabilityEnabled,
                String durabilityText,
                float cooldown) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.lore = lore;
        this.maxSize = maxSize;
        this.maxSizeText = maxSizeText;
        this.maxRayTraceDistance = maxRayTraceDistance;
        this.consumesItems = consumesItems;
        this.durabilityAmount = durabilityAmount;
        this.durabilityEnabled = durabilityEnabled;
        this.durabilityText = durabilityText;
        this.cooldown = cooldown;
    }

    public int getId() {
        return id;
    }

    public TextComponent getName() {
        return ConfigManager.deserializeToComponent(name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public String getRawName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public List<TextComponent> getLore() {
        List<TextComponent> finalLore = new ArrayList<>();
        for (String line : lore) finalLore.add(ConfigManager.deserializeToComponent(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        return finalLore;
    }

    public List<String> getRawLore() {
        return lore;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public TextComponent getMaxSizeText() {
        return ConfigManager.deserializeToComponent(maxSizeText).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public String getRawMaxSizeText() {
        return maxSizeText;
    }

    public int getMaxRayTraceDistance() {
        return maxRayTraceDistance;
    }

    public boolean consumesItems() {
        return consumesItems;
    }

    public int getDurabilityAmount() {
        return durabilityAmount;
    }

    public boolean isDurabilityEnabled() {
        return durabilityEnabled;
    }

    public TextComponent getDurabilityText() {
        return ConfigManager.deserializeToComponent(durabilityText).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public String getRawDurabilityText() {
        return durabilityText;
    }

    public float getCooldown() {
        return cooldown;
    }
}