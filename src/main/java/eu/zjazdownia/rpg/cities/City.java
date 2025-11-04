package eu.zjazdownia.rpg.cities;

import org.bukkit.Location;

public class City {
    private String name;
    private int ID;
    private Location location;
    private int radius;
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
