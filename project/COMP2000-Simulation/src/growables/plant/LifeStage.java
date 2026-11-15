package growables.plant;

import java.awt.Color;
import java.awt.Graphics;

/**
 * A single, data-driven PlantState. Behaviour that used to need one class per
 * (species x stage) is supplied as two lambdas: a Painter and a Transition.
 * Instances are immutable and shared between all plants of a species.
 */
public final class LifeStage implements PlantState {

    @FunctionalInterface
    public interface Painter {
        void paint(Plant plant, Graphics g);
    }

    /** Returns the stage to move to, or null to stay in the current stage. */
    @FunctionalInterface
    public interface Transition {
        PlantState next(Plant plant, long lifespan);
    }

    public static final String DEAD_NAME = "DEAD";
    private static final double KEEP_SIZE = -1;

    /** Terminal stage shared by every species. */
    public static final LifeStage DEAD =
            new LifeStage(DEAD_NAME, 0, KEEP_SIZE, square(Color.BLACK), (p, life) -> null, true);

    private final String name;
    private final int fightingPower;
    private final double sizeFraction;
    private final Painter painter;
    private final Transition transition;
    private final boolean dead;

    private LifeStage(String name, int fightingPower, double sizeFraction,
                      Painter painter, Transition transition, boolean dead) {
        this.name = name;
        this.fightingPower = fightingPower;
        this.sizeFraction = sizeFraction;
        this.painter = painter;
        this.transition = transition;
        this.dead = dead;
    }

    public static LifeStage of(String name, int fightingPower, double sizeFraction,
                               Painter painter, Transition transition) {
        return new LifeStage(name, fightingPower, sizeFraction, painter, transition, false);
    }

    public static Painter disc(Color colour) {
        return (plant, g) -> {
            g.setColor(colour);
            g.fillOval(0, 0, plant.getWidth(), plant.getHeight());
        };
    }

    public static Painter square(Color colour) {
        return (plant, g) -> {
            g.setColor(colour);
            g.fillRect(0, 0, plant.getWidth(), plant.getHeight());
        };
    }

    @Override public String getName() { return name; }
    @Override public boolean isDead() { return dead; }

    @Override
    public void onEnter(Plant plant) {
        plant.setFightingPower(fightingPower);
        if (sizeFraction != KEEP_SIZE) {
            plant.applyStageSize(sizeFraction);
        }
    }

    @Override
    public void checkChange(Plant plant, long lifespan) {
        PlantState next = transition.next(plant, lifespan);
        if (next != null) {
            plant.transitionTo(next);
        }
    }

    @Override
    public void paint(Plant plant, Graphics g) {
        painter.paint(plant, g);
    }
}
