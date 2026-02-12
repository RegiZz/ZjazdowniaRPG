package eu.zjazdownia.rpg.commands;

/**
 * Komendy administracyjne miast: /city create|delete|rename|showborder|list|info.
 * Zarządza listą miast (citiesById), zapisem/odczytem z cities.yml oraz wizualizacją
 * granic (showborder) i wykrywaniem miasta w danej lokacji (findCityAt, findNearestCity).
 */
import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.cities.City;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CityComands implements CommandExecutor, Listener {

    /** Mapa ID miasta -> obiekt City. */
    public final Map<Integer, City> citiesById = new HashMap<>();
    private Integer nextCityId = null;
    private final ZjazdowniaRPG plugin;
    /** Aktywne zadania pokazywania granic miasta (per gracz). */
    private final Map<UUID, BukkitTask> activeBorderTasks = new HashMap<>();
    /** Lokacje bloków granicy (do przywrócenia po wyłączeniu). */
    private final Map<UUID, List<Location>> activeBorderLocations = new HashMap<>();
    private final Map<UUID, List<BlockData>> activeBorderOriginalData = new HashMap<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();


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
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            p.sendMessage(ChatColor.BLUE + "Cities");
            p.sendMessage(ChatColor.YELLOW + "/city info - informacje o miastach");
            p.sendMessage(ChatColor.YELLOW + "/city create <nazwa> <promien> <moblvl> - tworzy nowe miasto");
            p.sendMessage(ChatColor.YELLOW + "/city delete <id> - usuwa miasto o podanym ID");
            p.sendMessage(ChatColor.YELLOW + "/city rename <nazwa> <nowa_nazwa> - zmienia nazwe miasta");
            p.sendMessage(ChatColor.YELLOW + "/city showborder <nazwa> - pokazuje granice miasta");
            p.sendMessage(ChatColor.YELLOW + "/city list - wyświetla liste miast");
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
                p.sendMessage(ChatColor.GOLD + "Lvl mob: " + ChatColor.AQUA + at.getMoblvl());
                break;
            }
            case "create": {
                if (args.length < 4) {
                    p.sendMessage(ChatColor.RED + "Użycie: /" + label + " create <nazwa> <promień> <mobLvl>");
                    break;
                }
                String cityName = args[1];
                int cityRadius;
                int mobLvl;
                try {
                    cityRadius = Integer.parseInt(args[2]);
                    mobLvl =  Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Muszą być liczby całkowite w promieniu i lvl mobów");
                    break;
                }
                int id = nextId();
                Location cityLocation = new Location(playerLocation.getWorld(), playerLocation.getX(), playerLocation.getY(), playerLocation.getZ());
                City city = new City(cityName, id, cityLocation, cityRadius, mobLvl);
                citiesById.put(id, city);
                p.sendMessage(ChatColor.GREEN + "Utworzono miasto '" + cityName + "' z ID=" + id + " i promieniem " + cityRadius + ".");
                p.sendMessage(ChatColor.GRAY + "Level mobów ustawiony na " + ChatColor.GOLD + mobLvl);
                saveCities();
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
                saveCities();
                break;
            }
            case "rename": {
                if(args.length == 3) {
                    String name = args[1];
                    String newName = args[2];
                    for(City city : citiesById.values()) {
                        if(city.getName().equals(name)){
                            p.sendMessage(ChatColor.GREEN + "Zmieniono nazwe miasta "+ city.getName()+ " na '" + newName + "'.");
                            city.setName(newName);
                        }
                    }
                    saveCities();
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
                        activeBorderTasks.get(uuid).cancel();
                        activeBorderTasks.remove(uuid);

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
                        List<Location> borderLocations = computeBorderLocations(targetCity);
                        List<BlockData> originalData = new ArrayList<>(borderLocations.size());
                        for (Location loc : borderLocations) {
                            Block b = loc.getBlock();
                            originalData.add(b.getBlockData());
                        }

                        activeBorderLocations.put(uuid, borderLocations);
                        activeBorderOriginalData.put(uuid, originalData);

                        long intervalTicks = Math.max(1, plugin.getConfig().getLong("cities.showborder-interval-ticks", 10));
                        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                            showCityBorderBlocks(p, borderLocations);
                        }, 0L, intervalTicks);

                        activeBorderTasks.put(uuid, task);
                        p.sendMessage(ChatColor.GREEN + "Pokazano granice miasta '" + targetCity.getName() + "'. Ponownie wpisz komendę, aby wyłączyć.");
                    }

                } else {
                    p.sendMessage(ChatColor.RED + "Poprawne użycie: /" + label + " showborder <nazwa_miasta>");
                }
                break;
            }

            case "list": {
                Component separator = mm.deserialize("<gradient:#ff00ff:#00ffff>============================</gradient>");
                Component cityListMessage = mm.deserialize("<gradient:#ff00ff:#00ffff>=========Lista Miast==========</gradient>");
                p.sendMessage(cityListMessage);
                for(City c : citiesById.values()){
                    Location l = c.getLocation();
                    p.sendMessage(ChatColor.GOLD + "Miasto: " + ChatColor.AQUA + c.getName());
                    p.sendMessage(ChatColor.GOLD + "Środek: " + ChatColor.AQUA
                            + l.getWorld().getName() + " (" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ")");
                    p.sendMessage(ChatColor.GOLD + "Promien: " +  ChatColor.AQUA + c.getRadius());
                    p.sendMessage(separator);
                }
                break;
            }

            default:
                p.sendMessage(ChatColor.RED + "Nieznana komenda.");
        }
        return true;
    }

    /** Oblicza listę lokacji bloków na obwodzie miasta (najwyższy blok w danym XZ). */
    public List<Location> computeBorderLocations(City city) {
        Location loc = city.getLocation();
        int radius = city.getRadius();
        World world = loc.getWorld();

        int points = (int) Math.ceil(2 * Math.PI * radius);

        List<Location> list = new ArrayList<>(points);
        Set<Block> addedBlocks = new HashSet<>();

        for (int i = 0; i < points; i++) {
            double angle = i * 2 * Math.PI / points;
            double x = loc.getX() + radius * Math.cos(angle);
            double z = loc.getZ() + radius * Math.sin(angle);

            int bx = (int) Math.round(x);
            int bz = (int) Math.round(z);

            Block highest = world.getHighestBlockAt(bx, bz);
            if (addedBlocks.add(highest)) {
                list.add(highest.getLocation());
            }
        }

        return list;
    }

    /** Wysyła graczowi fałszywe bloki (LIGHT_BLUE_GLASS) w miejscach granicy. */
    public void showCityBorderBlocks(Player p, List<Location> borderLocations) {
        BlockData fake = Material.LIGHT_BLUE_STAINED_GLASS.createBlockData();
        for (Location borderLoc : borderLocations) {
            p.sendBlockChange(borderLoc, fake);
        }
    }


    /** Zwraca miasto, w którego obrębie leży lokacja (najbliższy środek); null jeśli brak. */
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


    // zapis miast do pliku cities.yml
    public void saveCities() {
        File file = new File(plugin.getDataFolder(), "cities.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        cfg.set("cities", null); // wyczyść stare dane

        for (City city : citiesById.values()) {
            String path = "cities." + city.getID();
            cfg.set(path + ".name", city.getName());
            cfg.set(path + ".world", city.getLocation().getWorld().getName());
            cfg.set(path + ".x", city.getLocation().getX());
            cfg.set(path + ".y", city.getLocation().getY());
            cfg.set(path + ".z", city.getLocation().getZ());
            cfg.set(path + ".radius", city.getRadius());
            cfg.set(path + ".moblvl", city.getMoblvl());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać cities.yml: " + e.getMessage());
        }
    }

    // wczytywanie miast z pliku cities.yml
    public void loadCities() {
        File file = new File(plugin.getDataFolder(), "cities.yml");
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        citiesById.clear();

        if (!cfg.isConfigurationSection("cities")) return;

        for (String key : cfg.getConfigurationSection("cities").getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String name = cfg.getString("cities." + key + ".name", "Unknown");
                String worldName = cfg.getString("cities." + key + ".world", "world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                double x = cfg.getDouble("cities." + key + ".x");
                double y = cfg.getDouble("cities." + key + ".y");
                double z = cfg.getDouble("cities." + key + ".z");
                int radius = cfg.getInt("cities." + key + ".radius");
                int moblvl = cfg.getInt("cities." + key + ".moblvl");

                City city = new City(name, id, new Location(world, x, y, z), radius, moblvl);
                citiesById.put(id, city);
            } catch (Exception ex) {
                plugin.getLogger().warning("Błąd przy wczytywaniu miasta " + key + ": " + ex.getMessage());
            }
        }

        plugin.getLogger().info("Wczytano " + citiesById.size() + " miast z cities.yml");
    }

    /** Zwraca najbliższe miasto do danej lokacji (bez warunku bycia w środku). */
    public City findNearestCity(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        City nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (City city : citiesById.values()) {
            Location center = city.getLocation();
            if (center == null || center.getWorld() == null) continue;
            if (!center.getWorld().equals(loc.getWorld())) continue;

            double dx = loc.getX() - center.getX();
            double dz = loc.getZ() - center.getZ();
            double distance = Math.hypot(dx, dz);

            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = city;
            }
        }

        return nearest;
    }


}