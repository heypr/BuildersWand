package dev.heypr.buildersWand.hooks;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

import java.util.Optional;

public class BentoBoxHook {
    public static boolean canBuild(Player player, Location location) {
        Optional<Island> optionalIsland = BentoBox.getInstance().getIslandsManager().getIslandAt(location);

        if (optionalIsland.isEmpty()) return false;

        return optionalIsland.map(island -> island.isAllowed(User.getInstance(player), Flags.PLACE_BLOCKS)).orElse(false);
    }
}
