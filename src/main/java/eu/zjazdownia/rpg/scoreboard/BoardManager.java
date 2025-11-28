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

    // Mapa, czy pokazujemy party scoreboard
    private final Map<UUID, Boolean> showingParty = new HashMap<>();

    // Holder dla tasków gracza
    private final Map<UUID, BoardTasks> tasks = new HashMap<>();

    public BoardManager(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
    }

    // Holder tasków
    private static class BoardTasks {
        BukkitTask updateTask;
        BukkitTask rotationTask;
    }

    // Pokazuje scoreboard gracza
    public void show(Player p, AccountManager am, PartyManager pm) {
        hide(p);

        UUID uuid = p.getUniqueId();
        showingParty.put(uuid, false);

        // Stwórz nowy scoreboard
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(sb);

        // Task aktualizacji statystyk co 2 sekundy
        BukkitTask updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (showingParty.get(uuid)) return; // jeśli party scoreboard, nie aktualizuj

            showPlayerBoard(p, am); // aktualizacja zwykłego scoreboardu
        }, 20L, 40L);

        // Task rotacji między scoreboardem gracza a party co 15 sekund
        BukkitTask rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Party party = pm.getParty(uuid);

            if (party == null) {
                showingParty.put(uuid, false);
                return;
            }

            boolean showPartyNow = !showingParty.get(uuid);
            showingParty.put(uuid, showPartyNow);

            if (showPartyNow) {
                showPartyBoard(p, party);
            } else {
                showPlayerBoard(p, am);
            }

        }, 0L, 300L); // co 15 sekund

        // Zapisz taski w mapie
        BoardTasks bt = new BoardTasks();
        bt.updateTask = updateTask;
        bt.rotationTask = rotationTask;
        tasks.put(uuid, bt);

        showPlayerBoard(p, am);
    }

    // Pokazuje zwykły scoreboard gracza
    public void showPlayerBoard(Player p, AccountManager am) {
        UUID uuid = p.getUniqueId();
        Scoreboard sb = p.getScoreboard();

        Objective old = sb.getObjective(DisplaySlot.SIDEBAR);
        if (old != null) old.unregister();

        Objective obj = sb.registerNewObjective("zjazd", "dummy",
                ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboard.title")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        String clazz = am.getSelectedClass(uuid);
        int level = am.getLevel(uuid);

        int score = lines.size();
        for (String raw : lines) {
            String line = raw
                    .replace("%ip%", plugin.getConfig().getString("server.ip"))
                    .replace("%player%", p.getName())
                    .replace("%class%", clazz == null ? "—" : plugin.getConfig().getString("classes." + clazz + ".short"))
                    .replace("%level%", String.valueOf(level));

            obj.getScore(ChatColor.translateAlternateColorCodes('&', line)).setScore(score--);
        }
    }

    // Pokazuje scoreboard party
    public void showPartyBoard(Player p, Party party) {
        Scoreboard sb = p.getScoreboard();
        Objective old = sb.getObjective(DisplaySlot.SIDEBAR);
        if (old != null) old.unregister();
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
    }

    // Zatrzymuje wszystkie taski gracza
    public void hide(Player p) {
        UUID uuid = p.getUniqueId();
        BoardTasks bt = tasks.remove(uuid);
        if (bt == null) return;

        if (bt.updateTask != null) bt.updateTask.cancel();
        if (bt.rotationTask != null) bt.rotationTask.cancel();
    }
}
