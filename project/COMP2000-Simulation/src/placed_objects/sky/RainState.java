package placed_objects.sky;

import java.awt.Color;
import java.awt.Graphics;
import supplementary.Window;

public class RainState implements SkyState {
    public static final String NAME = "RAIN";
    private static final int DROPS = 90;
    private final Sky sky;

    public RainState(Sky sky) {
        this.sky = sky;
    }

    @Override
    public void paintComponent(Graphics g) {
        sky.setBackground(new Color(85, 100, 125));
        g.setColor(new Color(190, 210, 240));
        int fall = sky.getHour() / 3;   // time-driven, so the drops animate deterministically
        for (int i = 0; i < DROPS; i++) {
            int x = (i * 151) % Window.WIN_WIDTH;
            int y = (i * 37 + fall) % Window.SKY_HEIGHT;
            g.drawLine(x, y, x - 3, y + 10);
        }
    }

    @Override
    public void checkChange() {
        if (sky.getHour() >= Sky.HALF_DAY) {
            sky.transitionTo(sky.getNightState());
        } else if (!sky.isRaining()) {
            sky.transitionTo(sky.getSunnyState());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
