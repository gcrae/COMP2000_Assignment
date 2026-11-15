package growables.plant.weed;

import growables.plant.LifeStage;
import growables.plant.Plant;
import java.awt.Color;
import java.awt.Point;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import placed_objects.Ground;
import placed_objects.sky.Sky;
import supplementary.Direction;

/**
 * Represents an aggressive weed that creeps outwards along a direction.
 */
public class Weed extends Plant {
    private static final int GROWTH_DELAY = 1000;
    private static final int WEED_SIZE = 10;
    private static final long MIN_LIFESPAN_MS = 20000;
    private static final int LIFESPAN_JITTER_S = 20;
    private static final int BRANCH_ODDS = 5;           // 1-in-5 chance of a branching child
    private static final int MAX_DIRECTION = 20;
    private static final int STEP_MIN = 15;
    private static final int STEP_MAX = 20;

    private static final Color BROWN = new Color(79, 46, 9);
    private static final Color LIGHT_GREEN = new Color(2, 184, 9);
    private static final Color GREEN = new Color(1, 120, 5);

    // Shared, immutable lifecycle (see Sunflower for the pattern).
    private static final LifeStage SEED = LifeStage.of("SEED", 2, 1.0, LifeStage.square(BROWN),
            (p, life) -> life > p.getGrowthDelay() ? Weed.SEEDLING : null);

    private static final LifeStage SEEDLING = LifeStage.of("SEEDLING", 3, 1.0, LifeStage.square(LIGHT_GREEN),
            (p, life) -> life > 2L * p.getGrowthDelay() ? Weed.JUVENILE : null);

    private static final LifeStage JUVENILE = LifeStage.of("JUVENILE", 4, 1.0, LifeStage.square(GREEN),
            (p, life) -> life > 3L * p.getGrowthDelay() ? Weed.ADULT : null);

    private static final LifeStage ADULT = LifeStage.of("ADULT", 5, 1.0, LifeStage.square(GREEN),
            (p, life) -> {
                if (life > p.getMaxLifespan()) return LifeStage.DEAD;
                p.spread();   // no-op once the weed is spent
                return null;
            });

    private final Direction direction;
    private final long maxLifespan;   // rolled ONCE per weed (was re-rolled every tick)
    private boolean spent = false;

    public Weed(Point position, Sky sky, Ground ground) {
        this(position, randomDirection(), sky, ground);
    }

    private Weed(Point position, Direction direction, Sky sky, Ground ground) {
        super(position, WEED_SIZE, sky, ground, SEED);
        setGrowthDelay(GROWTH_DELAY);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.direction = direction;
        this.maxLifespan = MIN_LIFESPAN_MS + 1000L * ThreadLocalRandom.current().nextInt(LIFESPAN_JITTER_S);
    }

    private static Direction randomDirection() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new Direction(r.nextInt(-MAX_DIRECTION, MAX_DIRECTION), r.nextInt(-MAX_DIRECTION, MAX_DIRECTION));
    }

    private static Point stepFrom(Point origin, Direction d) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new Point((int) (origin.x + d.getDx() * r.nextInt(STEP_MIN, STEP_MAX)),
                         (int) (origin.y + d.getDy() * r.nextInt(STEP_MIN, STEP_MAX)));
    }

    @Override
    public long getMaxLifespan() { return maxLifespan; }

    @Override
    protected boolean canSpread() { return !spent; }

    @Override
    protected void onSpread() { spent = true; }

    /**
     * Factory Method: one child per spread slot along this weed's direction, each with a
     * chance of an extra branch heading in a new random direction. Rain doubles the slots.
     */
    @Override
    protected Stream<Weed> createOffspring() {
        Point origin = getPosition();
        return IntStream.range(0, getSpreadNum()).boxed()
                .flatMap(i -> roll(BRANCH_ODDS) ? Stream.of(direction, randomDirection()) : Stream.of(direction))
                .map(d -> spawn(() -> new Weed(stepFrom(origin, d), d, getSky(), getGround())))
                .flatMap(Optional::stream);
    }

    @Override
    public String toString() {
        return ("Pos: " + getPosition().x + ", " + getPosition().y);
    }
}
