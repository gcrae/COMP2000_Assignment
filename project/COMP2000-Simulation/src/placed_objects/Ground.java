package placed_objects;
import exceptions.ObjectLimitExceededException;
import growables.Growable;
import growables.PopulationStats;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.JPanel;
import supplementary.Window;

/**
 * The ground owns every Growable and Patch. All mutation happens on the Swing
 * event-dispatch thread (Window drives tick() from a javax.swing.Timer); only the
 * read-only analysis phases (collision detection, statistics) run in parallel.
 */
public class Ground extends JPanel{
    private static final int MAX_OBJECTS = 512;
    /** Below this population the fork/join overhead outweighs the benefit. */
    private static final int PARALLEL_THRESHOLD = 256;

    private final List<Growable> growables;
    private final List<Patch<? extends Growable>> patches;
    private PopulationStats stats = new PopulationStats();

    public Ground() {
        this.setPreferredSize(new Dimension(Window.WIN_WIDTH, Window.GROUND_HEIGHT));
        this.setBackground(Color.green);
        this.setLayout(null);   //Freeform layout

        growables = new ArrayList<>();
        patches = new ArrayList<>();
    }

    /** One simulation step: fights -> entity updates -> cleanup -> statistics -> repaint. */
    public void tick() {
        List<Growable> live = getGrowables();
        resolveFights(live);
        live.forEach(Growable::tick);
        removeDead();
        removeEmptyPatches();
        stats = streamOf(getGrowables()).collect(PopulationStats.collector());
        repaint();
    }

    /** A collision between two growables, found during the read-only phase. */
    private record Collision(Growable a, Growable b) {
        void resolve() {
            if (!a.isDead() && !b.isDead() && !a.getFighting() && !b.getFighting()) {
                a.fightAgainst(b);
            }
        }
    }

    /**
     * Phase 1 (parallel, read-only): test every pair on an immutable snapshot of hitboxes.
     * Phase 2 (sequential): resolve each collision, because fights mutate the entities.
     * encounter order is preserved by toList(), so the outcome does not depend on thread timing.
     */
    private void resolveFights(List<Growable> live) {
        List<Rectangle> boxes = live.stream().map(Growable::getHitbox).toList();
        IntStream indices = IntStream.range(0, boxes.size());
        if (boxes.size() >= PARALLEL_THRESHOLD) {
            indices = indices.parallel();
        }
        List<Collision> collisions = indices.boxed()
                .flatMap(i -> IntStream.range(i + 1, boxes.size())
                        .filter(j -> boxes.get(i).intersects(boxes.get(j)))
                        .mapToObj(j -> new Collision(live.get(i), live.get(j))))
                .toList();
        collisions.forEach(Collision::resolve);
    }

    private <T> Stream<T> streamOf(List<T> list) {
        return list.size() >= PARALLEL_THRESHOLD ? list.parallelStream() : list.stream();
    }

    private void removeDead() {
        getGrowables().stream()
                .filter(Growable::isDead)
                .forEach(g -> {
                    g.prepareForRemoval();
                    delete(g);
                });
    }

    private void removeEmptyPatches() {
        patches.stream().filter(Patch::isEmpty).toList().forEach(this::delete);
    }

    /** Compile-time guarantee that only things that are both drawable and growable are added. */
    public <T extends Component & Growable> void addGrowable(T g) throws ObjectLimitExceededException {
        if(isFull()) {
            throw new ObjectLimitExceededException();
        }
        growables.add(g);
        add(g);
    }

    public void addPatch(Patch<? extends Growable> p) {
        patches.add(p);
        add(p);
    }

    public void delete(Growable g) {
        growables.remove(g);
        remove((Component) g);   // safe: addGrowable only accepts Components
    }

    public void delete(Patch<? extends Growable> p) {
        patches.remove(p);
        remove(p);
    }

    public boolean isFull() {
        return growables.size() >= MAX_OBJECTS;
    }

    /** Defensive, immutable snapshot of the current population. */
    public List<Growable> getGrowables() {
        return List.copyOf(growables);
    }

    public PopulationStats getStats() {
        return stats;
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        paintHud(g);
    }

    private void paintHud(Graphics g) {
        List<String> lines = stats.describe();
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(8, 8, 170, 12 + 16 * lines.size());
        g.setColor(Color.WHITE);
        int y = 24;
        for (String line : lines) {
            g.drawString(line, 14, y);
            y += 16;
        }
    }
}
