package dev.heypr.buildersWand;

import net.kyori.adventure.text.TextComponent;
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
    private final boolean generatePreviewOnMove;
    private final int durabilityAmount;
    private final boolean durabilityEnabled;
    private final String durabilityText;
    private final float cooldown;
    private final List<Material> blockedMaterials;
    private final boolean isCraftable;

    public Wand(int id,
                String name,
                Material material,
                List<String> lore,
                int maxSize,
                String maxSizeText,
                int maxRayTraceDistance,
                boolean consumesItems,
                boolean generatePreviewOnMove,
                int durabilityAmount,
                boolean durabilityEnabled,
                String durabilityText,
                float cooldown,
                List<Material> blockedMaterials,
                boolean isCraftable) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.lore = lore;
        this.maxSize = maxSize;
        this.maxSizeText = maxSizeText;
        this.maxRayTraceDistance = maxRayTraceDistance;
        this.consumesItems = consumesItems;
        this.generatePreviewOnMove = generatePreviewOnMove;
        this.durabilityAmount = durabilityAmount;
        this.durabilityEnabled = durabilityEnabled;
        this.durabilityText = durabilityText;
        this.cooldown = cooldown;
        this.blockedMaterials = blockedMaterials;
        this.isCraftable = isCraftable;
    }

    public int getId() {
        return id;
    }

    public TextComponent getName() {
        return Util.toComponent(name);
    }

    public Material getMaterial() {
        return material;
    }

    public List<TextComponent> getLore() {
        List<TextComponent> finalLore = new ArrayList<>();
        for (String line : lore) finalLore.add(Util.toComponent(line));
        return finalLore;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public TextComponent getMaxSizeText() {
        return Util.toComponent(maxSizeText);
    }

    public int getMaxRayTraceDistance() {
        return maxRayTraceDistance;
    }

    public boolean consumesItems() {
        return consumesItems;
    }

    public boolean generatePreviewOnMove() {
        return generatePreviewOnMove;
    }

    public int getDurabilityAmount() {
        return durabilityAmount;
    }

    public boolean isDurabilityEnabled() {
        return durabilityEnabled;
    }

    public TextComponent getDurabilityText() {
        return Util.toComponent(durabilityText);
    }

    public float getCooldown() {
        return cooldown;
    }

    public List<Material> getBlockedMaterials() {
        return blockedMaterials;
    }

    public boolean isCraftable() {
        return isCraftable;
    }
}
