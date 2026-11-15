package growables.plant.flower;

import growables.plant.*;
import java.awt.*;
import placed_objects.Ground;
import placed_objects.sky.Sky;


public abstract class Flower extends Plant {
    private int bloomTime = 6;

    protected Flower(Point p, int size, Sky sky, Ground ground, PlantState initialState) {
        super(p, size, sky, ground, initialState);
    }

    public int getBloomTime() { return bloomTime; }
    public void setBloomTime(int bloomTime) { this.bloomTime = bloomTime; }

    /** Flowers live for bloomTime growth periods (not the generic 4). */
    @Override
    public long getMaxLifespan() { return (long) getBaseGrowthDelay() * bloomTime; }
}
