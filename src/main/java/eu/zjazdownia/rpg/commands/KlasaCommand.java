package eu.zjazdownia.rpg.commands;

import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.gui.ClassGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KlasaCommand implements CommandExecutor {
    private final ClassGUI gui;
    private final AccountManager am;
    public KlasaCommand(ClassGUI gui, AccountManager am) {
        this.gui = gui; this.am = am;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (am.getSelectedClass(p.getUniqueId()) == null) {
            gui.openFor(p);
        } else {
            p.sendMessage("§7Klasa już wybrana dla tego konta. Użyj §e/resetkonto §7aby zresetować (admin).");
        }
        return true;
    }
}
