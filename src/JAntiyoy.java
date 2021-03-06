import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.JFrame;

import core.Hex;
import core.Item;
import world.World;
import core.Assets;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

public class JAntiyoy implements MouseListener, MouseMotionListener, MouseWheelListener {
    public boolean debug = false;
    public int WIDTH, HEIGHT;
    public int xoffset, yoffset, zoomoffset = 1; // used to manipulate map
    public int frame = 0; // increments everytime draw loop fires
    public int seconds = 0;
    public int hopx = 0;
    public Graphics graphics; // used to draw to screen
    public World worldGen; // used to generate world
    public Hex[][] world; // world of hexs
    public Point2D[][] hexPoints; // click points, detected which hex was clicked
    public JFrame jframe;
    public int mousepressx, mousepressy;
    public int mousedragx, mousedragy;
    public int mousex, mousey;
    public int selectedhexx, selectedhexy;
    public PlayerTurn pt; // used to show end turn
    public boolean blink = false; // tells if hexs are highlighted
    private String toolbarSide = "left"; // soon to be options
    private int toolbarSize = 50; // ability to resize sidebars
    public Item selecteditem = new Item(""); // currently selected item
    public Item helditem = new Item("");
    public boolean highlightplayerhexsinrest = false;

    public JAntiyoy(int w, int h, JFrame frame) {
        // init
        this.WIDTH = w;
        this.HEIGHT = h;
        this.jframe = frame;

        // jframe code listens for resize then calls resize() method
        jframe.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent comp) {
                resize(jframe.getWidth(), jframe.getHeight());
            }
        });

        // generates world
        worldGen = new World(1);
        worldGen.generateWorld();
        world = worldGen.getWorld();
        worldGen.makePlayerCamp(worldGen.getRandomHex(1).x, worldGen.getRandomHex(1).y, 3, 2); // red
        worldGen.makePlayerCamp(worldGen.getRandomHex(1).x, worldGen.getRandomHex(1).y, 3, 3); // blue
        hexPoints = new Point2D[world.length][world[0].length];

        pt = new PlayerTurn(world, worldGen);

        // loads all the images

    }

    public void init() {

    }

    // draw loop
    public void tick(Graphics g) {
        if (frame == 0) {
            init();
        }

        if (frame % 100 == 0) {
            seconds++;
        }

        graphics = g;
        drawBackground(g, Color.blue);
        drawMap(g);
        drawUI(g);

        // checks selecteditem if anything was used
        if (selecteditem.getItemtype().equals("end_turn")) {
            pt.endTurn(worldGen);
            selecteditem = new Item();
        }

        if (selecteditem.getItemtype().equals("undo")) {
            pt.undo();
            selecteditem = new Item();
        }

        if (selecteditem.getPrice() > pt.getCurrentPlayerMoney()) {
            selecteditem = new Item();
        }

        if (selecteditem.getItemtype().equals("")) {
            worldGen.unhighlightAll();
            worldGen.highlightPlayerHexs(pt.currentturn);
        }

        if (selecteditem.toString().equals("soldier0") || helditem.toString().equals("soldier0"))
            worldGen.highlightValidSoldierHexs(pt.currentturn, 0);
        if (selecteditem.toString().equals("soldier1") || helditem.toString().equals("soldier1"))
            worldGen.highlightValidSoldierHexs(pt.currentturn, 1);
        if (selecteditem.toString().equals("soldier2") || helditem.toString().equals("soldier2"))
            worldGen.highlightValidSoldierHexs(pt.currentturn, 2);
        if (selecteditem.toString().equals("soldier3") || helditem.toString().equals("soldier3"))
            worldGen.highlightValidSoldierHexs(pt.currentturn, 3);

        // highlights player team hexs
        worldGen.highlightSpecificHexs(pt.currentturn + worldGen.predefinedstatuses);

        frame++;
    }

    public void drawUI(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        if (toolbarSide == "left") {
            // draw background
            g2.setColor(Assets.teamColors[pt.currentturn]);
            g2.fillRect(0, 0, toolbarSize, HEIGHT);

            // draw line
            g2.setColor(Color.black);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(toolbarSize, 0, toolbarSize, HEIGHT);

            // draw money and earnings
            Font oldFont = g2.getFont();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
            g2.setColor(Color.black);
            g2.drawString("$" + pt.getCurrentPlayerMoney(),
                    WIDTH - (("$" + pt.getCurrentPlayerMoney()).length() * g2.getFont().getSize()),
                    g2.getFontMetrics().getHeight());
            g2.drawString("+" + pt.getCurrentPlayerEarnings(),
                    WIDTH - ("+" + pt.getCurrentPlayerEarnings()).length() * g2.getFont().getSize(),
                    g2.getFontMetrics().getHeight() * 2);

            // draw soldier, farm, and tower icons
            // soldiers
            g2.drawImage(Assets.soldiers[0], 0, 0, toolbarSize, toolbarSize, null);
            g2.drawImage(Assets.soldiers[1], 0, toolbarSize, toolbarSize, toolbarSize, null);
            g2.drawImage(Assets.soldiers[2], 0, toolbarSize * 2, toolbarSize, toolbarSize, null);
            g2.drawImage(Assets.soldiers[3], 0, toolbarSize * 3, toolbarSize, toolbarSize, null);

            // towers
            g2.drawImage(Assets.towers[0], 0, toolbarSize * 5, toolbarSize, toolbarSize, null);
            g2.drawImage(Assets.towers[1], 0, toolbarSize * 6, toolbarSize, toolbarSize, null);

            // farm
            g2.drawImage(Assets.building[0], 0, toolbarSize * 7, toolbarSize, toolbarSize, null);

            // menu icons
            g2.drawImage(Assets.menuicons[1], 0, HEIGHT - toolbarSize * 2, toolbarSize, toolbarSize, null);
            g2.drawImage(Assets.menuicons[0], 0, HEIGHT - toolbarSize * 3, toolbarSize, toolbarSize, null);

            // show item pic and price
            if (!selecteditem.getItemtype().equals("")) {
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40));
                g2.drawImage(selecteditem.getImage(), toolbarSize, (HEIGHT / 2) - toolbarSize * 2, toolbarSize * 2,
                        toolbarSize * 2, null);
                g2.drawString("$" + selecteditem.getPrice(), (int) (toolbarSize * 3),
                        (HEIGHT / 2) - (toolbarSize * 2 / 3));
                g2.setFont(oldFont);
            }
        }

        if (!helditem.isEmpty()) {
            g2.drawImage(helditem.getImage(), mousex, mousey, toolbarSize, toolbarSize, null);
        }
    }

    public void drawMap(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world[0].length; j++) {
                double hexoffset = 0 * zoomoffset;
                double xgap = 30 * zoomoffset;
                double ygap = 8.5 * zoomoffset;
                double radius = 10 * zoomoffset;
                double imgsize = 20 * zoomoffset;

                // adds slight space on odd rows for hexagon effect
                if (i % 2 == 0) {
                    hexoffset = xgap / 2;
                } else {
                    hexoffset = 0;
                }

                if (world[i][j].getStatus() != 0 || debug) {
                    double drawx = j * xgap + hexoffset + xoffset;
                    double drawy = i * ygap + yoffset;
                    Font oldFont = g2.getFont();
                    Polygon hex = world[i][j].getPolygon(drawx, drawy, radius); // gets hexagon polygon from Hex.java
                    g.setColor(world[i][j].getColor()); // gets color from hexagon
                    g.fillPolygon(hex); // draws hex

                    // startes drawing text
                    g.setColor(Color.black);
                    g2.setStroke(new BasicStroke(1));

                    g2.drawPolygon(hex);
                    // System.out.println(world[i][j].getItem().toString());
                    if (world[i][j].getItem().isReady
                            && world[i][j].getStatus() == pt.currentturn + worldGen.predefinedstatuses) {
                        // draw item bouncing if ready
                        g2.drawImage(world[i][j].getItem().getImage(), (int) (drawx - radius),
                                (int) ((drawy - radius) + (Math.sin(seconds) * 5) - 10), (int) imgsize, (int) imgsize,
                                null);
                    } else {
                        // draw item with no bounce
                        g2.drawImage(world[i][j].getItem().getImage(), (int) (drawx - radius), (int) (drawy - radius),
                                (int) imgsize, (int) imgsize, null);
                    }
                    // System.out.println("occupied by: " + world[i][j].getOccupation());
                    if (debug) {
                        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
                        // g.drawString(world[i][j].getCleaned() + "", (int)drawx, (int)drawy);
                        g.drawString("x: " + j + "", (int) drawx - 20, (int) drawy + 10);
                        g.drawString("y: " + i + "", (int) drawx - 20, (int) drawy + 27);
                        g2.setFont(oldFont);
                    }
                }
                hexPoints[i][j] = new Point2D.Double(j * xgap + hexoffset, i * ygap);
                if (debug)
                    g.fillOval((int) hexPoints[i][j].getX() + xoffset, (int) hexPoints[i][j].getY() + yoffset, 5, 5);
            }
        }

        // whole thing ran again to highlight highlighted hexs based on world variable
        // TODO optimize, only draw what needs to be drawn. make O(1)
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world[0].length; j++) {
                double hexoffset = 0 * zoomoffset;
                double xgap = 30 * zoomoffset;
                double ygap = 8.5 * zoomoffset;
                double radius = 10 * zoomoffset;
                if (i % 2 == 0) {
                    hexoffset = xgap / 2;
                    // g.setColor(Color.green);
                } else {
                    hexoffset = 0;
                    // g.setColor(Color.red);
                }
                if (world[i][j].getStatus() != 0 || debug) {
                    double drawx = j * xgap + hexoffset + xoffset;
                    double drawy = i * ygap + yoffset;
                    Polygon hex = world[i][j].getPolygon(drawx, drawy, radius);
                    g.setColor(world[i][j].getColor());
                    // g.fillPolygon(hex);
                    g.setColor(Color.black);
                    if (world[i][j].isHighlighted && blink) {
                        g2.setStroke(new BasicStroke(3 + (zoomoffset)));
                        g2.drawPolygon(hex);
                    } else {
                        g2.setStroke(new BasicStroke(1));
                    }
                    // g2.drawPolygon(hex);
                }
            }
        }
    }

    public void drawBackground(Graphics g, Color c) {
        Color oldColor = g.getColor();
        g.setColor(c);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(oldColor);
    }

    public void resize(int x, int y) {
        WIDTH = x;
        HEIGHT = y;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if (e.getButton() == 3) {
            // clears selected item if right mouse clicked
            System.out.println("clearing selecteditem, clicked mousebutton: " + e.getButton());
            selecteditem = new Item();
        }

        if (toolbarSide.equals("left")) {
            if (e.getX() > toolbarSize) {
                // clicked on world view
                int hexx = -1, hexy = -1;
                double closestdist = 500;
                for (int i = 0; i < world.length; i++) {
                    for (int j = 0; j < world[0].length; j++) {
                        double mousedist = hexPoints[i][j].distance(e.getX() - xoffset, e.getY() - yoffset);
                        if (mousedist < closestdist && world[i][j].getStatus() != 0) {
                            closestdist = mousedist;
                            hexx = i;
                            hexy = j;
                        }
                    }
                }

                Item clickeditem = worldGen.getWorld()[hexx][hexy].getItem();
                Hex clickedhex = worldGen.getWorld()[hexx][hexy];

                if (!selecteditem.isEmpty()) {
                    // run if user has selected item
                    
                    if (selecteditem.getItemtype().equals("soldier")) {
                        selecteditem.isReady = true;
                    } else {
                        selecteditem.isReady = false;
                    }
                    
                    if (worldGen.placeItem(world[hexx][hexy], selecteditem, pt.currentturn)) {
                        pt.setCurrentPlayerMoney(pt.getCurrentPlayerMoney() - selecteditem.getPrice());
                    }

                    selecteditem = new Item();

                } else if (helditem.isEmpty() && clickeditem.isReady
                        && (clickedhex.getStatus() == pt.currentturn + worldGen.predefinedstatuses)) {
                    // picks up item if it is ready and the hex it is on is owned by the current
                    // player
                    System.out.println("grabbing: '" + clickeditem.toString() + "'");
                    helditem = clickeditem;
                    worldGen.placeItem(world[hexx][hexy], new Item(), pt.currentturn);

                } else if (!helditem.isEmpty()) {
                    // System.out.println("placing: '" + helditem.toString() + "'");
                    // places helditem
                    helditem.isReady = false;
                    worldGen.placeItem(world[hexx][hexy], helditem, pt.currentturn);
                    helditem = new Item();
                    worldGen.unhighlightAll();

                }

            } else {
                // clicked on menu

                if (e.getY() < toolbarSize) {
                    // first button - soldier0
                    if (debug)
                        System.out.println("clicked soldier0");

                    selecteditem = new Item("soldier", 0);
                }
                if (e.getY() < toolbarSize * 2 && e.getY() > toolbarSize) {
                    // second button - soldier1
                    if (debug)
                        System.out.println("clicked soldier1");

                    selecteditem = new Item("soldier", 1);
                }
                if (e.getY() < toolbarSize * 3 && e.getY() > toolbarSize * 2) {
                    // third button - soldier2
                    if (debug)
                        System.out.println("clicked soldier2");

                    selecteditem = new Item("soldier", 2);
                }
                if (e.getY() < toolbarSize * 4 && e.getY() > toolbarSize * 3) {
                    // fourth button - soldier3
                    if (debug)
                        System.out.println("clicked soldier3");

                    selecteditem = new Item("soldier", 3);
                }
                if (e.getY() < toolbarSize * 6 && e.getY() > toolbarSize * 5) {
                    // fourth button - tower0
                    if (debug)
                        System.out.println("clicked tower0");

                    selecteditem = new Item("tower", 0);
                }
                if (e.getY() < toolbarSize * 7 && e.getY() > toolbarSize * 6) {
                    // fourth button - tower1
                    if (debug)
                        System.out.println("clicked tower1");

                    selecteditem = new Item("tower", 1);
                }
                if (e.getY() < toolbarSize * 8 && e.getY() > toolbarSize * 7) {
                    // fourth button - farm
                    if (debug)
                        System.out.println("clicked farm");

                    selecteditem = new Item("farm", 0);
                }

                if (e.getY() < HEIGHT - toolbarSize * 2 && e.getY() > HEIGHT - toolbarSize * 3) {
                    // fifth button - undo
                    if (debug)
                        System.out.println("clicked undo");

                    selecteditem = new Item("undo");
                }
                if (e.getY() < HEIGHT - toolbarSize && e.getY() > HEIGHT - toolbarSize * 2) {
                    // sixth button - end turn
                    if (debug)
                        System.out.println("clicked end turn");

                    selecteditem = new Item("end_turn");
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousedragx = e.getX();
        mousedragy = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (e.getX() > toolbarSize && toolbarSide.equals("left")) {
            if (e.getX() - mousedragx < 20) {
                xoffset += e.getX() - mousedragx;
                yoffset += e.getY() - mousedragy;
            }

            mousedragx = e.getX();
            mousedragy = e.getY();
        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
        mousex = e.getX();
        mousey = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // zoom based on mouse wheel
        zoomoffset -= e.getUnitsToScroll() / 3;
        xoffset += e.getUnitsToScroll() * 50;
        yoffset += e.getUnitsToScroll() * 50;
        if (zoomoffset > 10) {
            zoomoffset = 10;
        }
        if (zoomoffset < 1) {
            zoomoffset = 1;
        }

    }
}
