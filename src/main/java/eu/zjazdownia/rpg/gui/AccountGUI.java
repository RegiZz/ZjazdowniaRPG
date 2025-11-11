package eu.zjazdownia.rpg.gui;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.magicItems.Heartstone;
import eu.zjazdownia.rpg.scoreboard.BoardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountGUI implements Listener {

    private final ZjazdowniaRPG plugin;

    public AccountGUI(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
    }

    public void openFor(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.account.title")));

        AccountManager am = plugin.accounts();
        p.getInventory().clear();

        // --- Konto 1 ---
        int idx1 = 1;
        am.setCurrentAccount(p.getUniqueId(), idx1);
        String clazz1 = am.getSelectedClass(p.getUniqueId());
        int level1 = am.getLevel(p.getUniqueId());
        int exp1 = am.getExp(p.getUniqueId());

        List<String> lore1 = new ArrayList<>(plugin.getConfig().getStringList("gui.account.free.lore"));
        lore1.add("");
        lore1.add(ChatColor.GRAY + "Klasa: " + ChatColor.YELLOW + (clazz1 == null ? "—" : clazz1));
        lore1.add(ChatColor.GRAY + "Poziom: " + ChatColor.GREEN + level1);
        lore1.add(ChatColor.GRAY + "Doświadczenie: " + ChatColor.AQUA + exp1);

        inv.setItem(3, makeItem(
                Material.valueOf(plugin.getConfig().getString("gui.account.free.icon")),
                plugin.getConfig().getString("gui.account.free.name"),
                lore1
        ));

        // --- Konto 2 ---
        int idx2 = 2;
        boolean unlocked = p.hasPermission("zjazdownia.account.2");
        String icon = plugin.getConfig().getString(unlocked ? "gui.account.paid.icon_unlocked" : "gui.account.paid.icon_locked");
        String name = plugin.getConfig().getString(unlocked ? "gui.account.paid.name_unlocked" : "gui.account.paid.name_locked");

        List<String> lore2 = new ArrayList<>(plugin.getConfig().getStringList(
                unlocked ? "gui.account.paid.lore_unlocked" : "gui.account.paid.lore_locked"
        ));

        if (unlocked) {
            am.setCurrentAccount(p.getUniqueId(), idx2);
            String clazz2 = am.getSelectedClass(p.getUniqueId());
            int level2 = am.getLevel(p.getUniqueId());
            int exp2 = am.getExp(p.getUniqueId());

            lore2.add("");
            lore2.add(ChatColor.GRAY + "Klasa: " + ChatColor.YELLOW + (clazz2 == null ? "—" : clazz2));
            lore2.add(ChatColor.GRAY + "Poziom: " + ChatColor.GREEN + level2);
            lore2.add(ChatColor.GRAY + "Doświadczenie: " + ChatColor.AQUA + exp2);
        }

        inv.setItem(5, makeItem(Material.valueOf(icon), name, lore2));

        // Przywróć obecne konto po przygotowaniu GUI
        am.setCurrentAccount(p.getUniqueId(), am.getCurrentAccount(p.getUniqueId()));

        p.openInventory(inv);
    }

    private ItemStack makeItem(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lore.stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).toList());
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.account.title")))) {
            e.setCancelled(true);
            HumanEntity he = e.getWhoClicked();
            if (!(he instanceof Player p)) return;

            if (e.getRawSlot() == 3) {
                selectAccount(p, 1);
            } else if (e.getRawSlot() == 5) {
                if (p.hasPermission("zjazdownia.account.2")) selectAccount(p, 2);
                else p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
            }
        }
    }

    private void selectAccount(Player p, int idx) {
        AccountManager am = plugin.accounts();
        am.setCurrentAccount(p.getUniqueId(), idx);

        // Przywróć ekwipunek dla nowego konta
        ItemStack[] inv = am.getInventory(p.getUniqueId(), idx);
        if(inv == null) inv = new ItemStack[36];
        p.getInventory().clear();
        p.getInventory().setContents(inv);

        p.closeInventory();
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        p.sendMessage(ChatColor.GREEN + "☑ Wybrano konto #" + idx + "!");

        boolean hasHeartstone = false;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.isSimilar(Heartstone.createHeartstone())) {
                hasHeartstone = true;
                break;
            }
        }

        if (!hasHeartstone) {
            p.getInventory().addItem(Heartstone.createHeartstone());
        }

        if (am.getSelectedClass(p.getUniqueId()) == null) {
            plugin.classGUI().openFor(p);
        } else {
            plugin.board().show(p, am);
        }
    }
}
