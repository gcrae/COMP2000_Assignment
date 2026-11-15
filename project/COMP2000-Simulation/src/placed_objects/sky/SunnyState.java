package placed_objects.sky;

import java.awt.Color;
import java.awt.Graphics;
import supplementary.Window;

public class SunnyState implements SkyState {
    public static final String NAME = "SUNNY";
    private final Sky sky;

    public SunnyState(Sky sky) {
        this.sky = sky;
    }

    @Override
    public void paintComponent(Graphics g) {
        sky.setBackground(new Color(140,200,255));
        g.setColor(Color.YELLOW);
        double p = (((double) sky.getHour()) / Sky.HALF_DAY);
        g.fillOval((int) (p * (double) Window.WIN_WIDTH) - 50, 0, 50, 50);
    }

    @Override
    public void checkChange() {
        if(sky.getHour() >= Sky.HALF_DAY) {
            sky.transitionTo(sky.getNightState());
        } else if(sky.isRaining()) {
            sky.transitionTo(sky.getRainState());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}