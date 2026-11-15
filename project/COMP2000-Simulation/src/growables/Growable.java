package growables;
import java.awt.*;

public interface Growable {
    public void spread();     //Different things spread in different ways, e.g. wind, bees, spores
    public Point getPosition();
    public String getState();
    public boolean isDead();
    public void increaseSpreadNum(double factor);
    public void increaseLifespan(double factor);
    public void tick();
    public Rectangle getHitbox();
    public void fightAgainst(Growable g);
    public int getFightingPower();
    public void loseFight();
    public void setFighting(boolean state);
    public boolean getFighting();
    public void prepareForRemoval();

    /** Species label used for statistics; defaults to the concrete class name. */
    default String getSpecies() { return getClass().getSimpleName(); }
}
