import java.util.Arrays;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Write a description of class Background here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class Background
{
    // stores RGB values

    private int[] starColor  = new int[3];
    private int[] dayColor   = new int[3]; 
    private int[] nightColor = new int[3];
    private final Quadrilateral backgroundNight;
    private final Quadrilateral backgroundDay;
    
    int cycle = 0;
    float opacityOfDay; 

    //
    final int MAXCYCLE = 1000;
    final int MIDDAY = MAXCYCLE/2;
    //  ---------------------------------------------------------------------------
    //night0              midDay150           day300        midDay450           Night600
    // 0 = day, 600 = night

    /**
     *  Default initializer
     *
     *  Set background night to black, stars to white (unused as of now), and day to cyan
     */
    public Background(){
        this(new int[]{255,255,255},new int[]{0,255,255},new int[]{0,0,56},new double[][]{{0, 0}, {2000, 0}, {2000, 2000}, {0, 2000}});        
    }

    /**
     * Initializes all element colors to a color in rgb array.
     *
     * @param newStarColor  [r,g,b]
     * @param newDayColor   [r,g,b]
     * @param newNightColor [r,g,b]
     * @param boundingBox Skybox coords
     */
    public Background(int[] newStarColor, int[] newDayColor, int[]newNightColor, double[][] boundingBox){
        this.backgroundNight = new Quadrilateral(boundingBox[0],boundingBox[1],boundingBox[2],boundingBox[3]);
        this.backgroundDay   = new Quadrilateral(boundingBox[0],boundingBox[1],boundingBox[2],boundingBox[3]);
        this.starColor  = Arrays.copyOf(newStarColor,3);
        this.dayColor   = Arrays.copyOf(newDayColor,3);
        this.nightColor = Arrays.copyOf(newNightColor,3);
    }

    /**
     * Updates the cycle based on current time, then updates opacity
     *
     * @param time current number of "ticks"
     */
    public void setCycle(int time){
        this.cycle = time%MAXCYCLE;
        this.setOpacityDay();
    }

    /**
     * Sets the opacity based on the current cycle
     */
    public void setOpacityDay(){
        this.opacityOfDay = ((float)(-1*Math.abs(cycle-MIDDAY)+MIDDAY))/MIDDAY;
    }

    /**
     * Returns the opacity
     *
     * @return opacity of day
     */
    public float getOpacity(){
        return this.opacityOfDay;
    }

    /**
     * Sets colors used to draw
     *
     * @return an Array of colors used to draw
     */
    public Color[] drawColors(){
        Color newStarColor  = new Color(((float)this.starColor[0])/255,
                                        ((float)this.starColor[1])/255,
                                        ((float)this.starColor[2])/255);
        Color newdayColor   = new Color(((float)this.dayColor[0])/255,
                                        ((float)this.dayColor[1])/255,
                                        ((float)this.dayColor[2])/255,
                                                   this.opacityOfDay);
        Color newnightColor = new Color(((float)this.nightColor[0])/255,
                                       ((float)this.nightColor[1])/255,
                                       ((float)this.nightColor[2])/255);

        return new Color[]{newStarColor,newdayColor,newnightColor};
    }

    /**
     * Draws the backgrounds
     *
     * @param g object that draws
     */
    public void draw(Graphics2D g){
        Color[] arrayOfColors = drawColors();
        //System.out.println();
        backgroundNight.draw(g,arrayOfColors[2]);
        backgroundDay.draw(g,arrayOfColors[1]);
    }
}