package dev.heypr.buildersWand.hooks;

import dev.heypr.buildersWand.BuildersWand;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LandsHook {
    public static boolean canBuild(Player player, Location loc, BuildersWand plugin) {
        LandsIntegration api = LandsIntegration.of(plugin);
        LandWorld world = api.getWorld(loc.getWorld());
        return world != null && world.hasRoleFlag(player.getUniqueId(), loc, Flags.BLOCK_PLACE);
    }
}
