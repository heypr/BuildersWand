package dev.heypr.buildersWand.hooks;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SuperiorSkyblockHook {
    public static boolean canBuild(Player player, Location loc) {
        Island island = SuperiorSkyblockAPI.getIslandAt(loc);
        if (island == null) return false;
        return island.hasPermission(SuperiorSkyblockAPI.getPlayer(player), IslandPrivilege.getByName("BUILD"));
    }
}
