package growables.plant.flower.sunflower;

import exceptions.OutOfPatchBoundsException;
import growables.plant.LifeStage;
import growables.plant.Plant;
import growables.plant.flower.Flower;
import java.awt.Color;
import java.awt.Point;
import java.util.stream.Stream;
import java.util.Optional;
import placed_objects.Ground;
import placed_objects.Patch;
import placed_objects.sky.Sky;
import placed_objects.sky.SunnyState;
import supplementary.Radius;

/**
 * Represents a Sunflower that relies on sunny weather to bloom and spread.
 */
public class Sunflower extends Flower {
    public static final int SIZE = 50;
    private static final int SPREAD_RADIUS = 100;
    private static final int BLOOM_SPREAD_ODDS = 150;   // 1-in-150 per tick while blooming
    private static final int PATCH_RADIUS = 100;
    private static final int NEW_PATCH_ODDS = 10;

    private static final Color BROWN = new Color(79, 46, 9);
    private static final Color LIGHT_GREEN = new Color(2, 184, 9);
    private static final Color DARK_GREEN = new Color(1, 71, 4);

    // ---- Lifecycle: shared, immutable stages. Lambdas replace the former
    // SunflowerSeed/Juvenile/Adult/Bloom state classes (and base Seed/Seedling/Dead).
    // Cross references are qualified (Sunflower.X) so that forward/cyclic references are legal.
    private static final LifeStage SEED = LifeStage.of("SEED", 2, 0.25, LifeStage.disc(BROWN),
            (p, life) -> life > p.getGrowthDelay() ? Sunflower.SEEDLING : null);

    private static final LifeStage SEEDLING = LifeStage.of("SEEDLING", 3, 0.25, LifeStage.disc(LIGHT_GREEN),
            (p, life) -> life > 2L * p.getGrowthDelay() ? Sunflower.JUVENILE : null);

    private static final LifeStage JUVENILE = LifeStage.of("JUVENILE", 4, 0.5, LifeStage.disc(LIGHT_GREEN),
            (p, life) -> life > 3L * p.getGrowthDelay() ? Sunflower.ADULT : null);

    private static final LifeStage ADULT = LifeStage.of("ADULT", 6, 1.0, LifeStage.disc(DARK_GREEN),
            (p, life) -> {
                if (life > p.getMaxLifespan()) return LifeStage.DEAD;
                return isSunny(p) ? Sunflower.BLOOM : null;
            });

    private static final LifeStage BLOOM = LifeStage.of("BLOOMING", 5, 1.0,
            (p, g) -> {
                g.setColor(Color.YELLOW);
                g.fillOval(0, 0, p.getWidth(), p.getHeight());
                g.setColor(BROWN);
                g.fillOval(p.getWidth() / 4, p.getHeight() / 4, p.getWidth() / 2, p.getHeight() / 2);
            },
            (p, life) -> {
                if (life > p.getMaxLifespan()) return LifeStage.DEAD;
                if (!isSunny(p)) return Sunflower.ADULT;
                if (Plant.roll(BLOOM_SPREAD_ODDS)) p.spread();
                return null;
            });

    private static boolean isSunny(Plant p) {
        return SunnyState.NAME.equals(p.getEnvironmentState());
    }

    private Patch<Sunflower> patch;

    public Sunflower(Point position, Sky sky, Ground ground) {
        super(position, SIZE, sky, ground, SEED);
    }

    public Patch<Sunflower> getPatch() { return patch; }
    public void setPatch(Patch<Sunflower> patch) { this.patch = patch; }

    @Override
    public void prepareForRemoval() {
        super.prepareForRemoval();   // unregister from the Sky
        if(patch != null) {
            patch.removeFromPatch(this);
        }
    }

    @Override
    public void increaseLifespan(double factor) {
        setBloomTime((int)(getBloomTime() * factor));
    }

    /** Factory Method: scatter seeds uniformly in a square around the parent. */
    @Override
    protected Stream<Sunflower> createOffspring() {
        Radius radius = new Radius(getPosition(), SPREAD_RADIUS);
        return Stream.generate(radius::getRandomPoint)
                .limit(getSpreadNum())
                .map(point -> spawn(() -> new Sunflower(point, getSky(), getGround())))
                .flatMap(Optional::stream);
    }

    /** Children join the parent's patch, or may found a new one. */
    @Override
    protected void onPlanted(Plant child) {
        if (child instanceof Sunflower sunflower) {
            if (patch != null) {
                sunflower.givePatch(patch);
            } else {
                sunflower.checkPatchCreate();
            }
        }
    }

    public void givePatch(Patch<Sunflower> p) {
        try {
            p.addToPatch(this);
            this.patch = p;
        } catch (OutOfPatchBoundsException e) {
            checkPatchCreate();   // outside the parent's patch: may found its own instead
        }
    }

    public void checkPatchCreate() {
        if(patch == null && Plant.roll(NEW_PATCH_ODDS)) {
            patch = new Patch<>(PATCH_RADIUS, getPosition());
            getGround().addPatch(patch);
            patch.addToPatch(this);   // the patch is centred on this flower, so it is always in bounds
        }
    }

    @Override
    public String toString() {
        return ("Pos: " + getPosition().x + ", " + getPosition().y);
    }
}
