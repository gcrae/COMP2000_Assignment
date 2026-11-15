package placed_objects.sky;

import java.awt.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JPanel;
import supplementary.Window;


public class Sky extends JPanel implements SkySubject {
    public static final int DAY_LENGTH = 24000;   // ms for one full day/night cycle
    public static final int HALF_DAY = 12000;     // daylight lasts the first half
    private static final double RAIN_CHANCE = 0.6;

    private final SkyState sunnyState;
    private final SkyState rainState;
    private final SkyState nightState;
    private SkyState state;

    private final Instant startTime;
    private long currentDay = -1;
    private int hour;               // ms elapsed in the current day
    private int rainStart;          // today's shower window (ms into the day); start == end -> no rain
    private int rainEnd;

    private final List<SkyObserver> observers;

    public Sky() {
        this.sunnyState = new SunnyState(this);
        this.rainState = new RainState(this);
        this.nightState = new NightState(this);
        this.state = sunnyState;

        this.observers = new ArrayList<>();
        this.startTime = Instant.now();
        this.setPreferredSize(new Dimension(Window.WIN_WIDTH, Window.SKY_HEIGHT));
    }

    public void tick() {
        long elapsed = Duration.between(startTime, Instant.now()).toMillis();
        long day = elapsed / DAY_LENGTH;
        if (day != currentDay) {
            currentDay = day;
            scheduleRain();
        }
        hour = (int) (elapsed % DAY_LENGTH);
        state.checkChange();
        repaint();
    }

    /** Each new day has a 60% chance of one daytime shower of 3-6 s. */
    private void scheduleRain() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (r.nextDouble() < RAIN_CHANCE) {
            rainStart = 1000 + r.nextInt(5000);
            rainEnd = rainStart + 3000 + r.nextInt(3000);   // always < HALF_DAY
        } else {
            rainStart = 0;
            rainEnd = 0;
        }
    }

    void transitionTo(SkyState next) {
        this.state = next;
        notifyObservers();
    }

    public boolean isRaining() { return hour >= rainStart && hour < rainEnd; }

    public int getHour() { return hour; }
    public SkyState getSunnyState() { return sunnyState; }
    public SkyState getRainState() { return rainState; }
    public SkyState getNightState() { return nightState; }

    public String getState() {
        return state.getName();
    }

    @Override
    public void registerObserver(SkyObserver o) { observers.add(o); }

    @Override
    public void removeObserver(SkyObserver o) { observers.remove(o); }

    @Override
    public void notifyObservers() {
        String name = state.getName();
        observers.forEach(o -> o.update(name, hour));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        state.paintComponent(g);
    }
}
