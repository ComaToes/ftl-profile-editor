package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.basic.BasicArrowButton;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.SectorMapLayout;
import net.blerf.ftl.ui.SectorMapLayout.SectorMapConstraints;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.ui.hud.SpriteSelector;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteCriteria;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteSelectionCallback;
import net.blerf.ftl.ui.hud.StatusViewport;
import net.blerf.ftl.xml.AugBlueprint;
import net.blerf.ftl.xml.BackgroundImage;
import net.blerf.ftl.xml.BackgroundImageList;
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.Encounters;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.ShipEvent;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameSectorMapPanel extends JPanel {
	// Dimensions for placing beacons' background sprite images.
	private static final int SCREEN_WIDTH = 1280;
	private static final int SCREEN_HEIGHT = 720;

	// mapHolderPanel Layers
	private static final Integer MAP_LAYER = new Integer(10);
	private static final Integer MISC_SELECTION_LAYER = new Integer(50);
	// mapPanel Layers
	private static final Integer BEACON_LAYER = new Integer(10);
	private static final Integer MISC_BOX_LAYER = new Integer(15);
	private static final Integer SHIP_LAYER = new Integer(30);
	private static final Integer CTRL_LAYER = new Integer(50);
	private static final Logger log = LogManager.getLogger(SavedGameSectorMapPanel.class);

	private FTLFrame frame;

	private ArrayList<BeaconSprite> beaconSprites = new ArrayList<BeaconSprite>();
	private ArrayList<StoreSprite> storeSprites = new ArrayList<StoreSprite>();
	private ArrayList<QuestSprite> questSprites = new ArrayList<QuestSprite>();
	private ArrayList<PlayerShipSprite> playerShipSprites = new ArrayList<PlayerShipSprite>();

	private HashMap<String, BufferedImage> cachedImages = new HashMap<String, BufferedImage>();

	private SectorMapLayout mapLayout = null;
	private JPanel mapPanel = null;
	private JLayeredPane mapHolderPanel = null;
	private StatusViewport mapViewport = null;
	private JPanel sidePanel = null;
	private JScrollPane sideScroll = null;

	private SpriteSelector miscSelector = null;
	private ActionListener columnCtrlListener = null;

	public SavedGameSectorMapPanel( FTLFrame frame ) {
		super( new BorderLayout() );
		this.frame = frame;

		mapHolderPanel = new JLayeredPane();

		mapLayout = new SectorMapLayout();
		mapPanel = new JPanel( mapLayout );
		mapPanel.setBackground( Color.BLACK );
		mapPanel.setOpaque( true );
		mapPanel.setSize( mapPanel.getPreferredSize() );
		mapHolderPanel.add( mapPanel, MAP_LAYER );

		sidePanel = new JPanel();
		sidePanel.setLayout( new BoxLayout(sidePanel, BoxLayout.Y_AXIS) );
		sidePanel.setBorder( BorderFactory.createEmptyBorder(4, 4, 4, 6) );

		miscSelector = new SpriteSelector( new ArrayList[] {} );
		miscSelector.setOpaque(false);
		miscSelector.setSize( mapPanel.getPreferredSize() );
		mapHolderPanel.add( miscSelector, MISC_SELECTION_LAYER );

		MouseInputAdapter miscListener = new MouseInputAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				miscSelector.setMousePoint( e.getX(), e.getY() );
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				// Left-click triggers callback. Other buttons cancel.

				if ( e.getButton() == MouseEvent.BUTTON1 ) {
					if ( !miscSelector.isCurrentSpriteValid() ) return;
					boolean keepSelecting = false;
					SpriteSelectionCallback callback = miscSelector.getCallback();
					if ( callback != null )
						keepSelecting = callback.spriteSelected( miscSelector, miscSelector.getSprite() );
					if ( keepSelecting == false )
						miscSelector.reset();
				}
				else if ( e.getButton() != MouseEvent.NOBUTTON ) {
					miscSelector.reset();
				}
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				//miscSelector.setDescriptionVisible( true );
				mapViewport.setStatusString( miscSelector.getCriteria().getDescription() );
			}
			@Override
			public void mouseExited(MouseEvent e) {
				//miscSelector.setDescriptionVisible( false );
				mapViewport.setStatusString( null );
				miscSelector.setMousePoint( -1, -1 );
			}
		};
		miscSelector.addMouseListener( miscListener );
		miscSelector.addMouseMotionListener( miscListener );

		Insets ctrlInsets = new Insets(3, 4, 3, 4);

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new BoxLayout(selectPanel, BoxLayout.X_AXIS) );
		selectPanel.setBorder( BorderFactory.createTitledBorder("Select") );
		final JButton selectBeaconBtn = new JButton("Beacon");
		selectBeaconBtn.setMargin(ctrlInsets);
		selectPanel.add( selectBeaconBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectPlayerShipBtn = new JButton("Player Ship");
		selectPlayerShipBtn.setMargin(ctrlInsets);
		selectPanel.add( selectPlayerShipBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectStoreBtn = new JButton("Store");
		selectStoreBtn.setMargin(ctrlInsets);
		selectPanel.add( selectStoreBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectQuestBtn = new JButton("Quest");
		selectQuestBtn.setMargin(ctrlInsets);
		selectPanel.add( selectQuestBtn );

		JPanel addPanel = new JPanel();
		addPanel.setLayout( new BoxLayout(addPanel, BoxLayout.X_AXIS) );
		addPanel.setBorder( BorderFactory.createTitledBorder("Add") );
		final JButton addStoreBtn = new JButton("Store");
		addStoreBtn.setMargin(ctrlInsets);
		addPanel.add( addStoreBtn );
		addPanel.add( Box.createHorizontalStrut(5) );
		final JButton addQuestBtn = new JButton("Quest");
		addQuestBtn.setMargin(ctrlInsets);
		addPanel.add( addQuestBtn );

		JPanel ctrlRowOnePanel = new JPanel();
		ctrlRowOnePanel.setLayout( new BoxLayout(ctrlRowOnePanel, BoxLayout.X_AXIS) );
		ctrlRowOnePanel.add( selectPanel );
		ctrlRowOnePanel.add( Box.createHorizontalStrut(15) );
		ctrlRowOnePanel.add( addPanel );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS) );
		ctrlPanel.add( ctrlRowOnePanel );
		//ctrlPanel.add( Box.createVerticalStrut(8) );

		ActionListener ctrlListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//if ( shipBlueprint == null ) return;  // No map to edit!

				Object source = e.getSource();
				if ( source == selectBeaconBtn ) {
					selectBeacon();
				}
				else if ( source == selectPlayerShipBtn ) {
					selectPlayerShip();
				}
				else if ( source == selectStoreBtn ) {
					selectStore();
				}
				else if ( source == selectQuestBtn ) {
					selectQuest();
				}
				else if ( source == addStoreBtn ) {
					addStore();
				}
				else if ( source == addQuestBtn ) {
					addQuest();
				}
			}
		};

		selectBeaconBtn.addActionListener( ctrlListener );
		selectPlayerShipBtn.addActionListener( ctrlListener );
		selectStoreBtn.addActionListener( ctrlListener );
		selectQuestBtn.addActionListener( ctrlListener );

		addStoreBtn.addActionListener( ctrlListener );
		addQuestBtn.addActionListener( ctrlListener );

		JPanel centerPanel = new JPanel( new GridBagLayout() );

		GridBagConstraints gridC = new GridBagConstraints();

		mapViewport = new StatusViewport();

		gridC.fill = GridBagConstraints.BOTH;
		gridC.weightx = 1.0;
		gridC.weighty = 1.0;
		gridC.gridx = 0;
		gridC.gridy = 0;
		JScrollPane mapScroll = new JScrollPane();
		mapScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		mapScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		mapScroll.setViewport( mapViewport );
		mapScroll.setViewportView( mapHolderPanel );
		centerPanel.add( mapScroll, gridC );

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

		JPanel borderPanel = new JPanel( new BorderLayout() );
		borderPanel.setBorder( BorderFactory.createTitledBorder("Sector Map") );


		//JLabel noticeLbl = new JLabel("The number of beacons in each column can't be determined automatically.");
		//noticeLbl.setHorizontalAlignment( SwingConstants.CENTER );
		//borderPanel.add( noticeLbl, BorderLayout.NORTH );

		columnCtrlListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();

				BasicArrowButton srcBtn = (BasicArrowButton)source;
				IncrementBox iBox = (IncrementBox)(srcBtn).getParent();
				int column = mapLayout.getCtrlColumn( iBox );
				if ( column != -1 ) {
					int colSize = mapLayout.getColumnSize( column );
					if ( srcBtn == iBox.decBtn )
						mapLayout.setColumnSize( column, colSize-1 );
					else if ( srcBtn == iBox.incBtn )
						mapLayout.setColumnSize( column, colSize+1 );

					mapPanel.revalidate();
					mapViewport.repaint();
				}
			}
		};
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		mapPanel.removeAll();
		miscSelector.setVisible( false );
		miscSelector.setMousePoint( -1, -1 );
		mapViewport.setStatusString( null );
		clearSidePanel();

		for ( BeaconSprite beaconSprite : beaconSprites )
			mapPanel.remove( beaconSprite );
		beaconSprites.clear();

		for ( StoreSprite storeSprite : storeSprites )
			mapPanel.remove( storeSprite );
		storeSprites.clear();

		for ( QuestSprite questSprite : questSprites )
			mapPanel.remove( questSprite );
		questSprites.clear();

		for ( PlayerShipSprite playerShipSprite : playerShipSprites )
			mapPanel.remove( playerShipSprite );
		playerShipSprites.clear();

		if ( gameState == null ) {
			mapPanel.revalidate();
			mapViewport.repaint();
			return;
		}

		int beaconId;
		List<SavedGameParser.BeaconState> beaconStateList = gameState.getBeaconList();

		// Beacons.
		for ( SavedGameParser.BeaconState beaconState : beaconStateList ) {
			BeaconSprite beaconSprite = new BeaconSprite( beaconState );
			SectorMapConstraints beaconC = new SectorMapConstraints( SectorMapConstraints.BEACON );
			beaconSprites.add( beaconSprite );
			mapPanel.add( beaconSprite, beaconC );
		}

		// Stores.
		beaconId = 0;
		for ( SavedGameParser.BeaconState beaconState : beaconStateList ) {
			if ( beaconState.isStorePresent() ) {
				StoreSprite storeSprite = new StoreSprite( beaconState.getStore() );
				SectorMapConstraints storeC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
				storeC.setBeaconId( beaconId );
				storeSprites.add( storeSprite );
				mapPanel.add( storeSprite, storeC );
			}
			beaconId++;
		}

		// Quests.
		for ( Map.Entry<String, Integer> entry : gameState.getQuestEventMap().entrySet() ) {
			String questEventId = entry.getKey();
			int questBeaconId = entry.getValue().intValue();

			QuestSprite questSprite = new QuestSprite( questEventId );
			SectorMapConstraints questC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
			questC.setBeaconId( questBeaconId );
			questSprites.add( questSprite );
			mapPanel.add( questSprite, questC );
		}

		// Player ship.
		PlayerShipSprite playerShipSprite = new PlayerShipSprite();
		SectorMapConstraints playerShipC = new SectorMapConstraints( SectorMapConstraints.PLAYER_SHIP );
		playerShipC.setBeaconId( gameState.getCurrentBeaconId() );
		playerShipSprites.add( playerShipSprite );
		mapPanel.add( playerShipSprite, playerShipC );

		// Add column controls.
		for (int i=0; i < 6; i++) {
			IncrementBox iBox = new IncrementBox();
			iBox.decBtn.addActionListener( columnCtrlListener );
			iBox.incBtn.addActionListener( columnCtrlListener );
			mapPanel.add( iBox, new SectorMapConstraints( SectorMapConstraints.COLUMN_CTRL ) );

			String message = "Adjust the number of beacons in this column.";
			iBox.addMouseListener( new StatusbarMouseListener(frame, message) );
			iBox.decBtn.addMouseListener( new StatusbarMouseListener(frame, message) );
			iBox.incBtn.addMouseListener( new StatusbarMouseListener(frame, message) );
		}

		mapPanel.revalidate();
		mapViewport.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		if ( gameState == null ) return;

		List<SavedGameParser.BeaconState> beaconStateList = gameState.getBeaconList();

		// Player ship.
		for ( PlayerShipSprite playerShipSprite : playerShipSprites ) {
			int beaconId = mapLayout.getBeaconId( playerShipSprite );
			if ( beaconId != -1 ) {
				gameState.setCurrentBeaconId( beaconId );
			}
		}

		// Reset Beacon states.
		for ( SavedGameParser.BeaconState beaconState : beaconStateList ) {
			beaconState.setVisited( false );
			beaconState.setBgStarscapeImageInnerPath( null );
			beaconState.setBgSpriteImageInnerPath( null );
			beaconState.setBgSpritePosX( -1 );
			beaconState.setBgSpritePosY( -1 );
			beaconState.setBgSpriteRotation( 0 );

			beaconState.setSeen( false );

			beaconState.setEnemyPresent( false );
			beaconState.setShipEventId( null );
			beaconState.setAutoBlueprintId( null );
			beaconState.setBeta( 0 );

			beaconState.setStorePresent( false );
			beaconState.setStore( null );
		}

		// Beacons.
		for ( BeaconSprite beaconSprite : beaconSprites) {
			int beaconId = mapLayout.getBeaconId( beaconSprite );
			if ( beaconId > 0 && beaconId < beaconStateList.size() ) {
				SavedGameParser.BeaconState beaconState = beaconStateList.get( beaconId );

				if ( beaconSprite.isVisited() ) {
					beaconState.setVisited( true );
					beaconState.setBgStarscapeImageInnerPath( beaconSprite.getBgStarscapeImageInnerPath() );
					beaconState.setBgSpriteImageInnerPath( beaconSprite.getBgSpriteImageInnerPath() );
					beaconState.setBgSpritePosX( beaconSprite.getBgSpritePosX() );
					beaconState.setBgSpritePosY( beaconSprite.getBgSpritePosY() );
					beaconState.setBgSpriteRotation( beaconSprite.getBgSpriteRotation() );
				}

				beaconState.setSeen( beaconSprite.isSeen() );

				if ( beaconSprite.isEnemyPresent() ) {
					beaconState.setEnemyPresent( true );
					beaconState.setShipEventId( beaconSprite.getShipEventId() );
					beaconState.setAutoBlueprintId( beaconSprite.getAutoBlueprintId() );
					beaconState.setBeta( beaconSprite.getBeta() );
				}

				beaconState.setFleetPresence( beaconSprite.getFleetPresence() );
				beaconState.setUnderAttack( beaconSprite.isUnderAttack() );
			}
		}

		// Stores.
		for ( StoreSprite storeSprite : storeSprites ) {
			int beaconId = mapLayout.getBeaconId( storeSprite );
			if ( beaconId > 0 && beaconId < beaconStateList.size() ) {
				SavedGameParser.StoreState storeState = new SavedGameParser.StoreState();
				storeState.setFuel( storeSprite.getFuel() );
				storeState.setMissiles( storeSprite.getMissiles() );
				storeState.setDroneParts( storeSprite.getDroneParts() );

				SavedGameParser.StoreShelf tmpShelf;
				SavedGameParser.StoreShelf topShelf = new SavedGameParser.StoreShelf();
				tmpShelf = storeSprite.getTopShelf();
				topShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					topShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}
				SavedGameParser.StoreShelf bottomShelf = new SavedGameParser.StoreShelf();
				tmpShelf = storeSprite.getBottomShelf();
				bottomShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					bottomShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}

				storeState.setTopShelf( topShelf );
				storeState.setBottomShelf( bottomShelf );

				SavedGameParser.BeaconState beaconState = beaconStateList.get( beaconId );
				beaconState.setStore( storeState );
				beaconState.setStorePresent( true );
			}
		}

		// Quests.
		gameState.getQuestEventMap().clear();
		for ( QuestSprite questSprite : questSprites ) {
			String questId = questSprite.getQuestId();
			int beaconId = mapLayout.getBeaconId( questSprite );
			if ( beaconId != -1 && questId != null && questId.length() > 0 ) {
				gameState.getQuestEventMap().put( questSprite.getQuestId(), new Integer(beaconId) );
			}
		}
	}

	public void selectBeacon() {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Beacon";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					showBeaconEditor( (BeaconSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	public void selectPlayerShip() {
		miscSelector.setSpriteLists( new ArrayList[] {playerShipSprites} );
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Player Ship";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof PlayerShipSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof PlayerShipSprite ) {
					showPlayerShipEditor( (PlayerShipSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	public void selectStore() {
		miscSelector.setSpriteLists( new ArrayList[] {storeSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Store";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof StoreSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof StoreSprite ) {
					showStoreEditor( (StoreSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	public void selectQuest() {
		miscSelector.setSpriteLists( new ArrayList[] {questSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Quest";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof QuestSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof QuestSprite ) {
					showQuestEditor( (QuestSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	private void addStore() {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Store";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						return true;
					}
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						StoreSprite storeSprite = new StoreSprite( null );
						SectorMapConstraints storeC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
						storeC.setBeaconId( beaconId );
						storeSprites.add( storeSprite );
						mapPanel.add( storeSprite, storeC );

						mapPanel.revalidate();
						mapViewport.repaint();
					}
				}

				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	private void addQuest() {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Quest";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						return true;
					}
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						QuestSprite questSprite = new QuestSprite("NOTHING");
						SectorMapConstraints questC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
						questC.setBeaconId( beaconId );
						questSprites.add( questSprite );
						mapPanel.add( questSprite, questC );

						mapPanel.revalidate();
						mapViewport.repaint();
					}
				}

				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	private void movePlayerShip( final PlayerShipSprite mobileSprite ) {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Move: Player Ship";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 ) {
						mapPanel.remove( mobileSprite );
						SectorMapConstraints mobileC = new SectorMapConstraints( SectorMapConstraints.PLAYER_SHIP );
						mobileC.setBeaconId( beaconId );
						mapPanel.add( mobileSprite, mobileC );

						mapPanel.revalidate();
						mapViewport.repaint();
					}
				}

				return false;
			}
		});
		miscSelector.setVisible(true);
	}

	/**
	 * Gets an image, and caches the result.
	 */
	private BufferedImage getImage( String innerPath ) {
		BufferedImage result = cachedImages.get(innerPath);
		if (result != null) return result;
		log.trace( "Image not in cache, loading and scaling...: "+ innerPath );

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			result = ImageIO.read( in );

		} catch (IOException e) {
			log.error( "Error reading image: "+ innerPath, e );
		}	finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
		cachedImages.put( innerPath, result );

		return result;
	}

	private void showSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension(sideWidth + vbarWidth, 1) );
		sideScroll.setVisible( true );

		this.revalidate();
		this.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				sideScroll.getVerticalScrollBar().setValue(0);
			}
		});
	}

	private void fitSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension(sideWidth + vbarWidth, 1) );

		this.revalidate();
		this.repaint();
	}

	private void clearSidePanel() {
		sideScroll.setVisible( false );
		sidePanel.removeAll();

		sidePanel.revalidate();
		this.revalidate();
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

	/**
	 * Adds a wrapped label the side panel.
	 */
	private void addSidePanelNote( String s ) {
		JTextArea labelArea = new JTextArea( s );
		labelArea.setBackground(null);
		labelArea.setEditable(false);
		labelArea.setBorder(null);
		labelArea.setLineWrap(true);
		labelArea.setWrapStyleWord(true);
		labelArea.setFocusable(false);
		sidePanel.add( labelArea );
	}

	private void showBeaconEditor( final BeaconSprite beaconSprite ) {
		final String VISITED = "Visited";
		final String STARS_LIST = "Bkg List";
		final String STARS_IMAGE = "Bkg Image";
		final String SPRITE_LIST = "Sprite List";
		final String SPRITE_IMAGE = "Sprite Image";
		final String SPRITE_X = "Sprite PosX";
		final String SPRITE_Y = "Sprite PosY";
		final String SPRITE_ROT = "Sprite Rot";
		final String SEEN = "Seen";
		final String ENEMY_PRESENT = "Enemy Present";
		final String SHIP_EVENT = "Ship Event";
		final String AUTO_SHIP = "Auto Ship";
		final String BETA = "Beta?";
		final String FLEET = "Fleet";
		final String UNDER_ATTACK = "Under Attack";

		final Map<String, BackgroundImageList> allImageListsMap = DataManager.get().getBackgroundImageLists();
		final Map<String, ShipEvent> allShipEventsMap = DataManager.get().getShipEvents();

		String title = String.format( "Beacon %02d", mapLayout.getBeaconId(beaconSprite) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( VISITED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(VISITED).setSelected( beaconSprite.isVisited() );
		editorPanel.getBoolean(VISITED).addMouseListener( new StatusbarMouseListener(frame, "The player has been to this beacon. (All nearby fields need values.)") );
		editorPanel.addRow( STARS_LIST, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(STARS_LIST).setEnabled( false );
		editorPanel.getCombo(STARS_LIST).addMouseListener( new StatusbarMouseListener(frame, "An image list from which to choose a background starscape. (BG_*)") );
		editorPanel.addRow( STARS_IMAGE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(STARS_IMAGE).setEnabled( false );
		editorPanel.getCombo(STARS_IMAGE).addMouseListener( new StatusbarMouseListener(frame, "Background starscape, a fullscreen image.") );
		editorPanel.addRow( SPRITE_LIST, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(SPRITE_LIST).setEnabled( false );
		editorPanel.getCombo(SPRITE_LIST).addMouseListener( new StatusbarMouseListener(frame, "An image list from which to choose a background sprite. (PLANET_*)") );
		editorPanel.addRow( SPRITE_IMAGE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(SPRITE_IMAGE).setEnabled( false );
		editorPanel.getCombo(SPRITE_IMAGE).addMouseListener( new StatusbarMouseListener(frame, "Background sprite, which appears in front of the starscape.") );
		editorPanel.addRow( SPRITE_X, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SPRITE_X).setEnabled( false );
		editorPanel.getInt(SPRITE_X).addMouseListener( new StatusbarMouseListener(frame, "Background sprite X position.") );
		editorPanel.addRow( SPRITE_Y, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SPRITE_Y).setEnabled( false );
		editorPanel.getInt(SPRITE_Y).addMouseListener( new StatusbarMouseListener(frame, "Background sprite Y position.") );
		editorPanel.addRow( SPRITE_ROT, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SPRITE_ROT).setText( "0" );
		editorPanel.getInt(SPRITE_ROT).setEnabled( false );
		editorPanel.getInt(SPRITE_ROT).addMouseListener( new StatusbarMouseListener(frame, "Background sprite rotation. (positive degrees clockwise)") );
		editorPanel.addBlankRow();
		editorPanel.addRow( SEEN, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(SEEN).setSelected( beaconSprite.isSeen() );
		editorPanel.getBoolean(SEEN).addMouseListener( new StatusbarMouseListener(frame, "The player has been within one hop of this beacon.") );
		editorPanel.addBlankRow();
		editorPanel.addRow( ENEMY_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(ENEMY_PRESENT).addMouseListener( new StatusbarMouseListener(frame, "A ship is waiting at this beacon. (All nearby fields need values.)") );
		editorPanel.addRow( SHIP_EVENT, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(SHIP_EVENT).setEnabled( false );
		editorPanel.getCombo(SHIP_EVENT).addMouseListener( new StatusbarMouseListener(frame, "A ship event to trigger and forget upon arrival (spawning a new nearby ship).") );
		editorPanel.addRow( AUTO_SHIP, FieldEditorPanel.ContentType.STRING );
		editorPanel.getString(AUTO_SHIP).setEditable( false );
		editorPanel.getString(AUTO_SHIP).setEnabled( false );
		editorPanel.getString(AUTO_SHIP).addMouseListener( new StatusbarMouseListener(frame, "The blueprint (or blueprintList) of an auto ship to appear.") );
		editorPanel.addRow( BETA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(BETA).setText( "0" );
		editorPanel.getInt(BETA).setEnabled( false );
		editorPanel.getInt(BETA).addMouseListener( new StatusbarMouseListener(frame, "Unknown erratic integer. (Observed values: 126 to 32424)") );
		editorPanel.addBlankRow();
		editorPanel.addRow( FLEET, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(FLEET).addMouseListener( new StatusbarMouseListener(frame, "Fleet background sprites.") );
		editorPanel.addRow( UNDER_ATTACK, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(UNDER_ATTACK).setSelected( beaconSprite.isUnderAttack() );
		editorPanel.getBoolean(UNDER_ATTACK).addMouseListener( new StatusbarMouseListener(frame, "The beacon is under attack by rebels (flashing red).") );

		editorPanel.getCombo(STARS_LIST).addItem( "" );
		editorPanel.getCombo(SPRITE_LIST).addItem( "" );
		editorPanel.getCombo(STARS_IMAGE).addItem( "" );
		editorPanel.getCombo(SPRITE_IMAGE).addItem( "" );
		for ( BackgroundImageList imageList : allImageListsMap.values() ) {
			editorPanel.getCombo(STARS_LIST).addItem( imageList );
			editorPanel.getCombo(SPRITE_LIST).addItem( imageList );
		}

		editorPanel.getCombo(SHIP_EVENT).addItem( "" );
		for ( ShipEvent shipEvent : allShipEventsMap.values() ) {
			editorPanel.getCombo(SHIP_EVENT).addItem( shipEvent );
		}

		if ( beaconSprite.isVisited() ) {
			editorPanel.getCombo(STARS_LIST).setEnabled( true );
			editorPanel.getCombo(STARS_IMAGE).setEnabled( true );
			editorPanel.getCombo(SPRITE_LIST).setEnabled( true );
			editorPanel.getCombo(SPRITE_IMAGE).setEnabled( true );
			editorPanel.getInt(SPRITE_X).setEnabled( true );
			editorPanel.getInt(SPRITE_Y).setEnabled( true );
			editorPanel.getInt(SPRITE_ROT).setEnabled( true );

			editorPanel.getBoolean(VISITED).setSelected( true );

			editorPanel.getCombo(STARS_IMAGE).addItem( beaconSprite.getBgStarscapeImageInnerPath() );
			editorPanel.getCombo(STARS_IMAGE).setSelectedItem( beaconSprite.getBgStarscapeImageInnerPath() );

			editorPanel.getCombo(SPRITE_IMAGE).addItem( beaconSprite.getBgSpriteImageInnerPath() );
			editorPanel.getCombo(SPRITE_IMAGE).setSelectedItem( beaconSprite.getBgSpriteImageInnerPath() );

			editorPanel.getInt(SPRITE_X).setText( ""+ beaconSprite.getBgSpritePosX() );
			editorPanel.getInt(SPRITE_Y).setText( ""+ beaconSprite.getBgSpritePosY() );
			editorPanel.getInt(SPRITE_ROT).setText( ""+ beaconSprite.getBgSpriteRotation() );
		}

		if ( beaconSprite.isEnemyPresent() ) {
			editorPanel.getCombo(SHIP_EVENT).setEnabled( true );
			editorPanel.getString(AUTO_SHIP).setEnabled( true );
			editorPanel.getInt(BETA).setEnabled( true );

			editorPanel.getBoolean(ENEMY_PRESENT).setSelected( true );

			ShipEvent currentShipEvent = allShipEventsMap.get( beaconSprite.getShipEventId() );
			if ( currentShipEvent != null )
				editorPanel.getCombo(SHIP_EVENT).setSelectedItem( currentShipEvent );

			editorPanel.getString(AUTO_SHIP).setText( beaconSprite.getAutoBlueprintId() );

			editorPanel.getInt(BETA).setText( ""+ beaconSprite.getBeta() );
		}

		for ( SavedGameParser.FleetPresence fleetPresence : SavedGameParser.FleetPresence.class.getEnumConstants() ) {
			editorPanel.getCombo(FLEET).addItem( fleetPresence );
		}
		editorPanel.getCombo(FLEET).setSelectedItem( beaconSprite.getFleetPresence() );

		editorPanel.getBoolean(UNDER_ATTACK).setSelected( beaconSprite.isUnderAttack() );

		final Runnable applyCallback = new Runnable() {
			public void run() {
				String newString;

				String bgStarscapeImageInnerPath = null;
				String bgSpriteImageInnerPath = null;
				int bgSpritePosX = 0;
				int bgSpritePosY = 0;
				int bgSpriteRotation = 0;
				boolean visited = editorPanel.getBoolean(VISITED).isSelected();

				Object bgStarscapeImageInnerPathObj = editorPanel.getCombo(STARS_IMAGE).getSelectedItem();
				if ( bgStarscapeImageInnerPathObj instanceof BackgroundImage ) {
					bgStarscapeImageInnerPath = ((BackgroundImage)bgStarscapeImageInnerPathObj).getInnerPath();
				}
				else if ( bgStarscapeImageInnerPathObj instanceof String ) {
					if ( !"".equals(bgStarscapeImageInnerPath) && !"null".equals(bgStarscapeImageInnerPath) )
						bgStarscapeImageInnerPath = (String)bgStarscapeImageInnerPathObj;
				}

				Object bgSpriteImageInnerPathObj = editorPanel.getCombo(SPRITE_IMAGE).getSelectedItem();
				if ( bgSpriteImageInnerPathObj instanceof BackgroundImage ) {
					bgSpriteImageInnerPath = ((BackgroundImage)bgSpriteImageInnerPathObj).getInnerPath();
				}
				else if ( bgSpriteImageInnerPathObj instanceof String ) {
					if ( !"".equals(bgSpriteImageInnerPathObj) && !"null".equals(bgSpriteImageInnerPathObj) )
						bgSpriteImageInnerPath = (String)bgSpriteImageInnerPathObj;
				}

				newString = editorPanel.getInt(SPRITE_X).getText();
				try { bgSpritePosX = Integer.parseInt(newString); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(SPRITE_Y).getText();
				try { bgSpritePosY = Integer.parseInt(newString); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(SPRITE_ROT).getText();
				try { bgSpriteRotation = Integer.parseInt(newString); }
				catch (NumberFormatException e) {}

				if ( "NONE".equals(bgSpriteImageInnerPath) ) {
					bgSpritePosX = 0;
					bgSpritePosY = 0;
					bgSpriteRotation = 0;
				}

				if ( visited && bgStarscapeImageInnerPath != null && bgSpriteImageInnerPath != null ) {
					beaconSprite.setVisited( true );
					beaconSprite.setBgStarscapeImageInnerPath( bgStarscapeImageInnerPath );
					beaconSprite.setBgSpriteImageInnerPath( bgSpriteImageInnerPath );
					beaconSprite.setBgSpritePosX( bgSpritePosX );
					beaconSprite.setBgSpritePosY( bgSpritePosY );
					beaconSprite.setBgSpriteRotation( bgSpriteRotation );
				} else {
					beaconSprite.setVisited( false );
					beaconSprite.setBgStarscapeImageInnerPath( null );
					beaconSprite.setBgSpriteImageInnerPath( null );
					beaconSprite.setBgSpritePosX( -1 );
					beaconSprite.setBgSpritePosY( -1 );
					beaconSprite.setBgSpriteRotation( 0 );
				}

				beaconSprite.setSeen( editorPanel.getBoolean(SEEN).isSelected() );

				String shipEventId = null;
				String autoBlueprintId = null;
				int beta = 0;
				boolean enemyPresent = editorPanel.getBoolean(ENEMY_PRESENT).isSelected();

				Object shipEventObj = editorPanel.getCombo(SHIP_EVENT).getSelectedItem();
				if ( shipEventObj instanceof ShipEvent )
					shipEventId = ((ShipEvent)shipEventObj).getId();

				autoBlueprintId = editorPanel.getString(AUTO_SHIP).getText();

				newString = editorPanel.getInt(BETA).getText();
				try { beta = Integer.parseInt(newString); }
				catch (NumberFormatException e) {}

				if ( enemyPresent && shipEventId != null && autoBlueprintId.length() > 0 && !"null".equals(autoBlueprintId) ) {
					beaconSprite.setEnemyPresent( true );
					beaconSprite.setShipEventId( shipEventId );
					beaconSprite.setAutoBlueprintId( autoBlueprintId );
					beaconSprite.setBeta( beta );
				} else {
					beaconSprite.setEnemyPresent( false );
					beaconSprite.setShipEventId( null );
					beaconSprite.setAutoBlueprintId( null );
					beaconSprite.setBeta( 0 );
				}

				beaconSprite.setFleetPresence( (SavedGameParser.FleetPresence)editorPanel.getCombo(FLEET).getSelectedItem() );
				beaconSprite.setUnderAttack( editorPanel.getBoolean(UNDER_ATTACK).isSelected() );

				beaconSprite.makeSane();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener beaconListener = new ActionListener() {
			private JCheckBox visitedCheck = editorPanel.getBoolean(VISITED);
			private JComboBox starsListCombo = editorPanel.getCombo(STARS_LIST);
			private JComboBox starsImageCombo = editorPanel.getCombo(STARS_IMAGE);
			private JComboBox spriteListCombo = editorPanel.getCombo(SPRITE_LIST);
			private JComboBox spriteImageCombo = editorPanel.getCombo(SPRITE_IMAGE);
			private JTextField spriteXField = editorPanel.getInt(SPRITE_X);
			private JTextField spriteYField = editorPanel.getInt(SPRITE_Y);
			private JTextField spriteRotField = editorPanel.getInt(SPRITE_ROT);

			private JCheckBox enemyPresentCheck = editorPanel.getBoolean(ENEMY_PRESENT);
			private JComboBox shipEventCombo = editorPanel.getCombo(SHIP_EVENT);
			private JTextField autoShipField = editorPanel.getString(AUTO_SHIP);
			private JTextField betaField = editorPanel.getInt(BETA);

			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				boolean resize = false;
				if ( source == visitedCheck ) {
					boolean visited = visitedCheck.isSelected();
					if ( !visited ) {
						starsListCombo.setSelectedItem( "" );
						spriteListCombo.setSelectedItem( "" );
						starsImageCombo.setSelectedItem( "" );
						spriteImageCombo.setSelectedItem( "" );
						spriteXField.setText( "" );
						spriteYField.setText( "" );
						spriteRotField.setText( "0" );
					}

					starsListCombo.setEnabled( visited );
					starsImageCombo.setEnabled( visited );
					spriteListCombo.setEnabled( visited );
					spriteImageCombo.setEnabled( visited );
					spriteXField.setEnabled( visited );
					spriteYField.setEnabled( visited );
					spriteRotField.setEnabled( visited );
				}
				else if ( source == starsListCombo ) {
					Object imageListObj = starsListCombo.getSelectedItem();
					starsImageCombo.removeAllItems();
					starsImageCombo.addItem( "" );
					if ( imageListObj instanceof BackgroundImageList ) {
						for ( BackgroundImage img : ((BackgroundImageList)imageListObj).getImages() ) {
							starsImageCombo.addItem( img );
						}
					}
					resize = true;
				}
				else if ( source == spriteListCombo ) {
					Object imageListObj = spriteListCombo.getSelectedItem();
					spriteImageCombo.removeAllItems();
					spriteImageCombo.addItem( "" );
					spriteImageCombo.addItem( "NONE" );
					if ( imageListObj instanceof BackgroundImageList ) {
						for ( BackgroundImage img : ((BackgroundImageList)imageListObj).getImages() ) {
							spriteImageCombo.addItem( img );
						}
					}
					resize = true;
				}
				else if ( source == enemyPresentCheck ) {
					boolean enemyPresent = enemyPresentCheck.isSelected();
					if ( !enemyPresent ) {
						shipEventCombo.setSelectedItem( "" );
						betaField.setText( "0" );
					}
					shipEventCombo.setEnabled( enemyPresent );
					autoShipField.setEnabled( enemyPresent );
					betaField.setEnabled( enemyPresent );
				}
				else if ( source == shipEventCombo ) {
					Object shipEventObj = shipEventCombo.getSelectedItem();
					if ( shipEventObj instanceof ShipEvent ) {
						String autoBlueprintId = ((ShipEvent)shipEventObj).getAutoBlueprintId();
						editorPanel.getString(AUTO_SHIP).setText( autoBlueprintId );
					} else {
						editorPanel.getString(AUTO_SHIP).setText( "" );
					}
				}
				if ( resize ) {
					editorPanel.setMaximumSize(editorPanel.getPreferredSize());
					fitSidePanel();
				}
			}
		};
		editorPanel.getBoolean(VISITED).addActionListener( beaconListener );
		editorPanel.getCombo(STARS_LIST).addActionListener( beaconListener );
		editorPanel.getCombo(SPRITE_LIST).addActionListener( beaconListener );
		editorPanel.getBoolean(ENEMY_PRESENT).addActionListener( beaconListener );
		editorPanel.getCombo(SHIP_EVENT).addActionListener( beaconListener );

		addSidePanelSeparator(6);

		JButton visitBtn = new JButton("Visit");
		visitBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		visitBtn.addMouseListener( new StatusbarMouseListener(frame, "Mark this beacon as visited, using random images.") );
		sidePanel.add(visitBtn);

		visitBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] listCombos = new String[] {STARS_LIST, SPRITE_LIST};
				String[] listNames = new String[] {"BACKGROUND", "PLANET"};
				BackgroundImageList[] defaultLists = new BackgroundImageList[listCombos.length];
				final String[] imageCombos = new String[] {STARS_IMAGE, SPRITE_IMAGE};
				final BackgroundImage[] randomImages = new BackgroundImage[listCombos.length];

				for ( int i=0; i < listCombos.length; i++ ) {
					defaultLists[i] = allImageListsMap.get( listNames[i] );
					if ( defaultLists[i] == null || defaultLists[i].getImages().size() == 0 ) {
						frame.setStatusText( "Random visit failed. The default \""+ listNames[i] +"\" image list was missing or empty." );
						return;
					}
					randomImages[i] = defaultLists[i].getImages().get( (int)(Math.random()*defaultLists[i].getImages().size()) );
				}

				if ( !editorPanel.getBoolean(VISITED).isSelected() )
					editorPanel.getBoolean(VISITED).doClick(); // setSelected() won't fire ActionEvents.

				for ( int i=0; i < listCombos.length; i++ ) {
					editorPanel.getCombo(listCombos[i]).setSelectedItem( defaultLists[i] );
				}

				// Wait for the image combos to populate.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						for ( int i=0; i < imageCombos.length; i++ ) {
							editorPanel.getCombo(imageCombos[i]).setSelectedItem( randomImages[i] );
							if ( imageCombos[i].equals(SPRITE_IMAGE) ) {
								int bgSpritePosX = (int)(Math.random() * (SCREEN_WIDTH - randomImages[i].getWidth()));
								int bgSpritePosY = (int)(Math.random() * (SCREEN_HEIGHT - randomImages[i].getHeight()));
								int bgSpriteRotation = (Math.random() >= 0.5 ? 0 : 180);
								editorPanel.getInt(SPRITE_X).setText( ""+ bgSpritePosX );
								editorPanel.getInt(SPRITE_Y).setText( ""+ bgSpritePosY );
								editorPanel.getInt(SPRITE_ROT).setText( ""+ bgSpriteRotation );
							}
						}
					}
				});
			}
		});

		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		showSidePanel();
	}

	private void showPlayerShipEditor( final PlayerShipSprite playerShipSprite ) {

		String title = "Player Ship";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );

		final Runnable applyCallback = new Runnable() {
			public void run() {
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton moveBtn = new JButton("Move To...");
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(moveBtn);

		moveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				movePlayerShip( playerShipSprite );
			}
		});

		showSidePanel();
	}

	private void showStoreEditor( final StoreSprite storeSprite ) {
		final String FUEL = "Fuel";
		final String MISSILES = "Missiles";
		final String DRONE_PARTS = "Drone Parts";
		final String TOP_TYPE = "Top Type";
		final String TOP_ONE = "T1 Item";
		final String TOP_AVAIL_ONE = "T1 In Stock";
		final String TOP_TWO = "T2 Item";
		final String TOP_AVAIL_TWO = "T2 In Stock";
		final String TOP_THREE = "T3 Item";
		final String TOP_AVAIL_THREE = "T3 In Stock";
		final String BOTTOM_TYPE = "Bottom Type";
		final String BOTTOM_ONE = "B1 Item";
		final String BOTTOM_AVAIL_ONE = "B1 In Stock";
		final String BOTTOM_TWO = "B2 Item";
		final String BOTTOM_AVAIL_TWO = "B2 In Stock";
		final String BOTTOM_THREE = "B3 Item";
		final String BOTTOM_AVAIL_THREE = "B3 In Stock";

		final String[] topSlots = new String[] { TOP_ONE, TOP_TWO, TOP_THREE };
		final String[] topAvail = new String[] { TOP_AVAIL_ONE, TOP_AVAIL_TWO, TOP_AVAIL_THREE };
		final String[] bottomSlots = new String[] { BOTTOM_ONE, BOTTOM_TWO, BOTTOM_THREE };
		final String[] bottomAvail = new String[] { BOTTOM_AVAIL_ONE, BOTTOM_AVAIL_TWO, BOTTOM_AVAIL_THREE };
		final SavedGameParser.StoreItemType[] itemTypes = SavedGameParser.StoreItemType.class.getEnumConstants();

		// Use array-loops to manage both shelves.
		final SavedGameParser.StoreShelf[] shelves = new SavedGameParser.StoreShelf[] { storeSprite.getTopShelf(), storeSprite.getBottomShelf() };
		final String[] types = new String[] { TOP_TYPE, BOTTOM_TYPE };
		final String[][] slots = new String[][] { topSlots, bottomSlots };
		final String[][] avails = new String[][] { topAvail, bottomAvail };

		// Build interchangeable id-vs-toStringable maps, mapped by item type.
		Map weaponLookup = DataManager.get().getWeapons();
		Map droneLookup = DataManager.get().getDrones();
		Map augmentLookup = DataManager.get().getAugments();
		Map crewLookup = new LinkedHashMap();
		for ( Object o : new Object[] {
			CrewBlueprint.RACE_CRYSTAL, CrewBlueprint.RACE_ENERGY,
			CrewBlueprint.RACE_ENGI, CrewBlueprint.RACE_HUMAN,
			CrewBlueprint.RACE_MANTIS, CrewBlueprint.RACE_ROCK,
			CrewBlueprint.RACE_SLUG } ) {
			crewLookup.put( o, o );
		}
		Map systemLookup = new LinkedHashMap();
		for ( Object o : new Object[] {
			SystemBlueprint.ID_PILOT, SystemBlueprint.ID_DOORS,
			SystemBlueprint.ID_SENSORS, SystemBlueprint.ID_MEDBAY,
			SystemBlueprint.ID_OXYGEN, SystemBlueprint.ID_SHIELDS,
			SystemBlueprint.ID_ENGINES, SystemBlueprint.ID_WEAPONS,
			SystemBlueprint.ID_DRONE_CTRL, SystemBlueprint.ID_TELEPORTER,
			SystemBlueprint.ID_CLOAKING, SystemBlueprint.ID_ARTILLERY } ) {
			systemLookup.put( o, o );
		}
		final Map<SavedGameParser.StoreItemType, Map> itemLookups = new HashMap<SavedGameParser.StoreItemType, Map>();
		itemLookups.put( SavedGameParser.StoreItemType.WEAPON, weaponLookup );
		itemLookups.put( SavedGameParser.StoreItemType.DRONE, droneLookup );
		itemLookups.put( SavedGameParser.StoreItemType.AUGMENT, augmentLookup );
		itemLookups.put( SavedGameParser.StoreItemType.CREW, crewLookup );
		itemLookups.put( SavedGameParser.StoreItemType.SYSTEM, systemLookup );

		String title = String.format("Store (Beacon %02d)", mapLayout.getBeaconId(storeSprite) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( FUEL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(FUEL).setText( ""+storeSprite.getFuel() );
		editorPanel.addRow( MISSILES, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(MISSILES).setText( ""+storeSprite.getMissiles() );
		editorPanel.addRow( DRONE_PARTS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(DRONE_PARTS).setText( ""+storeSprite.getDroneParts() );

		List<String> badIds = new ArrayList<String>();
		for (int i=0; i < shelves.length; i++) {
			editorPanel.addBlankRow();
			editorPanel.addRow( types[i], FieldEditorPanel.ContentType.COMBO );
			for (int j=0; j < itemTypes.length; j++) {
				editorPanel.getCombo(types[i]).addItem( itemTypes[j] );
			}
			editorPanel.getCombo(types[i]).setSelectedItem( shelves[i].getItemType() );

			for (int j=0; j < slots[i].length; j++) {
				editorPanel.addRow( slots[i][j], FieldEditorPanel.ContentType.COMBO );
				editorPanel.addRow( avails[i][j], FieldEditorPanel.ContentType.BOOLEAN );

				editorPanel.getCombo(slots[i][j]).addItem( "" );
				Map lookupMap = itemLookups.get( shelves[i].getItemType() );
				if ( lookupMap != null ) {
					for ( Object o : lookupMap.values() ) {
						editorPanel.getCombo(slots[i][j]).addItem( o );
					}
					if ( shelves[i].getItems().size() > j ) {
						String itemId = shelves[i].getItems().get(j).getItemId();
						Object itemChoice = lookupMap.get( itemId );
						if ( itemChoice != null ) {
							editorPanel.getCombo(slots[i][j]).setSelectedItem( itemChoice );
						} else {
							editorPanel.getCombo(slots[i][j]).setSelectedItem( "" );
							badIds.add( itemId );
						}

						editorPanel.getBoolean(avails[i][j]).setSelected( shelves[i].getItems().get(j).isAvailable() );
					}
				}
			}
		}
		if ( badIds.size() > 0 ) {
			String message = "The following item ids are unrecognized:";
			for ( String badId : badIds )
				message += " "+ badId;
			message += ".";
			log.debug( message );
			frame.setStatusText( message );
		}

		final Runnable applyCallback = new Runnable() {
			public void run() {
				String newString;

				newString = editorPanel.getInt(FUEL).getText();
				try { storeSprite.setFuel( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(MISSILES).getText();
				try { storeSprite.setMissiles( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(DRONE_PARTS).getText();
				try { storeSprite.setDroneParts( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				for (int i=0; i < shelves.length; i++) {
					SavedGameParser.StoreItemType itemType = (SavedGameParser.StoreItemType)editorPanel.getCombo(types[i]).getSelectedItem();
					shelves[i].setItemType( itemType );
					shelves[i].getItems().clear();
					for (int j=0; j < slots[i].length; j++) {
						Object selectedItem = editorPanel.getCombo(slots[i][j]).getSelectedItem();
						if ( "".equals( selectedItem ) == false && itemLookups.get(itemType) != null ) {
							// Do a reverse lookup on the map to get an item id.
							for ( Object entry : itemLookups.get(itemType).entrySet() ) {
								if ( ((Map.Entry)entry).getValue().equals( selectedItem ) ) {
									boolean available = editorPanel.getBoolean(avails[i][j]).isSelected();
									String id = (String)((Map.Entry)entry).getKey();

									SavedGameParser.StoreItem newItem = new SavedGameParser.StoreItem( available, id );
									shelves[i].getItems().add( newItem );
									break;
								}
							}
						}
					}
				}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener shelfListener = new ActionListener() {
			private boolean ignoreChanges = false;

			public void actionPerformed(ActionEvent e) {
				if ( ignoreChanges ) return;
				ignoreChanges = true;

				Object source = e.getSource();
				boolean resize = false;
				for (int i=0; i < shelves.length; i++) {
					JComboBox typeCombo = editorPanel.getCombo(types[i]);
					if ( source == typeCombo ) {
						for (int j=0; j < slots[i].length; j++) {
							JComboBox itemCombo = editorPanel.getCombo(slots[i][j]);
							itemCombo.removeAllItems();
							Object selectedType = typeCombo.getSelectedItem();

							itemCombo.addItem( "" );
							Map lookupMap = itemLookups.get( selectedType );
							if ( lookupMap != null ) {
								for ( Object o : lookupMap.values() ) {
									editorPanel.getCombo(slots[i][j]).addItem( o );
								}
							}

							editorPanel.getBoolean(avails[i][j]).setSelected( false );
						}
						resize = true;
					}
					else {
						// Check if the source was a slot combo.
						for (int j=0; j < slots[i].length; j++) {
							JComboBox itemCombo = editorPanel.getCombo(slots[i][j]);
							if ( source == itemCombo ) {
								// Toggle the avail checkbox to sell items and disable "".
								editorPanel.getBoolean(avails[i][j]).setSelected( !"".equals( itemCombo.getSelectedItem() ) );
								break;
							}
						}
					}
				}
				if ( resize ) {
					editorPanel.setMaximumSize(editorPanel.getPreferredSize());
					fitSidePanel();
				}

				// After all the secondary events, resume monitoring.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						ignoreChanges = false;
					}
				});
			}
		};
		for (int i=0; i < shelves.length; i++) {
			editorPanel.getCombo(types[i]).addActionListener( shelfListener );
			for (int j=0; j < slots[i].length; j++) {
				editorPanel.getCombo(slots[i][j]).addActionListener( shelfListener );
			}
		}

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
				storeSprites.remove( storeSprite );
				mapPanel.remove( storeSprite );
			}
		});

		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		showSidePanel();
	}

	private void showQuestEditor( final QuestSprite questSprite ) {
		final String ENCOUNTERS_FILE = "File";
		final String EVENT = "Event";

		final Map<String, Encounters> allEncountersMap = DataManager.get().getEncounters();

		String title = String.format("Quest (Beacon %02d)", mapLayout.getBeaconId(questSprite) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( ENCOUNTERS_FILE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( EVENT, FieldEditorPanel.ContentType.COMBO );

		editorPanel.getCombo(ENCOUNTERS_FILE).addItem( "" );
		for ( String fileName : allEncountersMap.keySet() ) {
			editorPanel.getCombo(ENCOUNTERS_FILE).addItem( fileName );
		}

		// Preselect the file of the current event.
		editorPanel.getCombo(EVENT).addItem( "" );
		for ( Map.Entry<String,Encounters> entry : allEncountersMap.entrySet() ) {
			FTLEvent currentEvent = entry.getValue().getEventById(questSprite.getQuestId());
			FTLEventList currentEventList = entry.getValue().getEventListById(questSprite.getQuestId());
			if ( currentEvent != null || currentEventList != null ) {
				editorPanel.getCombo(ENCOUNTERS_FILE).setSelectedItem(entry.getKey());

				for ( FTLEvent tmpEvent : entry.getValue().getEvents() ) {
					if ( tmpEvent.getId() != null )
						editorPanel.getCombo(EVENT).addItem( tmpEvent );
				}
				editorPanel.getCombo(EVENT).addItem( "- = Lists = -" );
				for ( FTLEventList tmpEventList : entry.getValue().getEventLists() ) {
					if ( tmpEventList.getId() != null )
						editorPanel.getCombo(EVENT).addItem( tmpEventList );
				}
				if ( currentEvent != null ) {
					editorPanel.getCombo(EVENT).setSelectedItem(currentEvent);
				} else if ( currentEventList != null ) {
					editorPanel.getCombo(EVENT).setSelectedItem(currentEventList);
				} else {
					editorPanel.getCombo(EVENT).setSelectedItem( "" );
				}
				break;
			}
		}
		// If no file contains the current event, complain.
		if ( "".equals( editorPanel.getCombo(ENCOUNTERS_FILE).getSelectedItem() ) ) {
			String message = "The current event/eventlist id is unrecognized: "+ questSprite.getQuestId() +".";
			log.debug( message );
			frame.setStatusText( message );
		}

		final Runnable applyCallback = new Runnable() {
			public void run() {
				Object evtObj = editorPanel.getCombo(EVENT).getSelectedItem();
				if ( evtObj instanceof FTLEvent ) {
					questSprite.setQuestId( ((FTLEvent)evtObj).getId() );
				} else if ( evtObj instanceof FTLEventList ) {
					questSprite.setQuestId( ((FTLEventList)evtObj).getId() );
				} else {
					frame.setStatusText( "No event/eventlist id has been selected." );
					return;
				}
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener questListener = new ActionListener() {
			private JComboBox fileCombo = editorPanel.getCombo(ENCOUNTERS_FILE);
			private JComboBox eventCombo = editorPanel.getCombo(EVENT);

			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				if ( source == fileCombo ) {
					eventCombo.removeAllItems();
					Object selectedFile = fileCombo.getSelectedItem();

					editorPanel.getCombo(EVENT).addItem( "" );
					if ( "".equals( selectedFile ) == false ) {
						Encounters tmpEncounters = allEncountersMap.get( selectedFile );
						if ( tmpEncounters != null ) {
							for ( FTLEvent tmpEvent : tmpEncounters.getEvents() ) {
								if ( tmpEvent.getId() != null )
									eventCombo.addItem( tmpEvent );
							}
							eventCombo.addItem( "- = Lists = -" );
							for ( FTLEventList tmpEventList : tmpEncounters.getEventLists() ) {
								if ( tmpEventList.getId() != null )
									eventCombo.addItem( tmpEventList );
							}
							editorPanel.setMaximumSize(editorPanel.getPreferredSize());
							fitSidePanel();
						}
					}
				}
			}
		};
		editorPanel.getCombo(ENCOUNTERS_FILE).addActionListener( questListener );

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
				questSprites.remove( questSprite );
				mapPanel.remove( questSprite );
			}
		});

		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		showSidePanel();
	}



	public class BeaconSprite extends JComponent {
		private boolean visited = false;
		private String bgStarscapeImageInnerPath = null;
		private String bgSpriteImageInnerPath = null;
		private int bgSpritePosX = -1;
		private int bgSpritePosY = -1;
		private int bgSpriteRotation = 0;

		private boolean seen = false;

		private boolean enemyPresent = false;
		private String shipEventId = null;
		private String autoBlueprintId = null;
		private int beta = 0;

		private SavedGameParser.FleetPresence fleetPresence = SavedGameParser.FleetPresence.NONE;
		private boolean underAttack = false;
		private BufferedImage currentImage = null;

		public BeaconSprite( SavedGameParser.BeaconState beaconState ) {
			if ( beaconState != null ) {
				visited = beaconState.isVisited();
				bgStarscapeImageInnerPath = beaconState.getBgStarscapeImageInnerPath();
				bgSpriteImageInnerPath = beaconState.getBgSpriteImageInnerPath();
				bgSpritePosX = beaconState.getBgSpritePosX();
				bgSpritePosY = beaconState.getBgSpritePosY();
				bgSpriteRotation = beaconState.getBgSpriteRotation();

				seen = beaconState.isSeen();

				enemyPresent = beaconState.isEnemyPresent();
				shipEventId = beaconState.getShipEventId();
				autoBlueprintId = beaconState.getAutoBlueprintId();
				beta = beaconState.getBeta();

				fleetPresence = beaconState.getFleetPresence();
				underAttack = beaconState.isUnderAttack();
			}
			makeSane();
		}

		public boolean isVisited() { return visited; }
		public String getBgStarscapeImageInnerPath() { return bgStarscapeImageInnerPath; }
		public String getBgSpriteImageInnerPath() { return bgSpriteImageInnerPath; }
		public int getBgSpritePosX() { return bgSpritePosX; }
		public int getBgSpritePosY() { return bgSpritePosY; }
		public int getBgSpriteRotation() { return bgSpriteRotation; }
		public boolean isSeen() { return seen; }
		public boolean isEnemyPresent() { return enemyPresent; }
		public String getShipEventId() { return shipEventId; }
		public String getAutoBlueprintId() { return autoBlueprintId; }
		public int getBeta() { return beta; }
		public SavedGameParser.FleetPresence getFleetPresence() { return fleetPresence; }
		public boolean isUnderAttack() { return underAttack; }

		public void setVisited( boolean b ) { visited = b; }
		public void setBgStarscapeImageInnerPath( String s ) { bgStarscapeImageInnerPath = s; }
		public void setBgSpriteImageInnerPath( String s ) { bgSpriteImageInnerPath = s; }
		public void setBgSpritePosX( int n ) { bgSpritePosX = n; }
		public void setBgSpritePosY( int n ) { bgSpritePosY = n; }
		public void setBgSpriteRotation( int n ) { bgSpriteRotation = n; }
		public void setSeen( boolean b ) { seen = b; }
		public void setEnemyPresent( boolean b ) { enemyPresent = b; }
		public void setShipEventId( String s ) { shipEventId = s; }
		public void setAutoBlueprintId( String s ) { autoBlueprintId = s; }
		public void setBeta( int n ) { beta = n; }
		public void setFleetPresence( SavedGameParser.FleetPresence fp ) { fleetPresence = fp; }
		public void setUnderAttack( boolean b ) { underAttack = b; }

		public void makeSane() {
			if ( visited && (bgStarscapeImageInnerPath == null || bgSpriteImageInnerPath == null) ) {
				visited = false;
			}
			if ( visited == false ) {
				bgStarscapeImageInnerPath = null;
				bgSpriteImageInnerPath = null;
				bgSpritePosX = -1;
				bgSpritePosY = -1;
				bgSpriteRotation = 0;
			}
			if ( "NONE".equals(bgSpriteImageInnerPath) ) {
				bgSpritePosX = 0;
				bgSpritePosY = 0;
				bgSpriteRotation = 0;
			}

			if ( enemyPresent && (shipEventId == null || autoBlueprintId == null) ) {
				enemyPresent = false;
			}
			if ( enemyPresent == false ) {
				enemyPresent = false;
				shipEventId = null;
				autoBlueprintId = null;
				beta = 0;
			}

			if ( fleetPresence == SavedGameParser.FleetPresence.REBEL ) {
				currentImage = getImage( "img/map/map_icon_warning.png" );
			}
			else if ( visited ) {
				currentImage = getImage( "img/map/map_icon_diamond_blue.png" );
			}
			else {
				currentImage = getImage( "img/map/map_icon_diamond_yellow.png" );
			}
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class StoreSprite extends JComponent {
		private int fuel;
		private int missiles;
		private int droneParts;
		private SavedGameParser.StoreShelf topShelf = new SavedGameParser.StoreShelf();
		private SavedGameParser.StoreShelf bottomShelf = new SavedGameParser.StoreShelf();
		private BufferedImage currentImage = null;

		public StoreSprite( SavedGameParser.StoreState storeState ) {
			if ( storeState != null ) {
				fuel = storeState.getFuel();
				missiles = storeState.getMissiles();
				droneParts = storeState.getDroneParts();

				SavedGameParser.StoreShelf tmpShelf;
				tmpShelf = storeState.getTopShelf();
				topShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					topShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}
				tmpShelf = storeState.getBottomShelf();
				bottomShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					bottomShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}
			}

			currentImage = getImage( "img/map/map_box_store.png" );
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		public void setFuel( int n ) { fuel = n; }
		public void setMissiles( int n ) { missiles = n; }
		public void setDroneParts( int n ) { droneParts = n; }

		public int getFuel() { return fuel; }
		public int getMissiles() { return missiles; }
		public int getDroneParts() { return droneParts; }
		public SavedGameParser.StoreShelf getTopShelf() { return topShelf; }
		public SavedGameParser.StoreShelf getBottomShelf() { return bottomShelf; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class QuestSprite extends JComponent {
		private String questId = null;
		private BufferedImage currentImage = null;

		public QuestSprite( String questId ) {
			this.questId = questId;
			currentImage = getImage( "img/map/map_box_quest.png" );
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		public void setQuestId( String s ) { questId = s; }
		public String getQuestId() { return questId; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class PlayerShipSprite extends JComponent {
		private BufferedImage currentImage = null;

		public PlayerShipSprite() {
			currentImage = getImage( "img/map/map_icon_ship.png" );
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class IncrementBox extends JPanel {
		public BasicArrowButton decBtn = new BasicArrowButton( BasicArrowButton.SOUTH );
		public BasicArrowButton incBtn = new BasicArrowButton( BasicArrowButton.NORTH );

		public IncrementBox() {
			super( new FlowLayout( FlowLayout.CENTER, 4, 2 ) );
			this.setBorder( BorderFactory.createEtchedBorder() );
			this.add( decBtn );
			this.add( incBtn );
		}
	}
}
