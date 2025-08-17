package eu.zjazdownia.rpg.gui;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.account.AccountManager;
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

        // Slot 1 – darmowy
        inv.setItem(3, makeItem(
                Material.valueOf(plugin.getConfig().getString("gui.account.free.icon")),
                plugin.getConfig().getString("gui.account.free.name"),
                plugin.getConfig().getStringList("gui.account.free.lore")
        ));

        // Slot 2 – płatny (zablokowany jeśli brak permisji)
        boolean unlocked = p.hasPermission("zjazdownia.account.2");
        String icon = plugin.getConfig().getString(unlocked ? "gui.account.paid.icon_unlocked" : "gui.account.paid.icon_locked");
        inv.setItem(5, makeItem(
                Material.valueOf(icon),
                plugin.getConfig().getString(unlocked ? "gui.account.paid.name_unlocked" : "gui.account.paid.name_locked"),
                plugin.getConfig().getStringList(unlocked ? "gui.account.paid.lore_unlocked" : "gui.account.paid.lore_locked")
        ));

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

        // jeśli klasa nie wybrana – otwórz kreator, w przeciwnym razie pokaż scoreboard
        if (am.getSelectedClass(p.getUniqueId()) == null) {
            plugin.classGUI().openFor(p);
        } else {
            plugin.board().show(p, am);
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
    }
}
