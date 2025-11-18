package eu.zjazdownia.rpg.scoreboard;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.party.Party;
import eu.zjazdownia.rpg.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoardManager {

    private final ZjazdowniaRPG plugin;
    private final Map<UUID, Boolean> showingParty = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public BoardManager(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
    }

    public void show(Player p, AccountManager am, PartyManager pm) {
        hide(p);

        UUID uuid = p.getUniqueId();
        showingParty.put(uuid, false); // start na scoreboardzie gracza

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("zjazd", "dummy",
                ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboard.title")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        p.setScoreboard(sb);

        // task aktualizacji statystyk CO 2 SEK (jak było)
        BukkitTask updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (showingParty.get(uuid)) return; // gdy party scoreboard jest aktywny -> nie ruszać

            List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
            String clazz = am.getSelectedClass(uuid);
            int level = am.getLevel(uuid);

            obj.unregister();
            Objective nobj = sb.registerNewObjective("zjazd", "dummy",
                    ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboard.title")));
            nobj.setDisplaySlot(DisplaySlot.SIDEBAR);

            int score = lines.size();
            for (String raw : lines) {
                String line = raw
                        .replace("%ip%", plugin.getConfig().getString("server.ip"))
                        .replace("%player%", p.getName())
                        .replace("%class%", clazz == null ? "—" : plugin.getConfig().getString("classes."+clazz+".short"))
                        .replace("%level%", String.valueOf(level));

                nobj.getScore(ChatColor.translateAlternateColorCodes('&', line)).setScore(score--);
            }
        }, 20L, 40L);

        // task rotacji co 15 sekund
        BukkitTask rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Party party = pm.getParty(uuid);

            // jeśli nie ma party → zawsze scoreboard gracza
            if (party == null) {
                showingParty.put(uuid, false);
                return;
            }

            // zmiana stanu
            boolean showPartyNow = !showingParty.get(uuid);
            showingParty.put(uuid, showPartyNow);

            if (showPartyNow) {
                showPartyBoard(p, party);
            } else {
                show(p, am, pm); // wróć do zwykłego scoreboardu
            }

        }, 0L, 300L); // co 15 sekund (20 tics * 15)

        tasks.put(uuid, updateTask);
        tasks.put(UUID.fromString(uuid + "-rot"), rotationTask);
    }


    public void showPartyBoard(Player p, Party party) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("zjazd_party", "dummy",
                ChatColor.LIGHT_PURPLE + "❤ Twoje Party ❤");

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = 15;

        obj.getScore(ChatColor.GOLD + "Lider:").setScore(score--);
        Player leader = Bukkit.getPlayer(party.getLeader());
        obj.getScore(ChatColor.YELLOW + (leader != null ? leader.getName() : "Offline")).setScore(score--);

        obj.getScore(" ").setScore(score--);

        obj.getScore(ChatColor.GOLD + "Członkowie:").setScore(score--);

        for (UUID id : party.getMembers()) {
            Player mem = Bukkit.getPlayer(id);
            obj.getScore(ChatColor.AQUA + "- " + (mem != null ? mem.getName() : "Offline"))
                    .setScore(score--);
        }

        p.setScoreboard(sb);
    }


    public void hide(Player p) {
        BukkitTask t = tasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
    }
}