package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pizza Parkour: A rooftop delivery game
 * The player controls a pizza delivery person jumping across rooftops,
 * collecting pizzas from a central location and delivering them to customers.
 */
public class PizzaParkour extends SimpleApplication implements ActionListener, PhysicsCollisionListener {

    // Physics
    private BulletAppState bulletAppState;
    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false, jump = false;
    private float playerSpeed = 8f;
    private float jumpSpeed = 20f;
    
    // Game objects
    private Node cityNode;
    private Spatial pizzaShop;
    private List<Spatial> deliveryLocations = new ArrayList<>();
    private List<Spatial> activeDeliveries = new ArrayList<>();
    private List<Spatial> activePizzas = new ArrayList<>();
    
    // Player state
    private boolean holdingPizza = false;
    private Spatial carriedPizza;
    private int score = 0;
    private int deliveriesCompleted = 0;
    private float gameTime = 180; // 3 minutes game time
    private float deliveryTimeLimit = 60; // 1 minute per delivery
    private float currentDeliveryTime = 0;
    
    // UI elements
    private BitmapText scoreText;
    private BitmapText timeText;
    private BitmapText messageText;
    private BitmapText deliveryTimerText;
    
    // Game state
    private enum GameState { PLAYING, GAME_OVER, WIN }
    private GameState state = GameState.PLAYING;
    
    public static void main(String[] args) {
        PizzaParkour app = new PizzaParkour();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Pizza Parkour: Rooftop Delivery");
        settings.setResolution(1280, 720);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Set up camera and controls
        flyCam.setMoveSpeed(30);
        cam.setLocation(new Vector3f(0, 10, 10));
        setUpKeys();
        
        // Set up physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -30f, 0));
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
        
        // Set up lighting
        setupLighting();
        
        // Create game environment
        createCity();
        
        // Create player
        createPlayer();
        
        // Set up UI
        createUI();
        
        // Start the game
        startNewGame();
    }
    
    private void setupLighting() {
        // Sun light
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(sun);
        
        // Ambient light
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);
    }
    
    private void createCity() {
        cityNode = new Node("City");
        rootNode.attachChild(cityNode);
        
        // Create ground
        Box groundBox = new Box(100, 0.5f, 100);
        Geometry ground = new Geometry("Ground", groundBox);
        Material groundMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        TextureKey groundKey = new TextureKey("Textures/Terrain/Asphalt/Asphalt.jpg");
        groundKey.setGenerateMips(true);
        Texture groundTex = assetManager.loadTexture(groundKey);
        groundTex.setWrap(Texture.WrapMode.Repeat);
        groundMat.setTexture("DiffuseMap", groundTex);
        ground.setMaterial(groundMat);
        ground.setLocalTranslation(0, -0.5f, 0);
        
        // Add physics to ground
        RigidBodyControl groundPhysics = new RigidBodyControl(0);
        ground.addControl(groundPhysics);
        bulletAppState.getPhysicsSpace().add(groundPhysics);
        cityNode.attachChild(ground);
        
        // Create buildings with rooftops
        Random random = new Random(1234); // Fixed seed for reproducible layout
        
        // Materials for buildings and rooftops
        Material[] buildingMats = new Material[5];
        for (int i = 0; i < 5; i++) {
            buildingMats[i] = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            buildingMats[i].setColor("Diffuse", new ColorRGBA(
                    0.4f + random.nextFloat() * 0.3f,
                    0.4f + random.nextFloat() * 0.3f,
                    0.4f + random.nextFloat() * 0.3f,
                    1.0f));
            buildingMats[i].setColor("Ambient", ColorRGBA.White);
            buildingMats[i].setBoolean("UseMaterialColors", true);
        }
        
        Material roofMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        roofMat.setColor("Diffuse", new ColorRGBA(0.8f, 0.2f, 0.2f, 1.0f)); // Reddish roofs
        roofMat.setColor("Ambient", ColorRGBA.White);
        roofMat.setBoolean("UseMaterialColors", true);
        
        // Create a grid of buildings
        int gridSize = 5; // 5x5 grid of buildings
        float spacing = 25f; // Space between building centers
        float startX = -(gridSize * spacing) / 2 + spacing / 2;
        float startZ = -(gridSize * spacing) / 2 + spacing / 2;
        
        // Track pizza shop location and potential delivery locations
        Vector3f pizzaShopLocation = null;
        List<Vector3f> potentialDeliveryLocations = new ArrayList<>();
        
        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                // Skip some positions randomly to create irregular city layout
                if (random.nextFloat() < 0.2f) continue;
                
                // Calculate building position
                float posX = startX + x * spacing;
                float posZ = startZ + z * spacing;
                
                // Randomize building dimensions
                float width = 8f + random.nextFloat() * 5f;
                float height = 10f + random.nextFloat() * 20f;
                float depth = 8f + random.nextFloat() * 5f;
                
                // Create building
                Box buildingBox = new Box(width/2, height/2, depth/2);
                Geometry building = new Geometry("Building", buildingBox);
                building.setMaterial(buildingMats[random.nextInt(buildingMats.length)]);
                building.setLocalTranslation(posX, height/2, posZ);
                
                // Create rooftop
                Box roofBox = new Box(width/2 + 0.5f, 0.2f, depth/2 + 0.5f);
                Geometry roof = new Geometry("Roof", roofBox);
                roof.setMaterial(roofMat);
                roof.setLocalTranslation(posX, height + 0.2f, posZ);
                
                // Add physics to building and roof
                RigidBodyControl buildingPhysics = new RigidBodyControl(0);
                building.addControl(buildingPhysics);
                
                RigidBodyControl roofPhysics = new RigidBodyControl(0);
                roof.addControl(roofPhysics);
                
                // Add to scene and physics space
                cityNode.attachChild(building);
                cityNode.attachChild(roof);
                bulletAppState.getPhysicsSpace().add(buildingPhysics);
                bulletAppState.getPhysicsSpace().add(roofPhysics);
                
                // Track as potential delivery location
                potentialDeliveryLocations.add(new Vector3f(posX, height + 0.5f, posZ));
                
                // If it's the center building, mark as pizza shop
                if (x == gridSize/2 && z == gridSize/2) {
                    pizzaShopLocation = new Vector3f(posX, height + 0.5f, posZ);
                }
            }
        }
        
        // Create pizza shop at the chosen location
        if (pizzaShopLocation != null) {
            pizzaShop = createPizzaShop(pizzaShopLocation);
            cityNode.attachChild(pizzaShop);
        }
        
        // Choose random delivery locations from potential locations (exclude pizza shop)
        potentialDeliveryLocations.remove(pizzaShopLocation);
        
        // Create initial delivery locations
        for (int i = 0; i < 3; i++) {
            if (!potentialDeliveryLocations.isEmpty()) {
                int index = random.nextInt(potentialDeliveryLocations.size());
                Vector3f location = potentialDeliveryLocations.get(index);
                potentialDeliveryLocations.remove(index);
                
                Spatial deliveryLocation = createDeliveryLocation(location);
                deliveryLocations.add(deliveryLocation);
                cityNode.attachChild(deliveryLocation);
            }
        }
    }
    
    private Spatial createPizzaShop(Vector3f location) {
        // Create a visible marker for the pizza shop
        Node shopNode = new Node("PizzaShop");
        
        // Base
        Box base = new Box(3f, 0.2f, 3f);
        Geometry baseGeom = new Geometry("ShopBase", base);
        Material baseMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        baseMat.setColor("Diffuse", new ColorRGBA(1f, 0.8f, 0f, 1f)); // Golden base
        baseMat.setColor("Ambient", ColorRGBA.White);
        baseMat.setBoolean("UseMaterialColors", true);
        baseGeom.setMaterial(baseMat);
        
        // Sign post
        Cylinder post = new Cylinder(12, 12, 0.3f, 2f);
        Geometry postGeom = new Geometry("ShopPost", post);
        Material postMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        postMat.setColor("Diffuse", new ColorRGBA(0.6f, 0.3f, 0f, 1f)); // Brown post
        postMat.setColor("Ambient", ColorRGBA.White);
        postMat.setBoolean("UseMaterialColors", true);
        postGeom.setMaterial(postMat);
        postGeom.setLocalTranslation(0, 1f, 0);
        
        // Shop sign (pizza shape)
        Cylinder sign = new Cylinder(24, 24, 2f, 0.2f);
        Geometry signGeom = new Geometry("ShopSign", sign);
        Material signMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        signMat.setColor("Diffuse", new ColorRGBA(0.9f, 0.1f, 0f, 1f)); // Red sign
        signMat.setColor("Ambient", ColorRGBA.White);
        signMat.setBoolean("UseMaterialColors", true);
        signGeom.setMaterial(signMat);
        
        // Rotate to horizontal pizza
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        signGeom.setLocalRotation(rotation);
        signGeom.setLocalTranslation(0, 2.5f, 0);
        
        // Add physics box for collisions
        BoxCollisionShape boxShape = new BoxCollisionShape(new Vector3f(3f, 1f, 3f));
        RigidBodyControl shopPhysics = new RigidBodyControl(boxShape, 0);
        shopNode.addControl(shopPhysics);
        bulletAppState.getPhysicsSpace().add(shopPhysics);
        
        // Add to node
        shopNode.attachChild(baseGeom);
        shopNode.attachChild(postGeom);
        shopNode.attachChild(signGeom);
        
        // Set location
        shopNode.setLocalTranslation(location);
        
        return shopNode;
    }
    
    private Spatial createDeliveryLocation(Vector3f location) {
        Node deliveryNode = new Node("DeliveryLocation");
        
        // Base platform
        Box platform = new Box(2f, 0.2f, 2f);
        Geometry platformGeom = new Geometry("DeliveryPlatform", platform);
        Material platformMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        platformMat.setColor("Diffuse", new ColorRGBA(0f, 0.7f, 0f, 1f)); // Green platform
        platformMat.setColor("Ambient", ColorRGBA.White);
        platformMat.setBoolean("UseMaterialColors", true);
        platformGeom.setMaterial(platformMat);
        
        // Arrow pointing down
        Box arrow = new Box(0.5f, 1f, 0.5f);
        Geometry arrowGeom = new Geometry("DeliveryArrow", arrow);
        Material arrowMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        arrowMat.setColor("Diffuse", new ColorRGBA(1f, 1f, 0f, 1f)); // Yellow arrow
        arrowMat.setColor("Ambient", ColorRGBA.White);
        arrowMat.setBoolean("UseMaterialColors", true);
        arrowGeom.setMaterial(arrowMat);
        arrowGeom.setLocalTranslation(0, 1.5f, 0);
        
        // Add physics box for collisions
        BoxCollisionShape boxShape = new BoxCollisionShape(new Vector3f(2f, 1f, 2f));
        RigidBodyControl deliveryPhysics = new RigidBodyControl(boxShape, 0);
        deliveryNode.addControl(deliveryPhysics);
        bulletAppState.getPhysicsSpace().add(deliveryPhysics);
        
        // Add to node
        deliveryNode.attachChild(platformGeom);
        deliveryNode.attachChild(arrowGeom);
        
        // Set location
        deliveryNode.setLocalTranslation(location);
        
        return deliveryNode;
    }
    
    private Spatial createPizza() {
        Cylinder pizza = new Cylinder(24, 24, 1f, 0.1f);
        Geometry pizzaGeom = new Geometry("Pizza", pizza);
        Material pizzaMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        pizzaMat.setColor("Diffuse", new ColorRGBA(0.9f, 0.8f, 0.3f, 1f)); // Pizza color
        pizzaMat.setColor("Ambient", ColorRGBA.White);
        pizzaMat.setBoolean("UseMaterialColors", true);
        pizzaGeom.setMaterial(pizzaMat);
        
        // Rotate to horizontal pizza
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        pizzaGeom.setLocalRotation(rotation);
        
        return pizzaGeom;
    }
    
    private void createPlayer() {
        // Create player physics capsule
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.5f, 1.8f, 1);
        player = new CharacterControl(capsuleShape, 0.1f);
        player.setJumpSpeed(jumpSpeed);
        player.setFallSpeed(30);
        player.setGravity(30);
        
        // Set initial player position on the pizza shop rooftop
        if (pizzaShop != null) {
            Vector3f shopPos = pizzaShop.getWorldTranslation();
            player.setPhysicsLocation(shopPos.add(0, 3, 0));
        } else {
            player.setPhysicsLocation(new Vector3f(0, 10, 0));
        }
        
        // Add to physics space
        bulletAppState.getPhysicsSpace().add(player);
    }
    
    private void createUI() {
        // Initialize UI elements
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        
        // Score text
        scoreText = new BitmapText(guiFont, false);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize());
        scoreText.setText("Score: 0  Deliveries: 0");
        scoreText.setLocalTranslation(10, settings.getHeight() - 10, 0);
        guiNode.attachChild(scoreText);
        
        // Game timer
        timeText = new BitmapText(guiFont, false);
        timeText.setSize(guiFont.getCharSet().getRenderedSize());
        timeText.setText("Time: 3:00");
        timeText.setLocalTranslation(settings.getWidth() - 150, settings.getHeight() - 10, 0);
        guiNode.attachChild(timeText);
        
        // Delivery timer
        deliveryTimerText = new BitmapText(guiFont, false);
        deliveryTimerText.setSize(guiFont.getCharSet().getRenderedSize());
        deliveryTimerText.setText("Delivery Time: 0:00");
        deliveryTimerText.setLocalTranslation(settings.getWidth() / 2 - 100, settings.getHeight() - 10, 0);
        deliveryTimerText.setColor(ColorRGBA.Green);
        guiNode.attachChild(deliveryTimerText);
        
        // Message text (centered)
        messageText = new BitmapText(guiFont, false);
        messageText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        messageText.setText("Deliver pizzas quickly to win!");
        messageText.setLocalTranslation(
                settings.getWidth() / 2 - messageText.getLineWidth() / 2,
                settings.getHeight() / 2,
                0);
        guiNode.attachChild(messageText);
    }
    
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Restart", new KeyTrigger(KeyInput.KEY_R));
        
        inputManager.addListener(this, "Left", "Right", "Up", "Down", "Jump", "Restart");
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (state == GameState.PLAYING) {
            // Update player movement
            updatePlayerMovement();
            
            // Update carried pizza position
            updateCarriedPizza();
            
            // Update game time
            updateGameTime(tpf);
            
            // Update current delivery time if holding pizza
            if (holdingPizza) {
                currentDeliveryTime += tpf;
                updateDeliveryTimer();
            }
            
            // Check for win condition
            checkWinCondition();
        }
    }
    
    private void updatePlayerMovement() {
        Vector3f camDir = cam.getDirection().clone().multLocal(0.6f);
        Vector3f camLeft = cam.getLeft().clone().multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        
        // Cancel out the y component for level movement
        camDir.y = 0;
        camLeft.y = 0;
        
        // Normalize
        camDir.normalizeLocal();
        camLeft.normalizeLocal();
        
        // Set move direction based on keys
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        
        // Apply jump
        if (jump && player.onGround()) {
            player.jump();
        }
        
        // Normalize and apply movement
        if (walkDirection.length() > 0) {
            walkDirection.normalizeLocal();
        }
        walkDirection.multLocal(playerSpeed);
        player.setWalkDirection(walkDirection);
        
        // Move camera with player
        Vector3f playerPos = player.getPhysicsLocation();
        cam.setLocation(new Vector3f(playerPos.x, playerPos.y + 2f, playerPos.z));
    }
    
    private void updateCarriedPizza() {
        if (holdingPizza && carriedPizza != null) {
            Vector3f playerPos = player.getPhysicsLocation();
            // Position pizza in front of player
            Vector3f pizzaPos = playerPos.add(cam.getDirection().mult(1.0f));
            pizzaPos.y = playerPos.y + 0.5f; // Position at chest height
            carriedPizza.setLocalTranslation(pizzaPos);
        }
    }
    
    private void updateGameTime(float tpf) {
        // Update game time
        gameTime -= tpf;
        if (gameTime <= 0) {
            gameTime = 0;
            endGame(false); // Game over - time's up
        }
        
        // Update time display
        int minutes = (int) (gameTime / 60);
        int seconds = (int) (gameTime % 60);
        timeText.setText(String.format("Time: %d:%02d", minutes, seconds));
        
        // Make timer red when less than 30 seconds
        if (gameTime < 30) {
            timeText.setColor(ColorRGBA.Red);
        }
    }
    
    private void updateDeliveryTimer() {
        // Update delivery timer display
        int seconds = (int) currentDeliveryTime;
        int minutes = seconds / 60;
        seconds %= 60;
        
        deliveryTimerText.setText(String.format("Delivery Time: %d:%02d", minutes, seconds));
        
        // Change color based on time remaining
        if (currentDeliveryTime > deliveryTimeLimit * 0.7f) {
            deliveryTimerText.setColor(ColorRGBA.Red);
        } else if (currentDeliveryTime > deliveryTimeLimit * 0.4f) {
            deliveryTimerText.setColor(ColorRGBA.Yellow);
        } else {
            deliveryTimerText.setColor(ColorRGBA.Green);
        }
    }
    
    private void startNewGame() {
        // Reset game state
        state = GameState.PLAYING;
        score = 0;
        deliveriesCompleted = 0;
        gameTime = 180; // 3 minutes
        holdingPizza = false;
        
        // Clear any existing pizzas
        for (Spatial pizza : activePizzas) {
            rootNode.detachChild(pizza);
        }
        activePizzas.clear();
        
        // Reset UI
        updateScoreText();
        messageText.setText("Get pizza from the shop (yellow) and deliver to green platforms!");
        messageText.setColor(ColorRGBA.White);
        
        // Make sure there's a pizza at the shop
        spawnPizzaAtShop();
        
        // Reset player position to the pizza shop
        if (pizzaShop != null) {
            Vector3f shopPos = pizzaShop.getWorldTranslation();
            player.setPhysicsLocation(shopPos.add(0, 3, 0));
        }
    }
    
    private void spawnPizzaAtShop() {
        // Only spawn if there are no pizzas at the shop already
        boolean pizzaAlreadyAtShop = false;
        for (Spatial pizza : activePizzas) {
            // Check if pizza is close to the shop
            if (pizza.getWorldTranslation().distance(pizzaShop.getWorldTranslation()) < 3f) {
                pizzaAlreadyAtShop = true;
                break;
            }
        }
        
        if (!pizzaAlreadyAtShop) {
            Spatial pizza = createPizza();
            Vector3f shopPos = pizzaShop.getWorldTranslation();
            pizza.setLocalTranslation(shopPos.add(0, 1.5f, 0));
            rootNode.attachChild(pizza);
            activePizzas.add(pizza);
        }
    }
    
    private void pickupPizza(Spatial pizza) {
        if (!holdingPizza) {
            holdingPizza = true;
            carriedPizza = pizza;
            // Reset delivery timer
            currentDeliveryTime = 0;
            updateDeliveryTimer();
            
            // Show message
            messageText.setText("Deliver the pizza to a green platform!");
            messageText.setColor(ColorRGBA.Green);
        }
    }
    
    private void deliverPizza(Spatial deliveryLocation) {
        if (holdingPizza) {
            // Calculate points based on delivery time
            int timePoints = (int) Math.max(1, deliveryTimeLimit - currentDeliveryTime);
            int deliveryPoints = 100 + timePoints;
            
            score += deliveryPoints;
            deliveriesCompleted++;
            
            // Update UI
            updateScoreText();
            messageText.setText("Pizza delivered! +" + deliveryPoints + " points");
            messageText.setColor(ColorRGBA.Yellow);
            
            // Remove carried pizza
            if (carriedPizza != null) {
                rootNode.detachChild(carriedPizza);
                activePizzas.remove(carriedPizza);
                carriedPizza = null;
            }
            
            holdingPizza = false;
            
            // Spawn new pizza at shop
            spawnPizzaAtShop();
            
            // Add time bonus for quick delivery
            if (currentDeliveryTime < deliveryTimeLimit * 0.5f) {
                gameTime += 15; // 15 seconds bonus
                messageText.setText(messageText.getText() + " Time bonus: +15 seconds!");
            }
        }
    }
    
    private void updateScoreText() {
        scoreText.setText("Score: " + score + "  Deliveries: " + deliveriesCompleted);
    }
    
    private void checkWinCondition() {
        // Win if enough deliveries are made
        if (deliveriesCompleted >= 10) {
            endGame(true);
        }
    }
    
    private void endGame(boolean win) {
        state = win ? GameState.WIN : GameState.GAME_OVER;
        
        if (win) {
            messageText.setText("You win! Final score: " + score + "\nPress R to play again");
            messageText.setColor(ColorRGBA.Green);
        } else {
            messageText.setText("Game Over! Final score: " + score + "\nPress R to play again");
            messageText.setColor(ColorRGBA.Red);
        }
        
        messageText.setLocalTranslation(
                settings.getWidth() / 2 - messageText.getLineWidth() / 2,
                settings.getHeight() / 2,
                0);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Left")) {
            left = isPressed;
        } else if (name.equals("Right")) {
            right = isPressed;
        } else if (name.equals("Up")) {
            up = isPressed;
        } else if (name.equals("Down")) {
            down = isPressed;
        } else if (name.equals("Jump")) {
            jump = isPressed;
        } else if (name.equals("Restart") && isPressed) {
            if (state == GameState.GAME_OVER || state == GameState.WIN) {
                startNewGame();
            }
        }
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        // Check for collision between player and objects
        if (state == GameState.PLAYING) {
            String a = event.getNodeA().getName();
            String b = event.getNodeB().getName();
            
            // Check for pizza pickup (player colliding with pizza)
            if (a.startsWith("Pizza") || b.startsWith("Pizza")) {
                Spatial pizza = a.startsWith("Pizza") ? event.getNodeA() : event.getNodeB();
                
                // Only pickup if we're near the pizza shop
                if (pizza.getWorldTranslation().distance(pizzaShop.getWorldTranslation()) < 5f &&
                        !holdingPizza) {
                    pickupPizza(pizza);
                }
            }
            
            // Check for pizza delivery (player with pizza colliding with delivery location)
            if ((a.startsWith("DeliveryLocation") || b.startsWith("DeliveryLocation")) && holdingPizza) {
                Spatial delivery = a.startsWith("DeliveryLocation") ? event.getNodeA() : event.getNodeB();
                deliverPizza(delivery);
            }
        }
    }
}
