package eu.zjazdownia.rpg.commands;

import eu.zjazdownia.rpg.cities.City;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CityComands implements CommandExecutor {

    private final Map<Integer, City> citiesById = new HashMap<>();
    private Integer nextCityId = null;

    public CityComands() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!sender.hasPermission("zjazdownia.admin")) {
            sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
            return true;
        }
        if (args.length == 0 || args[0].toLowerCase().equals("help")) {
            p.sendMessage(ChatColor.BLUE + "Cities");
            p.sendMessage(ChatColor.YELLOW + "/city info - informacje o miastach");
            p.sendMessage(ChatColor.YELLOW + "/city create <nazwa> <promien> - tworzy nowe miasto");
            p.sendMessage(ChatColor.YELLOW + "/city delete <id> - usuwa miasto o podanym ID");
            return true;
        }
        Location playerLocation = p.getLocation();

        switch (args[0].toLowerCase()) {
            case "info": {
                City at = findCityAt(playerLocation);
                if (at == null) {
                    p.sendMessage(ChatColor.YELLOW + "Nie znajdujesz się w obrębie żadnego miasta.");
                    break;
                }
                Location cLoc = at.getLocation();
                String world = cLoc.getWorld() != null ? cLoc.getWorld().getName() : "unknown";
                p.sendMessage(ChatColor.GOLD + "Miasto: " + ChatColor.AQUA + at.getName());
                p.sendMessage(ChatColor.GOLD + "ID: " + ChatColor.AQUA + at.getID());
                p.sendMessage(ChatColor.GOLD + "Środek: " + ChatColor.AQUA
                        + world + " (" + cLoc.getBlockX() + ", " + cLoc.getBlockY() + ", " + cLoc.getBlockZ() + ")");
                p.sendMessage(ChatColor.GOLD + "Promień: " + ChatColor.AQUA + at.getRadius());
                break;
            }
            case "create": {
                if (args.length < 3) {
                    p.sendMessage(ChatColor.RED + "Użycie: /" + label + " create <nazwa> <promień>");
                    break;
                }
                String cityName = args[1];
                int cityRadius;
                try {
                    cityRadius = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Promień musi być liczbą całkowitą.");
                    break;
                }
                int id = nextId();
                Location cityLocation = new Location(playerLocation.getWorld(), playerLocation.getX(), playerLocation.getY(), playerLocation.getZ());
                City city = new City(cityName, id, cityLocation, cityRadius);
                citiesById.put(id, city);
                p.sendMessage(ChatColor.GREEN + "Utworzono miasto '" + cityName + "' z ID=" + id + " i promieniem " + cityRadius + ".");
                break;
            }
            case "delete": {
                if (args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Użycie: /" + label + " delete <id>");
                    break;
                }
                int idToDelete;
                try {
                    idToDelete = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "ID musi być liczbą całkowitą.");
                    break;
                }
                City removed = citiesById.remove(idToDelete);
                if (removed == null) {
                    p.sendMessage(ChatColor.RED + "Nie znaleziono miasta o ID=" + idToDelete);
                    break;
                }
                p.sendMessage(ChatColor.GREEN + "Usunięto miasto '" + removed.getName() + "' (ID=" + idToDelete + ").");
                break;
            }
            default:
                p.sendMessage(ChatColor.RED + "Nieznana komenda.");
        }
        return true;
    }
    public City findCityAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        City found = null;
        double bestDist = Double.MAX_VALUE;
        for (City c : citiesById.values()) {
            Location center = c.getLocation();
            if (center == null || center.getWorld() == null) continue;
            if (!center.getWorld().equals(loc.getWorld())) continue;
            double dx = loc.getX() - center.getX();
            double dz = loc.getZ() - center.getZ();
            double dist2D = Math.hypot(dx, dz);
            if (dist2D <= c.getRadius() && dist2D < bestDist) {
                bestDist = dist2D;
                found = c;
            }
        }
        return found;
    }

    private int nextId() {
        if (nextCityId == null) {
            int max = 0;
            for (int id : citiesById.keySet()) {
                if (id > max) max = id;
            }
            nextCityId = max + 1;
        }
        return nextCityId++;
    }
}