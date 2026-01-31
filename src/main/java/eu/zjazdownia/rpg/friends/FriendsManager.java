package eu.zjazdownia.rpg.friends;

import jdk.jfr.Frequency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class FriendsManager {

    public FriendsManager(){

    }

    private final Map<UUID, Set<UUID>> friends = new HashMap<>();

    public Set<UUID> getFriends(UUID player) {
        return friends.getOrDefault(player, new HashSet<>());
    }

    public void addFriend(UUID player, UUID target) {
        friends.computeIfAbsent(player, k -> new HashSet<>()).add(target);
        friends.computeIfAbsent(target, k -> new HashSet<>()).add(player);
    }

    public boolean isFriend(UUID player, UUID target) {
        return friends.containsKey(player) && friends.get(player).contains(target);
    }

    public OfflinePlayer getOffline(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
