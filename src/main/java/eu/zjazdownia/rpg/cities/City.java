package eu.zjazdownia.rpg.cities;

import org.bukkit.Location;

/**
 * Model miasta: nazwa, ID, środek (Location), promień oraz poziom mobów (moblvl)
 * używany w obrębie miasta do spawnu i skalowania mobów.
 */
public class City {
    private String name;
    private int ID;
    /** Środek miasta (świat + współrzędne). */
    private Location location;
    /** Promień w blokach (okrąg w płaszczyźnie XZ). */
    private int radius;
    /** Poziom mobów spawnujących się w tym mieście. */
    private int moblvl;

    public City(String name, int ID, Location location, int radius, int moblvl) {
        this.name = name;
        this.ID = ID;
        this.location = location;
        this.radius = radius;
        this.moblvl = moblvl;
    }

    public int getID() {
        return ID;
    }
    public void setID(int ID) {
        this.ID = ID;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getMoblvl() {
        return moblvl;
    }


    public Location getLocation() {
        return location;
    }

    public int getRadius(){
        return radius;
    }

}
