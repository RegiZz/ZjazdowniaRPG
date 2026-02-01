package eu.zjazdownia.rpg.party;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class PartyManager {

    private final Map<UUID, Party> partyByMember = new HashMap<>();
    private final Map<UUID, UUID> invites = new HashMap<>();
    private final Plugin plugin;

    public PartyManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Party getParty(UUID uuid) {
        return partyByMember.get(uuid);
    }

    public Party createParty(Player leader) {
        Party party = new Party(leader.getUniqueId());
        partyByMember.put(leader.getUniqueId(), party);
        return party;
    }

    public void disband(Party party) {
        for (UUID member : party.getMembers()) {
            partyByMember.remove(member);
        }
    }

    public void leaveParty(Player player) {
        Party party = getParty(player.getUniqueId());
        if (party == null) return;

        party.removeMember(player.getUniqueId());
        partyByMember.remove(player.getUniqueId());

        if (party.getLeader().equals(player.getUniqueId())) {
            disband(party);
        }
    }

    public void invite(Player inviter, Player target) {
        invites.put(target.getUniqueId(), inviter.getUniqueId());
    }

    public boolean hasInvite(Player p) {
        return invites.containsKey(p.getUniqueId());
    }

    public UUID getInviteSender(Player p) {
        return invites.get(p.getUniqueId());
    }

    public void removeInvite(Player p) {
        invites.remove(p.getUniqueId());
    }

    public void join(Player p, UUID leader) {
        Party party = partyByMember.get(leader);
        if (party == null) return;

        party.addMember(p.getUniqueId());
        partyByMember.put(p.getUniqueId(), party);
    }
}
