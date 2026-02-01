package eu.zjazdownia.rpg.commands;

import eu.zjazdownia.rpg.account.AccountManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ResetKontoCommand implements CommandExecutor {
    private final AccountManager am;

    public ResetKontoCommand(AccountManager am) {
        this.am = am;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zjazdownia.admin")) {
            sender.sendMessage("§cBrak uprawnień.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§eUżycie: /" + label + " <nick>");
                return true;
            }
            am.clearCurrentAccount(p.getUniqueId());
            p.sendMessage("§aZresetowano dane bieżącego konta. Użyj §e/klasa §aaby wybrać klasę ponownie.");
            return true;
        }

        String name = args[0];
        Player online = Bukkit.getPlayerExact(name);
        UUID uuid;

        if (online != null) {
            uuid = online.getUniqueId();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(name);
            if (off == null || (!off.hasPlayedBefore() && !off.isOnline())) {
                sender.sendMessage("§cGracz §e" + name + " §cnie był jeszcze na serwerze.");
                return true;
            }
            uuid = off.getUniqueId();
        }

        am.clearCurrentAccount(uuid);

        sender.sendMessage("§aZresetowano konto gracza §e" + (online != null ? online.getName() : name) + "§a.");
        if (online != null) {
            online.sendMessage("§eAdministrator zresetował Twoje konto. Użyj §e/klasa §6aby wybrać klasę ponownie.");
        }
        return true;
    }
}