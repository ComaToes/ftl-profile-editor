package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.StoreItem;
import net.blerf.ftl.parser.SavedGameParser.StoreItemType;
import net.blerf.ftl.parser.SavedGameParser.StoreShelf;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class StoreShelfPanel extends JPanel implements ActionListener {

	private static final Logger log = LogManager.getLogger(StoreShelfPanel.class);

	private static final String SHELF_TYPE = "Type";
	private static final String ITEM_ZERO = "#0 Item";
	private static final String AVAIL_ZERO = "#0 In Stock";
	private static final String EXTRA_ZERO = "#0 Extra";
	private static final String ITEM_ONE = "#1 Item";
	private static final String AVAIL_ONE = "#1 In Stock";
	private static final String EXTRA_ONE = "#1 Extra";
	private static final String ITEM_TWO = "#2 Item";
	private static final String AVAIL_TWO = "#2 In Stock";
	private static final String EXTRA_TWO = "#2 Extra";

	private static final String[] SLOTS = new String[] { ITEM_ZERO, ITEM_ONE, ITEM_TWO };
	private static final String[] AVAIL = new String[] { AVAIL_ZERO, AVAIL_ONE, AVAIL_TWO };
	private static final String[] EXTRA = new String[] { EXTRA_ZERO, EXTRA_ONE, EXTRA_TWO };

	private FTLFrame frame;

	private StoreItemType[] itemTypes = null;
	private Map<StoreItemType, Map> itemLookups = null;

	private FieldEditorPanel editorPanel = null;


	public StoreShelfPanel( FTLFrame frame ) {
		super( new BorderLayout() );
		this.frame = frame;

		itemTypes = StoreItemType.values();

		// Build interchangeable id-vs-toStringable maps, mapped by item type.
		Map weaponLookup = DataManager.get().getWeapons();
		Map droneLookup = DataManager.get().getDrones();
		Map augmentLookup = DataManager.get().getAugments();
		Map<String, CrewType> crewLookup = new LinkedHashMap<String, CrewType>();
		for ( CrewType race : new CrewType[] {
			CrewType.ANAEROBIC, CrewType.CRYSTAL, CrewType.ENERGY,
			CrewType.ENGI, CrewType.HUMAN, CrewType.MANTIS,
			CrewType.ROCK, CrewType.SLUG
			} ) {
			crewLookup.put( race.getId(), race );
		}
		Map<String, SystemType> systemLookup = new LinkedHashMap<String, SystemType>();
		for ( SystemType systemType : SystemType.values() ) {
			systemLookup.put( systemType.getId(), systemType );
		}
		itemLookups = new HashMap<StoreItemType, Map>();
		itemLookups.put( StoreItemType.WEAPON, weaponLookup );
		itemLookups.put( StoreItemType.DRONE, droneLookup );
		itemLookups.put( StoreItemType.AUGMENT, augmentLookup );
		itemLookups.put( StoreItemType.CREW, crewLookup );
		itemLookups.put( StoreItemType.SYSTEM, systemLookup );

		editorPanel = new FieldEditorPanel( false );

		editorPanel.addRow( SHELF_TYPE, FieldEditorPanel.ContentType.COMBO );

		for (int i=0; i < itemTypes.length; i++) {
			editorPanel.getCombo(SHELF_TYPE).addItem( itemTypes[i] );
		}

		for (int i=0; i < SLOTS.length; i++) {
			editorPanel.addRow( SLOTS[i], FieldEditorPanel.ContentType.COMBO );
			editorPanel.addRow( AVAIL[i], FieldEditorPanel.ContentType.BOOLEAN );
			editorPanel.addRow( EXTRA[i], FieldEditorPanel.ContentType.INTEGER );

			editorPanel.getBoolean(AVAIL[i]).addMouseListener( new StatusbarMouseListener(frame, "Toggle whether this item has already been bought.") );
			editorPanel.getInt(EXTRA[i]).addMouseListener( new StatusbarMouseListener(frame, "Misc info (DroneCtrl system only, specifying bonus drone).") );

			editorPanel.getCombo(SLOTS[i]).addItem( "" );
		}

		this.add( editorPanel, BorderLayout.CENTER );

		setListenerEnabled( true );
	}

	public void setShelf( StoreShelf shelf ) {
		StoreItemType itemType = shelf.getItemType();
		setItemType( itemType );

		Map lookupMap = itemLookups.get( itemType );

		List<String> badIds = new ArrayList<String>();

		for (int i=0; i < SLOTS.length; i++) {
			if ( shelf.getItems().size() > i ) {
				if ( lookupMap != null ) {
					String itemId = shelf.getItems().get(i).getItemId();
					Object itemChoice = lookupMap.get( itemId );
					if ( itemChoice != null ) {
						editorPanel.getCombo(SLOTS[i]).setSelectedItem( itemChoice );
					} else {
						editorPanel.getCombo(SLOTS[i]).setSelectedItem( "" );
						badIds.add( itemId );
					}
				}

				editorPanel.getBoolean(AVAIL[i]).setSelected( shelf.getItems().get(i).isAvailable() );
				editorPanel.getInt(EXTRA[i]).setText( ""+shelf.getItems().get(i).getExtraData() );
			}
		}

		if ( badIds.size() > 0 ) {
			StringBuilder errorBuf = new StringBuilder();
			errorBuf.append( "Unrecognized store items:" );
			for ( String badId : badIds ) {
				errorBuf.append( " " ).append( badId );
			}
			errorBuf.append( "." );
			log.error( errorBuf.toString() );
		}
	}

	/**
	 * Returns a list of StoreItems constructed from non-blank selections.
	 */
	@SuppressWarnings("unchecked")
	public List<StoreItem> getItems() {
		List<StoreItem> result = new ArrayList<StoreItem>( 3 );

		Object selectedType = editorPanel.getCombo(SHELF_TYPE).getSelectedItem();

		for (int i=0; i < SLOTS.length; i++) {
			Object selectedItem = editorPanel.getCombo(SLOTS[i]).getSelectedItem();
			if ( "".equals(selectedItem) == false && itemLookups.get(selectedType) != null ) {
				// Do a reverse lookup on the map to get an item id.
				for ( Object entry : itemLookups.get(selectedType).entrySet() ) {
					if ( ((Map.Entry)entry).getValue().equals( selectedItem ) ) {
						boolean available = editorPanel.getBoolean(AVAIL[i]).isSelected();
						String id = (String)((Map.Entry)entry).getKey();

						StoreItem newItem = new StoreItem( id );
						newItem.setAvailable( available );

						try { newItem.setExtraData( editorPanel.parseInt(EXTRA[i]) ); }
						catch ( NumberFormatException e ) {}

						result.add( newItem );
					}
				}
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public StoreItemType getItemType() {
		Object selectedType = editorPanel.getCombo(SHELF_TYPE).getSelectedItem();
		return (StoreItemType)selectedType;
	}

	private void setItemType( StoreItemType itemType ) {
		setListenerEnabled( false );
		Map lookupMap = itemLookups.get( itemType );

		editorPanel.getCombo(SHELF_TYPE).setSelectedItem( itemType );

		for (int i=0; i < SLOTS.length; i++) {
			editorPanel.getCombo(SLOTS[i]).removeAllItems();
			editorPanel.getCombo(SLOTS[i]).addItem( "" );

			if ( lookupMap != null ) {
				for ( Object o : lookupMap.values() ) {
					editorPanel.getCombo(SLOTS[i]).addItem( o );
				}
			}

			editorPanel.getBoolean(AVAIL[i]).setSelected( false );
		}
		setListenerEnabled(true);
	}

	private void setListenerEnabled( boolean b ) {
		if ( b ) {
			editorPanel.getCombo(SHELF_TYPE).addActionListener( this );
			for (int i=0; i < SLOTS.length; i++) {
				editorPanel.getCombo(SLOTS[i]).addActionListener( this );
			}
		}
		else {
			editorPanel.getCombo(SHELF_TYPE).removeActionListener( this );
			for (int i=0; i < SLOTS.length; i++) {
				editorPanel.getCombo(SLOTS[i]).removeActionListener( this );
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed( ActionEvent e ) {

		Object source = e.getSource();

		if ( source == editorPanel.getCombo(SHELF_TYPE) ) {
			// Reset items when the type changes.

			Object selectedType = editorPanel.getCombo(SHELF_TYPE).getSelectedItem();
			setItemType( (StoreItemType)selectedType );
		}
		else {
			// Check if the source was a slot combo.
			for (int i=0; i < SLOTS.length; i++) {
				JComboBox itemCombo = editorPanel.getCombo(SLOTS[i]);
				if ( source == itemCombo ) {
					// Toggle the avail checkbox to true for items, false for "".
					editorPanel.getBoolean(AVAIL[i]).setSelected( !"".equals( itemCombo.getSelectedItem() ) );
					break;
				}
			}
		}
	}
}
