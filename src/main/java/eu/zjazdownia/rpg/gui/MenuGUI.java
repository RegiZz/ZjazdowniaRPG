package eu.zjazdownia.rpg.gui;

/**
 * Menu gracza (/menu): pozycje jak Znajomi; otwiera listę znajomych i AnvilGUI do dodawania
 * znajomego po nicku. Używa FriendsManager.
 */
import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.friends.FriendsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import net.wesjd.anvilgui.AnvilGUI;


import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class MenuGUI implements Listener {

    private final ZjazdowniaRPG plugin;
    private final FriendsManager friendsManager;

    public MenuGUI(ZjazdowniaRPG plugin, FriendsManager friendsManager){
        this.plugin = plugin;
        this.friendsManager = friendsManager;
    }

    public void openFor(Player p) {
        String name = "&5Menu Gracza";
        Inventory inv = Bukkit.createInventory(null, 18, ChatColor.translateAlternateColorCodes('&',
                name));

        int[] slots = {2};

        Material icon = Material.ENDER_EYE;

        ItemStack it = new ItemStack(icon);
        ItemMeta meta = it.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aZnajomi"));
        it.setItemMeta(meta);
        inv.setItem(slots[0], it);

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        e.setCancelled(true);

        if (title.equals(ChatColor.translateAlternateColorCodes('&',"&5Menu Gracza"))) {
            if (e.getCurrentItem().getType() == Material.ENDER_EYE) {
                openFriends(p);
            }
        }

        if (title.equals("§8Znajomi")) {
            if (e.getCurrentItem().getType() == Material.PAPER) {
                p.closeInventory();
                openAddFriendAnvil(p);
            }

            if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                String name = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "");
                p.sendMessage("§eZnajomy: §f" + name);
            }
        }
    }

    private void openFriends(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Znajomi");

        inv.setItem(49, item(Material.PAPER, "§bDodaj znajomego"));

        int slot = 0;
        for (UUID friendId : friendsManager.getFriends(player.getUniqueId())) {
            if (slot == 49) slot++;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(friendsManager.getOffline(friendId));
            meta.setDisplayName("§e" + meta.getOwningPlayer().getName());
            head.setItemMeta(meta);

            inv.setItem(slot++, head);
        }

        player.openInventory(inv);
    }


    private ItemStack item(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        it.setItemMeta(meta);
        return it;
    }


    private void openAddFriendAnvil(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Dodaj znajomego")
                .itemLeft(new ItemStack(Material.PAPER))
                .text("Nick gracza")
                .onClick((slot, stateSnapshot) -> {

                    String text = stateSnapshot.getText();

                    if (text == null || text.isBlank()) {
                        player.sendMessage("§cPodaj nick gracza!");
                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                    }

                    OfflinePlayer target = Bukkit.getOfflinePlayer(text);

                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        player.sendMessage("§cGracz §e" + text + " §cnie istnieje!");
                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                    }

                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage("§cNie możesz dodać sam siebie!");
                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                    }

                    if (friendsManager.isFriend(player.getUniqueId(), target.getUniqueId())) {
                        player.sendMessage("§cGracz §e" + target.getName() + " §cjest już Twoim znajomym!");
                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                    }

                    friendsManager.addFriend(player.getUniqueId(), target.getUniqueId());
                    player.sendMessage("§aDodano znajomego: §e" + target.getName());

                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }


}
