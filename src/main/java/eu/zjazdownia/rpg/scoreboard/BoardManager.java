package eu.zjazdownia.rpg.scoreboard;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.party.Party;
import eu.zjazdownia.rpg.party.PartyManager;
import org.bukkit.Bukkit;

/**
 * Menedżer scoreboardu: pokazuje sidebar z nickiem, klasą, poziomem, IP (z configu)
 * lub przełącza na listę party (rotacja co 15s). Taski odświeżania i rotacji per gracz.
 */
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

    private final Map<UUID, BoardTasks> tasks = new HashMap<>();

    private long updateIntervalTicks;
    private long rotationIntervalTicks;

    public BoardManager(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    /** Przeładowuje parametry z config.yml (scoreboard.update-interval-ticks, rotation-interval-ticks). */
    public void reloadFromConfig() {
        var cfg = plugin.getConfig();
        updateIntervalTicks = Math.max(1, cfg.getLong("scoreboard.update-interval-ticks", 40));
        rotationIntervalTicks = Math.max(1, cfg.getLong("scoreboard.rotation-interval-ticks", 300));
    }

    /** Holder tasków: odświeżanie linii i rotacja party/player. */
    private static class BoardTasks {
        BukkitTask updateTask;
        BukkitTask rotationTask;
    }

    /** Inicjuje scoreboard dla gracza (player board + rotacja na party board). */
    public void show(Player p, AccountManager am, PartyManager pm) {
        hide(p);

        UUID uuid = p.getUniqueId();
        showingParty.put(uuid, false);

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(sb);

        BukkitTask updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (showingParty.getOrDefault(uuid, false)) return;
            showPlayerBoard(p, am);
        }, 0L, updateIntervalTicks);

        BukkitTask rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Party party = pm.getParty(uuid);

            if (party == null) {
                showingParty.put(uuid, false);
                showPlayerBoard(p, am);
                return;
            }

            boolean showPartyNow = !showingParty.getOrDefault(uuid, false);
            showingParty.put(uuid, showPartyNow);

            if (showPartyNow) {
                showPartyBoard(p, party);
            } else {
                showPlayerBoard(p, am);
            }

        }, 20L, rotationIntervalTicks);

        BoardTasks bt = new BoardTasks();
        bt.updateTask = updateTask;
        bt.rotationTask = rotationTask;
        tasks.put(uuid, bt);
    }

    /** Ustawia sidebar na standardowy (nick, klasa, poziom, IP z configu). */
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

    /** Ustawia sidebar na listę party (lider + członkowie). */
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

    /** Zatrzymuje taski scoreboardu dla gracza (np. przy quit). */
    public void hide(Player p) {
        UUID uuid = p.getUniqueId();
        BoardTasks bt = tasks.remove(uuid);
        if (bt == null) return;

        if (bt.updateTask != null) bt.updateTask.cancel();
        if (bt.rotationTask != null) bt.rotationTask.cancel();
    }
}
