package eu.zjazdownia.rpg.commands;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.level.LevelManager;
import eu.zjazdownia.rpg.magicItems.LightningWand;
import eu.zjazdownia.rpg.magicItems.RicochetBow;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Komenda administracyjna /zradmin: setlevel, addxp, setclass, givewand, givebow, info, setfirstspawn.
 * Wymaga uprawnienia zjazdownia.admin.
 */
public class ZRAdminCommand implements CommandExecutor {

    private final ZjazdowniaRPG plugin;
    private final AccountManager am;
    private final LevelManager lm;
    private final NamespacedKey wandKey;
    private final LightningWand lightningWand;

    public ZRAdminCommand(ZjazdowniaRPG plugin, LightningWand lightningWand) {
        this.plugin = plugin;
        this.am = plugin.accounts();
        this.lm = plugin.levels();
        this.wandKey = new NamespacedKey(plugin, "mage_wand");
        this.lightningWand = lightningWand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zjazdownia.admin")) {
            sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "setlevel" -> cmdSetLevel(sender, args);
                case "addxp" -> cmdAddXp(sender, args);
                case "setclass" -> cmdSetClass(sender, args);
                case "givewand" -> cmdGiveWand(sender, args);
                case "givebow" -> cmdBow(sender, args);
                case "info" -> cmdInfo(sender, args);
                case "setfirstspawn" -> cmdSetFirstSpawn(sender);
                default -> sendHelp(sender);
            }
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Błąd: " + ex.getMessage());
            plugin.getLogger().warning("ZRAdminCommand error: " + ex.getMessage());
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "ZRAdmin:");
        s.sendMessage(ChatColor.YELLOW + "/zradmin setlevel <nick> <level>");
        s.sendMessage(ChatColor.YELLOW + "/zradmin addxp <nick> <amount>");
        s.sendMessage(ChatColor.YELLOW + "/zradmin setclass <nick> <classKey>");
        s.sendMessage(ChatColor.YELLOW + "/zradmin givewand <nick> <wandName>");
        s.sendMessage(ChatColor.YELLOW + "/zradmin givebow <nick> <bowName>");
        s.sendMessage(ChatColor.YELLOW + "/zradmin info <nick>");
        s.sendMessage(ChatColor.YELLOW + "/zradmin setfirstspawn");
    }

    private void cmdSetFirstSpawn(CommandSender s) {
        Player p = (Player) s;
        Location playerLocation =  p.getLocation();
        FileConfiguration config = plugin.getConfig();

        config.set("firstSpawn.world", playerLocation.getWorld().getName());
        config.set("firstSpawn.x", playerLocation.getBlockX());
        config.set("firstSpawn.y", playerLocation.getBlockY());
        config.set("firstSpawn.z", playerLocation.getBlockZ());
        config.set("firstSpawn.yaw", playerLocation.getYaw());
        config.set("firstSpawn.pitch", playerLocation.getPitch());
        p.sendMessage(ChatColor.GREEN + "Ustawiono miejsce w którym gracze będą sie spawnować po pierwszym wejsciu na serwer!");
    }

    private void cmdSetLevel(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage(ChatColor.RED + "Użycie: /zradmin setlevel <nick> <level>"); return; }
        String name = args[1];
        int lvl = Integer.parseInt(args[2]);
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off == null || ( !off.hasPlayedBefore() && !off.isOnline() )) {
            s.sendMessage(ChatColor.RED + "Nieznany gracz: " + name);
            return;
        }
        UUID uuid = off.getUniqueId();
        am.ensureLoaded(uuid);
        am.setLevel(uuid, lvl);
        am.flush(uuid);
        s.sendMessage(ChatColor.GREEN + "Ustawiono level " + lvl + " dla " + name);
// jeśli online — odśwież atrybuty i zapisz
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            plugin.levels().refreshAttributes(online);
            online.sendMessage(ChatColor.YELLOW + "Administrator ustawił Twój poziom na " + lvl);
        }
    }

    private void cmdAddXp(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage(ChatColor.RED + "Użycie: /zradmin addxp <nick> <amount>"); return; }
        String name = args[1];
        int amt = Integer.parseInt(args[2]);
        Player p = Bukkit.getPlayerExact(name);
        if (p != null && p.isOnline()) {
            lm.addExp(p, amt);
            s.sendMessage(ChatColor.GREEN + "Dodano " + amt + " XP do " + name);
            p.sendMessage(ChatColor.YELLOW + "Otrzymałeś " + amt + " XP (admin).");
            return;
        }
// offline: modyfikuj plik konta bez odpalania efektów runtime
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off == null) { s.sendMessage(ChatColor.RED + "Nieznany gracz: " + name); return; }
        UUID uuid = off.getUniqueId();
        am.ensureLoaded(uuid);
        int currentExp = am.getExp(uuid);
        int currentLevel = am.getLevel(uuid);
        int exp = currentExp + amt;
        int level = currentLevel;
// prosta pętla, używamy LevelManager.requiredExpFor
        while (level < lm.maxLevel) { // maxLevel musimy mieć publiczny getter albo udostępnić metodę; jeśli nie, zmodyfikuj
            int req = lm.requiredExpFor(level);
            if (exp >= req) {
                exp -= req;
                level++;
            } else break;
        }
        am.setExp(uuid, exp);
        am.setLevel(uuid, level);
        am.flush(uuid);
        s.sendMessage(ChatColor.GREEN + "Dodano " + amt + " XP do (offline) " + name + ". Nowy lvl=" + level + " exp=" + exp);
    }

    private void cmdSetClass(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage(ChatColor.RED + "Użycie: /zradmin setclass <nick> <classKey>"); return; }
        String name = args[1], key = args[2];
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off == null) { s.sendMessage(ChatColor.RED + "Nieznany gracz: " + name); return; }
        UUID uuid = off.getUniqueId();
        am.ensureLoaded(uuid);
        am.setSelectedClass(uuid, key);
        am.flush(uuid);
        s.sendMessage(ChatColor.GREEN + "Ustawiono klasę " + key + " dla " + name);
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) online.sendMessage(ChatColor.YELLOW + "Administrator ustawił Twoją klasę: " + key);
    }

    private void cmdGiveWand(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage(ChatColor.RED + "Użycie: /zradmin givewand <nick> <mage/lightning>"); return; }
        String name = args[1];
        String key = args[2];
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) { s.sendMessage(ChatColor.RED + "Gracz offline: " + name); return; }

        switch(key){
            case "mage":{
                ItemStack wand = new ItemStack(org.bukkit.Material.BLAZE_ROD);
                ItemMeta m = wand.getItemMeta();
                if (m != null) {
                    m.setDisplayName(ChatColor.LIGHT_PURPLE + "Różdżka");
                    m.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
                    wand.setItemMeta(m);
                }
                Map<Integer, ItemStack> leftover = p.getInventory().addItem(wand);
                if (!leftover.isEmpty()) leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
                s.sendMessage(ChatColor.GREEN + "Dano różdżkę graczowi " + name);
                p.sendMessage(ChatColor.YELLOW + "Otrzymałeś różdżkę (admin).");
                break;
            }
            case "lightning":{
                p.getInventory().addItem(LightningWand.createWand());
                break;
            }

            default:{p.sendMessage(ChatColor.RED + "Nie znaleziono różdżki");}

        }


    }

    private void cmdBow(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage(ChatColor.RED + "Użycie: /zradmin givebow <nick> <ricochet>"); return; }
        String name = args[1];
        String key = args[2];
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) { s.sendMessage(ChatColor.RED + "Gracz offline: " + name); return; }
        switch(key){
            case "ricochet":{
                p.getInventory().addItem(RicochetBow.getItem());
            }
        }

    }

    private void cmdInfo(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage(ChatColor.RED + "Użycie: /zradmin info <nick>"); return; }
        String name = args[1];
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off == null) { s.sendMessage(ChatColor.RED + "Nieznany gracz: " + name); return; }
        UUID uuid = off.getUniqueId();
        am.ensureLoaded(uuid);
        int level = am.getLevel(uuid);
        int exp = am.getExp(uuid);
        String cls = am.getSelectedClass(uuid);
        int account = am.getCurrentAccount(uuid);
        s.sendMessage(ChatColor.GOLD + "Info " + name + ": " + ChatColor.AQUA + "lvl=" + level + " exp=" + exp + " class=" + cls + " acc=" + account);
    }
}