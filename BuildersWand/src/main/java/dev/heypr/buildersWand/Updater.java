package dev.heypr.buildersWand;

import dev.heypr.buildersWand.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater {
    private static final String RESOURCE_ID = "125977";
    private static final String SPIGOT_VERSION_URL_TEMPLATE = "https://api.spigotmc.org/legacy/update.php?resource=%s";

    private static BukkitTask task;

    public static void start(BuildersWand plugin) {
        if (!ConfigManager.isUpdaterEnabled()) {
            Util.debug("Updater disabled in config.");
            return;
        }

        if (task != null && !task.isCancelled()) {
            stop();
        }

        long intervalMinutes = Math.max(1L, ConfigManager.getUpdaterIntervalMinutes());
        long intervalTicks = intervalMinutes * 60L * 20L;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> checkAndNotify(plugin));

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> checkAndNotify(plugin), intervalTicks, intervalTicks);

        Util.log("Updater started. Checking every " + intervalMinutes + " minutes.");
    }

    public static void stop() {
        if (task != null) {
            if (!task.isCancelled()) {
                task.cancel();
            }
            task = null;
        }
    }

    private static void checkAndNotify(BuildersWand plugin) {
        try {
            String latest = fetchSpigotLatestVersion();
            if (latest == null || latest.isEmpty()) {
                Util.debug("Unable to fetch latest version (empty).");
                return;
            }

            String current = plugin.getDescription().getVersion();
            Util.debug("Updater: current=" + current + " latest=" + latest);

            if (isNewerVersion(current, latest)) {
                Bukkit.getScheduler().runTask(plugin, () -> notifyUpdateAvailable(plugin, current, latest));
            }
            else {
                Bukkit.getScheduler().runTask(plugin, () -> Util.log("Plugin is up to date!"));
            }
        }
        catch (Exception e) {
            Util.error("Updater error: " + e.getMessage());
            plugin.getLogger().fine("Updater exception: " + e);
        }
    }

    private static String fetchSpigotLatestVersion() {
        String target = String.format(SPIGOT_VERSION_URL_TEMPLATE, RESOURCE_ID);
        HttpURLConnection con = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(target);
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "BuildersWand-Updater");

            int code = con.getResponseCode();
            if (code != 200) {
                Util.debug("Updater HTTP returned " + code + " for resource " + RESOURCE_ID);
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = reader.readLine();
            return line != null ? line.trim() : null;
        }
        catch (Exception e) {
            Util.debug("Updater fetch error: " + e.getMessage());
            return null;
        }
        finally {
            try {
                if (reader != null) reader.close();
            }
            catch (Exception ignored) {}
            if (con != null) con.disconnect();
        }
    }

    private static boolean isNewerVersion(String current, String latest) {
        if (current == null || latest == null) return false;
        String[] a = current.split("[^0-9]+");
        String[] b = latest.split("[^0-9]+");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int va = i < a.length && !a[i].isEmpty() ? Integer.parseInt(a[i]) : 0;
            int vb = i < b.length && !b[i].isEmpty() ? Integer.parseInt(b[i]) : 0;
            if (vb > va) return true;
            if (vb < va) return false;
        }
        return false;
    }

    private static void notifyUpdateAvailable(BuildersWand plugin, String current, String latest) {
        boolean console = ConfigManager.notifyUpdateInConsole();
        boolean ingame = ConfigManager.notifyUpdateInGame();

        String consoleText = String.format("&aUpdate available! &c%s -> &a%s (https://www.spigotmc.org/resources/builderswand.125977)", current, latest);
        if (console) Util.log(consoleText);

        if (ingame) {
            String notifyMessage = ConfigManager.getUpdaterNotifyMessage();
            String permission = ConfigManager.getUpdaterNotifyPermission();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (permission.isEmpty() || player.hasPermission(permission)) {
                    player.sendMessage(Util.toPrefixedComponent(notifyMessage));
                }
            }
        }
    }
}
