package net.blerf.ftl.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import javax.swing.JSeparator;
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
	private static final Integer DECOR_LAYER = new Integer(13);
	private static final Integer WALL_LAYER = new Integer(15);
	private static final Integer SYSTEM_LAYER = new Integer(16);
	private static final Integer BREACH_LAYER = new Integer(17);
	private static final Integer FIRE_LAYER = new Integer(18);
	private static final Integer CREW_LAYER = new Integer(30);
	private static final Integer DOOR_LAYER = new Integer(40);
	private static final Integer SQUARE_SELECTION_LAYER = new Integer(50);
	private static final int squareSize = 35, tileEdge = 1;
	private static final Logger log = LogManager.getLogger(SavedGameFloorplanPanel.class);

	private GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	private GraphicsDevice gs = ge.getDefaultScreenDevice();
	private GraphicsConfiguration gc = gs.getDefaultConfiguration();

	private FTLFrame frame;

	private ShipBlueprint shipBlueprint = null;
	private ShipLayout shipLayout = null;
	private ShipChassis shipChassis = null;
	private String shipGfxBaseName = null;
	private int originX=0, originY=0;
	private HashMap<Rectangle, Integer> roomRegions = new HashMap<Rectangle, Integer>();
	private HashMap<Rectangle, Integer> squareRegions = new HashMap<Rectangle, Integer>();
	private ArrayList<Rectangle> blockedRegions = new ArrayList<Rectangle>();
	private ArrayList<JComponent> roomDecorations = new ArrayList<JComponent>();
	private ArrayList<RoomSprite> roomSprites = new ArrayList<RoomSprite>();
	private ArrayList<SystemSprite> systemSprites = new ArrayList<SystemSprite>();
	private ArrayList<BreachSprite> breachSprites = new ArrayList<BreachSprite>();
	private ArrayList<FireSprite> fireSprites = new ArrayList<FireSprite>();
	private ArrayList<DoorSprite> doorSprites = new ArrayList<DoorSprite>();
	private ArrayList<CrewSprite> crewSprites = new ArrayList<CrewSprite>();
	private HashMap<String, HashMap<Rectangle, BufferedImage>> cachedImages = new HashMap<String, HashMap<Rectangle, BufferedImage>>();

	private JLayeredPane shipPanel = null;
	private JPanel sidePanel = null;
	private JScrollPane sideScroll = null;

	private JLabel baseLbl = null;
	private JLabel floorLbl = null;
	private JLabel wallLbl = null;
	private JLabel crewLbl = null;
	private SquareSelector squareSelector = null;
	private MouseListener doorSelectListener = null;

	public SavedGameFloorplanPanel( FTLFrame frame ) {
		super( new BorderLayout() );
		this.frame = frame;

		shipPanel = new JLayeredPane();
		shipPanel.setBackground( new Color(212, 208, 200) );
		shipPanel.setOpaque(true);
		shipPanel.setPreferredSize( new Dimension(50, 50) );

		sidePanel = new JPanel();
		sidePanel.setLayout( new BoxLayout(sidePanel, BoxLayout.Y_AXIS) );
		sidePanel.setBorder( BorderFactory.createEmptyBorder(4, 4, 4, 6) );

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

		Insets ctrlInsets = new Insets(3, 4, 3, 4);

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new BoxLayout(selectPanel, BoxLayout.X_AXIS) );
		selectPanel.setBorder( BorderFactory.createTitledBorder("Select") );
		final JButton selectRoomBtn = new JButton("Room");
		selectRoomBtn.setMargin(ctrlInsets);
		selectPanel.add( selectRoomBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectCrewBtn = new JButton("Crew");
		selectCrewBtn.setMargin(ctrlInsets);
		selectPanel.add( selectCrewBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectBreachBtn = new JButton("Breach");
		selectBreachBtn.setMargin(ctrlInsets);
		selectPanel.add( selectBreachBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectFireBtn = new JButton("Fire");
		selectFireBtn.setMargin(ctrlInsets);
		selectPanel.add( selectFireBtn );

		JPanel addPanel = new JPanel();
		addPanel.setLayout( new BoxLayout(addPanel, BoxLayout.X_AXIS) );
		addPanel.setBorder( BorderFactory.createTitledBorder("Add") );
		final JButton addCrewBtn = new JButton("Crew");
		addCrewBtn.setMargin(ctrlInsets);
		addPanel.add( addCrewBtn );
		addPanel.add( Box.createHorizontalStrut(5) );
		final JButton addBreachBtn = new JButton("Breach");
		addBreachBtn.setMargin(ctrlInsets);
		addPanel.add( addBreachBtn );
		addPanel.add( Box.createHorizontalStrut(5) );
		final JButton addFireBtn = new JButton("Fire");
		addFireBtn.setMargin(ctrlInsets);
		addPanel.add( addFireBtn );

		JPanel resetPanel = new JPanel();
		resetPanel.setLayout( new BoxLayout(resetPanel, BoxLayout.X_AXIS) );
		resetPanel.setBorder( BorderFactory.createTitledBorder("Reset") );
		final JButton resetOxygenBtn = new JButton("Oxygen");
		resetOxygenBtn.setMargin(ctrlInsets);
		resetPanel.add( resetOxygenBtn );
		resetPanel.add( Box.createHorizontalStrut(5) );
		final JButton resetIntrudersBtn = new JButton("Intruders");
		resetIntrudersBtn.setMargin(ctrlInsets);
		resetPanel.add( resetIntrudersBtn );
		resetPanel.add( Box.createHorizontalStrut(5) );
		final JButton resetBreachesBtn = new JButton("Breaches");
		resetBreachesBtn.setMargin(ctrlInsets);
		resetPanel.add( resetBreachesBtn );
		resetPanel.add( Box.createHorizontalStrut(5) );
		final JButton resetFiresBtn = new JButton("Fires");
		resetFiresBtn.setMargin(ctrlInsets);
		resetPanel.add( resetFiresBtn );

		JPanel ctrlRowOnePanel = new JPanel();
		ctrlRowOnePanel.setLayout( new BoxLayout(ctrlRowOnePanel, BoxLayout.X_AXIS) );
		ctrlRowOnePanel.add( selectPanel );
		ctrlRowOnePanel.add( Box.createHorizontalStrut(15) );
		ctrlRowOnePanel.add( addPanel );

		JPanel ctrlRowTwoPanel = new JPanel();
		ctrlRowTwoPanel.setLayout( new BoxLayout(ctrlRowTwoPanel, BoxLayout.X_AXIS) );
		ctrlRowTwoPanel.add( resetPanel );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS) );
		ctrlPanel.add( ctrlRowOnePanel );
		ctrlPanel.add( Box.createVerticalStrut(8) );
		ctrlPanel.add( ctrlRowTwoPanel );

		ActionListener ctrlListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				if ( source == selectRoomBtn ) {
					selectRoom();
				} else if (source == selectCrewBtn ) {
					selectCrew();
				} else if (source == selectBreachBtn ) {
					selectBreach();
				} else if (source == selectFireBtn ) {
					selectFire();
				} else if (source == addCrewBtn ) {
					addCrew();
				} else if (source == addBreachBtn ) {
					addBreach();
				} else if (source == addFireBtn ) {
					addFire();
				} else if (source == resetOxygenBtn ) {
					for (RoomSprite roomSprite : roomSprites) {
						if ( roomSprite.getOxygen() != 100 ) {
							roomSprite.setOxygen(100);
						}
					}
					shipPanel.repaint();

				} else if (source == resetIntrudersBtn ) {
					for (ListIterator<CrewSprite> it = crewSprites.listIterator(); it.hasNext(); ) {
						CrewSprite crewSprite = it.next();
						if ( !crewSprite.isPlayerControlled() ) {
							shipPanel.remove( crewSprite );
							it.remove();
						}
					}
					shipPanel.repaint();

				} else if (source == resetBreachesBtn ) {
					for (BreachSprite breachSprite : breachSprites)
						shipPanel.remove( breachSprite );
					breachSprites.clear();
					shipPanel.repaint();

				} else if (source == resetFiresBtn ) {
					for (FireSprite fireSprite : fireSprites)
						shipPanel.remove( fireSprite );
					fireSprites.clear();
					shipPanel.repaint();
				}
			}
		};

		selectRoomBtn.addActionListener( ctrlListener );
		selectCrewBtn.addActionListener( ctrlListener );
		selectBreachBtn.addActionListener( ctrlListener );
		selectFireBtn.addActionListener( ctrlListener );

		addCrewBtn.addActionListener( ctrlListener );
		addBreachBtn.addActionListener( ctrlListener );
		addFireBtn.addActionListener( ctrlListener );

		resetOxygenBtn.addActionListener( ctrlListener );
		resetIntrudersBtn.addActionListener( ctrlListener );
		resetBreachesBtn.addActionListener( ctrlListener );
		resetFiresBtn.addActionListener( ctrlListener );

		resetOxygenBtn.addMouseListener( new StatusbarMouseListener(frame, "Set all rooms' oxygen to 100%.") );
		resetIntrudersBtn.addMouseListener( new StatusbarMouseListener(frame, "Remove all NPC crew.") );
		resetBreachesBtn.addMouseListener( new StatusbarMouseListener(frame, "Remove all breaches.") );
		resetFiresBtn.addMouseListener( new StatusbarMouseListener(frame, "Remove all fires.") );

		JPanel centerPanel = new JPanel( new GridBagLayout() );

		GridBagConstraints gridC = new GridBagConstraints();

		gridC.fill = GridBagConstraints.BOTH;
		gridC.weightx = 1.0;
		gridC.weighty = 1.0;
		gridC.gridx = 0;
		gridC.gridy = 0;
		JScrollPane shipScroll = new JScrollPane( shipPanel );
		shipScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		shipScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		centerPanel.add( shipScroll, gridC );

		gridC.insets = new Insets(4, 4, 4, 4);

		gridC.anchor = GridBagConstraints.CENTER;
		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.weightx = 1.0;
		gridC.weighty = 0.0;
		gridC.gridx = 0;
		gridC.gridy++;
		centerPanel.add( ctrlPanel, gridC );

		this.add( centerPanel, BorderLayout.CENTER );

		sideScroll = new JScrollPane( sidePanel );
		sideScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		sideScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		sideScroll.setVisible( false );
		this.add( sideScroll, BorderLayout.EAST );

		doorSelectListener = new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				Object source = e.getSource();
				if ( source instanceof DoorSprite ) {
					((DoorSprite)source).setSelectionRectVisible(true);
					((DoorSprite)source).repaint();
				}
			}
			public void mouseExited(MouseEvent e) {
				Object source = e.getSource();
				if ( source instanceof DoorSprite ) {
					((DoorSprite)source).setSelectionRectVisible(false);
					((DoorSprite)source).repaint();
				}
			}
			public void mouseClicked(MouseEvent e) {
				Object source = e.getSource();
				if ( source instanceof DoorSprite )
					showDoorEditor( (DoorSprite)source );
			}
		};
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		String prevGfxBaseName = shipGfxBaseName;
		squareSelector.reset();
		clearSidePanel();

		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
		shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );
		shipGfxBaseName = shipState.getShipGraphicsBaseName();
		originX = shipChassis.getImageBounds().x * -1;
		originY = shipChassis.getImageBounds().y * -1;
		ShipBlueprint.SystemList blueprintSystems = shipBlueprint.getSystemList();

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
			// Find squares that don't allow crew in them (medbay's slot).
			blockedRegions.clear();
			ShipBlueprint.SystemList.SystemRoom medicalSystem = blueprintSystems.getMedicalRoom();
			if ( medicalSystem != null ) {
				ShipBlueprint.SystemList.RoomSlot medicalSlot = medicalSystem.getSlot();
				if ( medicalSlot != null ) {
					int badRoomId = medicalSystem.getRoomId();
					int badSquareId = medicalSlot.getNumber();
					if ( badSquareId >= 0 ) {
						log.trace(String.format("Found a blocked region: roomId: %2d, squareId: %d", badRoomId, badSquareId) );

						EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(badRoomId);
						int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
						int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
						int roomX = originX + squareSize * roomLocX;
						int roomY = originY + squareSize * roomLocY;
						int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
						int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

						int squareX = roomX + tileEdge + (badSquareId%squaresH)*squareSize;
						int squareY = roomY + tileEdge + (badSquareId/squaresH)*squareSize;
						Rectangle squareRect = new Rectangle(squareX, squareY, squareSize, squareSize);
						blockedRegions.add( squareRect );
					}
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

			for (JComponent roomDecor : roomDecorations)
				shipPanel.remove( roomDecor );
			roomDecorations.clear();
			for (ShipBlueprint.SystemList.SystemRoom systemRoom : blueprintSystems.getSystemRooms()) {
				String roomImgPath = systemRoom.getImg();

				int roomId = systemRoom.getRoomId();
				EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(roomId);
				int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
				int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
				int roomX = originX + squareSize * roomLocX;
				int roomY = originY + squareSize * roomLocY;
				int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
				int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

				if ( roomImgPath != null ) {
					// Gotta scale because Zoltan #2's got a tall Doors image for a wide room. :/
					BufferedImage decorImage = getScaledImage( "img/ship/interior/"+ roomImgPath +".png", squaresH*squareSize, squaresV*squareSize );
					JLabel decorLbl = new JLabel( new ImageIcon(decorImage) );
					decorLbl.setOpaque(false);
					decorLbl.setBounds( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
					roomDecorations.add( decorLbl );
					shipPanel.add( decorLbl, DECOR_LAYER );
				}

				if ( systemRoom == blueprintSystems.getTeleporterRoom() ) {
					for (int s=0; s < squaresH*squaresV; s++) {
						int decorX = roomX + (s%squaresH)*squareSize + squareSize/2;
						int decorY = roomY + (s/squaresH)*squareSize + squareSize/2;

						BufferedImage decorImage = getScaledImage( "img/ship/interior/teleporter_off.png", 20, 20 );
						JLabel decorLbl = new JLabel( new ImageIcon(decorImage) );
						decorLbl.setOpaque(false);
						decorLbl.setSize( squaresH*squareSize, squaresV*squareSize );
						placeSprite( decorX, decorY, decorLbl );
						roomDecorations.add( decorLbl );
						shipPanel.add( decorLbl, DECOR_LAYER );
					}
				}
			}
		}

		// Draw walls and floor crevices.
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

		// Add doors.
		int doorLevel = shipState.getSystem("Doors").getCapacity()-1;  // Convert to 0-based.
		for (Map.Entry<ShipLayout.DoorCoordinate, SavedGameParser.DoorState> entry : shipState.getDoorMap().entrySet()) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();
			SavedGameParser.DoorState doorState = entry.getValue();
			int doorX = originX + doorCoord.x*squareSize + (doorCoord.v==1 ? 0 : squareSize/2);
			int doorY = originY + doorCoord.y*squareSize + (doorCoord.v==1 ? squareSize/2 : 0);

			addDoorSprite( doorX, doorY, doorLevel, doorCoord, doorState );
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

		// Doors.
		Map<ShipLayout.DoorCoordinate, SavedGameParser.DoorState> shipDoorMap = shipState.getDoorMap();
		shipDoorMap.clear();
		for (DoorSprite doorSprite : doorSprites) {
			SavedGameParser.DoorState doorState = new SavedGameParser.DoorState();
			doorState.setOpen( doorSprite.isOpen() );
			doorState.setWalkingThrough( doorSprite.isWalkingThrough() );
			shipDoorMap.put( doorSprite.getCoordinate(), doorState );
		}

		// Crew.
		ArrayList<SavedGameParser.CrewState> crewList = shipState.getCrewList();
		crewList.clear();
		for (CrewSprite crewSprite : crewSprites) {
			SavedGameParser.CrewState crewState = new SavedGameParser.CrewState();

			crewState.setName( crewSprite.getName() );
			crewState.setRace( crewSprite.getRace() );
			crewState.setHealth( crewSprite.getHealth() );

			crewState.setPilotSkill( crewSprite.getPilotSkill() );
			crewState.setEngineSkill( crewSprite.getEngineSkill() );
			crewState.setShieldSkill( crewSprite.getShieldSkill() );
			crewState.setWeaponSkill( crewSprite.getWeaponSkill() );
			crewState.setRepairSkill( crewSprite.getRepairSkill() );
			crewState.setCombatSkill( crewSprite.getCombatSkill() );

			int masteries = 0;
			masteries += crewSprite.getPilotSkill() / SavedGameParser.CrewState.MASTERY_INTERVAL_PILOT;
			masteries += crewSprite.getEngineSkill() / SavedGameParser.CrewState.MASTERY_INTERVAL_ENGINE;
			masteries += crewSprite.getShieldSkill() / SavedGameParser.CrewState.MASTERY_INTERVAL_SHIELD;
			masteries += crewSprite.getWeaponSkill() / SavedGameParser.CrewState.MASTERY_INTERVAL_WEAPON;
			masteries += crewSprite.getRepairSkill() / SavedGameParser.CrewState.MASTERY_INTERVAL_REPAIR;
			masteries += crewSprite.getCombatSkill() / SavedGameParser.CrewState.MASTERY_INTERVAL_COMBAT;
			crewState.setSkillMasteries(masteries);

			crewState.setRepairs( crewSprite.getRepairs() );
			crewState.setCombatKills( crewSprite.getCombatKills() );
			crewState.setPilotedEvasions( crewSprite.getPilotedEvasions() );
			crewState.setJumpsSurvived( crewSprite.getJumpsSurvived() );

			crewState.setPlayerControlled( crewSprite.isPlayerControlled() );
			crewState.setEnemyBoardingDrone( crewSprite.isEnemyBoardingDrone() );
			crewState.setMale( crewSprite.isMale() );

			crewState.setRoomId( crewSprite.getRoomId() );
			crewState.setRoomSquare( crewSprite.getSquareId() );
			crewState.setSpriteX( crewSprite.getX()+crewSprite.getImageWidth()/2 - originX - tileEdge + shipLayout.getOffsetX()*squareSize );
			crewState.setSpriteY( crewSprite.getY()+crewSprite.getImageHeight()/2 - originY - tileEdge + shipLayout.getOffsetY()*squareSize );

			crewList.add( crewState );
		}
	}

	private void selectRoom() {
		squareSelector.reset();
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				RoomSprite roomSprite = roomSprites.get( roomId );
				showRoomEditor( roomSprite );
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void selectCrew() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
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
						showCrewEditor( crewSprite );
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
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
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
						showBreachEditor( breachSprite );
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
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
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
						showFireEditor( fireSprite );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void addCrew() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for (CrewSprite crewSprite : crewSprites) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				SavedGameParser.CrewState crewState = new SavedGameParser.CrewState();
				crewState.setRoomId( roomId );
				crewState.setRoomSquare( squareId );
				crewState.setSpriteX( center.x - originX - tileEdge + shipLayout.getOffsetX()*squareSize );
				crewState.setSpriteY( center.y - originY - tileEdge + shipLayout.getOffsetY()*squareSize );
				addCrewSprite( center.x, center.y, crewState );
				shipPanel.repaint();
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void addBreach() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for (BreachSprite breachSprite : breachSprites) {
					if ( breachSprite.getRoomId() == roomId && breachSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				addBreachSprite( center.x, center.y, roomId, squareId, 100 );
				shipPanel.repaint();
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void addFire() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for (FireSprite fireSprite : fireSprites) {
					if ( fireSprite.getRoomId() == roomId && fireSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				addFireSprite( center.x, center.y, roomId, squareId, 100 );
				shipPanel.repaint();
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void moveCrew( final CrewSprite mobileSprite ) {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for (CrewSprite crewSprite : crewSprites) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				placeSprite( center.x, center.y, mobileSprite );
				mobileSprite.setRoomId( roomId );
				mobileSprite.setSquareId( squareId );
				return false;
			}
		});
		squareSelector.setVisible(true);
	}

	/**
	 * Gets a cropped area of an image and caches the result.
	 *
	 * If something goes wrong, a dummy image will be created with
	 * the expected dimensions.
	 */
	private BufferedImage getCroppedImage( String innerPath, int x, int y, int w, int h) {
		Rectangle keyRect = new Rectangle( x, y, w, h );
		BufferedImage result = null;
		HashMap<Rectangle, BufferedImage> cacheMap = cachedImages.get(innerPath);
		if ( cacheMap != null ) result = cacheMap.get(keyRect);
		if (result != null) return result;
		log.trace( "Image not in cache, loading and cropping...: "+ innerPath );

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage bigImage = ImageIO.read(in);
			result = bigImage.getSubimage(x, y, w, h);

		} catch (RasterFormatException e) {
			log.error( "Failed to load and crop image: "+ innerPath, e );
		} catch (IOException e) {
			log.error( "Failed to load and crop image: "+ innerPath, e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}

		if ( result == null ) {  // Guarantee a returned image, with a stand-in.
			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = (Graphics2D)result.createGraphics();
			g2d.setColor( new Color(150, 150, 200) );
			g2d.fillRect( 0, 0, w-1, h-1 );
			g2d.dispose();
		}

		if ( cacheMap == null ) {
			cacheMap = new HashMap<Rectangle, BufferedImage>();
			cachedImages.put( innerPath, cacheMap );
		}
		cacheMap.put( keyRect, result );

		return result;
	}

	/**
	 * Gets an image, scaling if necessary, and caches the result.
	 *
	 * If something goes wrong, a dummy image will be created with
	 * the expected dimensions.
	 *
	 * If the dimensions are negative, the original unscaled image
	 * will be returned if possible, or the absolute values will be
	 * used for the dummy image.
	 */
	private BufferedImage getScaledImage( String innerPath, int w, int h) {
		Rectangle keyRect = new Rectangle( 0, 0, w, h );
		BufferedImage result = null;
		HashMap<Rectangle, BufferedImage> cacheMap = cachedImages.get(innerPath);
		if ( cacheMap != null ) result = cacheMap.get(keyRect);
		if (result != null) return result;
		log.trace( "Image not in cache, loading and scaling...: "+ innerPath );


		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage origImage = ImageIO.read(in);

			if ( w <= 0 || h <= 0 || (origImage.getWidth() == w && origImage.getHeight() == h) ) {
				result = origImage;
			} else {
				BufferedImage scaledImage = new BufferedImage(w, h, Transparency.TRANSLUCENT);
				Graphics2D g2d = scaledImage.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2d.drawImage(origImage, 0, 0, w, h, null);
				g2d.dispose();
				result = scaledImage;
			}

		} catch (RasterFormatException e) {
			log.error( "Failed to load and scale image: "+ innerPath, e );
		} catch (IOException e) {
			log.error( "Failed to load and scale image: "+ innerPath, e );
		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}

		if ( result == null ) {  // Guarantee a returned image, with a stand-in.
			w = Math.abs(w);
			h = Math.abs(h);
			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = (Graphics2D)result.createGraphics();
			g2d.setColor( new Color(150, 150, 200) );
			g2d.fillRect( 0, 0, w-1, h-1 );
			g2d.dispose();
		}

		if ( cacheMap == null ) {
			cacheMap = new HashMap<Rectangle, BufferedImage>();
			cachedImages.put( innerPath, cacheMap );
		}
		cacheMap.put( keyRect, result );

		return result;
	}

	private void addDoorSprite( int centerX, int centerY, int level, ShipLayout.DoorCoordinate doorCoord, SavedGameParser.DoorState doorState ) {
		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		int levelCount = 3;

		// Dom't scale the image, but pass negative size to define the fallback dummy image.
		BufferedImage bigImage = getScaledImage( "img/effects/door_sheet.png", -1*((offsetX+4*w)+w), -1*((offsetY+(levelCount-1)*h)+h) );

		BufferedImage[] closedImages = new BufferedImage[levelCount];
		BufferedImage[] openImages = new BufferedImage[levelCount];

		for (int i=0; i < levelCount; i++) {
			int chop = 10;  // Chop 10 pixels off the sides for skinny doors.
			closedImages[i] = bigImage.getSubimage(offsetX+chop, offsetY+i*h, w-chop*2, h);
			openImages[i] = bigImage.getSubimage(offsetX+4*w+chop, offsetY+i*h, w-chop*2, h);
		}

		DoorSprite doorSprite = new DoorSprite( closedImages, openImages, level, doorCoord, doorState );
		if ( doorCoord.v == 1 )
			doorSprite.setSize( closedImages[level].getWidth(), closedImages[level].getHeight() );
		else
			doorSprite.setSize( closedImages[level].getHeight(), closedImages[level].getWidth() );

		placeSprite( centerX, centerY, doorSprite );
		doorSprites.add( doorSprite );
		shipPanel.add( doorSprite, DOOR_LAYER );

		doorSprite.addMouseListener(doorSelectListener);
	}

	private void addSystemSprite( int centerX, int centerY, String overlayBaseName ) {
		int w = 32, h = 32;
		BufferedImage overlayImage = getScaledImage( "img/icons/s_"+ overlayBaseName +"_overlay.png", w, h );

		// Darken the white icon to gray...
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
	}

	private void addBreachSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 19, h = 19;

		BufferedImage breachImage = getCroppedImage( "img/effects/breach.png", offsetX+6*w, offsetY, w, h );

		BreachSprite breachSprite = new BreachSprite( breachImage, roomId, squareId, health );
		breachSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		breachSprites.add( breachSprite );
		shipPanel.add( breachSprite, BREACH_LAYER );
	}

	private void addFireSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 32, h = 32;

		BufferedImage fireImage = getCroppedImage( "img/effects/fire_L1_strip8.png", offsetX, offsetY, w, h );

		FireSprite fireSprite = new FireSprite( fireImage, roomId, squareId, health );
		fireSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		fireSprites.add( fireSprite );
		shipPanel.add( fireSprite, FIRE_LAYER );
	}

	private void addCrewSprite( int centerX, int centerY, SavedGameParser.CrewState crewState ) {
		CrewSprite crewSprite = new CrewSprite( crewState );
		int w = crewSprite.getImageWidth();
		int h = crewSprite.getImageHeight();
		crewSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		crewSprites.add( crewSprite );
		shipPanel.add( crewSprite, CREW_LAYER );
	}

	/** Relocates a JComponent within its parent's null layout. */
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
		Stroke roomBorderStroke = new BasicStroke(4);
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

	private void showSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension(sideWidth + vbarWidth, 1) );
		sideScroll.setVisible( true );

		this.revalidate();
		this.repaint();
	}

	private void clearSidePanel() {
		sideScroll.setVisible( false );
		sidePanel.removeAll();

		sidePanel.revalidate();
		this.repaint();
	}

	/**
	 * Clears and ropulates the side panel.
	 * Includes a title, arbitrary content, and
	 * cancel/apply buttons.
	 *
	 * More can be added to the side panel manually.
	 *
	 * Afterward, showSidePanel() should be called.
	 */
	private void createSidePanel( String title, final FieldEditorPanel editorPanel, final Runnable applyCallback ) {
		clearSidePanel();
		JLabel titleLbl = new JLabel(title);
		titleLbl.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( titleLbl );
		addSidePanelSeparator(4);

		// Keep the editor from growing and creating gaps around it.
		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		sidePanel.add( editorPanel );

		sidePanel.add( Box.createVerticalStrut(10) );

		JPanel applyPanel = new JPanel();
		applyPanel.setLayout( new BoxLayout(applyPanel, BoxLayout.X_AXIS) );
		JButton cancelBtn = new JButton("Cancel");
		applyPanel.add( cancelBtn );
		applyPanel.add( Box.createRigidArea( new Dimension(15, 1)) );
		JButton applyBtn = new JButton("Apply");
		applyPanel.add( applyBtn );
		sidePanel.add( applyPanel );

		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
			}
		});

		applyBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				applyCallback.run();
			}
		});
	}

	/**
	 * Adds a separator to the side panel.
	 */
	private void addSidePanelSeparator( int spacerSize ) {
		sidePanel.add( Box.createVerticalStrut( spacerSize ) );
		JSeparator newSep = new JSeparator(JSeparator.HORIZONTAL);
		newSep.setMaximumSize( new Dimension(Short.MAX_VALUE, newSep.getPreferredSize().height) );
		sidePanel.add( newSep );
		sidePanel.add( Box.createVerticalStrut( spacerSize ) );
	}

	private void showRoomEditor( final RoomSprite roomSprite ) {
		final String OXYGEN = "Oxygen";

		int roomId = roomSprite.getRoomId();
		String title = String.format("Room %2d", roomId);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( OXYGEN, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(OXYGEN).setMaximum( 100 );
		editorPanel.getSlider(OXYGEN).setValue( roomSprite.getOxygen() );
		sidePanel.add( editorPanel );

		final Runnable applyCallback = new Runnable() {
			public void run() {
				roomSprite.setOxygen( editorPanel.getSlider(OXYGEN).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showBreachEditor( final BreachSprite breachSprite ) {
		final String HEALTH = "Health";

		String title = "Breach";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(HEALTH).setMaximum( 100 );
		editorPanel.getSlider(HEALTH).setValue( breachSprite.getHealth() );
		sidePanel.add( editorPanel );

		final Runnable applyCallback = new Runnable() {
			public void run() {
				breachSprite.setHealth( editorPanel.getSlider(HEALTH).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
				breachSprites.remove( breachSprite );
				shipPanel.remove( breachSprite );
			}
		});

		showSidePanel();
	}

	private void showFireEditor( final FireSprite fireSprite ) {
		final String HEALTH = "Health";

		String title = "Fire";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(HEALTH).setMaximum( 100 );
		editorPanel.getSlider(HEALTH).setValue( fireSprite.getHealth() );
		sidePanel.add( editorPanel );

		final Runnable applyCallback = new Runnable() {
			public void run() {
				fireSprite.setHealth( editorPanel.getSlider(HEALTH).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
				fireSprites.remove( fireSprite );
				shipPanel.remove( fireSprite );
			}
		});

		showSidePanel();
	}

	private void showDoorEditor( final DoorSprite doorSprite ) {
		final String OPEN = "Open";
		final String WALKING_THROUGH = "Walking Through";

		String title = "Door";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( OPEN, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(OPEN).setSelected( doorSprite.isOpen() );
		editorPanel.addRow( WALKING_THROUGH, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(WALKING_THROUGH).setSelected( doorSprite.isWalkingThrough() );
		editorPanel.getBoolean(WALKING_THROUGH).addMouseListener( new StatusbarMouseListener(frame, "Momentarily open as someone walks through.") );

		sidePanel.add( editorPanel );

		final Runnable applyCallback = new Runnable() {
			public void run() {
				doorSprite.setOpen( editorPanel.getBoolean(OPEN).isSelected() );
				doorSprite.setWalkingThrough( editorPanel.getBoolean(WALKING_THROUGH).isSelected() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showCrewEditor( final CrewSprite crewSprite ) {
		final String NAME = "Name";
		final String RACE = "Race";
		final String HEALTH = "Health";
		final String PILOT_SKILL = "Pilot Skill";
		final String ENGINE_SKILL = "Engine Skill";
		final String SHIELD_SKILL = "Shield Skill";
		final String WEAPON_SKILL = "Weapon Skill";
		final String REPAIR_SKILL = "Repair Skill";
		final String COMBAT_SKILL = "Combat Skill";
		final String REPAIRS = "Repairs";
		final String COMBAT_KILLS = "Combat Kills";
		final String PILOTED_EVASIONS = "Piloted Evasions";
		final String JUMPS_SURVIVED = "Jumps Survived";
		final String PLAYER_CONTROLLED = "Player Ctrl";
		final String ENEMY_DRONE = "Enemy Drone";
		final String GENDER = "Male";

		int pilotInterval = SavedGameParser.CrewState.MASTERY_INTERVAL_PILOT;
		int engineInterval = SavedGameParser.CrewState.MASTERY_INTERVAL_ENGINE;
		int shieldInterval = SavedGameParser.CrewState.MASTERY_INTERVAL_SHIELD;
		int weaponInterval = SavedGameParser.CrewState.MASTERY_INTERVAL_WEAPON;
		int repairInterval = SavedGameParser.CrewState.MASTERY_INTERVAL_REPAIR;
		int combatInterval = SavedGameParser.CrewState.MASTERY_INTERVAL_COMBAT;

		int maxHealth = SavedGameParser.CrewState.getMaxHealth( crewSprite.getRace() );

		String title = "Crew";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( NAME, FieldEditorPanel.ContentType.STRING );
		editorPanel.getString(NAME).setText( crewSprite.getName() );
		editorPanel.addRow( RACE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(RACE).addItem("battle");
		editorPanel.getCombo(RACE).addItem("crystal");
		editorPanel.getCombo(RACE).addItem("energy");
		editorPanel.getCombo(RACE).addItem("engi");
		editorPanel.getCombo(RACE).addItem("human");
		editorPanel.getCombo(RACE).addItem("mantis");
		editorPanel.getCombo(RACE).addItem("rock");
		editorPanel.getCombo(RACE).addItem("slug");
		editorPanel.getCombo(RACE).setSelectedItem( crewSprite.getRace() );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(HEALTH).setMaximum( maxHealth );
		editorPanel.getSlider(HEALTH).setValue( crewSprite.getHealth() );
		editorPanel.addRow( PILOT_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(PILOT_SKILL).setMaximum( pilotInterval*2 );
		editorPanel.getSlider(PILOT_SKILL).setValue( crewSprite.getPilotSkill() );
		editorPanel.addRow( ENGINE_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(ENGINE_SKILL).setMaximum( engineInterval*2 );
		editorPanel.getSlider(ENGINE_SKILL).setValue( crewSprite.getEngineSkill() );
		editorPanel.addRow( SHIELD_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(SHIELD_SKILL).setMaximum( shieldInterval*2 );
		editorPanel.getSlider(SHIELD_SKILL).setValue( crewSprite.getShieldSkill() );
		editorPanel.addRow( WEAPON_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(WEAPON_SKILL).setMaximum( weaponInterval*2 );
		editorPanel.getSlider(WEAPON_SKILL).setValue( crewSprite.getWeaponSkill() );
		editorPanel.addRow( REPAIR_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(REPAIR_SKILL).setMaximum( repairInterval*2 );
		editorPanel.getSlider(REPAIR_SKILL).setValue( crewSprite.getRepairSkill() );
		editorPanel.addRow( COMBAT_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(COMBAT_SKILL).setMaximum( combatInterval*2 );
		editorPanel.getSlider(COMBAT_SKILL).setValue( crewSprite.getCombatSkill() );
		editorPanel.addBlankRow();
		editorPanel.addRow( REPAIRS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(REPAIRS).setText( ""+crewSprite.getRepairs() );
		editorPanel.addRow( COMBAT_KILLS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(COMBAT_KILLS).setText( ""+crewSprite.getCombatKills() );
		editorPanel.addRow( PILOTED_EVASIONS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(PILOTED_EVASIONS).setText( ""+crewSprite.getPilotedEvasions() );
		editorPanel.addRow( JUMPS_SURVIVED, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(JUMPS_SURVIVED).setText( ""+crewSprite.getJumpsSurvived() );
		editorPanel.addRow( PLAYER_CONTROLLED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(PLAYER_CONTROLLED).setSelected( crewSprite.isPlayerControlled() );
		editorPanel.getBoolean(PLAYER_CONTROLLED).addMouseListener( new StatusbarMouseListener(frame, "Player controlled vs NPC.") );
		editorPanel.addRow( ENEMY_DRONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(ENEMY_DRONE).setSelected( crewSprite.isEnemyBoardingDrone() );
		editorPanel.getBoolean(ENEMY_DRONE).addMouseListener( new StatusbarMouseListener(frame, "Turn into a boarding drone (clobbering other fields).") );
		editorPanel.addRow( GENDER, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(GENDER).setSelected( crewSprite.isMale() );
		editorPanel.getBoolean(GENDER).addMouseListener( new StatusbarMouseListener(frame, "Only humans can be female (no effect on other races).") );

		sidePanel.add( editorPanel );

		final Runnable applyCallback = new Runnable() {
			public void run() {
				String newString;
				crewSprite.setName( editorPanel.getString(NAME).getText() );
				crewSprite.setRace( (String)editorPanel.getCombo(RACE).getSelectedItem() );
				crewSprite.setHealth( editorPanel.getSlider(HEALTH).getValue() );
				crewSprite.setPilotSkill( editorPanel.getSlider(PILOT_SKILL).getValue() );
				crewSprite.setEngineSkill( editorPanel.getSlider(ENGINE_SKILL).getValue() );
				crewSprite.setShieldSkill( editorPanel.getSlider(SHIELD_SKILL).getValue() );
				crewSprite.setWeaponSkill( editorPanel.getSlider(WEAPON_SKILL).getValue() );
				crewSprite.setRepairSkill( editorPanel.getSlider(REPAIR_SKILL).getValue() );
				crewSprite.setCombatSkill( editorPanel.getSlider(COMBAT_SKILL).getValue() );

				newString = editorPanel.getInt(REPAIRS).getText();
				try { crewSprite.setRepairs( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(COMBAT_KILLS).getText();
				try { crewSprite.setCombatKills( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(PILOTED_EVASIONS).getText();
				try { crewSprite.setPilotedEvasions( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(JUMPS_SURVIVED).getText();
				try { crewSprite.setJumpsSurvived( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				crewSprite.setPlayerControlled( editorPanel.getBoolean(PLAYER_CONTROLLED).isSelected() );
				crewSprite.setEnemyBoardingDrone( editorPanel.getBoolean(ENEMY_DRONE).isSelected() );
				crewSprite.setMale( editorPanel.getBoolean(GENDER).isSelected() );

				crewSprite.makeSane();
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton moveBtn = new JButton("Move To...");
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(moveBtn);

		sidePanel.add( Box.createVerticalStrut(6) );

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		moveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveCrew( crewSprite );
			}
		});

		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
				crewSprites.remove( crewSprite );
				shipPanel.remove( crewSprite );
			}
		});

		showSidePanel();
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
		private ShipLayout.DoorCoordinate doorCoord;
		private boolean open;
		private boolean walkingThrough;

		private Color validColor = Color.GREEN.darker();
		private boolean selectionRectVisible = false;

		public DoorSprite( BufferedImage[] closedImages, BufferedImage[] openImages, int level, ShipLayout.DoorCoordinate doorCoord, SavedGameParser.DoorState doorState ) {
			this.closedImages = closedImages;
			this.openImages = openImages;
			this.level = level;
			this.doorCoord = doorCoord;
			this.open = doorState.isOpen();
			this.walkingThrough = doorState.isWalkingThrough();
			this.setOpaque(false);
		}

		public void setLevel( int n ) { level = n; }
		public void setCoordinate( ShipLayout.DoorCoordinate c ) { doorCoord = c; }
		public void setOpen( boolean b ) { open = b; }
		public void setWalkingThrough( boolean b ) { walkingThrough = b; }

		public int getLevel() { return level; }
		public ShipLayout.DoorCoordinate getCoordinate() { return doorCoord; }
		public boolean isOpen() { return open; }
		public boolean isWalkingThrough() { return walkingThrough; }

		public void setSelectionRectVisible( boolean b ) { selectionRectVisible = b; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();
			int w = this.getSize().width, h = this.getSize().height;

			if ( doorCoord.v == 0 ) {  // Use rotated coordinates to draw AS IF vertical.
				g2d.rotate( Math.toRadians(90) );   // Clockwise.
				w = this.getSize().height; h = this.getSize().width;
				g2d.translate( 0, -(h-1) );
			}

			BufferedImage doorImage;
			if ( open )
				doorImage = openImages[level];
			else
				doorImage = closedImages[level];

			g2d.drawImage( doorImage, 0, 0, this);

			if ( selectionRectVisible ) {
				g2d.setColor( validColor );
				g2d.drawRect( 0, 0, w-1, h-1 );
				g2d.drawRect( 1, 1, (w-1)-2, (h-1)-2 );
				g2d.drawRect( 2, 2, (w-1)-4, (h-1)-4 );
			}

			g2d.setColor( prevColor );
		}
	}



	public class CrewSprite extends JComponent {
		private BufferedImage crewImage;
		private int roomId;
		private int squareId;
		private String name;
		private String race;
		private int health;
		private int pilotSkill, engineSkill, shieldSkill;
		private int weaponSkill, repairSkill, combatSkill;
		private int repairs, combatKills, pilotedEvasions;
		private int jumpsSurvived;
		private boolean playerControlled;
		private boolean enemyBoardingDrone;
		private boolean male;
		private int spriteX;
		private int spriteY;

		public CrewSprite( SavedGameParser.CrewState crewState ) {
			this.crewImage = crewImage;
			roomId = crewState.getRoomId();
			squareId = crewState.getRoomSquare();
			name = crewState.getName();
			race = crewState.getRace();
			health = crewState.getHealth();
			pilotSkill = crewState.getPilotSkill();
			engineSkill = crewState.getEngineSkill();
			shieldSkill = crewState.getShieldSkill();
			weaponSkill = crewState.getWeaponSkill();
			repairSkill = crewState.getRepairSkill();
			combatSkill = crewState.getCombatSkill();
			repairs = crewState.getRepairs();
			combatKills = crewState.getCombatKills();
			pilotedEvasions = crewState.getPilotedEvasions();
			jumpsSurvived = crewState.getJumpsSurvived();
			playerControlled = crewState.isPlayerControlled();
			enemyBoardingDrone = crewState.isEnemyBoardingDrone();
			male = crewState.isMale();
			spriteX = crewState.getSpriteX();
			spriteY = crewState.getSpriteY();
			makeSane();
			this.setOpaque(false);
		}

		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }
		public void setName( String s ) { name = s; }
		public void setRace( String s ) { race = s; }
		public void setHealth( int n ) {health = n; }
		public void setPilotSkill( int n ) {pilotSkill = n; }
		public void setEngineSkill( int n ) {engineSkill = n; }
		public void setShieldSkill( int n ) {shieldSkill = n; }
		public void setWeaponSkill( int n ) {weaponSkill = n; }
		public void setRepairSkill( int n ) {repairSkill = n; }
		public void setCombatSkill( int n ) {combatSkill = n; }
		public void setRepairs( int n ) { repairs = n; }
		public void setCombatKills( int n ) { combatKills = n; }
		public void setPilotedEvasions( int n ) { pilotedEvasions = n; }
		public void setJumpsSurvived( int n ) { jumpsSurvived = n; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public void setEnemyBoardingDrone( boolean b ) { enemyBoardingDrone = b; }
		public void setMale( boolean b ) { male = b; }
		public void setSpriteX( int n ) { spriteX = n; }
		public void setSpriteY( int n ) { spriteY = n; }

		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }
		public String getName() { return name; }
		public String getRace() { return race; }
		public int getHealth() { return health; }
		public int getPilotSkill() { return pilotSkill; }
		public int getEngineSkill() { return engineSkill; }
		public int getShieldSkill() { return shieldSkill; }
		public int getWeaponSkill() { return weaponSkill; }
		public int getRepairSkill() { return repairSkill; }
		public int getCombatSkill() { return combatSkill; }
		public int getRepairs() { return repairs; }
		public int getCombatKills() { return combatKills; }
		public int getPilotedEvasions() { return pilotedEvasions; }
		public int getJumpsSurvived() { return jumpsSurvived; }
		public boolean isPlayerControlled() { return playerControlled; }
		public boolean isEnemyBoardingDrone() { return enemyBoardingDrone; }
		public boolean isMale() { return male; }
		public int getSpriteX() { return spriteX; }
		public int getSpriteY() { return spriteY; }

		public int getImageWidth() { return crewImage.getWidth(); }
		public int getImageHeight() { return crewImage.getHeight(); }

		public void makeSane() {
			if ( isEnemyBoardingDrone() && !getRace().equals("battle") )
				setRace( "battle" );              // The game would do this when loaded.

			if ( isEnemyBoardingDrone() && !getName().equals("Anti-Personnel Drone") )
				setName("Anti-Personnel Drone");  // The game would do this when loaded.

			if ( isEnemyBoardingDrone() && isPlayerControlled() )
				setPlayerControlled( false );     // The game would do this when loaded.

			if ( isEnemyBoardingDrone() && !getRace().equals("battle") )

			if ( getRace().equals("battle") && !isEnemyBoardingDrone() )
				setRace( "human" );               // The game would do this when loaded.

			// Cap the health at the race's max.
			health = Math.min( health, SavedGameParser.CrewState.getMaxHealth(race) );

			// Always same size: no repositioning needed to align image's center with the square's.
			int offsetX = 0, offsetY = 0, w = 35, h = 35;
			String imgRace = race;
			String suffix = "";

			if ( getRace().equals("battle") ) {
				suffix = "_enemy_sheet";
			} else {
				// Only humans can be female. Other races keep the flag but ignore it.
				if ( !isMale() && getRace().equals("human") ) {
					imgRace = "female";  // Never an actual race?
				}

				if ( isPlayerControlled() ) {
					suffix = "_player_yellow";
				} else {
					suffix = "_enemy_red";
				}
			}
			crewImage = getCroppedImage( "img/people/"+ imgRace + suffix +".png", offsetX, offsetY, w, h );
		}

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

		public Rectangle getSquareRectangle() {
			return currentRect;
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
			return squareCriteria.isSquareValid( this, getRoomId(), getSquareId() );
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
				Color squareColor = squareCriteria.getSquareColor( this, getRoomId(), getSquareId() );
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
		public Color getSquareColor( SquareSelector squareSelector, int roomId, int squareId ) {
			if ( roomId < 0 || squareId < 0 ) return null;
			if ( isSquareValid(squareSelector, roomId, squareId) )
				return validColor;
			else
				return invalidColor;
		}

		/** Returns true if a square can be selected, false otherwise. */
		public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
			if ( roomId < 0 || squareId < 0 ) return false;
			return true;
		}
	}


	public interface SquareSelectionCallback {
		/** Responds to a clicked square, returning true to continue selecting. */
		public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId );
	}
}
