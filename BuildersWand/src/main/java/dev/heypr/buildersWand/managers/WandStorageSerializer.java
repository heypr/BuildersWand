package dev.heypr.buildersWand.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.heypr.buildersWand.utility.ComponentUtil;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class WandStorageSerializer {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String serializeMap(Map<Integer, ItemStack> content) {
        HashMap<Integer, String> encodedMap = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : content.entrySet()) {
            if (entry.getValue() == null || entry.getValue().getType().isAir()) continue;
            try {
                byte[] serialized = entry.getValue().serializeAsBytes();
                encodedMap.put(entry.getKey(), Base64.encodeBase64String(serialized));
            }
            catch (Exception e) {
                ComponentUtil.error("Failed to serialize item in slot " + entry.getKey() + ": " + e.getMessage());
            }
        }
        return gson.toJson(encodedMap);
    }

    public Map<Integer, ItemStack> deserializeMap(String json) {
        HashMap<Integer, ItemStack> loadedItems = new HashMap<>();
        try {
            Type hashMapType = new TypeToken<HashMap<Integer, String>>() {}.getType();
            HashMap<Integer, String> map = gson.fromJson(json, hashMapType);
            if (map == null) return loadedItems;
            for (Map.Entry<Integer, String> entry : map.entrySet()) {
                byte[] decoded = Base64.decodeBase64(entry.getValue());
                ItemStack deserialized = ItemStack.deserializeBytes(decoded);
                loadedItems.put(entry.getKey(), deserialized);
            }
        }
        catch (Exception e) {
            ComponentUtil.error("An error occurred while deserializing wand storage: " + e.getMessage());
        }
        return loadedItems;
    }
}
