package eu.zjazdownia.rpg.account;

/**
 * Menedżer kont graczy: wielokrotne konta na gracza (UUID), zapis/odczyt z plików YAML
 * w katalogu players/, cache w pamięci. Przechowuje klasę, poziom, exp, ostatnią lokację
 * oraz ekwipunek per konto.
 */
import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.util.LocUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AccountManager {
    private final ZjazdowniaRPG plugin;
    /** Cache: UUID gracza -> jego plik YAML (accounts, currentAccount, lastLocation). */
    private final Map<UUID, YamlConfiguration> cache = new HashMap<>();
    /** Katalog players/ w data folderze pluginu. */
    private final File dir;

    public AccountManager(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
        String dirName = plugin.getConfig().getString("account.players-dir", "players");
        if (dirName == null || dirName.isBlank()) dirName = "players";
        this.dir = new File(plugin.getDataFolder(), dirName);
        if (!dir.exists()) dir.mkdirs();
    }

    private File file(UUID id) {
        return new File(dir, id.toString() + ".yml");
    }

    /** Ładuje dane gracza do cache (jeśli jeszcze nie załadowane) i uzupełnia domyślne klucze. */
    public void ensureLoaded(UUID id) {
        cache.computeIfAbsent(id, k -> {
            File f = file(k);
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
            if (!yml.contains("currentAccount")) yml.set("currentAccount", 1);
            if (!yml.contains("accounts.1.level")) yml.set("accounts.1.level", 1);
            if (!yml.contains("accounts.1.exp")) yml.set("accounts.1.exp", 0);
            return yml;
        });
    }

    /** Zapisuje dane gracza z cache do pliku na dysku. */
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

    /** Ścieżka w YAML do pola bieżącego konta (np. accounts.1.class). */
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
        var sec = LocUtil.toSection(loc);
        if (sec != null) {
            cache.get(id).set("lastLocation", sec);
        }
    }

    public Location getLastLocation(UUID id) {
        ensureLoaded(id);
        return LocUtil.fromSection(cache.get(id).getConfigurationSection("lastLocation"));
    }

    /** Czyści dane bieżącego konta: klasa=null, level=1, exp=0. */
    public void clearCurrentAccount(UUID id) {
        ensureLoaded(id);
        cache.get(id).set(path(id, "class"), null);
        cache.get(id).set(path(id, "level"), 1);
        cache.get(id).set(path(id, "exp"), 0); // reset exp
    }

    private final Set<UUID> activePlayers = new HashSet<>();

    public void setPlayerActive(UUID id, boolean active) {
        if (active) activePlayers.add(id);
        else activePlayers.remove(id);
    }

    public boolean isPlayerActive(UUID id) {
        return activePlayers.contains(id);
    }

    /** Zapisuje ekwipunek (contents + armor + offhand) do konta o podanym indeksie. */
    public void saveInventory(UUID id, int accountIdx, ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        ensureLoaded(id);
        cache.get(id).set("accounts." + accountIdx + ".inventory", Arrays.asList(contents));
        cache.get(id).set("accounts." + accountIdx + ".armor", Arrays.asList(armor));
        cache.get(id).set("accounts." + accountIdx + ".offhand", offHand);
    }

    /** Zwraca zapisany ekwipunek konta (36 slotów); pusty jeśli brak danych. */
    public ItemStack[] getInventory(UUID id, int accountIdx) {
        ensureLoaded(id);
        List<?> list = cache.get(id).getList("accounts." + accountIdx + ".inventory");
        if (list == null) return new ItemStack[36];
        ItemStack[] inv = new ItemStack[36];
        for (int i = 0; i < Math.min(list.size(), 36); i++) {
            inv[i] = deserializeItem(list.get(i));
        }
        return inv;
    }

    /** Zwraca zapisany pancerz konta (4 sloty); pusty jeśli brak danych. */
    public ItemStack[] getArmor(UUID id, int accountIdx) {
        ensureLoaded(id);
        List<?> list = cache.get(id).getList("accounts." + accountIdx + ".armor");
        if (list == null) return new ItemStack[4];
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < Math.min(list.size(), 4); i++) {
            armor[i] = deserializeItem(list.get(i));
        }
        return armor;
    }

    public ItemStack getOffHand(UUID id, int accountIdx) {
        ensureLoaded(id);
        Object o = cache.get(id).get("accounts." + accountIdx + ".offhand");
        return deserializeItem(o);
    }

    private ItemStack deserializeItem(Object o) {
        if (o == null) return null;
        if (o instanceof ItemStack is) return is;
        if (o instanceof Map<?, ?> map) {
            try {
                return ItemStack.deserialize((Map<String, Object>) map);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}