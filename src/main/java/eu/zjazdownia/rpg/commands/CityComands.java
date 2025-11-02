package eu.zjazdownia.rpg.commands;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.cities.City;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CityComands implements CommandExecutor {

    private final Map<Integer, City> citiesById = new HashMap<>();
    private Integer nextCityId = null;
    private final Map<Player, BukkitTask> activeBorders = new HashMap<>();
    private final ZjazdowniaRPG plugin;
    private final Map<UUID, BukkitTask> activeBorderTasks = new HashMap<>();
    private final Map<UUID, List<Location>> activeBorderLocations = new HashMap<>();
    private final Map<UUID, List<BlockData>> activeBorderOriginalData = new HashMap<>();


    public CityComands(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
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
            p.sendMessage(ChatColor.YELLOW + "/city rename <nazwa> <nowa_nazwa> - zmienia nazwe miasta");
            p.sendMessage(ChatColor.YELLOW + "/city showBorder <nazwa> - pokazuje granice miasta");
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
            case "rename": {
                if(args.length == 3) {
                    String name = args[1];
                    String newName = args[2];
                    for(City city : citiesById.values()) {
                        if(city.getName().equals(name)){
                            city.setName(newName);
                        }
                    }
                }
                else{
                    p.sendMessage(ChatColor.RED + "Poprawne uzycie /" + label + " rename <nazwa_przed> <nazwa_po>");
                }
                break;
            }

            case "showborder": {
                if (args.length == 2) {
                    String cityName = args[1];
                    City targetCity = citiesById.values().stream()
                            .filter(c -> c.getName().equalsIgnoreCase(cityName))
                            .findFirst().orElse(null);

                    if (targetCity == null) {
                        p.sendMessage(ChatColor.RED + "Nie znaleziono miasta o nazwie '" + cityName + "'.");
                        return true;
                    }

                    UUID uuid = p.getUniqueId();

                    // Jeśli już pokazujemy granice — wyłącz i przywróć bloki
                    if (activeBorderTasks.containsKey(uuid)) {
                        // cancel task
                        activeBorderTasks.get(uuid).cancel();
                        activeBorderTasks.remove(uuid);

                        // przywróć oryginalne bloki
                        List<Location> locs = activeBorderLocations.remove(uuid);
                        List<BlockData> orig = activeBorderOriginalData.remove(uuid);
                        if (locs != null && orig != null) {
                            for (int i = 0; i < locs.size(); i++) {
                                Location loc = locs.get(i);
                                BlockData bd = orig.get(i);
                                p.sendBlockChange(loc, bd);
                            }
                        }

                        p.sendMessage(ChatColor.YELLOW + "Wyłączono podgląd granic miasta '" + targetCity.getName() + "'.");
                    } else {
                        // oblicz punkty granicy i zapamiętaj oryginalne blockdata
                        List<Location> borderLocations = computeBorderLocations(targetCity);
                        List<BlockData> originalData = new ArrayList<>(borderLocations.size());
                        for (Location loc : borderLocations) {
                            Block b = loc.getBlock();
                            originalData.add(b.getBlockData());
                        }

                        activeBorderLocations.put(uuid, borderLocations);
                        activeBorderOriginalData.put(uuid, originalData);

                        // uruchom task, który co tick rysuje (wysyła) bloki
                        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                            showCityBorderBlocks(p, borderLocations);
                        }, 0L, 10L); // co 10 ticków

                        activeBorderTasks.put(uuid, task);
                        p.sendMessage(ChatColor.GREEN + "Pokazano granice miasta '" + targetCity.getName() + "'. Ponownie wpisz komendę, aby wyłączyć.");
                    }

                } else {
                    p.sendMessage(ChatColor.RED + "Poprawne użycie: /" + label + " showborder <nazwa_miasta>");
                }
                break;
            }

            default:
                p.sendMessage(ChatColor.RED + "Nieznana komenda.");
        }
        return true;
    }

    public List<Location> computeBorderLocations(City city) {
        Location loc = city.getLocation();
        int radius = city.getRadius();
        World world = loc.getWorld();
        int points = 120;
        List<Location> list = new ArrayList<>(points);

        for (int i = 0; i < points; i++) {
            double angle = i * 2 * Math.PI / points;
            double x = loc.getX() + radius * Math.cos(angle);
            double z = loc.getZ() + radius * Math.sin(angle);
            Block highest = world.getHighestBlockAt((int) x, (int) z);
            Location borderLoc = highest.getLocation();
            list.add(borderLoc);
        }
        return list;
    }

    public void showCityBorderBlocks(Player p, List<Location> borderLocations) {
        BlockData fake = Material.LIGHT_BLUE_STAINED_GLASS.createBlockData();
        for (Location borderLoc : borderLocations) {
            p.sendBlockChange(borderLoc, fake);
        }
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

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (activeBorderTasks.containsKey(uuid)) {
            activeBorderTasks.get(uuid).cancel();
            activeBorderTasks.remove(uuid);

            List<Location> locs = activeBorderLocations.remove(uuid);
            List<BlockData> orig = activeBorderOriginalData.remove(uuid);
            if (locs != null && orig != null) {
                for (int i = 0; i < locs.size(); i++) {
                    p.sendBlockChange(locs.get(i), orig.get(i));
                }
            }
        }
    }

}