# COMP2000 Worksheet 2 — Final Submission

**Student name:** Grace Bohra

**GitHub repo URL:** https://github.com/gcrae/COMP2000_Assignment.git

---

## 1. Design Patterns

Don't forget to submit a pdf file which shows a diagram of your pattern implemenation to supplement this text description.

**1.1.** Which design pattern(s) did you implement? Name them and state where each appears in your codebase.
I implemented five design patterns:

1. **State (plant lifecycle).** `PlantState` is the interface and `LifeStage` is its single implementation, both in `growables/plant/`. `Plant` is the context, and `Plant.transitionTo()` swaps the current state. The stages (SEED to ADULT, plus `LifeStage.DEAD`) are defined as static constants in `Sunflower.java` and `Weed.java`.
2. **State (sky).** `SkyState` is implemented by `SunnyState`, `RainState` and `NightState` in `placed_objects/sky/`. `Sky` is the context, and `Sky.transitionTo()` switches state.
3. **Observer.** `Sky` is the subject (`SkySubject`: `registerObserver`, `removeObserver`, `notifyObservers`). `Plant` implements `SkyObserver` and receives the weather in `Plant.update()`. Plants register in the `Plant` constructor and deregister in `Plant.prepareForRemoval()`.
4. **Template Method.** `Plant.spread()` is a `final` method that fixes the spreading sequence. Subclasses customise it through the hooks `canSpread()`, `onPlanted()` and `onSpread()`, which `Sunflower` and `Weed` override.
5. **Factory Method.** `Plant.createOffspring()` is abstract. `Sunflower.createOffspring()` and `Weed.createOffspring()` each create their own species' offspring.



**1.2.** For each pattern, paste a code snippet showing the key interaction between participants.
### 1. State: plant lifecycle
`Plant` (context) delegates to its current `PlantState`; `LifeStage` (concrete state) decides the next state via a lambda.

// Plant.java: context
public void transitionTo(PlantState next) {
    PlantState target = Objects.requireNonNull(next, "Target state cannot be null");
    if (target == state) return;
    this.state = target;
    target.onEnter(this);          // entry action: sets fighting power and size
}

@Override
public void tick() {
    long lifespan = Duration.between(startTime, Instant.now()).toMillis();
    state.checkChange(this, lifespan);   // delegate to the current state
}

// LifeStage.java: concrete state, driven by a Transition lambda
@Override
public void checkChange(Plant plant, long lifespan) {
    PlantState next = transition.next(plant, lifespan);
    if (next != null) plant.transitionTo(next);   // state triggers its own transition
}

// Sunflower.java: one stage, defined with lambdas instead of a class
private static final LifeStage ADULT = LifeStage.of("ADULT", 6, 1.0, LifeStage.disc(DARK_GREEN),
        (p, life) -> {
            if (life > p.getMaxLifespan()) return LifeStage.DEAD;
            return isSunny(p) ? Sunflower.BLOOM : null;
        });

### 2. State: sky
`Sky` (context) holds a `SkyState`; each state decides the next one.

// SunnyState.java
@Override
public void checkChange() {
    if (sky.getHour() >= Sky.HALF_DAY) {
        sky.transitionTo(sky.getNightState());
    } else if (sky.isRaining()) {
        sky.transitionTo(sky.getRainState());
    }
}

// Sky.java
void transitionTo(SkyState next) {
    this.state = next;
    notifyObservers();      // links the State change to the Observer pattern
}

### 3. Observer: weather to plants
Plants register on construction, receive the weather in `update()`, and deregister on removal.

// Sky.java: subject
@Override
public void notifyObservers() {
    String name = state.getName();
    observers.forEach(o -> o.update(name, hour));
}

// Plant.java: observer
@Override
public void update(String timeState, int hour) {
    this.environmentState = timeState;
}

public int getGrowthDelay() {            // behaviour reacts to the observed weather
    return isRaining() ? (int) (growthDelay * RAIN_GROWTH_FACTOR) : growthDelay;
}

// last line of the Plant constructor:
sky.registerObserver(this);

@Override
public void prepareForRemoval() {
    sky.removeObserver(this);            // prevents the observer leak
}

### 4. Template Method: spreading
The skeleton in `Plant` is `final`. `Weed` customises only the hooks.

// Plant.java: template method
@Override
public final void spread() {
    if (!canSpread() || ground.isFull()) return;
    int planted = 0;
    for (Plant child : createOffspring().toList()) {
        if (plantInGround(child)) planted++;
    }
    if (planted > 0) onSpread();
}

protected boolean canSpread() { return true; }   // hooks with default behaviour
protected void onPlanted(Plant child) { }
protected void onSpread() { }

// Weed.java: overrides hooks only, never the skeleton
@Override protected boolean canSpread() { return !spent; }
@Override protected void onSpread()     { spent = true; }

### 5. Factory Method: offspring creation
`Plant` declares the factory method; each species decides what it creates.

// Plant.java: creator
protected abstract Stream<? extends Plant> createOffspring();

// Sunflower.java: scatters seeds around the parent
@Override
protected Stream<Sunflower> createOffspring() {
    Radius radius = new Radius(getPosition(), SPREAD_RADIUS);
    return Stream.generate(radius::getRandomPoint)
            .limit(getSpreadNum())
            .map(point -> spawn(() -> new Sunflower(point, getSky(), getGround())))
            .flatMap(Optional::stream);
}

// Weed.java: creeps along its direction, sometimes branching
@Override
protected Stream<Weed> createOffspring() {
    Point origin = getPosition();
    return IntStream.range(0, getSpreadNum()).boxed()
            .flatMap(i -> roll(BRANCH_ODDS) ? Stream.of(direction, randomDirection())
                                            : Stream.of(direction))
            .map(d -> spawn(() -> new Weed(stepFrom(origin, d), d, getSky(), getGround())))
            .flatMap(Optional::stream);
}



**1.3.** For each pattern, explain why you chose it over an alternative. What trade-off did you make?

For each pattern I considered at least one alternative. The trade-off I accepted is stated with each.

**State: plant lifecycle (`PlantState`, `LifeStage`)**
- *Chosen over:* One subclass per species and stage (my original design), and an enum with abstract methods.
- *Why:* The subclass approach grew with every new species (14 classes, and 9 state objects allocated per `Sunflower`). An enum could not carry per-species colours and timings without a constant for every species and stage. A single data-driven `LifeStage` configured with `Painter` and `Transition` lambdas keeps the lifecycle in one place, and one stateless instance is shared by every plant of a species.
- *Trade-off:* Behaviour now lives in lambdas rather than named classes, so it is harder to find and debug, and stack traces are less descriptive. Because the states are shared, they cannot hold per-plant data, so that data (growth delay, max lifespan, environment) has to be exposed through `Plant`. The stages in a species also refer to each other cyclically (ADULT to BLOOM and back), which forced qualified static references such as `Sunflower.BLOOM`.

**State: sky (`SkyState`)**
- *Chosen over:* A single `if/else` on the hour inside `Sky`.
- *Why:* Each weather state draws itself differently and decides its own next transition. Adding rain meant a new `RainState` and one extra branch in `SunnyState`, with `Sky`'s drawing and notification code unchanged.
- *Trade-off:* Three small classes for a very small state machine. An enum and a switch would be shorter, and I accepted extra classes in exchange for being able to add a state without editing the others.

**Observer: weather to plants (`SkySubject`, `SkyObserver`)**
- *Chosen over:* Each plant polling `sky.getState()` every tick.
- *Why:* The weather changes rarely but there are up to 512 plants, so pushing a change only when it happens avoids that per-tick work and keeps `Sky` unaware of plant classes.
- *Trade-off:* Observers must be deregistered. My original code did not do this, and I measured 129 registered observers for 70 live plants at 45 seconds. I fixed it by deregistering in `Plant.prepareForRemoval()` and by registering as the last constructor step. A second cost is that the weather is pushed as a `String`, which is not type-safe. An enum would be safer, but I kept the string to avoid changing every `update()` signature. New plants also have to read the current weather in their constructor, because they miss earlier notifications.

**Template Method: `Plant.spread()`**
- *Chosen over:* Strategy (a `SpreadStrategy` object per plant).
- *Why:* Offspring creation needs private subclass state (a weed's direction, a sunflower's patch), and a plant's species never changes at runtime, so there was no need to swap behaviour dynamically. A strategy object would have needed access to those private fields, breaking encapsulation. Template Method also removed duplicated try/catch code that had existed separately in both subclasses.
- *Trade-off:* It relies on inheritance, so the behaviour is fixed at compile time and the skeleton is rigid (it is `final`). It also widens the protected API with three hooks, and `Sunflower.onPlanted()` uses an `instanceof` check because the hook takes a `Plant`.

**Factory Method: `Plant.createOffspring()`**
- *Chosen over:* Scattered `new Sunflower(...)` / `new Weed(...)` calls inside each spread method, and an Abstract Factory.
- *Why:* The superclass can plant children without knowing their species. An Abstract Factory was rejected because there is only one product family, so it would add classes without solving a problem.
- *Trade-off:* Each new species needs its own subclass to override the method. Offspring are built lazily in a `Stream`, which composes cleanly but is a little less obvious to read than a loop. Falling off the map is handled by catching `InvalidPositionException` in `spawn()` and returning an empty `Optional`, which uses an exception for an expected case.




---

## 2. Streams / Lambdas / Parallelism

**2.1.** List every file in your project that uses lambda expressions or the Streams API.
Files using lambda expressions or the Streams API (8 files):

| File | Lambdas / method references | Streams API |
|---|---|---|
| `growables/plant/LifeStage.java` | `Painter` and `Transition` are `@FunctionalInterface`s. `disc()` and `square()` return lambdas, and `DEAD` is built with `(p, life) -> null` | |
| `growables/plant/flower/sunflower/Sunflower.java` | Five `LifeStage` definitions use `Transition`/`Painter` lambdas; `() -> new Sunflower(...)` (a `Supplier`); `radius::getRandomPoint`, `Optional::stream` | `Stream.generate().limit().map().flatMap()` in `createOffspring()` |
| `growables/plant/weed/Weed.java` | Four `LifeStage` lambdas; `() -> new Weed(...)` (a `Supplier`); `Optional::stream` | `IntStream.range().boxed().flatMap().map().flatMap()` in `createOffspring()` |
| `growables/plant/Plant.java` | `Supplier<T>` parameter of `spawn()` | `createOffspring()` returns `Stream<? extends Plant>`; `spread()` consumes it with `.toList()` |
| `placed_objects/Ground.java` | Lambdas in `resolveFights()` and `removeDead()`; method references `Growable::tick`, `Growable::getHitbox`, `Growable::isDead`, `Patch::isEmpty`, `Collision::resolve`, `this::delete` | Parallel `IntStream` collision detection, `list.stream()` / `parallelStream()` for stats and cleanup |
| `growables/PopulationStats.java` | `PopulationStats::new`, `::accept`, `::merge`, `Integer::sum`; `Map.forEach` lambdas | Custom `Collector.of(...)` |
| `placed_objects/sky/Sky.java` | `observers.forEach(o -> o.update(name, hour))` | |
| `supplementary/Window.java` | `javax.swing.Timer` callback `e -> { ground.tick(); sky.tick(); }` | |
| `App.java` | `SwingUtilities.invokeLater(App::start)` (method reference) | |




**2.2.** For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

The most sophisticated use is the collision detection in `Ground.resolveFights()` (`placed_objects/Ground.java`).
private record Collision(Growable a, Growable b) {
    void resolve() {
        if (!a.isDead() && !b.isDead() && !a.getFighting() && !b.getFighting()) {
            a.fightAgainst(b);
        }
    }
}

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

***What it does.***
Each tick it finds every pair of overlapping plants and then resolves each pair as a fight.
1. It copies each plant's hitbox into an immutable `List<Rectangle>` (`getHitbox()` returns `getBounds()`, which creates a new `Rectangle`).
2. For every index `i`, a nested `IntStream` tests the later indices `j > i` against `i`, so each pair is checked once, and turns the overlaps into `Collision` records.
3. `flatMap` merges those per-`i` results into one `List<Collision>`. Only this detection phase can run in parallel.
4. The fights are then resolved one at a time in the original order.

***Why a stream was better than a loop.***
- **Parallelism is one line.** The equivalent nested `for` loops would need manual thread partitioning and a thread-safe results collection. Here, switching to `.parallel()` is a single call, because each pair test is an independent, side-effect-free function of the snapshot.
- **Thread safety is by construction, not by locking.** The parallel phase only reads `boxes` and `live`, and both are effectively final and not modified during the stream. The mutating work (fights, `tick()`, adding and removing Swing components) is kept in the sequential phase on the Swing event-dispatch thread (EDT), where it has to run anyway.
- **Deterministic results.** `toList()` on an ordered stream preserves encounter order even when parallel, so collisions are resolved in the same order every run regardless of thread timing. I checked this with a benchmark harness that compared the sequential and parallel paths on random hitboxes. They returned identical collision sets at every size tested (128 to 8192 entities).
- **It replaced a worse design.** The original `Plant.tick()` called `ground.getGrowables()` for every plant, copying the whole list each time (up to 512 × 512 element copies per tick), and let dead plants keep fighting.



**2.3.** Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

Yes, I used parallel streams and a custom collector. I did not use the Fork/Join framework directly, although parallel streams run on the common `ForkJoinPool` underneath.

***Where they are used (`placed_objects/Ground.java`, `growables/PopulationStats.java`)***
- **Parallel stream:** `Ground.resolveFights()` tests every pair of plant hitboxes for overlap with a parallel `IntStream`.
- **Parallel stream + custom collector:** `Ground.tick()` builds the on-screen statistics with `streamOf(getGrowables()).collect(PopulationStats.collector())`, where `PopulationStats.collector()` is a custom `Collector` made with `Collector.of(...)`.
- Both are parallel only when the population is at least `PARALLEL_THRESHOLD` (256). Below that, they run sequentially, because the fork/join overhead outweighs the benefit.

***How I ensured thread safety***
1. **Parallel work is read-only and works on immutable data.** The pair tests run on a `List<Rectangle>` snapshot taken before the stream starts (`getHitbox()` returns a new `Rectangle`), and the population list comes from `List.copyOf()`. The lambdas have no side effects and write to no shared state.
2. **All mutation stays sequential, on the Swing event-dispatch thread.** Fights, `tick()`, and adding or removing Swing components happen after the parallel phase finishes. `Window` drives the simulation with a `javax.swing.Timer`, so ticks run on the EDT. The original used `java.util.Timer`, which mutated Swing components from a background thread, and that was a real race that I fixed.
3. **No locks are needed in the collector.** `Collector.of(supplier, accumulator, combiner)` gives each worker thread its own `PopulationStats`, and the results are combined in `merge()`. No container is shared between threads while accumulating.
4. **The result is deterministic.** `toList()` on an ordered stream keeps encounter order, so fights are resolved in the same order regardless of thread timing.
5. **Testing.** A benchmark harness compared the sequential and parallel collision pipelines on random hitboxes from 128 to 8192 entities, and they returned identical collision sets every time.



---

## 3. Uniqueness and Creativity

**3.1.** What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.
My Week 7 version already had a day/night cycle (`SunnyState` and `NightState` in the `Sky` State machine), sunflowers that bloom only when it is sunny, and State and Observer patterns for the plant lifecycle and the sky. After Week 7 I added the following.

***Rain weather system***
- What it does: `Sky` rolls a daily 60% chance of a 3 to 6 second daytime shower (`Sky.scheduleRain()`), handled by a new `RainState` that extends the existing sky State machine (`SunnyState` now transitions to it). Plants are told about the change through the existing Observer pattern.
- Effect on the plants: rain halves a plant's growth delay (`Plant.getGrowthDelay()`) so plants mature faster, and doubles a weed's spread slots (`Plant.getSpreadNum()`). Sunflowers only bloom and seed when it is sunny, so rain interrupts their reproduction.
- Why it matters: in Week 7 the weather (day or night) had little effect on how the species competed. Now it does. Weeds gain an advantage in rain and sunflowers gain one in sun, so which species dominates depends on how the weather happens to fall, plus fights between overlapping plants. Nothing about the outcome is scripted.
- Observed in my runs: rain transitions appear in the logs, and sunflowers reproduced from 2 to 31 in a 100 second run. I have not tested whether this is statistically different from the Week 7 behaviour, and populations varied noticeably between runs.
- Inspiration: The weather in Sydney was rapidly changing with sunny one day and rain the other. It had a noticeable effect on the plants near me. So, I decided to implement it in my simulation.



**3.2.** Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

The design decision that produces emergent behaviour is that no rule says which species wins. The outcome comes from three independent rules interacting through shared weather.


// Plant.java: rain accelerates growth and, for weeds, multiplies spread
public int getGrowthDelay() {
    return isRaining() ? (int) (growthDelay * RAIN_GROWTH_FACTOR) : growthDelay;
}
public int getSpreadNum() {
    return isRaining() ? spreadNum * RAIN_SPREAD_MULTIPLIER : spreadNum;
}

// Sunflower.java: a sunflower may only bloom and seed while it is sunny
(p, life) -> {
    if (life > p.getMaxLifespan()) return LifeStage.DEAD;
    if (!isSunny(p)) return Sunflower.ADULT;          // rain interrupts blooming
    if (Plant.roll(BLOOM_SPREAD_ODDS)) p.spread();    // 1-in-150 per tick
    return null;
}

// Plant.java: overlapping plants fight; the odds depend on each one's current stage
double victoryChance = 0.5 + 0.1 * (getFightingPower() - opponent.getFightingPower());


Each rule is simple and local, and none refers to the others.
- `Sky` randomly schedules showers (60% chance per day) and pushes the weather to every plant through the Observer pattern.
- Each plant reacts to the weather in its own way: weeds grow and spread faster in rain, and sunflowers only reproduce in sun.
- Plants that overlap fight, and the odds depend on the stage each one is in, which is itself affected by the weather.

Nothing in the code says "weeds dominate in wet weather" or "sunflowers recover after rain". Whatever population pattern appears is a result of how the showers happened to fall relative to each plant's lifecycle and where the plants happened to land. Starting from the identical scene (two sunflowers and one weed), separate runs of the same code ended up very differently. In one run sunflowers and weeds finished at comparable levels, and in another weeds heavily outnumbered sunflowers. Weeds spread outward in straight lines that occasionally branch, which produces the tendril patterns visible on screen. The branching rule is in `Weed.createOffspring()`, but the shapes that result depend on where seeds land and which plants survive fights.



**3.3.** If you had another 6 weeks, what would you add or change?

With more time, I would add a third species that closes the ecological loop. 

- Idea: a fungus that grows from dead plants and prefers wet weather, which would give dead weeds and sunflowers a role instead of simply disappearing. This was hinted at in my Week 7 code (an unused `Fungus` stub with a comment about overcast days).
- How it would fit: a new `Plant` subclass using the existing pieces (`LifeStage` definitions, `createOffspring()`), plus an event when a plant dies. That event would be a second use of the Observer pattern.



---

## 4. Log Book and Final Reflection

**4.1.** Attach or link your log book entries for Weeks 7–13.

**4.2.** Reflect on the semester:

- What was the most important concept you learned?
The most important concept I learned is that a design pattern is a response to a specific problem not a feature to add. My Week 7 code already had State and Observer but auditing it showed they were poorly applied: the State pattern needed a new class for every species and stage and the Observer never removed dead plants. So they stayed registered with the sky. Fixing this taught me to ask what actually varies and what the pattern costs to maintain which led me to replace the per-stage classes with one lambda-driven state and to choose Template Method and Factory Method over Strategy for spreading. Rejecting an alternative for a clear reason taught me more than adding the pattern itself.

- What design trade-off did you face, and how did you resolve it?
The main trade-off was between readable named classes and a compact, data-driven design for the plant lifecycle. My Week 7 code had one State class per species and stage which was easy to find and debug but meant every new species added more classes with copied code. I replaced them with a single `LifeStage` class configured by lambdas so a species is now a few lines of definitions and one stateless instance is shared by every plant of that species. The cost is that behaviour now lives in lambdas which are less discoverable and give less descriptive stack traces and the shared states cannot hold per-plant data so that data had to be exposed through `Plant`. I accepted this because growth in the number of species was the real problem and I reduced the readability cost by giving each stage a name and defining all of a species' stages together in one place.

- If you started over, what would you do differently?
If I started over, I would separate the simulation model from the Swing view from the beginning. In my project every entity extends `JPanel` so a plant's hitbox is tied to its Swing bounds and logic is tangled with rendering which is why I had to move size changes out of the paint code and why the simulation can't run or be tested without a window. I would make plants plain objects with a position and size and draw them all in one view class. I would also decide which thread is allowed to change the simulation before writing any code because my original timer mutated Swing components from a background thread and I only found the race during the audit. Finally, I would write small automated tests from the start so that refactors such as replacing the per-stage state classes with a single lambda-driven one could be checked repeatably and not with throwaway harnesses.

- What feedback from the Week 7 submission did you act on?
The only critical point in my Week 7 feedback was that my final history did not retain a separate personal branch or pull request. I did not act on this for Week 13, I continued committing directly to my fork's main branch. This was due to time constraints and the fact that I worked alone on the final changes. The rest of the feedback was positive, and I built on those areas instead such as collision handling and reproduction, exceptions and validations.
