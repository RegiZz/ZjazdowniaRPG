package eu.zjazdownia.rpg.commands;

import eu.zjazdownia.rpg.gui.AccountGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Komenda /konto: otwiera GUI wyboru konta (konto 1 / konto 2 z uprawnieniem).
 */
public class KontoCommand implements CommandExecutor {
    private final AccountGUI gui;
    public KontoCommand(AccountGUI gui) { this.gui = gui; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        gui.openFor(p);
        return true;
    }
}
