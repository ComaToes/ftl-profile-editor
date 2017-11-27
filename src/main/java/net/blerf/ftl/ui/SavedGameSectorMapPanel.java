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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
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
import net.blerf.ftl.parser.SavedGameParser.BeaconState;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.FleetPresence;
import net.blerf.ftl.parser.SavedGameParser.StoreShelf;
import net.blerf.ftl.parser.SavedGameParser.StoreState;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.ImageUtilities;
import net.blerf.ftl.ui.RegexDocument;
import net.blerf.ftl.ui.SectorMapLayout;
import net.blerf.ftl.ui.SectorMapLayout.SectorMapConstraints;
import net.blerf.ftl.ui.SpriteReference;
import net.blerf.ftl.ui.StoreShelfPanel;
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
	//private static final Integer BEACON_LAYER = new Integer(10);
	//private static final Integer MISC_BOX_LAYER = new Integer(15);
	//private static final Integer SHIP_LAYER = new Integer(30);
	//private static final Integer CTRL_LAYER = new Integer(50);
	private static final Logger log = LogManager.getLogger(SavedGameSectorMapPanel.class);

	private FTLFrame frame;

	private List<SpriteReference<BeaconState>> beaconRefs = new ArrayList<SpriteReference<BeaconState>>();

	private List<BeaconSprite> beaconSprites = new ArrayList<BeaconSprite>();
	private List<StoreSprite> storeSprites = new ArrayList<StoreSprite>();
	private List<QuestSprite> questSprites = new ArrayList<QuestSprite>();
	private List<PlayerShipSprite> playerShipSprites = new ArrayList<PlayerShipSprite>();

	private Map<String, Map<Rectangle, BufferedImage>> cachedImages = new HashMap<String, Map<Rectangle, BufferedImage>>();

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
		sidePanel.setLayout( new BoxLayout( sidePanel, BoxLayout.Y_AXIS ) );
		sidePanel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 6 ) );

		miscSelector = new SpriteSelector();
		miscSelector.setOpaque( false );
		miscSelector.setSize( mapPanel.getPreferredSize() );
		mapHolderPanel.add( miscSelector, MISC_SELECTION_LAYER );

		MouseInputAdapter miscListener = new MouseInputAdapter() {
			@Override
			public void mouseMoved( MouseEvent e ) {
				miscSelector.setMousePoint( e.getX(), e.getY() );
			}
			@Override
			public void mouseClicked( MouseEvent e ) {
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
			public void mouseEntered( MouseEvent e ) {
				//miscSelector.setDescriptionVisible( true );
				mapViewport.setStatusString( miscSelector.getCriteria().getDescription() +"   (Right-click to cancel)" );
			}
			@Override
			public void mouseExited( MouseEvent e ) {
				//miscSelector.setDescriptionVisible( false );
				mapViewport.setStatusString( null );
				miscSelector.setMousePoint( -1, -1 );
			}
		};
		miscSelector.addMouseListener( miscListener );
		miscSelector.addMouseMotionListener( miscListener );

		Insets ctrlInsets = new Insets( 3, 4, 3, 4 );

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new BoxLayout( selectPanel, BoxLayout.X_AXIS ) );
		selectPanel.setBorder( BorderFactory.createTitledBorder( "Select" ) );
		final JButton selectBeaconBtn = new JButton( "Beacon" );
		selectBeaconBtn.setMargin( ctrlInsets );
		selectPanel.add( selectBeaconBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectPlayerShipBtn = new JButton( "Player Ship" );
		selectPlayerShipBtn.setMargin( ctrlInsets );
		selectPanel.add( selectPlayerShipBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectStoreBtn = new JButton( "Store" );
		selectStoreBtn.setMargin( ctrlInsets );
		selectPanel.add( selectStoreBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectQuestBtn = new JButton( "Quest" );
		selectQuestBtn.setMargin( ctrlInsets );
		selectPanel.add( selectQuestBtn );

		JPanel addPanel = new JPanel();
		addPanel.setLayout( new BoxLayout( addPanel, BoxLayout.X_AXIS ) );
		addPanel.setBorder( BorderFactory.createTitledBorder( "Add" ) );
		final JButton addStoreBtn = new JButton( "Store" );
		addStoreBtn.setMargin( ctrlInsets );
		addPanel.add( addStoreBtn );
		addPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton addQuestBtn = new JButton( "Quest" );
		addQuestBtn.setMargin( ctrlInsets );
		addPanel.add( addQuestBtn );

		JPanel ctrlRowOnePanel = new JPanel();
		ctrlRowOnePanel.setLayout( new BoxLayout( ctrlRowOnePanel, BoxLayout.X_AXIS ) );
		ctrlRowOnePanel.add( selectPanel );
		ctrlRowOnePanel.add( Box.createHorizontalStrut( 15 ) );
		ctrlRowOnePanel.add( addPanel );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS) );
		ctrlPanel.add( ctrlRowOnePanel );
		//ctrlPanel.add( Box.createVerticalStrut( 8 ) );

		ActionListener ctrlListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
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
		mapScroll.getVerticalScrollBar().setUnitIncrement( 14 );
		mapScroll.setViewport( mapViewport );
		mapScroll.setViewportView( mapHolderPanel );
		centerPanel.add( mapScroll, gridC );

		gridC.insets = new Insets( 4, 4, 4, 4 );

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
		sideScroll.getVerticalScrollBar().setUnitIncrement( 14 );
		sideScroll.setVisible( false );
		this.add( sideScroll, BorderLayout.EAST );

		JPanel borderPanel = new JPanel( new BorderLayout() );
		borderPanel.setBorder( BorderFactory.createTitledBorder( "Sector Map" ) );


		//JLabel noticeLbl = new JLabel( "The number of beacons in each column can't be determined automatically." );
		//noticeLbl.setHorizontalAlignment( SwingConstants.CENTER );
		//borderPanel.add( noticeLbl, BorderLayout.NORTH );

		columnCtrlListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();

				BasicArrowButton srcBtn = (BasicArrowButton)source;
				IncrementBox iBox = (IncrementBox)srcBtn.getParent();
				int column = mapLayout.getCtrlColumn( iBox );
				if ( column != -1 ) {
					int colSize = mapLayout.getColumnSize( column );
					if ( srcBtn == iBox.decBtn ) {
						mapLayout.setColumnSize( column, colSize-1 );
					} else if ( srcBtn == iBox.incBtn ) {
						mapLayout.setColumnSize( column, colSize+1 );
					}

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

		beaconRefs.clear();

		if ( gameState == null ) {
			mapPanel.revalidate();
			mapViewport.repaint();
			return;
		}

		int beaconId;
		List<BeaconState> beaconStateList = gameState.getBeaconList();

		// Beacons.
		for ( BeaconState beaconState : beaconStateList ) {
			SpriteReference<BeaconState> beaconRef = new SpriteReference<BeaconState>( new BeaconState( beaconState ) );
			beaconRefs.add( beaconRef );

			BeaconSprite beaconSprite = new BeaconSprite( beaconRef );
			SectorMapConstraints beaconC = new SectorMapConstraints( SectorMapConstraints.BEACON );
			beaconSprites.add( beaconSprite );
			mapPanel.add( beaconSprite, beaconC );
		}

		// Stores.
		beaconId = 0;
		for ( SpriteReference<BeaconState> beaconRef : beaconRefs ) {
			if ( beaconRef.get().getStore() != null ) {
				StoreSprite storeSprite = new StoreSprite( beaconRef );
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
		for ( int i=0; i < 6; i++ ) {
			IncrementBox iBox = new IncrementBox();
			iBox.decBtn.addActionListener( columnCtrlListener );
			iBox.incBtn.addActionListener( columnCtrlListener );
			mapPanel.add( iBox, new SectorMapConstraints( SectorMapConstraints.COLUMN_CTRL ) );

			String message = "Adjust the number of beacons in this column.";
			iBox.addMouseListener( new StatusbarMouseListener( frame, message ) );
			iBox.decBtn.addMouseListener( new StatusbarMouseListener( frame, message ) );
			iBox.incBtn.addMouseListener( new StatusbarMouseListener( frame, message ) );
		}

		mapPanel.revalidate();
		mapViewport.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		if ( gameState == null ) return;

		List<BeaconState> beaconStateList = gameState.getBeaconList();
		beaconStateList.clear();

		// Player ship.
		for ( PlayerShipSprite playerShipSprite : playerShipSprites ) {
			int beaconId = mapLayout.getBeaconId( playerShipSprite );
			if ( beaconId != -1 ) {
				gameState.setCurrentBeaconId( beaconId );
			}
		}

		// Beacons.
		for ( SpriteReference<BeaconState> beaconRef : beaconRefs ) {
			beaconStateList.add( new BeaconState( beaconRef.get() ) );
		}

		// Stores are included in the beacons.

		// Quests.
		gameState.getQuestEventMap().clear();
		for ( QuestSprite questSprite : questSprites ) {
			String questId = questSprite.getQuestId();
			int beaconId = mapLayout.getBeaconId( questSprite );
			if ( beaconId != -1 && questId != null && questId.length() > 0 ) {
				gameState.getQuestEventMap().put( questSprite.getQuestId(), new Integer( beaconId ) );
			}
		}
	}

	public void selectBeacon() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Beacon";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					SpriteReference<BeaconState> beaconRef = ((BeaconSprite)sprite).getReference();
					showBeaconEditor( beaconRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	public void selectPlayerShip() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( playerShipSprites );
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Player Ship";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof PlayerShipSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof PlayerShipSprite ) {
					showPlayerShipEditor( (PlayerShipSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	public void selectStore() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( storeSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Store";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof StoreSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof StoreSprite ) {
					SpriteReference<BeaconState> beaconRef = ((StoreSprite)sprite).getReference();
					showStoreEditor( beaconRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	public void selectQuest() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( questSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Quest";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof QuestSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof QuestSprite ) {
					showQuestEditor( (QuestSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	private void addStore() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Store";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
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
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId( beaconId ) == null ) {
						SpriteReference<BeaconState> beaconRef = ((BeaconSprite)sprite).getReference();

						beaconRef.get().setStore( new StoreState() );
						beaconRef.get().getStore().addShelf( new StoreShelf() );

						StoreSprite storeSprite = new StoreSprite( beaconRef );
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
		miscSelector.setVisible( true );
	}

	private void addQuest() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Quest";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
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
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						QuestSprite questSprite = new QuestSprite( "NOTHING" );
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
		miscSelector.setVisible( true );
	}

	private void movePlayerShip( final PlayerShipSprite mobileSprite ) {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Move: Player Ship";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
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
		miscSelector.setVisible( true );
	}

	private void showSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension( sideWidth + vbarWidth, 1 ) );
		sideScroll.setVisible( true );

		this.revalidate();
		this.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				sideScroll.getVerticalScrollBar().setValue( 0 );
			}
		});
	}

	private void fitSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension( sideWidth + vbarWidth, 1 ) );

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
	 *
	 * @param title
	 * @param editorPanel
	 * @param extraContent optional component(s) to add before cancel/apply buttons
	 * @param applyCallback method to call when the apply button is clicked
	 */
	private void createSidePanel( String title, final FieldEditorPanel editorPanel, JComponent extraContent, final Runnable applyCallback ) {
		clearSidePanel();
		JLabel titleLbl = new JLabel( title );
		titleLbl.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( titleLbl );
		addSidePanelSeparator( 4 );

		// Keep the editor from growing and creating gaps around it.
		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		sidePanel.add( editorPanel );

		if ( extraContent != null ) {
			sidePanel.add( Box.createVerticalStrut( 10 ) );
			sidePanel.add( extraContent );
		}

		sidePanel.add( Box.createVerticalStrut( 10 ) );

		JPanel applyPanel = new JPanel();
		applyPanel.setLayout( new BoxLayout( applyPanel, BoxLayout.X_AXIS ) );
		JButton cancelBtn = new JButton( "Cancel" );
		applyPanel.add( cancelBtn );
		applyPanel.add( Box.createRigidArea( new Dimension( 15, 1 ) ) );
		JButton applyBtn = new JButton( "Apply" );
		applyPanel.add( applyBtn );
		sidePanel.add( applyPanel );

		cancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
			}
		});

		applyBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				applyCallback.run();
			}
		});
	}

	/**
	 * Adds a separator to the side panel.
	 */
	private void addSidePanelSeparator( int spacerSize ) {
		sidePanel.add( Box.createVerticalStrut( spacerSize ) );
		JSeparator newSep = new JSeparator( JSeparator.HORIZONTAL );
		newSep.setMaximumSize( new Dimension( Short.MAX_VALUE, newSep.getPreferredSize().height ) );
		sidePanel.add( newSep );
		sidePanel.add( Box.createVerticalStrut( spacerSize ) );
	}

	/**
	 * Adds a wrapped label the side panel.
	 */
	private void addSidePanelNote( String s ) {
		JTextArea labelArea = new JTextArea( s );
		labelArea.setBackground( null );
		labelArea.setEditable( false );
		labelArea.setBorder( null );
		labelArea.setLineWrap( true );
		labelArea.setWrapStyleWord( true );
		labelArea.setFocusable( false );
		sidePanel.add( labelArea );
	}

	private void showBeaconEditor( final SpriteReference<BeaconState> beaconRef ) {
		final String VISIT_COUNT = "Visit Count";
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
		final String SHIP_EVENT_SEED = "Ship Event Seed";
		final String FLEET = "Fleet";
		final String UNDER_ATTACK = "Under Attack";

		final Map<String, BackgroundImageList> allImageListsMap = DataManager.get().getBackgroundImageLists();
		final Map<String, ShipEvent> allShipEventsMap = DataManager.get().getShipEvents();

		int beaconId = mapLayout.getBeaconId( beaconRef.getSprite( BeaconSprite.class ) );
		String title = String.format( "Beacon %02d", beaconId );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( VISIT_COUNT, FieldEditorPanel.ContentType.SPINNER );
		editorPanel.getSpinnerField(VISIT_COUNT).addMouseListener( new StatusbarMouseListener( frame, "Number of times the player has been here. If visited, random events won't occur. (All nearby fields need values) Hit enter after typing." ) );
		editorPanel.addRow( STARS_LIST, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(STARS_LIST).setEnabled( false );
		editorPanel.getCombo(STARS_LIST).addMouseListener( new StatusbarMouseListener( frame, "An image list from which to choose a background starscape. (BG_*)" ) );
		editorPanel.addRow( STARS_IMAGE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(STARS_IMAGE).setEnabled( false );
		editorPanel.getCombo(STARS_IMAGE).addMouseListener( new StatusbarMouseListener( frame, "Background starscape, a fullscreen image." ) );
		editorPanel.addRow( SPRITE_LIST, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(SPRITE_LIST).setEnabled( false );
		editorPanel.getCombo(SPRITE_LIST).addMouseListener( new StatusbarMouseListener( frame, "An image list from which to choose a background sprite. (PLANET_*)" ) );
		editorPanel.addRow( SPRITE_IMAGE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(SPRITE_IMAGE).setEnabled( false );
		editorPanel.getCombo(SPRITE_IMAGE).addMouseListener( new StatusbarMouseListener( frame, "Background sprite, which appears in front of the starscape." ) );
		editorPanel.addRow( SPRITE_X, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SPRITE_X).setEnabled( false );
		editorPanel.getInt(SPRITE_X).addMouseListener( new StatusbarMouseListener( frame, "Background sprite X position." ) );
		editorPanel.addRow( SPRITE_Y, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SPRITE_Y).setEnabled( false );
		editorPanel.getInt(SPRITE_Y).addMouseListener( new StatusbarMouseListener( frame, "Background sprite Y position." ) );
		editorPanel.addRow( SPRITE_ROT, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SPRITE_ROT).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.getInt(SPRITE_ROT).setText( "0" );
		editorPanel.getInt(SPRITE_ROT).setEnabled( false );
		editorPanel.getInt(SPRITE_ROT).addMouseListener( new StatusbarMouseListener( frame, "Background sprite rotation. (degrees, positive = clockwise)" ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( SEEN, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(SEEN).addMouseListener( new StatusbarMouseListener( frame, "The player has been within one hop of this beacon." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( ENEMY_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(ENEMY_PRESENT).addMouseListener( new StatusbarMouseListener( frame, "A ship is waiting at this beacon. (All nearby fields need values)" ) );
		editorPanel.addRow( SHIP_EVENT, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(SHIP_EVENT).setEnabled( false );
		editorPanel.getCombo(SHIP_EVENT).addMouseListener( new StatusbarMouseListener( frame, "A ship event to trigger and forget upon arrival (spawning a new nearby ship)." ) );
		editorPanel.addRow( AUTO_SHIP, FieldEditorPanel.ContentType.STRING );
		editorPanel.getString(AUTO_SHIP).setEditable( false );
		editorPanel.getString(AUTO_SHIP).setEnabled( false );
		editorPanel.getString(AUTO_SHIP).addMouseListener( new StatusbarMouseListener( frame, "The blueprint (or blueprintList) of an auto ship to appear." ) );
		editorPanel.addRow( SHIP_EVENT_SEED, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SHIP_EVENT_SEED).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.getInt(SHIP_EVENT_SEED).setText( "0" );
		editorPanel.getInt(SHIP_EVENT_SEED).setEnabled( false );
		editorPanel.getInt(SHIP_EVENT_SEED).addMouseListener( new StatusbarMouseListener( frame, "A constant that seeds the random generation of the enemy ship." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( FLEET, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(FLEET).addMouseListener( new StatusbarMouseListener( frame, "Fleet background sprites." ) );
		editorPanel.addRow( UNDER_ATTACK, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(UNDER_ATTACK).addMouseListener( new StatusbarMouseListener( frame, "The beacon is under attack by rebels (flashing red)." ) );

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

		if ( beaconRef.get().getVisitCount() > 0 ) {
			editorPanel.getCombo(STARS_LIST).setEnabled( true );
			editorPanel.getCombo(STARS_IMAGE).setEnabled( true );
			editorPanel.getCombo(SPRITE_LIST).setEnabled( true );
			editorPanel.getCombo(SPRITE_IMAGE).setEnabled( true );
			editorPanel.getInt(SPRITE_X).setEnabled( true );
			editorPanel.getInt(SPRITE_Y).setEnabled( true );
			editorPanel.getInt(SPRITE_ROT).setEnabled( true );

			editorPanel.getSpinner(VISIT_COUNT).setValue( new Integer( beaconRef.get().getVisitCount() ) );

			editorPanel.getCombo(STARS_IMAGE).addItem( beaconRef.get().getBgStarscapeImageInnerPath() );
			editorPanel.getCombo(STARS_IMAGE).setSelectedItem( beaconRef.get().getBgStarscapeImageInnerPath() );

			editorPanel.getCombo(SPRITE_IMAGE).addItem( beaconRef.get().getBgSpriteImageInnerPath() );
			editorPanel.getCombo(SPRITE_IMAGE).setSelectedItem( beaconRef.get().getBgSpriteImageInnerPath() );

			editorPanel.getInt(SPRITE_X).setText( ""+ beaconRef.get().getBgSpritePosX() );
			editorPanel.getInt(SPRITE_Y).setText( ""+ beaconRef.get().getBgSpritePosY() );
			editorPanel.getInt(SPRITE_ROT).setText( ""+ beaconRef.get().getBgSpriteRotation() );
		}

		editorPanel.getBoolean(SEEN).setSelected( beaconRef.get().isSeen() );

		if ( beaconRef.get().isEnemyPresent() ) {
			editorPanel.getCombo(SHIP_EVENT).setEnabled( true );
			editorPanel.getString(AUTO_SHIP).setEnabled( true );
			editorPanel.getInt(SHIP_EVENT_SEED).setEnabled( true );

			editorPanel.getBoolean(ENEMY_PRESENT).setSelected( true );

			ShipEvent currentShipEvent = allShipEventsMap.get( beaconRef.get().getShipEventId() );
			if ( currentShipEvent != null )
				editorPanel.getCombo(SHIP_EVENT).setSelectedItem( currentShipEvent );

			editorPanel.getString(AUTO_SHIP).setText( beaconRef.get().getAutoBlueprintId() );

			editorPanel.getInt(SHIP_EVENT_SEED).setText( ""+ beaconRef.get().getShipEventSeed() );
		}

		for ( FleetPresence fleetPresence : FleetPresence.values() ) {
			editorPanel.getCombo(FLEET).addItem( fleetPresence );
		}
		editorPanel.getCombo(FLEET).setSelectedItem( beaconRef.get().getFleetPresence() );

		editorPanel.getBoolean(UNDER_ATTACK).setSelected( beaconRef.get().isUnderAttack() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				String newString;

				int visitCount = 0;
				String bgStarscapeImageInnerPath = null;
				String bgSpriteImageInnerPath = null;
				int bgSpritePosX = 0;
				int bgSpritePosY = 0;
				int bgSpriteRotation = 0;

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
					if ( !"".equals( bgSpriteImageInnerPathObj ) && !"null".equals( bgSpriteImageInnerPathObj ) )
						bgSpriteImageInnerPath = (String)bgSpriteImageInnerPathObj;
				}

				visitCount = editorPanel.parseSpinnerInt( VISIT_COUNT );

				try { bgSpritePosX = editorPanel.parseInt( SPRITE_X ); }
				catch ( NumberFormatException e ) {}

				try { bgSpritePosY = editorPanel.parseInt( SPRITE_Y ); }
				catch ( NumberFormatException e ) {}

				try { bgSpriteRotation = editorPanel.parseInt( SPRITE_ROT ); }
				catch ( NumberFormatException e ) {}

				if ( "NONE".equals( bgSpriteImageInnerPath ) ) {
					bgSpritePosX = 0;
					bgSpritePosY = 0;
					bgSpriteRotation = 0;
				}

				if ( visitCount > 0 && bgStarscapeImageInnerPath != null && bgSpriteImageInnerPath != null ) {
					beaconRef.get().setVisitCount( visitCount );
					beaconRef.get().setBgStarscapeImageInnerPath( bgStarscapeImageInnerPath );
					beaconRef.get().setBgSpriteImageInnerPath( bgSpriteImageInnerPath );
					beaconRef.get().setBgSpritePosX( bgSpritePosX );
					beaconRef.get().setBgSpritePosY( bgSpritePosY );
					beaconRef.get().setBgSpriteRotation( bgSpriteRotation );
				} else {
					beaconRef.get().setVisitCount( visitCount );
					beaconRef.get().setBgStarscapeImageInnerPath( null );
					beaconRef.get().setBgSpriteImageInnerPath( null );
					beaconRef.get().setBgSpritePosX( -1 );
					beaconRef.get().setBgSpritePosY( -1 );
					beaconRef.get().setBgSpriteRotation( 0 );
				}

				beaconRef.get().setSeen( editorPanel.getBoolean(SEEN).isSelected() );

				String shipEventId = null;
				String autoBlueprintId = null;
				int shipEventSeed = 0;
				boolean enemyPresent = editorPanel.getBoolean(ENEMY_PRESENT).isSelected();

				Object shipEventObj = editorPanel.getCombo(SHIP_EVENT).getSelectedItem();
				if ( shipEventObj instanceof ShipEvent )
					shipEventId = ((ShipEvent)shipEventObj).getId();

				autoBlueprintId = editorPanel.getString(AUTO_SHIP).getText();

				newString = editorPanel.getInt(SHIP_EVENT_SEED).getText();
				try { shipEventSeed = Integer.parseInt( newString ); }
				catch ( NumberFormatException e ) {}

				if ( enemyPresent && shipEventId != null && autoBlueprintId.length() > 0 && !"null".equals( autoBlueprintId ) ) {
					beaconRef.get().setEnemyPresent( true );
					beaconRef.get().setShipEventId( shipEventId );
					beaconRef.get().setAutoBlueprintId( autoBlueprintId );
					beaconRef.get().setShipEventSeed( shipEventSeed );
				} else {
					beaconRef.get().setEnemyPresent( false );
					beaconRef.get().setShipEventId( null );
					beaconRef.get().setAutoBlueprintId( null );
					beaconRef.get().setShipEventSeed( 0 );
				}

				beaconRef.get().setFleetPresence( (FleetPresence)editorPanel.getCombo(FLEET).getSelectedItem() );
				beaconRef.get().setUnderAttack( editorPanel.getBoolean(UNDER_ATTACK).isSelected() );

				beaconRef.fireReferenceChange();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		ActionListener beaconListener = new ActionListener() {
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
			private JTextField shipEventSeedField = editorPanel.getInt(SHIP_EVENT_SEED);

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				boolean resize = false;

				if ( source == starsListCombo ) {
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
						shipEventSeedField.setText( "0" );
					}
					shipEventCombo.setEnabled( enemyPresent );
					autoShipField.setEnabled( enemyPresent );
					shipEventSeedField.setEnabled( enemyPresent );
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
					editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
					fitSidePanel();
				}
			}
		};
		editorPanel.getCombo(STARS_LIST).addActionListener( beaconListener );
		editorPanel.getCombo(SPRITE_LIST).addActionListener( beaconListener );
		editorPanel.getBoolean(ENEMY_PRESENT).addActionListener( beaconListener );
		editorPanel.getCombo(SHIP_EVENT).addActionListener( beaconListener );

		ChangeListener visitListener = new ChangeListener() {
			private JSpinner visitCountSpinner = editorPanel.getSpinner(VISIT_COUNT);
			private JComboBox starsListCombo = editorPanel.getCombo(STARS_LIST);
			private JComboBox starsImageCombo = editorPanel.getCombo(STARS_IMAGE);
			private JComboBox spriteListCombo = editorPanel.getCombo(SPRITE_LIST);
			private JComboBox spriteImageCombo = editorPanel.getCombo(SPRITE_IMAGE);
			private JTextField spriteXField = editorPanel.getInt(SPRITE_X);
			private JTextField spriteYField = editorPanel.getInt(SPRITE_Y);
			private JTextField spriteRotField = editorPanel.getInt(SPRITE_ROT);

			@Override
			public void stateChanged( ChangeEvent e ) {
				Object source = e.getSource();
				boolean resize = false;

				if ( source == visitCountSpinner ) {
					int visitCount = editorPanel.parseSpinnerInt( VISIT_COUNT );
					if ( visitCount == 0 ) {
						starsListCombo.setSelectedItem( "" );
						spriteListCombo.setSelectedItem( "" );
						starsImageCombo.setSelectedItem( "" );
						spriteImageCombo.setSelectedItem( "" );
						spriteXField.setText( "" );
						spriteYField.setText( "" );
						spriteRotField.setText( "0" );
					}
					starsListCombo.setEnabled( (visitCount > 0) );
					starsImageCombo.setEnabled( (visitCount > 0) );
					spriteListCombo.setEnabled( (visitCount > 0) );
					spriteImageCombo.setEnabled( (visitCount > 0) );
					spriteXField.setEnabled( (visitCount > 0) );
					spriteYField.setEnabled( (visitCount > 0) );
					spriteRotField.setEnabled( (visitCount > 0) );
				}
			}
		};
		editorPanel.getSpinner(VISIT_COUNT).addChangeListener( visitListener );

		addSidePanelSeparator(6);

		JButton visitBtn = new JButton( "Visit" );
		visitBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		visitBtn.addMouseListener( new StatusbarMouseListener( frame, "Mark this beacon as visited, using random images." ) );
		sidePanel.add(visitBtn);

		visitBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
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

				if ( editorPanel.parseSpinnerInt( VISIT_COUNT ) == 0 ) {
					editorPanel.getSpinner(VISIT_COUNT).setValue( new Integer( 1 ) );
				}

				for ( int i=0; i < listCombos.length; i++ ) {
					editorPanel.getCombo( listCombos[i] ).setSelectedItem( defaultLists[i] );
				}

				// Wait for the image combos to populate.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						for ( int i=0; i < imageCombos.length; i++ ) {
							editorPanel.getCombo( imageCombos[i] ).setSelectedItem( randomImages[i] );
							if ( imageCombos[i].equals( SPRITE_IMAGE ) ) {
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

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}

	private void showPlayerShipEditor( final PlayerShipSprite playerShipSprite ) {

		String title = String.format("Player Ship (Beacon %02d)", mapLayout.getBeaconId( playerShipSprite ) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		addSidePanelSeparator( 6 );

		JButton moveBtn = new JButton( "Move To..." );
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( moveBtn );

		moveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				movePlayerShip( playerShipSprite );
			}
		});

		showSidePanel();
	}

	private void showStoreEditor( final SpriteReference<BeaconState> beaconRef ) {
		final String FUEL = "Fuel";
		final String MISSILES = "Missiles";
		final String DRONE_PARTS = "Drone Parts";

		final StoreSprite storeSprite = beaconRef.getSprite( StoreSprite.class );

		String title = String.format("Store (Beacon %02d)", mapLayout.getBeaconId( storeSprite ) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( FUEL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( MISSILES, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( DRONE_PARTS, FieldEditorPanel.ContentType.INTEGER );

		editorPanel.getInt(FUEL).setText( ""+ beaconRef.get().getStore().getFuel() );
		editorPanel.getInt(MISSILES).setText( ""+ beaconRef.get().getStore().getMissiles() );
		editorPanel.getInt(DRONE_PARTS).setText( ""+ beaconRef.get().getStore().getDroneParts() );

		final JTabbedPane shelfTabsPane = new JTabbedPane();
		shelfTabsPane.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		final List<StoreShelfPanel> shelfPanels = new ArrayList<StoreShelfPanel>();

		for ( StoreShelf shelf : beaconRef.get().getStore().getShelfList() ) {
			StoreShelfPanel shelfPanel = new StoreShelfPanel( frame );
			shelfPanel.setShelf( shelf );
			shelfPanels.add( shelfPanel );
			shelfTabsPane.addTab( "Shelf #"+ (shelfPanels.size()-1), shelfPanel );
		}

		JPanel extraPanel = new JPanel();
		extraPanel.setLayout( new BoxLayout( extraPanel, BoxLayout.Y_AXIS ) );

		JPanel shelfCtrlPanel = new JPanel();
		shelfCtrlPanel.setLayout( new BoxLayout( shelfCtrlPanel, BoxLayout.X_AXIS ) );
		JButton shelfRemBtn = new JButton( "-1 Shelf" );
		shelfRemBtn.addMouseListener( new StatusbarMouseListener( frame, "Remove a shelf. (FTL 1.01-1.03.3 is limited to two shelves)" ) );
		shelfCtrlPanel.add( shelfRemBtn );
		shelfCtrlPanel.add( Box.createRigidArea( new Dimension( 15, 1 ) ) );
		JButton shelfAddBtn = new JButton( "+1 Shelf" );
		shelfAddBtn.addMouseListener( new StatusbarMouseListener( frame, "Add a shelf. (FTL 1.01-1.03.3 is limited to two shelves)" ) );
		shelfCtrlPanel.add( shelfAddBtn );
		extraPanel.add( shelfCtrlPanel );

		extraPanel.add( Box.createVerticalStrut( 10 ) );

		shelfTabsPane.setMaximumSize( new Dimension( Integer.MAX_VALUE, shelfTabsPane.getPreferredSize().height ) );
		extraPanel.add( shelfTabsPane );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				try { beaconRef.get().getStore().setFuel( editorPanel.parseInt( FUEL ) ); }
				catch ( NumberFormatException e ) {}

				try { beaconRef.get().getStore().setMissiles( editorPanel.parseInt( MISSILES ) ); }
				catch ( NumberFormatException e ) {}

				try { beaconRef.get().getStore().setDroneParts( editorPanel.parseInt( DRONE_PARTS ) ); }
				catch ( NumberFormatException e ) {}

				beaconRef.get().getStore().getShelfList().clear();
				for ( StoreShelfPanel shelfPanel : shelfPanels ) {
					StoreShelf newShelf = new StoreShelf();

					SavedGameParser.StoreItemType itemType = shelfPanel.getItemType();
					List<SavedGameParser.StoreItem> items = shelfPanel.getItems();

					newShelf.setItemType( itemType );
					for ( SavedGameParser.StoreItem item : items ) {
						newShelf.addItem( item );
					}

					beaconRef.get().getStore().addShelf( newShelf );
				}

				beaconRef.fireReferenceChange();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, extraPanel, applyCallback );

		addSidePanelSeparator( 6 );

		JButton removeBtn = new JButton( "Remove" );
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( removeBtn );

		shelfRemBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( shelfPanels.size() > 1 ) {
					int lastIndex = shelfPanels.size()-1;
					shelfPanels.remove( lastIndex );
					shelfTabsPane.removeTabAt( lastIndex );
				}
			}
		});

		shelfAddBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				StoreShelfPanel shelfPanel = new StoreShelfPanel( frame );
				shelfPanels.add( shelfPanel );
				shelfTabsPane.addTab( "Shelf #"+ (shelfPanels.size()-1), shelfPanel );
				shelfTabsPane.setSelectedIndex( shelfPanels.size()-1 );
			}
		});

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				storeSprites.remove( storeSprite );
				mapPanel.remove( storeSprite );
				beaconRef.get().setStore( null );

				beaconRef.fireReferenceChange();
			}
		});

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}

	private void showQuestEditor( final QuestSprite questSprite ) {
		final String ENCOUNTERS_FILE = "File";
		final String EVENT = "Event";

		final Map<String, Encounters> allEncountersMap = DataManager.get().getEncounters();

		String title = String.format("Quest (Beacon %02d)", mapLayout.getBeaconId( questSprite ) );

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
			FTLEvent currentEvent = entry.getValue().getEventById( questSprite.getQuestId() );
			FTLEventList currentEventList = entry.getValue().getEventListById( questSprite.getQuestId() );
			if ( currentEvent != null || currentEventList != null ) {
				editorPanel.getCombo(ENCOUNTERS_FILE).setSelectedItem( entry.getKey() );

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
					editorPanel.getCombo(EVENT).setSelectedItem( currentEvent );
				} else if ( currentEventList != null ) {
					editorPanel.getCombo(EVENT).setSelectedItem( currentEventList );
				} else {
					editorPanel.getCombo(EVENT).setSelectedItem( "" );
				}
				break;
			}
		}
		// If no file contains the current event, complain.
		if ( "".equals( editorPanel.getCombo(ENCOUNTERS_FILE).getSelectedItem() ) ) {
			String message = "The current event/eventlist id is unrecognized: "+ questSprite.getQuestId() +".";
			log.error( message );
			frame.setStatusText( message );
		}

		final Runnable applyCallback = new Runnable() {
			@Override
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
		createSidePanel( title, editorPanel, null, applyCallback );

		ActionListener questListener = new ActionListener() {
			private JComboBox fileCombo = editorPanel.getCombo(ENCOUNTERS_FILE);
			private JComboBox eventCombo = editorPanel.getCombo(EVENT);

			@Override
			public void actionPerformed( ActionEvent e ) {
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
							editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
							fitSidePanel();
						}
					}
				}
			}
		};
		editorPanel.getCombo(ENCOUNTERS_FILE).addActionListener( questListener );

		addSidePanelSeparator( 6 );

		JButton removeBtn = new JButton( "Remove" );
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( removeBtn );

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				questSprites.remove( questSprite );
				mapPanel.remove( questSprite );
			}
		});

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}



	public class BeaconSprite extends JComponent implements ReferenceSprite<BeaconState> {
		private BufferedImage currentImage = null;

		private SpriteReference<BeaconState> beaconRef;


		public BeaconSprite( SpriteReference<BeaconState> beaconRef ) {
			this.beaconRef = beaconRef;

			beaconRef.addSprite( this );
			referenceChanged();
		}

		@Override
		public SpriteReference<BeaconState> getReference() {
			return beaconRef;
		}

		@Override
		public void referenceChanged() {
			if ( FleetPresence.REBEL.equals( beaconRef.get().getFleetPresence() ) ) {
				currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_warning.png", -1*32, -1*32, cachedImages );
			}
			else if ( beaconRef.get().getVisitCount() > 0 ) {
				currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_diamond_blue.png", -1*32, -1*32, cachedImages );
			}
			else {
				currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_diamond_yellow.png", -1*32, -1*32, cachedImages );
			}
			this.setPreferredSize( new Dimension( currentImage.getWidth(), currentImage.getHeight() ) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;

			// If under attack, paint a 24x24 red (#AA2D1F) circle.
			if ( beaconRef.get().isUnderAttack() ) {
				g2d.setColor( new Color(170, 45, 31) );
				int diameter = 24;
				g2d.fill( new Ellipse2D.Double(this.getWidth()/2-diameter/2, this.getHeight()/2-diameter/2, diameter, diameter) );
			}
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class StoreSprite extends JComponent implements ReferenceSprite<BeaconState> {
		private BufferedImage currentImage = null;

		private SpriteReference<BeaconState> beaconRef;


		public StoreSprite( SpriteReference<BeaconState> beaconRef ) {
			this.beaconRef = beaconRef;

			currentImage = ImageUtilities.getScaledImage( "img/map/map_box_store.png", -1*80, -1*40, cachedImages );
			this.setPreferredSize( new Dimension( currentImage.getWidth(), currentImage.getHeight() ) );

			beaconRef.addSprite( this );
			referenceChanged();
		}

		@Override
		public SpriteReference<BeaconState> getReference() {
			return beaconRef;
		}

		@Override
		public void referenceChanged() {
		}

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
			currentImage = ImageUtilities.getScaledImage( "img/map/map_box_quest.png", -1*80, -1*40, cachedImages );
			this.setPreferredSize( new Dimension( currentImage.getWidth(), currentImage.getHeight() ) );
		}

		public void setQuestId( String s ) { questId = s; }
		public String getQuestId() { return questId; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this );
		}
	}



	public class PlayerShipSprite extends JComponent {
		private BufferedImage currentImage = null;

		public PlayerShipSprite() {
			currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_ship.png", -1*64, -1*64, cachedImages );
			this.setPreferredSize( new Dimension( currentImage.getWidth(), currentImage.getHeight() ) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this );
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
