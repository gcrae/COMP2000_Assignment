package supplementary;
import growables.Growable;
import java.awt.*;
import javax.swing.*;
import placed_objects.Ground;
import placed_objects.Patch;
import placed_objects.sky.Sky;

/**
 * Main application window holding the environment panels.
 */
public class Window extends JFrame{
    //Window of the application
    //Defines screenspace

    public static final int WIN_WIDTH = 1200;
    public static final int WIN_HEIGHT = 900;
    public static final int SKY_HEIGHT = WIN_HEIGHT / 4;
    public static final int GROUND_HEIGHT = WIN_HEIGHT - SKY_HEIGHT;
    private static final int TICK_MS = 20;
    
    private final Sky sky;
    private final Ground ground;
    private final Timer timer;

    public Window() {
        sky = new Sky();
        ground = new Ground();

        // javax.swing.Timer fires on the event-dispatch thread, so every Swing/model
        // mutation made by a tick is single-threaded (java.util.Timer used its own thread).
        timer = new Timer(TICK_MS, e -> {
            ground.tick();
            sky.tick();
        });

        //Basic window props
        this.setTitle("GAASK Plant Simulation COMP2000");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   //Exit appliction when x pressed
        this.setResizable(false);
        this.setLayout(new GridBagLayout());    //Main window only contains sky and ground

        //Keep the main sky and ground in a single column
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth=GridBagConstraints.REMAINDER;
        c.fill=GridBagConstraints.HORIZONTAL;

        this.add(sky,c);
        this.add(ground,c);
        
        this.pack();
        this.setLocationRelativeTo(null);   // must follow pack() to centre the real size

        this.setVisible(true);
        timer.start();   // only start once the UI exists
    }

    /** True if p lies inside the ground area (the only place plants may live). */
    public static boolean isOnGround(Point p) {
        return p != null && p.x >= 0 && p.x <= WIN_WIDTH && p.y >= 0 && p.y <= GROUND_HEIGHT;
    }

    //Sky will deal with it's own components, so anything added to the window 
    //must be added to the ground.

    public Sky getSky() {
        return sky;
    }

    public Ground getGround() {
        return ground;
    }

    public <T extends Component & Growable> void addToGround(T g) {
        ground.addGrowable(g);
    }

    public void addToGround(Patch<?> p) {
        ground.addPatch(p);
    }

    public void removeFromGround(Component comp) {
        ground.remove(comp);
    }

    public void refresh() {
        ground.revalidate();
        ground.repaint();
    }
}