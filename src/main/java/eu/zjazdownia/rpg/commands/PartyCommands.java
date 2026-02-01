package eu.zjazdownia.rpg.commands;

import eu.zjazdownia.rpg.party.Party;
import eu.zjazdownia.rpg.party.PartyManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.defaults.BukkitCommand;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Komenda /party: create, invite, join, leave, disband, list. Obsługa party przez PartyManager.
 */
public class PartyCommands implements CommandExecutor {

    private final PartyManager manager;

    public PartyCommands(ZjazdowniaRPG plugin, PartyManager manager) {
        this.manager = manager;
        plugin.getCommand("party").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Użycie:");
            p.sendMessage("/party create");
            p.sendMessage("/party invite <gracz>");
            p.sendMessage("/party join");
            p.sendMessage("/party leave");
            p.sendMessage("/party disband");
            p.sendMessage("/party list");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "create": {
                if (manager.getParty(p.getUniqueId()) != null) {
                    p.sendMessage(ChatColor.RED + "Masz już party!");
                    return true;
                }
                manager.createParty(p);
                p.sendMessage(ChatColor.GREEN + "Utworzyłeś party!");
                return true;
            }

            case "invite": {
                if (args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Użycie: /party invite <gracz>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "Gracza nie ma online!");
                    return true;
                }

                Party party = manager.getParty(p.getUniqueId());
                if (party == null || !party.isLeader(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "Nie jesteś liderem party!");
                    return true;
                }

                manager.invite(p, target);
                p.sendMessage(ChatColor.AQUA + "Wysłałeś zaproszenie do " + target.getName());
                target.sendMessage(ChatColor.LIGHT_PURPLE + p.getName() + " zaprosił cię do party! /party join");

                return true;
            }

            case "join": {
                if (!manager.hasInvite(p)) {
                    p.sendMessage(ChatColor.RED + "Nie masz żadnych zaproszeń!");
                    return true;
                }

                UUID leader = manager.getInviteSender(p);
                manager.join(p, leader);
                manager.removeInvite(p);

                Party party = manager.getParty(p.getUniqueId());
                for (UUID m : party.getMembers()) {
                    Player mem = Bukkit.getPlayer(m);
                    if (mem != null)
                        mem.sendMessage(ChatColor.GREEN + p.getName() + " dołączył do party!");
                }
                return true;
            }

            case "leave": {
                Party party = manager.getParty(p.getUniqueId());
                if (party == null) {
                    p.sendMessage(ChatColor.RED + "Nie jesteś w party!");
                    return true;
                }

                manager.leaveParty(p);

                for (UUID mem : party.getMembers()) {
                    Player pl = Bukkit.getPlayer(mem);
                    if (pl != null)
                        pl.sendMessage(ChatColor.YELLOW + p.getName() + " opuścił party.");
                }

                p.sendMessage(ChatColor.GRAY + "Opuściłeś party.");
                return true;
            }

            case "disband": {
                Party party = manager.getParty(p.getUniqueId());
                if (party == null) {
                    p.sendMessage(ChatColor.RED + "Nie jesteś w party!");
                    return true;
                }

                if (!party.isLeader(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "Tylko lider może rozwiązać party!");
                    return true;
                }

                for (UUID m : party.getMembers()) {
                    Player mem = Bukkit.getPlayer(m);
                    if (mem != null)
                        mem.sendMessage(ChatColor.RED + "Party zostało rozwiązane!");
                }

                manager.disband(party);
                return true;
            }

            case "list": {
                Party party = manager.getParty(p.getUniqueId());
                if (party == null) {
                    p.sendMessage(ChatColor.RED + "Nie jesteś w party!");
                    return true;
                }

                p.sendMessage(ChatColor.AQUA + "Członkowie party:");
                for (UUID m : party.getMembers()) {
                    Player pl = Bukkit.getPlayer(m);
                    assert pl != null;
                    if(pl.getUniqueId() == party.getLeader()){
                        p.sendMessage(ChatColor.GRAY + "- " + (pl != null ? pl.getName() : "Offline") + ChatColor.GOLD + " 👑");
                        continue;
                    }
                    p.sendMessage(ChatColor.GRAY + "- " + (pl != null ? pl.getName() : "Offline"));
                }
                return true;
            }
        }

        return true;
    }
}

