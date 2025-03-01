import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;

public class WorldMapApplication {
    
    // City class to hold city data
    static class City {
        String name;
        int x, y;
        String url;
        
        public City(String name, int x, int y, String url) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.url = url;
        }
    }
    
    private JFrame frame;
    private JPanel mapPanel;
    private ArrayList<City> cities = new ArrayList<>();
    private BufferedImage worldMapImage;
    private String hoveredCity = null;
    
    public WorldMapApplication() {
        // Initialize the list of cities with coordinates and URLs
        initializeCities();
        
        // Create main frame
        frame = new JFrame("Interactive World Map");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLocationRelativeTo(null);
        
        // Load world map image
        try {
            // Try to load the world map image from file first
            File imageFile = new File("resources/world_map.jpg");
            if (imageFile.exists()) {
                worldMapImage = ImageIO.read(imageFile);
            } else {
                // Try to load from classpath resources as a fallback
                InputStream is = getClass().getClassLoader().getResourceAsStream("world_map.jpg");
                if (is != null) {
                    worldMapImage = ImageIO.read(is);
                    is.close();
                } else {
                    // If both methods fail, throw exception to trigger the fallback
                    throw new IOException("World map image not found");
                }
            }
            
            // If the image fails to load from file, create a fallback image with text
            if (worldMapImage == null) {
                System.out.println("Could not load world map image. Creating placeholder.");
                worldMapImage = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = worldMapImage.createGraphics();
                g.setColor(new Color(100, 180, 220)); // Light blue background
                g.fillRect(0, 0, 1200, 700);
                
                // Draw some simple continent outlines in green for the fallback map
                g.setColor(new Color(120, 220, 120));
                // North America
                g.fillOval(150, 150, 300, 200);
                // South America
                g.fillOval(300, 350, 180, 250);
                // Europe and Asia
                g.fillOval(500, 150, 450, 250);
                // Africa
                g.fillOval(550, 300, 200, 250);
                // Australia
                g.fillOval(850, 450, 150, 100);
                
                // Add text message about the missing image
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.drawString("Using fallback map. Place world_map.jpg in resources/ folder for a real map.", 250, 650);
                g.dispose();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error loading map image: " + e.getMessage(), 
                                         "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        // Create map panel
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                // Draw the world map
                if (worldMapImage != null) {
                    g.drawImage(worldMapImage, 0, 0, this.getWidth(), this.getHeight(), this);
                }
                
                // Draw city markers
                for (City city : cities) {
                    // Convert the city coordinates to the panel scale
                    int x = (int) (city.x * getWidth() / 1200.0);
                    int y = (int) (city.y * getHeight() / 700.0);
                    
                    // Draw city marker (red circle)
                    g.setColor(Color.RED);
                    g.fillOval(x - 6, y - 6, 12, 12);
                    
                    // Draw city name if hovered
                    if (city.name.equals(hoveredCity)) {
                        // White background for text
                        g.setColor(new Color(0, 0, 0, 180));
                        FontMetrics fm = g.getFontMetrics();
                        int textWidth = fm.stringWidth(city.name);
                        g.fillRoundRect(x - textWidth / 2 - 5, y - 30, textWidth + 10, 20, 5, 5);
                        
                        // City name text
                        g.setColor(Color.WHITE);
                        g.drawString(city.name, x - textWidth / 2, y - 15);
                    }
                }
            }
        };
        
        // Add mouse listeners to the map panel
        mapPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                checkHover(e.getX(), e.getY());
            }
        });
        
        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredCity = null;
                mapPanel.repaint();
            }
        });
        
        // Add title label
        JLabel titleLabel = new JLabel("Interactive World Map - Click on a city to visit its website", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(255, 255, 255, 200));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        
        // Set up layout
        frame.setLayout(new BorderLayout());
        frame.add(titleLabel, BorderLayout.NORTH);
        frame.add(mapPanel, BorderLayout.CENTER);
    }
    
    private void initializeCities() {
        // Add city data: name, x-coordinate, y-coordinate, URL
        cities.add(new City("New York", 300, 220, "https://www.nyc.gov"));
        cities.add(new City("London", 550, 200, "https://www.london.gov.uk"));
        cities.add(new City("Tokyo", 900, 250, "https://www.metro.tokyo.lg.jp/english/index.html"));
        cities.add(new City("Paris", 550, 220, "https://www.paris.fr/en"));
        cities.add(new City("Sydney", 950, 480, "https://www.sydney.com"));
        cities.add(new City("Rio de Janeiro", 380, 420, "https://www.rio.rj.gov.br"));
        cities.add(new City("Cairo", 600, 300, "https://www.cairo.gov.eg"));
        cities.add(new City("Mumbai", 700, 330, "https://www.mumbai.org.uk"));
        cities.add(new City("Moscow", 650, 180, "https://www.mos.ru/en"));
        cities.add(new City("Beijing", 830, 240, "http://english.beijing.gov.cn"));
        cities.add(new City("Los Angeles", 180, 250, "https://www.lacity.org"));
        cities.add(new City("Cape Town", 580, 480, "https://www.capetown.gov.za"));
        cities.add(new City("Mexico City", 230, 320, "https://www.cdmx.gob.mx"));
        cities.add(new City("Berlin", 580, 200, "https://www.berlin.de/en"));
        cities.add(new City("Singapore", 780, 380, "https://www.visitsingapore.com"));
    }
    
    private void checkHover(int mouseX, int mouseY) {
        String previousHoveredCity = hoveredCity;
        hoveredCity = null;
        
        for (City city : cities) {
            // Convert coordinates to panel scale
            int x = (int) (city.x * mapPanel.getWidth() / 1200.0);
            int y = (int) (city.y * mapPanel.getHeight() / 700.0);
            
            // Check if mouse is over this city (within 10 pixels)
            if (Math.sqrt(Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2)) <= 10) {
                hoveredCity = city.name;
                break;
            }
        }
        
        // Only repaint if the hovered city changed
        if ((hoveredCity == null && previousHoveredCity != null) || 
            (hoveredCity != null && !hoveredCity.equals(previousHoveredCity))) {
            mapPanel.repaint();
        }
    }
    
    private void handleClick(int mouseX, int mouseY) {
        for (City city : cities) {
            // Convert coordinates to panel scale
            int x = (int) (city.x * mapPanel.getWidth() / 1200.0);
            int y = (int) (city.y * mapPanel.getHeight() / 700.0);
            
            // Check if click is on this city (within 10 pixels)
            if (Math.sqrt(Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2)) <= 10) {
                try {
                    // Open the URL in the default browser
                    Desktop.getDesktop().browse(new URI(city.url));
                } catch (IOException | URISyntaxException e) {
                    JOptionPane.showMessageDialog(frame, 
                        "Error opening URL: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
                break;
            }
        }
    }
    
    public void show() {
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Start the application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            WorldMapApplication app = new WorldMapApplication();
            app.show();
        });
    }
}