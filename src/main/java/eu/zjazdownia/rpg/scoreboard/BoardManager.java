package eu.zjazdownia.rpg.scoreboard;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.account.AccountManager;
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
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public BoardManager(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
    }

    public void show(Player p, AccountManager am) {
        hide(p);

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("zjazd", "dummy", ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("scoreboard.title")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        p.setScoreboard(sb);

        // task aktualizacji co 2s
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
            String clazz = am.getSelectedClass(p.getUniqueId());
            int level = am.getLevel(p.getUniqueId());

            // Czyść poprzednie wpisy – po prostu twórz nowy objective (bez migotania)
            obj.unregister();
            Objective nobj = sb.registerNewObjective("zjazd", "dummy", ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("scoreboard.title")));
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
        }, 1L, 40L);

        tasks.put(p.getUniqueId(), task);
    }

    public void hide(Player p) {
        BukkitTask t = tasks.remove(p.getUniqueId());
        if (t != null) t.cancel();
        // nie wyłączamy scoreboardu – niech zostanie ostatni stan
    }
}
