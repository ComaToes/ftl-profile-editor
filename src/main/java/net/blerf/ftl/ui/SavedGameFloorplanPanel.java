package net.blerf.ftl.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.RescaleOp;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipChassis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameFloorplanPanel extends JLayeredPane {

	private static final Integer BASE_LAYER = new Integer(10);
	private static final Integer FLOOR_LAYER = new Integer(11);
	private static final Integer OXYGEN_LAYER = new Integer(12);
	private static final Integer WALL_LAYER = new Integer(15);
	private static final Integer SYSTEM_LAYER = new Integer(16);
	private static final Integer BREACH_LAYER = new Integer(17);
	private static final Integer FIRE_LAYER = new Integer(18);
	private static final Integer DOOR_LAYER = new Integer(30);
	private static final Integer CREW_LAYER = new Integer(40);
	private static final int squareSize = 35, tileEdge = 1;
	private static final Logger log = LogManager.getLogger(SavedGameFloorplanPanel.class);

	private FTLFrame frame;

	private ShipBlueprint shipBlueprint = null;
	private ShipLayout shipLayout = null;
	private ShipChassis shipChassis = null;
	private String shipGfxBaseName = null;
	private int originX=0, originY=0;
	private HashMap<Rectangle, Integer> roomRegions = new HashMap<Rectangle, Integer>();
	private ArrayList<DoorSprite> doorSprites = new ArrayList<DoorSprite>();
	private ArrayList<SystemSprite> systemSprites = new ArrayList<SystemSprite>();
	private ArrayList<BreachSprite> breachSprites = new ArrayList<BreachSprite>();
	private ArrayList<FireSprite> fireSprites = new ArrayList<FireSprite>();
	private ArrayList<CrewSprite> crewSprites = new ArrayList<CrewSprite>();

	private JLabel baseLbl = null;
	private JLabel floorLbl = null;
	private JLabel oxygenLbl = null;
	private JLabel wallLbl = null;
	private JLabel crewLbl = null;

	public SavedGameFloorplanPanel( FTLFrame frame ) {
		this.frame = frame;

		baseLbl = new JLabel();
		baseLbl.setOpaque(false);
		baseLbl.setBounds( 0, 0, 50, 50 );
		this.add( baseLbl, BASE_LAYER );

		floorLbl = new JLabel();
		floorLbl.setOpaque(false);
		floorLbl.setBounds( 0, 0, 50, 50 );
		this.add( floorLbl, FLOOR_LAYER );

		oxygenLbl = new JLabel();
		oxygenLbl.setOpaque(false);
		oxygenLbl.setBounds( 0, 0, 50, 50 );
		this.add( oxygenLbl, OXYGEN_LAYER );

		wallLbl = new JLabel();
		wallLbl.setOpaque(false);
		wallLbl.setBounds( 0, 0, 50, 50 );
		this.add( wallLbl, WALL_LAYER );

		wallLbl.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				for (Map.Entry<Rectangle, Integer> entry : roomRegions.entrySet()) {
					if ( entry.getKey().contains(e.getPoint()) ) {
						SavedGameFloorplanPanel.this.frame.setStatusText( String.format("RoomId: %2d", entry.getValue().intValue()) );
						break;
					}
				}
			}
		});

		this.setBackground( new Color(212, 208, 200) );
		this.setOpaque(true);
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		String prevGfxBaseName = shipGfxBaseName;

		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
		shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );
		shipGfxBaseName = shipState.getShipGraphicsBaseName();
		originX = shipChassis.getImageBounds().x * -1;
		originY = shipChassis.getImageBounds().y * -1;

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gs = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gs.getDefaultConfiguration();

		for (DoorSprite doorSprite : doorSprites)
			this.remove( doorSprite );
		doorSprites.clear();

		for (SystemSprite systemSprite : systemSprites)
			this.remove( systemSprite );
		systemSprites.clear();

		for (BreachSprite breachSprite : breachSprites)
			this.remove( breachSprite );
		breachSprites.clear();

		for (FireSprite fireSprite : fireSprites)
			this.remove( fireSprite );
		fireSprites.clear();

		for (CrewSprite crewSprite : crewSprites)
			this.remove( crewSprite );
		crewSprites.clear();

		if ( shipGfxBaseName != prevGfxBaseName ) {
			// Associate graphical regions with roomIds.
			roomRegions.clear();
			for (int i=0; i < shipLayout.getRoomCount(); i++) {
				EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
				int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
				int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
				int roomX = originX + squareSize * roomLocX;
				int roomY = originY + squareSize * roomLocY;
				int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
				int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();
				roomRegions.put( new Rectangle(roomX, roomY, squaresH*squareSize, squaresV*squareSize), i );
			}

			// Load the fuselage image.
			InputStream in = null;
			try {
				in = DataManager.get().getResourceInputStream("img/ship/"+ shipGfxBaseName +"_base.png");
				BufferedImage baseImage = ImageIO.read( in );
				in.close();
				baseLbl.setIcon( new ImageIcon(baseImage) );
				baseLbl.setSize( new Dimension(baseImage.getWidth(), baseImage.getHeight()) );

			} catch (IOException e) {
				log.error( "Failed to load ship base image ("+ shipGfxBaseName +")", e );

			} finally {
				try {if (in != null) in.close();}
				catch (IOException f) {}
	    }

			// Load the interior image.
			try {
				in = DataManager.get().getResourceInputStream("img/ship/"+ shipGfxBaseName +"_floor.png");
				BufferedImage floorImage = ImageIO.read( in );
				in.close();
				floorLbl.setIcon( new ImageIcon(floorImage) );
				floorLbl.setSize( new Dimension(floorImage.getWidth(), floorImage.getHeight()) );

			} catch (IOException e) {
				log.error( "Failed to load ship floor image ("+ shipGfxBaseName +")", e );

			} finally {
				try {if (in != null) in.close();}
				catch (IOException f) {}
	    }

			// Draw walls and floor crevices.
			BufferedImage wallImage = gc.createCompatibleImage( floorLbl.getIcon().getIconWidth(), floorLbl.getIcon().getIconHeight(), Transparency.BITMASK );
			Graphics2D wallG = (Graphics2D)wallImage.createGraphics();
			drawWalls( wallG, originX, originY, shipState, shipLayout );
			wallG.dispose();
			wallLbl.setIcon( new ImageIcon(wallImage) );
			wallLbl.setSize( new Dimension(wallImage.getWidth(), wallImage.getHeight()) );
		}

		// Draw oxygen.
		BufferedImage oxygenImage = gc.createCompatibleImage( baseLbl.getIcon().getIconWidth(), baseLbl.getIcon().getIconHeight(), Transparency.BITMASK );
		Graphics2D oxygenG = (Graphics2D)oxygenImage.createGraphics();
		drawOxygen( oxygenG, originX, originY, shipState, shipLayout );
		oxygenG.dispose();
		oxygenLbl.setIcon( new ImageIcon(oxygenImage) );
		oxygenLbl.setSize( new Dimension(oxygenImage.getWidth(), oxygenImage.getHeight()) );

		// Associate systems' normal names with their image basenames.
		HashMap<String, String> systemNames = new HashMap<String, String>();
		systemNames.put( ShipBlueprint.SystemList.NAME_PILOT, "pilot" );
		systemNames.put( ShipBlueprint.SystemList.NAME_DOORS, "doors" );
		systemNames.put( ShipBlueprint.SystemList.NAME_SENSORS, "sensors" );
		systemNames.put( ShipBlueprint.SystemList.NAME_MEDBAY, "medbay" );
		systemNames.put( ShipBlueprint.SystemList.NAME_OXYGEN, "oxygen" );
		systemNames.put( ShipBlueprint.SystemList.NAME_SHIELDS, "shields" );
		systemNames.put( ShipBlueprint.SystemList.NAME_ENGINES, "engines" );
		systemNames.put( ShipBlueprint.SystemList.NAME_WEAPONS, "weapons" );
		systemNames.put( ShipBlueprint.SystemList.NAME_DRONE_CTRL, "drones" );
		systemNames.put( ShipBlueprint.SystemList.NAME_TELEPORTER, "teleporter" );
		systemNames.put( ShipBlueprint.SystemList.NAME_CLOAKING, "cloaking" );
		systemNames.put( ShipBlueprint.SystemList.NAME_ARTILLERY, "artillery" );

		for (Map.Entry<String, String> entry : systemNames.entrySet()) {
			int[] roomIds = shipBlueprint.getSystemList().getRoomIdBySystemName( entry.getKey() );
			if ( roomIds != null ) {
				for (int i=0; i < roomIds.length; i++) {
					EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo( roomIds[i] );
					int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
					int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
					int roomX = originX + squareSize * roomLocX;
					int roomY = originY + squareSize * roomLocY;
					int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
					int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();
					int systemX = roomX + tileEdge + squaresH*squareSize/2;
					int systemY = roomY + tileEdge + squaresV*squareSize/2;
					addSystemSprite( systemX, systemY, entry.getValue() );
				}
			}
		}

		// Add breaches
		for (Map.Entry<Point, Integer> entry : shipState.getBreachMap().entrySet()) {
			int breachCoordX = entry.getKey().x-shipLayout.getOffsetX();
			int breachCoordY = entry.getKey().y-shipLayout.getOffsetY();
			int breachX = originX+tileEdge + breachCoordX*squareSize + squareSize/2;
			int breachY = originY+tileEdge + breachCoordY*squareSize + squareSize/2;
			addBreachSprite( breachX, breachY, entry.getValue().intValue() );
		}

		// Add fires.
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + squareSize * roomLocX;
			int roomY = originY + squareSize * roomLocY;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			SavedGameParser.RoomState roomState = shipState.getRoom(i);
			for (int s=0; s < squaresH*squaresV; s++) {
				int fireHealth = roomState.getSquare(s)[0];
				if ( fireHealth > 0 ) {
					int fireX = roomX+tileEdge + (s%squaresH)*squareSize + squareSize/2;
					int fireY = roomY+tileEdge + (s/squaresH)*squareSize + squareSize/2;
					addFireSprite( fireX, fireY, fireHealth );
				}
			}
		}

		// Add crew.
		for (SavedGameParser.CrewState crewState : shipState.getCrewList()) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo( crewState.getRoomId() );
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + squareSize * roomLocX;
			int roomY = originY + squareSize * roomLocY;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			int crewX = roomX + tileEdge + (crewState.getRoomSquare()%squaresH)*squareSize + squareSize/2;
			int crewY = roomY + tileEdge + (crewState.getRoomSquare()/squaresH)*squareSize + squareSize/2;
			addCrewSprite( crewX, crewY, crewState.getRace(), crewState.getName(), crewState.isPlayerControlled(), crewState.isEnemyBoardingDrone() );
		}

		this.revalidate();
		this.repaint();
	}

	private void addDoorSprite( int centerX, int centerY, int level, boolean vertical, boolean open ) {
		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( "img/effects/door_sheet.png" );
			BufferedImage bigImage = ImageIO.read(in);

			BufferedImage[] closedImages = new BufferedImage[3];
			BufferedImage[] openImages = new BufferedImage[3];

			for (int i=0; i < openImages.length; i++) {
				closedImages[i] = bigImage.getSubimage(offsetX, offsetY+i*h, w, h);
				openImages[i] = bigImage.getSubimage(offsetX+4*w, offsetY+i*h, w, h);
			}

			DoorSprite doorSprite = new DoorSprite( closedImages, openImages, level, vertical, open );
			doorSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			doorSprites.add( doorSprite );
			this.add( doorSprite, DOOR_LAYER );

		} catch (RasterFormatException e) {
			log.error( "Failed to load and crop door images (door_sheet)", e );
		} catch (IOException e) {
			log.error( "Failed to load and crop door images (door_sheet)", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	private void addSystemSprite( int centerX, int centerY, String overlayBaseName ) {
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( "img/icons/s_"+ overlayBaseName +"_overlay.png" );
			BufferedImage overlayImage = ImageIO.read(in);
			int w = overlayImage.getWidth();
			int h = overlayImage.getHeight();

			// Darken the white icon to gray...
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			BufferedImage canvas = gc.createCompatibleImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = canvas.createGraphics();
			g2d.drawImage(overlayImage, 0, 0, null);
			g2d.dispose();
			RescaleOp op = new RescaleOp(new float[] { 0.49f, 0.49f, 0.49f, 1f }, new float[] { 0, 0, 0, 0 }, null);
			overlayImage = op.filter(canvas, null);

			SystemSprite systemSprite = new SystemSprite( overlayImage );
			systemSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			systemSprites.add( systemSprite );
			this.add( systemSprite, SYSTEM_LAYER );

		} catch (IOException e) {
			log.error( "Failed to load system overlay image ("+ overlayBaseName +")", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	private void addBreachSprite( int centerX, int centerY, int health ) {
		int offsetX = 0, offsetY = 0, w = 19, h = 19;
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( "img/effects/breach.png" );
			BufferedImage bigImage = ImageIO.read(in);
			BufferedImage breachImage = bigImage.getSubimage(offsetX+6*w, offsetY, w, h);

			BreachSprite breachSprite = new BreachSprite( breachImage, health );
			breachSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			breachSprites.add( breachSprite );
			this.add( breachSprite, BREACH_LAYER );

		} catch (RasterFormatException e) {
			log.error( "Failed to load and crop breach image (breach)", e );
		} catch (IOException e) {
			log.error( "Failed to load and crop breach image (breach)", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	private void addFireSprite( int centerX, int centerY, int health ) {
		int offsetX = 0, offsetY = 0, w = 32, h = 32;
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( "img/effects/fire_L1_strip8.png" );
			BufferedImage bigImage = ImageIO.read(in);
			BufferedImage fireImage = bigImage.getSubimage(offsetX, offsetY, w, h);

			FireSprite fireSprite = new FireSprite( fireImage, health );
			fireSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			fireSprites.add( fireSprite );
			this.add( fireSprite, FIRE_LAYER );

		} catch (RasterFormatException e) {
			log.error( "Failed to load and crop fire image (fire_L1_strip8)", e );
		} catch (IOException e) {
			log.error( "Failed to load and crop fire image (fire_L1_strip8)", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	private void addCrewSprite( int centerX, int centerY, String race, String name, boolean playerControlled, boolean enemyBoardingDrone ) {
		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		String suffix = "";
		InputStream in = null;
		try {
			if ( enemyBoardingDrone ) {
				race = "battle";
				suffix = "_enemy_sheet";
			} else if ( playerControlled ) {
				suffix = "_player_yellow";
			} else {
				suffix = "_enemy_red";
			}
			in = DataManager.get().getResourceInputStream( "img/people/"+ race + suffix +".png" );
			BufferedImage bigImage = ImageIO.read(in);
			BufferedImage crewImage = bigImage.getSubimage(offsetX, offsetY, w, h);

			CrewSprite crewSprite = new CrewSprite( crewImage );
			crewSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			crewSprites.add( crewSprite );
			this.add( crewSprite, CREW_LAYER );
			crewSprite.addMouseListener( new StatusbarMouseListener(frame, name) );

		} catch (RasterFormatException e) {
			log.error( "Failed to load and crop race image ("+ race + suffix +")", e );
		} catch (IOException e) {
			log.error( "Failed to load and crop race image ("+ race + suffix +")", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	/** Relocates a JComponent within its parent's space. */
	private void placeSprite( int centerX, int centerY, JComponent sprite ) {
		Dimension spriteSize = sprite.getSize();
		sprite.setLocation( centerX-spriteSize.width/2, centerY-spriteSize.height/2 );
	}

	/** Draws each room's walls, door openings, and floor crevices. */
	private void drawWalls( Graphics2D wallG, int originX, int originY, SavedGameParser.ShipState shipState, ShipLayout shipLayout ) {
		int doorLevel = shipState.getSystem("Doors").getCapacity()-1;  // Convert to 0-based.

		Color prevColor = wallG.getColor();
		Stroke prevStroke = wallG.getStroke();
		Color floorCrackColor = new Color(125, 125, 125);
		Stroke floorCrackStroke = new BasicStroke(1);
		Color roomBorderColor = new Color(15, 15, 15);
		Stroke roomBorderStroke = new BasicStroke(3);
		Color closedDoorColor = new Color(250, 150, 50);
		Stroke closedDoorStroke = new BasicStroke(5);
		SavedGameParser.DoorState doorState = null;
		int fromX, fromY, toX, toY;

		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + squareSize * roomLocX;
			int roomY = originY + squareSize * roomLocY;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			// Draw floor lines within rooms.
			wallG.setColor( floorCrackColor );
			wallG.setStroke( floorCrackStroke );
			for (int n=1; n <= squaresV-1; n++)  // H lines.
				wallG.drawLine( roomX+1, roomY+n*squareSize, roomX+squaresH*squareSize-1, roomY+n*squareSize );
			for (int n=1; n <= squaresH-1; n++)  // V lines.
				wallG.drawLine( roomX+n*squareSize, roomY+1, roomX+n*squareSize, roomY+squaresV*squareSize-1 );

			// Draw borders around rooms.
			for (int n=1; n <= squaresV; n++) {  // V lines.
				// West side.
				fromX = roomX;
				fromY = roomY+(n-1)*squareSize;
				toX = roomX;
				toY = roomY+n*squareSize;
				doorState = shipState.getDoor(roomLocX, roomLocY+n-1, 1);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, fromY+(toY-fromY)/8 );
				  wallG.drawLine( fromX, fromY+(toY-fromY)/8*7, toX, toY );

					addDoorSprite( fromX+tileEdge, fromY+tileEdge+(toY-fromY)/2, doorLevel, true, doorState.isOpen() );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}

				// East Side.
				fromX = roomX+squaresH*squareSize;
				fromY = roomY+(n-1)*squareSize;
				toX = roomX+squaresH*squareSize;
				toY = roomY+n*squareSize;
				doorState = shipState.getDoor(roomLocX+squaresH, roomLocY+n-1, 1);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, fromY+(toY-fromY)/8 );
				  wallG.drawLine( fromX, fromY+(toY-fromY)/8*7, toX, toY );

					addDoorSprite( fromX+tileEdge, fromY+tileEdge+(toY-fromY)/2, doorLevel, true, doorState.isOpen() );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}
			}

			wallG.setStroke( roomBorderStroke );
			wallG.setColor( roomBorderColor );
			for (int n=1; n <= squaresH; n++) {  // H lines.
				// North side.
				fromX = roomX+(n-1)*squareSize;
				fromY = roomY;
				toX = roomX+n*squareSize;
				toY = roomY;
				doorState = shipState.getDoor(roomLocX+n-1, roomLocY, 0);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, fromX+(toX-fromX)/8, fromY );
				  wallG.drawLine( fromX+(toX-fromX)/8*7, fromY, toX, toY );

					addDoorSprite( fromX+tileEdge+(toX-fromX)/2, fromY+tileEdge, doorLevel, false, doorState.isOpen() );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}

				// South side.
				fromX = roomX+(n-1)*squareSize;
				fromY = roomY+squaresV*squareSize;
				toX = roomX+n*squareSize;
				toY = roomY+squaresV*squareSize;
				doorState = shipState.getDoor(roomLocX+n-1, roomLocY+squaresV, 0);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, fromX+(toX-fromX)/8, fromY );
				  wallG.drawLine( fromX+(toX-fromX)/8*7, fromY, toX, toY );

					addDoorSprite( fromX+tileEdge+(toX-fromX)/2, fromY+tileEdge, doorLevel, false, doorState.isOpen() );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}
			}
		}
		wallG.setColor( prevColor );
		wallG.setStroke( prevStroke );
	}

	/** Draws each room in shades of red to indicate oxygen levels. */
	private void drawOxygen( Graphics2D oxygenG, int originX, int originY, SavedGameParser.ShipState shipState, ShipLayout shipLayout ) {
		Color prevColor = oxygenG.getColor();
		Stroke prevStroke = oxygenG.getStroke();
		Color maxColor = new Color( 230, 226, 219 );
		Color minColor = new Color( 255, 176, 169 );
		Color vacuumBorderColor = new Color(255, 180, 0);

		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + squareSize * roomLocX;
			int roomY = originY + squareSize * roomLocY;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();
			int oxygen = shipState.getRoom(i).getOxygen();

			if ( oxygen == 100 ) {
				oxygenG.setColor( maxColor );
			} else if ( oxygen == 0 ) {
				oxygenG.setColor( minColor );
			} else {
				int p = oxygen / 100;
				int maxRed = maxColor.getRed();
				int maxGreen = maxColor.getGreen();
				int maxBlue = maxColor.getBlue();
				int minRed = minColor.getRed();
				int minGreen = minColor.getGreen();
				int minBlue = minColor.getBlue();
				Color partialColor = new Color( minRed+p*(maxRed-minRed), minGreen+p*(maxGreen-minGreen), minBlue+p*(maxBlue-minBlue) );
				oxygenG.setColor( partialColor );
			}
			oxygenG.fillRect( roomX, roomY, squaresH*squareSize, squaresV*squareSize );

			if ( oxygen == 0 ) {  // Draw the yellow border.
				oxygenG.setColor( vacuumBorderColor );
				oxygenG.drawRect( roomX+2, roomY+2, squaresH*squareSize-4, squaresV*squareSize-4 );
				oxygenG.drawRect( roomX+3, roomY+3, squaresH*squareSize-6, squaresV*squareSize-6 );
			}
		}

		oxygenG.setColor( prevColor );
		oxygenG.setStroke( prevStroke );
	}



	public class DoorSprite extends JComponent {
		private BufferedImage[] closedImages;
		private BufferedImage[] openImages;
		private int level;
		private boolean vertical;
		private boolean open;

		public DoorSprite( BufferedImage[] closedImages, BufferedImage[] openImages, int level, boolean vertical, boolean open ) {
			this.closedImages = closedImages;
			this.openImages = openImages;
			this.level = level;
			this.vertical = vertical;
			this.open = open;
			this.setOpaque(false);
		}

		public void setLevel( int n ) { level = n; }
		public int getLevel() { return level; }
		public void setVertical( boolean b ) { vertical = b; }
		public boolean isVertical() { return vertical; }
		public void setOpen( boolean b ) { open = b; }
		public boolean isOpen() { return open; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			int w = this.getSize().width, h = this.getSize().height;

			if ( !vertical ) {  // Use rotated coordinates to draw AS IF vertical.
				g2d.rotate( Math.toRadians(90) );   // Clockwise.
				w = this.getSize().height; h = this.getSize().width;
				g2d.translate( 0, -(h-1) );
				//g2d.translate( (w-1)/2, (h-1)/2 );  // Down+Right (Right+Up in that perspective).
			}

			BufferedImage doorImage;
			if ( open )
				doorImage = openImages[level];
			else
				doorImage = closedImages[level];

			g2d.drawImage( doorImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class SystemSprite extends JComponent {
		private BufferedImage overlayImage;

		public SystemSprite( BufferedImage overlayImage ) {
			this.overlayImage = overlayImage;
			this.setOpaque(false);
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( overlayImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class BreachSprite extends JComponent {
		private BufferedImage breachImage;
		private int health;

		public BreachSprite( BufferedImage breachImage, int health ) {
			this.breachImage = breachImage;
			this.health = health;
			this.setOpaque(false);
		}

		public void setHealth( int n ) { health = n; }
		public int getHealth() { return health; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( breachImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class FireSprite extends JComponent {
		private BufferedImage fireImage;
		private int health;

		public FireSprite( BufferedImage fireImage, int health ) {
			this.fireImage = fireImage;
			this.health = health;
			this.setOpaque(false);
		}

		public void setHealth( int n ) { health = n; }
		public int getHealth() { return health; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( fireImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class CrewSprite extends JComponent {
		private BufferedImage crewImage;

		public CrewSprite( BufferedImage crewImage ) {
			this.crewImage = crewImage;
			this.setOpaque(false);
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( crewImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}
}
