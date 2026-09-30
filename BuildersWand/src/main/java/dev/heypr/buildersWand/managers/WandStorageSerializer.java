package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.utility.ComponentUtil;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class WandStorageSerializer {

    public byte[] serialize(Map<Integer, ItemStack> content) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            Map<Integer, byte[]> encoded = new HashMap<>();
            for (Map.Entry<Integer, ItemStack> entry : content.entrySet()) {
                ItemStack item = entry.getValue();
                if (item == null || item.getType().isAir()) continue;
                try {
                    encoded.put(entry.getKey(), item.serializeAsBytes());
                }
                catch (Exception exception) {
                    ComponentUtil.error("Failed to serialize item in slot " + entry.getKey() + ": " + exception.getMessage());
                }
            }
            out.writeInt(encoded.size());
            for (Map.Entry<Integer, byte[]> entry : encoded.entrySet()) {
                out.writeInt(entry.getKey());
                out.writeInt(entry.getValue().length);
                out.write(entry.getValue());
            }
        }
        catch (IOException exception) {
            ComponentUtil.error("Failed to serialize wand storage: " + exception.getMessage());
        }
        return buffer.toByteArray();
    }

    public Map<Integer, ItemStack> deserialize(byte[] data) {
        Map<Integer, ItemStack> result = new HashMap<>();
        if (data == null || data.length == 0) return result;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int slot = in.readInt();
                int length = in.readInt();
                byte[] itemBytes = in.readNBytes(length);
                try {
                    result.put(slot, ItemStack.deserializeBytes(itemBytes));
                }
                catch (Exception exception) {
                    ComponentUtil.error("Failed to deserialize item in slot " + slot + ": " + exception.getMessage());
                }
            }
        }
        catch (IOException exception) {
            ComponentUtil.error("Failed to deserialize wand storage: " + exception.getMessage());
        }
        return result;
    }
}
