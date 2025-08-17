package eu.zjazdownia.rpg.account;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.util.LocUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AccountManager {
    private final ZjazdowniaRPG plugin;
    private final Map<UUID, YamlConfiguration> cache = new HashMap<>();
    private final File dir;

    public AccountManager(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
    }

    private File file(UUID id) {
        return new File(dir, id.toString() + ".yml");
    }

    public void ensureLoaded(UUID id) {
        cache.computeIfAbsent(id, k -> {
            File f = file(k);
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
// domyślne: currentAccount = 1, level = 1, exp = 0
            if (!yml.contains("currentAccount")) yml.set("currentAccount", 1);
            if (!yml.contains("accounts.1.level")) yml.set("accounts.1.level", 1);
            if (!yml.contains("accounts.1.exp")) yml.set("accounts.1.exp", 0);
            return yml;
        });
    }

    public void flush(UUID id) {
        YamlConfiguration yml = cache.get(id);
        if (yml == null) return;
        try {
            yml.save(file(id));
        } catch (IOException e) {
            plugin.getLogger().warning("Nie moge zapisac danych " + id + ": " + e.getMessage());
        }
    }

    public void flushAll() {
        cache.keySet().forEach(this::flush);
    }

    public int getCurrentAccount(UUID id) {
        ensureLoaded(id);
        return cache.get(id).getInt("currentAccount", 1);
    }

    public void setCurrentAccount(UUID id, int idx) {
        ensureLoaded(id);
        cache.get(id).set("currentAccount", idx);
    }

    private String path(UUID id, String key) {
        return "accounts." + getCurrentAccount(id) + "." + key;
    }

    public String getSelectedClass(UUID id) {
        ensureLoaded(id);
        return cache.get(id).getString(path(id, "class"), null);
    }

    public void setSelectedClass(UUID id, String clazz) {
        ensureLoaded(id);
        cache.get(id).set(path(id, "class"), clazz);
    }

    public int getLevel(UUID id) {
        ensureLoaded(id);
        return cache.get(id).getInt(path(id, "level"), 1);
    }

    public void setLevel(UUID id, int lvl) {
        ensureLoaded(id);
        cache.get(id).set(path(id, "level"), Math.max(1, lvl));
    }

    // NOWE: EXP
    public int getExp(UUID id) {
        ensureLoaded(id);
        return cache.get(id).getInt(path(id, "exp"), 0);
    }

    public void setExp(UUID id, int exp) {
        ensureLoaded(id);
        cache.get(id).set(path(id, "exp"), Math.max(0, exp));
    }

    public void saveLastLocation(UUID id, Location loc) {
        ensureLoaded(id);
        cache.get(id).set("lastLocation", LocUtil.toSection(loc));
    }

    public Location getLastLocation(UUID id) {
        ensureLoaded(id);
        return LocUtil.fromSection(cache.get(id).getConfigurationSection("lastLocation"));
    }

    public void clearCurrentAccount(UUID id) {
        ensureLoaded(id);
        cache.get(id).set(path(id, "class"), null);
        cache.get(id).set(path(id, "level"), 1);
        cache.get(id).set(path(id, "exp"), 0); // reset exp
    }
}