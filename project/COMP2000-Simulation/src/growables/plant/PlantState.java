package growables.plant;

import java.awt.Graphics;

/**
 * State role of the State pattern. Implementations hold NO per-plant data: the
 * context (Plant) is passed in, so one instance can be shared by every plant of a species.
 */
public interface PlantState {
    String getName();
    boolean isDead();
    /** Called exactly once when a plant enters this state (size, fighting power). */
    void onEnter(Plant plant);
    /** Called every tick; may call plant.transitionTo(...). */
    void checkChange(Plant plant, long lifespan);
    /** Pure drawing - must not change the plant's bounds or any simulation state. */
    void paint(Plant plant, Graphics g);
}
