package growables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;

/**
 * Mutable reduction container with a custom Collector. Each worker thread of a
 * parallel stream accumulates into its OWN instance; instances are only combined
 * through merge(), so no locking is needed.
 */
public final class PopulationStats {
    private final Map<String, Integer> bySpecies = new TreeMap<>();
    private final Map<String, Integer> byStage = new TreeMap<>();
    private int total;
    private long totalPower;

    public static Collector<Growable, PopulationStats, PopulationStats> collector() {
        return Collector.of(PopulationStats::new, PopulationStats::accept,
                PopulationStats::merge, Collector.Characteristics.UNORDERED);
    }

    private void accept(Growable g) {
        bySpecies.merge(g.getSpecies(), 1, Integer::sum);
        byStage.merge(g.getState(), 1, Integer::sum);
        total++;
        totalPower += g.getFightingPower();
    }

    private PopulationStats merge(PopulationStats other) {
        other.bySpecies.forEach((k, v) -> bySpecies.merge(k, v, Integer::sum));
        other.byStage.forEach((k, v) -> byStage.merge(k, v, Integer::sum));
        total += other.total;
        totalPower += other.totalPower;
        return this;
    }

    public int getTotal() { return total; }
    public int getSpeciesCount(String species) { return bySpecies.getOrDefault(species, 0); }
    public double getMeanFightingPower() { return total == 0 ? 0 : (double) totalPower / total; }

    /** Text lines for the on-screen overlay. */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        lines.add("Population: " + total);
        bySpecies.forEach((k, v) -> lines.add("  " + k + ": " + v));
        byStage.forEach((k, v) -> lines.add("  " + k.toLowerCase() + ": " + v));
        lines.add(String.format("Mean power: %.2f", getMeanFightingPower()));
        return lines;
    }
}
