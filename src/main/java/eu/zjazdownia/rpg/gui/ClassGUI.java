package eu.zjazdownia.rpg.gui;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.util.LocUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassGUI implements Listener {
    private final ZjazdowniaRPG plugin;

    public ClassGUI(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
    }

    public void openFor(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.class.title")));
        ConfigurationSection classes = plugin.getConfig().getConfigurationSection("classes");
        int[] slots = {2, 4, 6};
        int i = 0;
        for (String key : classes.getKeys(false)) {
            ConfigurationSection sec = classes.getConfigurationSection(key);
            Material icon = Material.valueOf(sec.getString("icon"));
            String name = sec.getString("display");
            List<String> lore = new ArrayList<>();
            for (String l : sec.getStringList("lore"))
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            ItemStack it = new ItemStack(icon);
            ItemMeta meta = it.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(lore);
            it.setItemMeta(meta);
            inv.setItem(slots[i++], it);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
// Sprawdź tytuł GUI (po stripColor)
        String cfgTitleRaw = plugin.getConfig().getString("gui.class.title");
        if (cfgTitleRaw == null) return;
        String guiTitle = ChatColor.translateAlternateColorCodes('&', cfgTitleRaw);
        String currentTitle = e.getView().getTitle();
        if (currentTitle == null || !ChatColor.stripColor(currentTitle).equals(ChatColor.stripColor(guiTitle))) return;

// Reaguj tylko, gdy kliknięto w górną część GUI
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        e.setCancelled(true);

        HumanEntity he = e.getWhoClicked();
        if (!(he instanceof Player)) return;
        Player p = (Player) he;

        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;
        if (!e.getCurrentItem().hasItemMeta()) return;
        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String chosen = ChatColor.stripColor(meta.getDisplayName());

// znajdź klucz klasy po display
        ConfigurationSection classesSec = plugin.getConfig().getConfigurationSection("classes");
        if (classesSec == null) return;

        String key = classesSec.getKeys(false).stream()
                .filter(k -> {
                    String disp = plugin.getConfig().getString("classes." + k + ".display");
                    if (disp == null) return false;
                    String dispCol = ChatColor.translateAlternateColorCodes('&', disp);
                    return ChatColor.stripColor(dispCol).equals(chosen);
                })
                .findFirst().orElse(null);
        if (key == null) return;

        AccountManager am = plugin.accounts();
        am.setSelectedClass(p.getUniqueId(), key);

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.closeInventory();
        p.sendMessage(ChatColor.GREEN + "Wybrałeś klasę: " + ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("classes." + key.toLowerCase(Locale.ROOT) + ".display")));


// daj podstawowe przedmioty w zależności od klasy
        giveStarterKit(p, key);

// pokaż stały scoreboard
        plugin.board().show(p, am);
    }

    private void giveStarterKit(Player p, String classKey) {
        String k = classKey == null ? "" : classKey.toLowerCase(Locale.ROOT);

// Wojownik / Warrior
        if (k.equals("wojownik") || k.equals("warrior")) {
            giveIfMissing(p, new ItemStack(Material.STONE_SWORD), 1);
            giveIfMissing(p, new ItemStack(Material.STONE_AXE), 1);
            p.sendMessage(ChatColor.YELLOW + "Otrzymałeś podstawowy ekwipunek Wojownika.");
            return;
        }

        if (k.equals("mag") || k.equals("mage")) {
            NamespacedKey wandKey = new NamespacedKey(plugin, "mage_wand");

// przygotuj różdżkę z tagiem
            ItemStack wand = new ItemStack(Material.BLAZE_ROD);
            ItemMeta m = wand.getItemMeta();
            if (m != null) {
                m.setDisplayName(ChatColor.LIGHT_PURPLE + "Różdżka");
// dodaj tag PDC, tylko przedmiot z tym tagiem będzie działać jako różdżka
                m.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
                m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                wand.setItemMeta(m);
            }

// sprawdź, czy gracz już posiada oznaczoną różdżkę (szukamy tagu)
            boolean hasWand = false;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                ItemMeta im = it.getItemMeta();
                if (im != null && im.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE)) {
                    hasWand = true;
                    break;
                }
            }

            if (!hasWand) {
                giveOrDrop(p, wand);
            }

            p.sendMessage(ChatColor.YELLOW + "Otrzymałeś różdżkę maga.");
            return;
        }

// Łucznik / Archer
        if (k.equals("łucznik") || k.equals("lucznik") || k.equals("archer")) {
            giveIfMissing(p, new ItemStack(Material.BOW), 1);
// zapewnij co najmniej 32 strzały
            int arrows = countInInventory(p, Material.ARROW);
            if (arrows < 32) {
                giveOrDrop(p, new ItemStack(Material.ARROW, 32 - arrows));
            }
            p.sendMessage(ChatColor.YELLOW + "Otrzymałeś podstawowy ekwipunek Łucznika.");
        }
    }

    private void giveIfMissing(Player p, ItemStack item, int minAmount) {
        Material type = item.getType();
        int have = countInInventory(p, type);
        if (have < minAmount) {
            giveOrDrop(p, new ItemStack(type, minAmount - have));
        }
    }

    private int countInInventory(Player p, Material type) {
        int count = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == type) {
                count += it.getAmount();
            }
        }
        return count;
    }

    private void giveOrDrop(Player p, ItemStack item) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(item);
        if (!left.isEmpty()) {
            for (ItemStack it : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), it);
            }
        }
    }
}