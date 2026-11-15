package growables.plant;

import exceptions.InvalidPositionException;
import exceptions.ObjectLimitExceededException;
import growables.Growable;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.swing.JPanel;
import placed_objects.Ground;
import placed_objects.sky.RainState;
import placed_objects.sky.Sky;
import placed_objects.sky.SkyObserver;
import supplementary.Window;

/**
 * Represents a basic plant entity in the ecosystem.
 *
 * Patterns: State (lifecycle, see PlantState/LifeStage), Observer (SkyObserver),
 * Template Method (spread()) + Factory Method (createOffspring()).
 */
public abstract class Plant extends JPanel implements Growable, SkyObserver {
    public static final int MAX_SIZE = 150;
    private static final double RAIN_GROWTH_FACTOR = 0.5;   // growth delay multiplier while raining
    private static final int RAIN_SPREAD_MULTIPLIER = 2;

    private PlantState state;

    private final Instant startTime = Instant.now();
    private final int fullSize;
    private final Point position;
    private final Sky sky;
    private final Ground ground;
    private String environmentState;

    private int growthDelay = 5000;
    private int spreadNum = 1;
    private int fightingPower = 5;
    private boolean fighting = false;

    protected Plant(Point p, int size, Sky sky, Ground ground, PlantState initialState) {
        if (p == null) {
            throw new InvalidPositionException("Position is null");
        } else if (!Window.isOnGround(p)) {
            throw new InvalidPositionException("Position: " + p.x + ", " + p.y);
        }
        this.position = new Point(p);
        this.fullSize = Math.min(Math.max(size, 1), MAX_SIZE);
        this.sky = sky;
        this.ground = ground;
        this.environmentState = sky.getState();
        this.setOpaque(false);

        transitionTo(initialState);
        sky.registerObserver(this);   // last: a constructor that throws must not leak an observer
    }

    // ------------------------------------------------------------------ State pattern

    /**
     * Moves the plant to a new state and runs that state's entry action.
     * Public so that State implementations in other packages can trigger transitions.
     */
    public void transitionTo(PlantState next) {
        PlantState target = Objects.requireNonNull(next, "Target state cannot be null");
        if (target == state) {
            return;
        }
        this.state = target;
        target.onEnter(this);
    }

    /** Resizes (centred on position) to a fraction of this plant's full size. */
    void applyStageSize(double fraction) {
        int s = Math.max(1, (int) Math.round(fullSize * fraction));
        setBounds(position.x - s / 2, position.y - s / 2, s, s);
    }

    @Override
    public void tick() {
        long lifespan = Duration.between(startTime, Instant.now()).toMillis();
        state.checkChange(this, lifespan);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        state.paint(this, g);
    }

    // ------------------------------------------------------------------ Observer pattern

    /** SkyObserver callback: remember the current weather/time-of-day. */
    @Override
    public void update(String timeState, int hour) {
        this.environmentState = timeState;
    }

    public String getEnvironmentState() { return environmentState; }
    private boolean isRaining() { return RainState.NAME.equals(environmentState); }

    @Override
    public void prepareForRemoval() {
        sky.removeObserver(this);
    }

    // ------------------------------------------------------------------ Template Method + Factory Method

    /**
     * Template method. The skeleton (guards, planting, bookkeeping) is fixed here;
     * subclasses vary only canSpread(), createOffspring(), onPlanted() and onSpread().
     */
    @Override
    public final void spread() {
        if (!canSpread() || ground.isFull()) {
            return;
        }
        int planted = 0;
        for (Plant child : createOffspring().toList()) {
            if (plantInGround(child)) {
                planted++;
            }
        }
        if (planted > 0) {
            onSpread();
        }
    }

    /** Factory Method: each species decides where and what its offspring are. */
    protected abstract Stream<? extends Plant> createOffspring();

    protected boolean canSpread() { return true; }
    protected void onPlanted(Plant child) { }
    protected void onSpread() { }

    private boolean plantInGround(Plant child) {
        try {
            ground.addGrowable(child);
            onPlanted(child);
            return true;
        } catch (ObjectLimitExceededException e) {
            child.prepareForRemoval();   // never became part of the ecosystem: unregister it
            return false;
        }
    }

    /**
     * Builds an offspring, treating "landed outside the ground" as an expected,
     * non-fatal outcome (the seed is simply lost) rather than an error.
     */
    protected static <T extends Plant> Optional<T> spawn(Supplier<T> factory) {
        try {
            return Optional.of(factory.get());
        } catch (InvalidPositionException e) {
            return Optional.empty();
        }
    }

    /** True with probability 1/odds. */
    public static boolean roll(int odds) {
        return ThreadLocalRandom.current().nextInt(odds) == 0;
    }

    // ------------------------------------------------------------------ combat

    @Override
    public void fightAgainst(Growable opponent) {
        setFighting(true);
        opponent.setFighting(true);
        int outcome = getFightingPower() - opponent.getFightingPower();
        double victoryChance = 0.5 + 0.1 * outcome;
        if (Math.random() <= victoryChance) {
            opponent.loseFight();
            setFighting(false);
        } else {
            loseFight();
            opponent.setFighting(false);
        }
    }

    @Override
    public void loseFight() {
        transitionTo(LifeStage.DEAD);
    }

    // ------------------------------------------------------------------ accessors

    @Override public Point getPosition() { return new Point(position); }
    @Override public String getState() { return state.getName(); }
    @Override public boolean isDead() { return state.isDead(); }
    @Override public Rectangle getHitbox() { return this.getBounds(); }

    public Sky getSky() { return sky; }
    public Ground getGround() { return ground; }

    /** Effective growth delay: halves while it is raining. */
    public int getGrowthDelay() {
        return isRaining() ? (int) (growthDelay * RAIN_GROWTH_FACTOR) : growthDelay;
    }
    protected int getBaseGrowthDelay() { return growthDelay; }
    protected void setGrowthDelay(int delay) { this.growthDelay = delay; }

    /** Age (ms) at which a plant in its final living stage dies of old age. */
    public long getMaxLifespan() { return 4L * growthDelay; }

    public int getSpreadNum() { return isRaining() ? spreadNum * RAIN_SPREAD_MULTIPLIER : spreadNum; }

    @Override
    public void increaseSpreadNum(double factor) {
        this.spreadNum = Math.max(1, (int) Math.round(spreadNum * factor));
    }

    @Override
    public void increaseLifespan(double factor) {
        this.growthDelay = (int) (growthDelay * factor);
    }

    @Override public int getFightingPower() { return fightingPower; }
    public void setFightingPower(int power) { this.fightingPower = power; }
    @Override public boolean getFighting() { return fighting; }
    @Override public void setFighting(boolean fighting) { this.fighting = fighting; }

    @Override
    public String toString() { return "Plant at " + position.x + ", " + position.y; }
}
