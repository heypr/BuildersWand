package dev.heypr.buildersWand.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.*;

public class Wand {
    public enum WandType {
        STANDARD,
        STATIC
    }

    private final String id;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final WandType wandType;
    private final int staticLength;
    private final int staticWidth;
    private final int maxSize;
    private final String maxSizeText;
    private final int maxRayTraceDistance;
    private final boolean consumesItems;
    private final boolean generatePreviewOnMove;
    private final int durabilityAmount;
    private final boolean durabilityEnabled;
    private final String durabilityText;
    private final boolean breakSoundEnabled;
    private final Sound breakSound;
    private final String breakSoundMessage;
    private final String previewParticle;
    private final int previewParticleCount;
    private final double previewParticleOffsetX;
    private final double previewParticleOffsetY;
    private final double previewParticleOffsetZ;
    private final double previewParticleSpeed;
    private final Color previewParticleColor;
    private final int previewParticleOptionsSize;
    private final float cooldown;
    private final List<Material> blockedMaterials;
    private final boolean isCraftable;
    private final boolean craftingRecipeEnabled;
    private final List<String> recipeShape;
    private final Map<Character, Material> recipeIngredients;
    private final int undoHistorySize;
    private final boolean canBreakBlocksWhileCrouched;
    private final int storageMinLines;
    private final int storageMaxLines;

    private Wand(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.material = builder.material;
        this.lore = builder.lore;
        this.wandType = builder.wandType;
        this.staticLength = builder.staticLength;
        this.staticWidth = builder.staticWidth;
        this.maxSize = builder.maxSize;
        this.maxSizeText = builder.maxSizeText;
        this.maxRayTraceDistance = builder.maxRayTraceDistance;
        this.consumesItems = builder.consumesItems;
        this.generatePreviewOnMove = builder.generatePreviewOnMove;
        this.durabilityAmount = builder.durabilityAmount;
        this.durabilityEnabled = builder.durabilityEnabled;
        this.durabilityText = builder.durabilityText;
        this.breakSoundEnabled = builder.breakSoundEnabled;
        this.breakSound = builder.breakSound;
        this.breakSoundMessage = builder.breakSoundMessage;
        this.previewParticle = builder.previewParticle;
        this.previewParticleCount = builder.previewParticleCount;
        this.previewParticleOffsetX = builder.previewParticleOffsetX;
        this.previewParticleOffsetY = builder.previewParticleOffsetY;
        this.previewParticleOffsetZ = builder.previewParticleOffsetZ;
        this.previewParticleSpeed = builder.previewParticleSpeed;
        this.previewParticleColor = builder.previewParticleColor;
        this.previewParticleOptionsSize = builder.previewParticleOptionsSize;
        this.cooldown = builder.cooldown;
        this.blockedMaterials = builder.blockedMaterials;
        this.isCraftable = builder.isCraftable;
        this.craftingRecipeEnabled = builder.craftingRecipeEnabled;
        this.recipeShape = builder.recipeShape;
        this.recipeIngredients = builder.recipeIngredients;
        this.undoHistorySize = builder.undoHistorySize;
        this.canBreakBlocksWhileCrouched = builder.canBreakBlocksWhileCrouched;
        this.storageMinLines = builder.storageMinLines;
        this.storageMaxLines = builder.storageMaxLines;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public String getRawName() {
        return name;
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

    public WandType getWandType() {
        return wandType;
    }

    public int getStaticLength() {
        return staticLength;
    }

    public int getStaticWidth() {
        return staticWidth;
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

    public Color getPreviewParticleColor() {
        return previewParticleColor;
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

    public int getUndoHistorySize() {
        return undoHistorySize;
    }

    public boolean isBreakSoundEnabled() {
        return breakSoundEnabled;
    }

    public Sound getBreakSound() {
        return breakSound;
    }

    public Component getBreakSoundMessage() {
        return Util.toPrefixedComponent(breakSoundMessage);
    }

    public boolean canBreakBlocksWhileCrouched() {
        return canBreakBlocksWhileCrouched;
    }

    public int getStorageMinLines() {
        return storageMinLines;
    }

    public int getStorageMaxLines() {
        return storageMaxLines;
    }

    public static final class Builder {
        private final String id;
        private String name = "&3Builders Wand";
        private Material material = Material.BLAZE_ROD;
        private List<String> lore = new ArrayList<>();
        private WandType wandType = WandType.STANDARD;
        private int staticLength = 3;
        private int staticWidth = 3;
        private int maxSize = 8;
        private String maxSizeText = "&3Max Size: {maxSize}";
        private int maxRayTraceDistance = 16;
        private boolean consumesItems = true;
        private boolean generatePreviewOnMove = false;
        private int durabilityAmount = 100;
        private boolean durabilityEnabled = true;
        private String durabilityText = "&3Durability: {durability}";
        private boolean breakSoundEnabled = false;
        private Sound breakSound = Sound.ENTITY_ITEM_BREAK;
        private String breakSoundMessage = "&cYour wand broke!";
        private String previewParticle = null;
        private int previewParticleCount = 1;
        private double previewParticleOffsetX = 0;
        private double previewParticleOffsetY = 0;
        private double previewParticleOffsetZ = 0;
        private double previewParticleSpeed = 0;
        private Color previewParticleColor = Color.fromRGB(0, 0, 0);
        private int previewParticleOptionsSize = 1;
        private float cooldown = 0;
        private List<Material> blockedMaterials = new ArrayList<>();
        private boolean isCraftable = false;
        private boolean craftingRecipeEnabled = false;
        private List<String> recipeShape = new ArrayList<>();
        private Map<Character, Material> recipeIngredients = new HashMap<>();
        private int undoHistorySize = 10;
        private boolean canBreakBlocksWhileCrouched = false;
        private int storageMinLines = 1;
        private int storageMaxLines = 5;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setMaterial(Material material) {
            this.material = material;
            return this;
        }

        public Builder setLore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder setWandType(WandType wandType) {
            this.wandType = wandType;
            return this;
        }

        public Builder setStaticLength(int staticLength) {
            this.staticLength = staticLength;
            return this;
        }

        public Builder setStaticWidth(int staticWidth) {
            this.staticWidth = staticWidth;
            return this;
        }

        public Builder setMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder setMaxSizeText(String maxSizeText) {
            this.maxSizeText = maxSizeText;
            return this;
        }

        public Builder setMaxRayTraceDistance(int maxRayTraceDistance) {
            this.maxRayTraceDistance = maxRayTraceDistance;
            return this;
        }

        public Builder setConsumesItems(boolean consumesItems) {
            this.consumesItems = consumesItems;
            return this;
        }

        public Builder setGeneratePreviewOnMove(boolean generatePreviewOnMove) {
            this.generatePreviewOnMove = generatePreviewOnMove;
            return this;
        }

        public Builder setDurabilityAmount(int durabilityAmount) {
            this.durabilityAmount = durabilityAmount;
            return this;
        }

        public Builder setDurabilityEnabled(boolean durabilityEnabled) {
            this.durabilityEnabled = durabilityEnabled;
            return this;
        }

        public Builder setDurabilityText(String durabilityText) {
            this.durabilityText = durabilityText;
            return this;
        }

        public Builder setBreakSoundEnabled(boolean breakSoundEnabled) {
            this.breakSoundEnabled = breakSoundEnabled;
            return this;
        }

        public Builder setBreakSound(Sound breakSound) {
            this.breakSound = breakSound;
            return this;
        }

        public Builder setBreakSoundMessage(String breakSoundMessage) {
            this.breakSoundMessage = breakSoundMessage;
            return this;
        }

        public Builder setPreviewParticle(String previewParticle) {
            this.previewParticle = previewParticle;
            return this;
        }

        public Builder setPreviewParticleCount(int previewParticleCount) {
            this.previewParticleCount = previewParticleCount;
            return this;
        }

        public Builder setPreviewParticleOffsetX(double previewParticleOffsetX) {
            this.previewParticleOffsetX = previewParticleOffsetX;
            return this;
        }

        public Builder setPreviewParticleOffsetY(double previewParticleOffsetY) {
            this.previewParticleOffsetY = previewParticleOffsetY;
            return this;
        }

        public Builder setPreviewParticleOffsetZ(double previewParticleOffsetZ) {
            this.previewParticleOffsetZ = previewParticleOffsetZ;
            return this;
        }

        public Builder setPreviewParticleSpeed(double previewParticleSpeed) {
            this.previewParticleSpeed = previewParticleSpeed;
            return this;
        }

        public Builder setPreviewParticleColor(Color previewParticleColor) {
            this.previewParticleColor = previewParticleColor;
            return this;
        }

        public Builder setPreviewParticleOptionsSize(int previewParticleOptionsSize) {
            this.previewParticleOptionsSize = previewParticleOptionsSize;
            return this;
        }

        public Builder setCooldown(float cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder setBlockedMaterials(List<Material> blockedMaterials) {
            this.blockedMaterials = blockedMaterials;
            return this;
        }

        public Builder setCraftable(boolean isCraftable) {
            this.isCraftable = isCraftable;
            return this;
        }

        public Builder setCraftingRecipeEnabled(boolean craftingRecipeEnabled) {
            this.craftingRecipeEnabled = craftingRecipeEnabled;
            return this;
        }

        public Builder setRecipeShape(List<String> recipeShape) {
            this.recipeShape = recipeShape;
            return this;
        }

        public Builder setRecipeIngredients(Map<Character, Material> recipeIngredients) {
            this.recipeIngredients = recipeIngredients;
            return this;
        }

        public Builder setUndoHistorySize(int undoHistorySize) {
            this.undoHistorySize = undoHistorySize;
            return this;
        }

        public Builder setCanBreakBlocksWhileCrouched(boolean canBreakBlocksWhileCrouched) {
            this.canBreakBlocksWhileCrouched = canBreakBlocksWhileCrouched;
            return this;
        }

        public Builder setStorageMinLines(int storageMinLines) {
            this.storageMinLines = storageMinLines;
            return this;
        }

        public Builder setStorageMaxLines(int storageMaxLines) {
            this.storageMaxLines = storageMaxLines;
            return this;
        }

        public Wand build() {
            return new Wand(this);
        }
    }
}
