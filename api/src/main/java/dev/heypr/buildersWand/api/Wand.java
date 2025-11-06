package dev.heypr.buildersWand.api;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final String previewParticle;
    private final int previewParticleCount;
    private final double previewParticleOffsetX;
    private final double previewParticleOffsetY;
    private final double previewParticleOffsetZ;
    private final double previewParticleSpeed;
    private final int previewParticleOptionsRed;
    private final int previewParticleOptionsGreen;
    private final int previewParticleOptionsBlue;
    private final int previewParticleOptionsSize;
    private final float cooldown;
    private final List<Material> blockedMaterials;
    private final boolean isCraftable;
    private final boolean craftingRecipeEnabled;
    private final List<String> recipeShape;
    private final Map<Character, Material> recipeIngredients;

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
                String previewParticle,
                int previewParticleCount,
                double previewParticleOffsetX,
                double previewParticleOffsetY,
                double previewParticleOffsetZ,
                double previewParticleSpeed,
                int previewParticleOptionsRed,
                int previewParticleOptionsGreen,
                int previewParticleOptionsBlue,
                int previewParticleOptionsSize,
                float cooldown,
                List<Material> blockedMaterials,
                boolean isCraftable,
                boolean craftingRecipeEnabled,
                List<String> recipeShape,
                Map<Character, Material> recipeIngredients) {
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
        this.previewParticle = previewParticle;
        this.previewParticleCount = previewParticleCount;
        this.previewParticleOffsetX = previewParticleOffsetX;
        this.previewParticleOffsetY = previewParticleOffsetY;
        this.previewParticleOffsetZ = previewParticleOffsetZ;
        this.previewParticleSpeed = previewParticleSpeed;
        this.previewParticleOptionsRed = previewParticleOptionsRed;
        this.previewParticleOptionsGreen = previewParticleOptionsGreen;
        this.previewParticleOptionsBlue = previewParticleOptionsBlue;
        this.previewParticleOptionsSize = previewParticleOptionsSize;
        this.cooldown = cooldown;
        this.blockedMaterials = blockedMaterials;
        this.isCraftable = isCraftable;
        this.craftingRecipeEnabled = craftingRecipeEnabled;
        this.recipeShape = recipeShape;
        this.recipeIngredients = recipeIngredients;
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

    public String getPreviewParticle() {
        return previewParticle;
    }

    public int getPreviewParticleCount() {
        return previewParticleCount;
    }

    public double getPreviewParticleOffsetX() {
        return previewParticleOffsetX;
    }

    public double getPreviewParticleOffsetY() {
        return previewParticleOffsetY;
    }

    public double getPreviewParticleOffsetZ() {
        return previewParticleOffsetZ;
    }

    public double getPreviewParticleSpeed() {
        return previewParticleSpeed;
    }

    public int getPreviewParticleOptionsRed() {
        return previewParticleOptionsRed;
    }

    public int getPreviewParticleOptionsGreen() {
        return previewParticleOptionsGreen;
    }

    public int getPreviewParticleOptionsBlue() {
        return previewParticleOptionsBlue;
    }

    public int getPreviewParticleOptionsSize() {
        return previewParticleOptionsSize;
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

    public boolean isCraftingRecipeEnabled() {
        return craftingRecipeEnabled;
    }

    public List<String> getRecipeShape() {
        return recipeShape;
    }

    public Map<Character, Material> getRecipeIngredients() {
        return recipeIngredients;
    }
}
