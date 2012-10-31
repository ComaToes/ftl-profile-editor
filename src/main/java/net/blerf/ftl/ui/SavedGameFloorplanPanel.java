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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipChassis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameFloorplanPanel extends JPanel {

	private static final Integer BASE_LAYER = new Integer(10);
	private static final Integer FLOOR_LAYER = new Integer(11);
	private static final Integer OXYGEN_LAYER = new Integer(12);
	private static final Integer WALL_LAYER = new Integer(15);
	private static final Integer SYSTEM_LAYER = new Integer(16);
	private static final Integer BREACH_LAYER = new Integer(17);
	private static final Integer FIRE_LAYER = new Integer(18);
	private static final Integer DOOR_LAYER = new Integer(30);
	private static final Integer CREW_LAYER = new Integer(40);
	private static final Integer SQUARE_SELECTION_LAYER = new Integer(50);
	private static final int squareSize = 35, tileEdge = 1;
	private static final Logger log = LogManager.getLogger(SavedGameFloorplanPanel.class);

	private FTLFrame frame;

	private ShipBlueprint shipBlueprint = null;
	private ShipLayout shipLayout = null;
	private ShipChassis shipChassis = null;
	private String shipGfxBaseName = null;
	private int originX=0, originY=0;
	private HashMap<Rectangle, Integer> roomRegions = new HashMap<Rectangle, Integer>();
	private HashMap<Rectangle, Integer> squareRegions = new HashMap<Rectangle, Integer>();
	private ArrayList<RoomSprite> roomSprites = new ArrayList<RoomSprite>();
	private ArrayList<SystemSprite> systemSprites = new ArrayList<SystemSprite>();
	private ArrayList<BreachSprite> breachSprites = new ArrayList<BreachSprite>();
	private ArrayList<FireSprite> fireSprites = new ArrayList<FireSprite>();
	private ArrayList<DoorSprite> doorSprites = new ArrayList<DoorSprite>();
	private ArrayList<CrewSprite> crewSprites = new ArrayList<CrewSprite>();

	private JLayeredPane shipPanel = null;
	private JLabel baseLbl = null;
	private JLabel floorLbl = null;
	private JLabel wallLbl = null;
	private JLabel crewLbl = null;
	private SquareSelector squareSelector = null;

	public SavedGameFloorplanPanel( FTLFrame frame ) {
		super( new GridBagLayout() );
		this.frame = frame;

		shipPanel = new JLayeredPane();
		shipPanel.setBackground( new Color(212, 208, 200) );
		shipPanel.setOpaque(true);
		shipPanel.setPreferredSize( new Dimension(50, 50) );

		baseLbl = new JLabel();
		baseLbl.setOpaque(false);
		baseLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( baseLbl, BASE_LAYER );

		floorLbl = new JLabel();
		floorLbl.setOpaque(false);
		floorLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( floorLbl, FLOOR_LAYER );

		wallLbl = new JLabel();
		wallLbl.setOpaque(false);
		wallLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( wallLbl, WALL_LAYER );

		squareSelector = new SquareSelector( roomRegions, squareRegions );
		squareSelector.setOpaque(false);
		squareSelector.setBounds( 0, 0, 50, 50 );
		shipPanel.add( squareSelector, SQUARE_SELECTION_LAYER );

		MouseInputAdapter squareListener = new MouseInputAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				squareSelector.setMousePoint( e.getX(), e.getY() );
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				// Left-click triggers callback. Other buttons cancel.

				if ( e.getButton() == MouseEvent.BUTTON1 ) {
					if ( !squareSelector.isCurrentSquareValid() ) return;
					boolean keepSelecting = false;
					SquareSelectionCallback callback = squareSelector.getCallback();
					if ( callback != null )
						keepSelecting = callback.squareSelected( squareSelector, squareSelector.getRoomId(), squareSelector.getSquareId() );
					if ( keepSelecting == false )
						squareSelector.reset();
				}
				else if ( e.getButton() != MouseEvent.NOBUTTON ) {
					squareSelector.reset();
				}
			}
			@Override
			public void mouseExited(MouseEvent e) {
				squareSelector.setMousePoint( -1, -1 );
			}
		};
		squareSelector.addMouseListener( squareListener );
		squareSelector.addMouseMotionListener( squareListener );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout(ctrlPanel, BoxLayout.X_AXIS) );
		ctrlPanel.add( new JLabel("Select: ") );
		JButton selectRoomBtn = new JButton("Room");
		ctrlPanel.add( selectRoomBtn );
		ctrlPanel.add( Box.createHorizontalStrut(5) );
		JButton selectCrewBtn = new JButton("Crew");
		ctrlPanel.add( selectCrewBtn );
		ctrlPanel.add( Box.createHorizontalStrut(5) );
		JButton selectBreachBtn = new JButton("Breach");
		ctrlPanel.add( selectBreachBtn );
		ctrlPanel.add( Box.createHorizontalStrut(5) );
		JButton selectFireBtn = new JButton("Fire");
		ctrlPanel.add( selectFireBtn );

		ctrlPanel.add( Box.createHorizontalStrut(15) );
		ctrlPanel.add( new JLabel("Reset: ") );
		JButton resetOxygenBtn = new JButton("Oxygen");
		ctrlPanel.add( resetOxygenBtn );
		JButton resetBreachesBtn = new JButton("Breaches");
		ctrlPanel.add( resetBreachesBtn );
		JButton resetFiresBtn = new JButton("Fires");
		ctrlPanel.add( resetFiresBtn );

		selectRoomBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectRoom();
			}
		});
		selectCrewBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectCrew();
			}
		});
		selectBreachBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectBreach();
			}
		});
		selectFireBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectFire();
			}
		});

		resetOxygenBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (RoomSprite roomSprite : roomSprites) {
					if ( roomSprite.getOxygen() != 100 ) {
						roomSprite.setOxygen(100);
					}
				}
				shipPanel.repaint();
			}
		});

		resetBreachesBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (BreachSprite breachSprite : breachSprites)
					shipPanel.remove( breachSprite );
				breachSprites.clear();
				shipPanel.repaint();
			}
		});

		resetFiresBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (FireSprite fireSprite : fireSprites)
					shipPanel.remove( fireSprite );
				fireSprites.clear();
				shipPanel.repaint();
			}
		});

		GridBagConstraints gridC = new GridBagConstraints();

		gridC.fill = GridBagConstraints.BOTH;
		gridC.weightx = 1.0;
		gridC.weighty = 1.0;
		gridC.gridx = 0;
		gridC.gridy = 0;
		JScrollPane shipScroll = new JScrollPane( shipPanel );
		shipScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		shipScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		this.add( shipScroll, gridC );

		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.weightx = 1.0;
		gridC.weighty = 0.0;
		gridC.insets = new Insets(4, 4, 4, 4);
		gridC.gridx = 0;
		gridC.gridy++;
		this.add( ctrlPanel, gridC );
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		String prevGfxBaseName = shipGfxBaseName;
		squareSelector.reset();

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

		for (RoomSprite roomSprite : roomSprites)
			shipPanel.remove( roomSprite );
		roomSprites.clear();

		for (SystemSprite systemSprite : systemSprites)
			shipPanel.remove( systemSprite );
		systemSprites.clear();

		for (BreachSprite breachSprite : breachSprites)
			shipPanel.remove( breachSprite );
		breachSprites.clear();

		for (FireSprite fireSprite : fireSprites)
			shipPanel.remove( fireSprite );
		fireSprites.clear();

		for (DoorSprite doorSprite : doorSprites)
			shipPanel.remove( doorSprite );
		doorSprites.clear();

		for (CrewSprite crewSprite : crewSprites)
			shipPanel.remove( crewSprite );
		crewSprites.clear();

		if ( shipGfxBaseName != prevGfxBaseName ) {
			// Associate graphical regions with roomIds and squares.
			roomRegions.clear();
			squareRegions.clear();
			for (int i=0; i < shipLayout.getRoomCount(); i++) {
				EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
				int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
				int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
				int roomX = originX + squareSize * roomLocX;
				int roomY = originY + squareSize * roomLocY;
				int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
				int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

				for (int j=0; j < squaresH*squaresV; j++) {
					int squareX = roomX + tileEdge + (j%squaresH)*squareSize;
					int squareY = roomY + tileEdge + (j/squaresH)*squareSize;
					Rectangle squareRect = new Rectangle(squareX, squareY, squareSize, squareSize);
					roomRegions.put( squareRect, i );
					squareRegions.put( squareRect, j );
				}
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
		}

		// Add doors and draw walls and floor crevices.
		BufferedImage wallImage = gc.createCompatibleImage( floorLbl.getIcon().getIconWidth(), floorLbl.getIcon().getIconHeight(), Transparency.BITMASK );
		Graphics2D wallG = (Graphics2D)wallImage.createGraphics();
		drawWalls( wallG, originX, originY, shipState, shipLayout );
		wallG.dispose();
		wallLbl.setIcon( new ImageIcon(wallImage) );
		wallLbl.setSize( new Dimension(wallImage.getWidth(), wallImage.getHeight()) );

		// Add oxygen.
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + squareSize * roomLocX;
			int roomY = originY + squareSize * roomLocY;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();
			int oxygen = shipState.getRoom(i).getOxygen();

			RoomSprite roomSprite = new RoomSprite( i, oxygen );
			roomSprite.setBounds( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
			roomSprites.add( roomSprite );
			shipPanel.add( roomSprite, OXYGEN_LAYER );
		}

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
		for (Map.Entry<Point, Integer> breachEntry : shipState.getBreachMap().entrySet()) {
			int breachCoordX = breachEntry.getKey().x-shipLayout.getOffsetX();
			int breachCoordY = breachEntry.getKey().y-shipLayout.getOffsetY();
			int breachX = originX+tileEdge + breachCoordX*squareSize + squareSize/2;
			int breachY = originY+tileEdge + breachCoordY*squareSize + squareSize/2;

			Rectangle squareRect = null;
			int roomId = -1;
			int squareId = -1;
			for (Map.Entry<Rectangle, Integer> regionEntry : roomRegions.entrySet()) {
				if ( regionEntry.getKey().contains( breachX, breachY ) ) {
					squareRect = regionEntry.getKey();
					roomId = regionEntry.getValue().intValue();
					break;
				}
			}
			if ( squareRegions.containsKey(squareRect) )
				squareId = squareRegions.get(squareRect).intValue();

			addBreachSprite( breachX, breachY, roomId, squareId, breachEntry.getValue().intValue() );
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
					addFireSprite( fireX, fireY, i, s, fireHealth );
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
			addCrewSprite( crewX, crewY, crewState );
		}

		squareSelector.setSize( floorLbl.getIcon().getIconWidth(), floorLbl.getIcon().getIconHeight() );

		shipPanel.setPreferredSize( new Dimension(floorLbl.getIcon().getIconWidth(), floorLbl.getIcon().getIconHeight()) );

		shipPanel.revalidate();
		shipPanel.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
		shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );

		// Oxygen.
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			SavedGameParser.RoomState roomState = shipState.getRoom(i);
			roomState.setOxygen( roomSprites.get(i).getOxygen() );
		}

		// Breaches.
		Map<Point, Integer> breachMap = shipState.getBreachMap();
		breachMap.clear();
		for (BreachSprite breachSprite : breachSprites) {
			int roomId = breachSprite.getRoomId();
			int squareId = breachSprite.getSquareId();

			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo( roomId );
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			int breachX = roomLocX + squareId%squaresH + shipLayout.getOffsetX();
			int breachY = roomLocY + squareId/squaresV + shipLayout.getOffsetY();
			breachMap.put( new Point(breachX, breachY), new Integer(breachSprite.getHealth()) );
		}

		// Fires.
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			SavedGameParser.RoomState roomState = shipState.getRoom(i);
			for (int[] squareState : roomState.getSquareList())
				squareState[0] = 0;
		}
		for (FireSprite fireSprite : fireSprites) {
			SavedGameParser.RoomState roomState = shipState.getRoom( fireSprite.getRoomId() );
			int [] squareState = roomState.getSquareList().get( fireSprite.getSquareId() );
			squareState[0] = fireSprite.getHealth();
		}
	}

	private void selectRoom() {
		squareSelector.reset();
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				int oxygen = roomSprites.get( roomId ).getOxygen();
				SavedGameFloorplanPanel.this.frame.setStatusText( String.format("RoomId: %2d, Square: %d, Oxygen: %d%%", roomId, squareId, oxygen) );
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void selectCrew() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				for (CrewSprite crewSprite : crewSprites) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				for (CrewSprite crewSprite : crewSprites) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						SavedGameFloorplanPanel.this.frame.setStatusText( String.format("Name: %s", crewSprite.getName()) );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void selectBreach() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				for (BreachSprite breachSprite : breachSprites) {
					if ( breachSprite.getRoomId() == roomId && breachSprite.getSquareId() == squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				for (BreachSprite breachSprite : breachSprites) {
					if ( breachSprite.getRoomId() == roomId && breachSprite.getSquareId() == squareId ) {
						SavedGameFloorplanPanel.this.frame.setStatusText( String.format("Breach HP: %d", breachSprite.getHealth()) );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void selectFire() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				for (FireSprite fireSprite : fireSprites) {
					if ( fireSprite.getRoomId() == roomId && fireSprite.getSquareId() == squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				for (FireSprite fireSprite : fireSprites) {
					if ( fireSprite.getRoomId() == roomId && fireSprite.getSquareId() == squareId ) {
						SavedGameFloorplanPanel.this.frame.setStatusText( String.format("Fire HP: %d", fireSprite.getHealth()) );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);
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
			shipPanel.add( doorSprite, DOOR_LAYER );

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
			shipPanel.add( systemSprite, SYSTEM_LAYER );

		} catch (IOException e) {
			log.error( "Failed to load system overlay image ("+ overlayBaseName +")", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	private void addBreachSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 19, h = 19;
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( "img/effects/breach.png" );
			BufferedImage bigImage = ImageIO.read(in);
			BufferedImage breachImage = bigImage.getSubimage(offsetX+6*w, offsetY, w, h);

			BreachSprite breachSprite = new BreachSprite( breachImage, roomId, squareId, health );
			breachSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			breachSprites.add( breachSprite );
			shipPanel.add( breachSprite, BREACH_LAYER );

		} catch (RasterFormatException e) {
			log.error( "Failed to load and crop breach image (breach)", e );
		} catch (IOException e) {
			log.error( "Failed to load and crop breach image (breach)", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	private void addFireSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 32, h = 32;
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( "img/effects/fire_L1_strip8.png" );
			BufferedImage bigImage = ImageIO.read(in);
			BufferedImage fireImage = bigImage.getSubimage(offsetX, offsetY, w, h);

			FireSprite fireSprite = new FireSprite( fireImage, roomId, squareId, health );
			fireSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			fireSprites.add( fireSprite );
			shipPanel.add( fireSprite, FIRE_LAYER );

		} catch (RasterFormatException e) {
			log.error( "Failed to load and crop fire image (fire_L1_strip8)", e );
		} catch (IOException e) {
			log.error( "Failed to load and crop fire image (fire_L1_strip8)", e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	private void addCrewSprite( int centerX, int centerY, SavedGameParser.CrewState crewState ) {
		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		String race = crewState.getRace();
		String suffix = "";
		InputStream in = null;
		try {
			if ( crewState.isEnemyBoardingDrone() ) {
				race = "battle";
				suffix = "_enemy_sheet";
			} else if ( crewState.isPlayerControlled() ) {
				suffix = "_player_yellow";
			} else {
				suffix = "_enemy_red";
			}
			in = DataManager.get().getResourceInputStream( "img/people/"+ race + suffix +".png" );
			BufferedImage bigImage = ImageIO.read(in);
			BufferedImage crewImage = bigImage.getSubimage(offsetX, offsetY, w, h);

			CrewSprite crewSprite = new CrewSprite( crewImage, crewState );
			crewSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
			crewSprites.add( crewSprite );
			shipPanel.add( crewSprite, CREW_LAYER );
			//crewSprite.addMouseListener( new StatusbarMouseListener(frame, name) );

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



	public class RoomSprite extends JComponent {
		private final Color maxColor = new Color( 230, 226, 219 );
		private final Color minColor = new Color( 255, 176, 169 );
		private final Color vacuumBorderColor = new Color(255, 180, 0);

		private int roomId;
		private int oxygen;
		private Color bgColor;

		public RoomSprite( int roomId, int oxygen ) {
			this.roomId = roomId;
			setOxygen( oxygen );
			this.setOpaque(true);
		}

		public void setRoomId( int n ) { roomId = n; }
		public int getRoomId() { return roomId; }

		public void setOxygen( int n ) {
			oxygen = n;
			if ( oxygen == 100 ) {
				bgColor = maxColor;
			} else if ( oxygen == 0 ) {
				bgColor = minColor;
			} else {
				double p = oxygen / 100.0;
				int maxRed = maxColor.getRed();
				int maxGreen = maxColor.getGreen();
				int maxBlue = maxColor.getBlue();
				int minRed = minColor.getRed();
				int minGreen = minColor.getGreen();
				int minBlue = minColor.getBlue();
				bgColor = new Color( (int)(minRed+p*(maxRed-minRed)), (int)(minGreen+p*(maxGreen-minGreen)), (int)(minBlue+p*(maxBlue-minBlue)) );
			}
		}
		public int getOxygen() { return oxygen; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();

			g2d.setColor( bgColor );
			g2d.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1);

			if ( oxygen == 0 ) {  // Draw the yellow border.
				g2d.setColor( vacuumBorderColor );
				g2d.drawRect( 2, 2, (this.getWidth()-1)-4, (this.getHeight()-1)-4 );
				g2d.drawRect( 3, 3, (this.getWidth()-1)-6, (this.getHeight()-1)-6 );
			}

			g2d.setColor(prevColor);
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
		private int roomId;
		private int squareId;
		private int health;

		public BreachSprite( BufferedImage breachImage, int roomId, int squareId, int health ) {
			this.breachImage = breachImage;
			this.roomId = roomId;
			this.squareId = squareId;
			this.health = health;
			this.setOpaque(false);
		}

		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }
		public void setHealth( int n ) { health = n; }

		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }
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
		private int roomId;
		private int squareId;
		private int health;

		public FireSprite( BufferedImage fireImage, int roomId, int squareId, int health ) {
			this.fireImage = fireImage;
			this.roomId = roomId;
			this.squareId = squareId;
			this.health = health;
			this.setOpaque(false);
		}

		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }
		public void setHealth( int n ) { health = n; }

		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }
		public int getHealth() { return health; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( fireImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
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



	public class CrewSprite extends JComponent {
		private BufferedImage crewImage;
		private String name;
		private int roomId;
		private int squareId;

		public CrewSprite( BufferedImage crewImage, SavedGameParser.CrewState crewState ) {
			this.crewImage = crewImage;
			name = crewState.getName();
			roomId = crewState.getRoomId();
			squareId = crewState.getRoomSquare();
			this.setOpaque(false);
		}

		public void setName( String s ) { name = s; }
		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }

		public String getName() { return name; }
		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( crewImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	/**
	 * A glasspane-like layer to select squares.
	 *
	 * Usage: setVisible(), setCriteria(), setCallback()
	 * To cancel selection, call reset();
	 */
	public class SquareSelector extends JComponent {
		private SquareCriteria defaultCriteria = new SquareCriteria();

		private HashMap<Rectangle, Integer> roomRegions;
		private HashMap<Rectangle, Integer> squareRegions;
		private SquareCriteria squareCriteria = defaultCriteria;
		private SquareSelectionCallback callback = null;
		private Point mousePoint = new Point( -1, -1 );
		private Rectangle currentRect = null;

		public SquareSelector( HashMap<Rectangle, Integer> roomRegions, HashMap<Rectangle, Integer> squareRegions ) {
			this.roomRegions = roomRegions;
			this.squareRegions = squareRegions;
		}

		public void setMousePoint( int x, int y ) {
			if ( mousePoint.x != x || mousePoint.y != y) {
				mousePoint.x = x;
				mousePoint.y = y;

				Rectangle newRect = null;
				if ( mousePoint.x > 0 && mousePoint.y > 0 ) {
					for (Map.Entry<Rectangle, Integer> entry : squareRegions.entrySet()) {
						if ( entry.getKey().contains( mousePoint ) ) {
							newRect = entry.getKey();
							break;
						}
					}
				}
				if ( newRect != currentRect ) {
					if ( currentRect != null ) this.repaint( currentRect );
					currentRect = newRect;
					if ( currentRect != null ) this.repaint( currentRect );
				}
			}
		}

		public int getRoomId() {
			int roomId = -1;
			if ( roomRegions.containsKey( currentRect ) )
				roomId = roomRegions.get( currentRect ).intValue();

			return roomId;
		}

		public int getSquareId() {
			int squareId = -1;
			if ( squareRegions.containsKey( currentRect ) )
				squareId = squareRegions.get( currentRect ).intValue();

			return squareId;
		}

		public Point getSquareCenter() {
			Point result = null;
			if ( currentRect != null ) {
				int centerX = currentRect.x + currentRect.width/2;
				int centerY = currentRect.y + currentRect.height/2;
				result = new Point( centerX, centerY );
			}
			return result;
		}

		/** Sets the logic which decides square color and selectability. */
		public void setCriteria( SquareCriteria sc ) {
			if (sc != null)
				squareCriteria = sc;
			else
				squareCriteria = defaultCriteria;
		}

		public boolean isCurrentSquareValid() {
			return squareCriteria.isSquareValid( getRoomId(), getSquareId() );
		}

		public void setCallback( SquareSelectionCallback cb ) {
			callback = cb;
		}
		public SquareSelectionCallback getCallback() {
			return callback;
		}

		public void reset() {
			this.setVisible(false);
			setCriteria(null);
			setCallback(null);
			setMousePoint( -1, -1 );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();

			if ( currentRect != null ) {
				Color squareColor = squareCriteria.getSquareColor( getRoomId(), getSquareId() );
				if ( squareColor != null ) {
					g2d.setColor( squareColor );
					g2d.drawRect( currentRect.x, currentRect.y, (currentRect.width-1), (currentRect.height-1) );
					g2d.drawRect( currentRect.x+1, currentRect.y+1, (currentRect.width-1)-2, (currentRect.height-1)-2 );
					g2d.drawRect( currentRect.x+2, currentRect.y+2, (currentRect.width-1)-4, (currentRect.height-1)-4 );
				}
			}

			g2d.setColor( prevColor );
		}
	}



	public class SquareCriteria {
		private Color validColor = Color.GREEN.darker();
		private Color invalidColor = Color.RED.darker();

		/** Returns a highlight color when hovering over a square, or null for none. */
		public Color getSquareColor( int roomId, int squareId ) {
			if ( roomId < 0 || squareId < 0 ) return null;
			if ( isSquareValid(roomId, squareId) )
				return validColor;
			else
				return invalidColor;
		}

		/** Returns true if a square can be selected, false otherwise. */
		public boolean isSquareValid( int roomId, int squareId ) {
			if ( roomId < 0 || squareId < 0 ) return false;
			return true;
		}
	}


	public interface SquareSelectionCallback {
		/** Responds to a clicked square, returning true to continue selecting. */
		public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId );
	}
}
