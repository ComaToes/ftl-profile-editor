package net.blerf.ftl.ui;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.ShipAvailability;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.IconCycleButton;
import net.blerf.ftl.ui.ImageUtilities;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.ShipBlueprint;


public class ProfileShipUnlockPanel extends JPanel implements ActionListener {

	private static final Logger log = LoggerFactory.getLogger( ProfileShipUnlockPanel.class );

	private static final int ACH_LOCKED = 0;
	private static final int SHIP_LOCKED = 0;
	private static final int SHIP_UNLOCKED = 1;

	private Map<String, IconCycleButton> shipABoxes = new HashMap<String, IconCycleButton>();
	private Map<String, IconCycleButton> shipCBoxes = new HashMap<String, IconCycleButton>();
	private Map<Achievement, IconCycleButton> shipAchBoxes = new HashMap<Achievement, IconCycleButton>();

	private FTLFrame frame;

	private JButton allShipsBtn;
	private JButton allShipAchsBtn;


	public ProfileShipUnlockPanel( FTLFrame frame ) throws IOException {
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		this.frame = frame;

		ImageIcon unlockIcon = new ImageIcon( ImageUtilities.getBundledImage( "unlock.png", FTLFrame.class ) );

		// Unlocks.
		JPanel shipsPanel = new JPanel();
		shipsPanel.setLayout( new GridLayout( 0, 3 ) );
		shipsPanel.setBorder( BorderFactory.createTitledBorder( "Ship Unlocks" ) );
		this.add( shipsPanel );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			JPanel panel = createShipUnlockPanel( baseId );
			if ( panel != null ) shipsPanel.add( panel );
		}

		JPanel allShipsPanel = new JPanel( new GridBagLayout() );
		allShipsPanel.setBorder( BorderFactory.createTitledBorder( " " ) );
		allShipsBtn = new JButton( "Unlock All", unlockIcon );
		allShipsBtn.addMouseListener( new StatusbarMouseListener( frame, "Unlock All Ships (except Type-B)." ) );
		allShipsBtn.addActionListener( this );
		allShipsPanel.add( allShipsBtn );
		shipsPanel.add( allShipsPanel );

		// Layout achievements.
		JPanel shipAchsPanel = new JPanel();
		shipAchsPanel.setLayout( new GridLayout( 0, 3 ) );
		shipAchsPanel.setBorder( BorderFactory.createTitledBorder( "Ship Achievements" ) );
		this.add( shipAchsPanel );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			JPanel panel = createShipAchPanel( baseId );
			if ( panel != null ) shipAchsPanel.add( panel );
		}

		JPanel allShipAchsPanel = new JPanel( new GridBagLayout() );
		allShipAchsPanel.setBorder( BorderFactory.createTitledBorder( " " ) );
		allShipAchsBtn = new JButton( "Unlock All", unlockIcon );
		allShipAchsBtn.addMouseListener( new StatusbarMouseListener( frame, "Unlock All Ship Achievements (and Type-B ships)." ) );
		allShipAchsBtn.addActionListener( this );
		allShipAchsPanel.add( allShipAchsBtn );
		shipAchsPanel.add( allShipAchsPanel );
	}

	private JPanel createShipUnlockPanel( String baseId ) {

		log.trace( "Creating ship unlock panel for: "+ baseId );

		ShipBlueprint variantAShip = DataManager.get().getPlayerShipVariant( baseId, 0 );
		ShipBlueprint variantBShip = DataManager.get().getPlayerShipVariant( baseId, 1 );
		ShipBlueprint variantCShip = DataManager.get().getPlayerShipVariant( baseId, 2 );
		if ( variantAShip == null ) return null;

		String shipClass = variantAShip.getShipClass().getTextValue();

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.X_AXIS ) );
		panel.setBorder( BorderFactory.createTitledBorder( shipClass ) );

		IconCycleButton shipABox = ImageUtilities.createCycleButton( "img/ship/"+ variantAShip.getGraphicsBaseName() +"_base.png", false );
		shipABox.addMouseListener( new StatusbarMouseListener( frame, "Type-A: "+ variantAShip.getName().getTextValue() ) );
		shipABoxes.put( baseId, shipABox );
		panel.add( shipABox );

		if ( variantBShip != null ) {
			IconCycleButton shipBBox = ImageUtilities.createDummyCycleButton();
			shipBBox.addMouseListener( new StatusbarMouseListener( frame, "Type-B: "+ variantBShip.getName().getTextValue() +" (To unlock, choose two ship achievements below.)" ) );
			panel.add( shipBBox );
		}
		else {
			IconCycleButton shipBBox = ImageUtilities.createDummyCycleButton();
			shipBBox.addMouseListener( new StatusbarMouseListener( frame, "Type-B: N/A" ) );
			panel.add( shipBBox );
		}

		if ( variantCShip != null ) {
			IconCycleButton shipCBox = ImageUtilities.createCycleButton( "img/ship/"+ variantCShip.getGraphicsBaseName() +"_base.png", false );
			shipCBox.addMouseListener( new StatusbarMouseListener( frame, "Type-C: "+ variantCShip.getName().getTextValue() ) );
			shipCBoxes.put( baseId, shipCBox );
			panel.add( shipCBox );
		}
		else {
			IconCycleButton shipCBox = ImageUtilities.createDummyCycleButton();
			shipCBox.addMouseListener( new StatusbarMouseListener( frame, "Type-C: N/A" ) );
			shipCBoxes.put( baseId, shipCBox );
			panel.add( shipCBox );
		}

		return panel;
	}

	private JPanel createShipAchPanel( String baseId ) {

		log.trace( "Creating ship achievements panel for: "+ baseId );

		ShipBlueprint variantAShip = DataManager.get().getPlayerShipVariant( baseId, 0 );
		if ( variantAShip == null ) return null;

		String shipClass = variantAShip.getShipClass().getTextValue();

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		panel.setBorder( BorderFactory.createTitledBorder( shipClass ) );

		List<Achievement> shipAchs = DataManager.get().getShipAchievements( variantAShip );		
		if ( shipAchs != null ) {
			for ( Achievement shipAch : shipAchs ) {
				if ( shipAch.isVictory() || shipAch.isQuest() ) continue;

				IconCycleButton box = ImageUtilities.createCycleButton( "img/" + shipAch.getImagePath(), true );
				box.setToolTipText( shipAch.getName().getTextValue() );

				String achDesc = shipAch.getDescription().getTextValue().replaceAll( "(\r\n|\r|\n)+", " " );
				box.addMouseListener( new StatusbarMouseListener( frame, achDesc ) );

				shipAchBoxes.put( shipAch, box );
				panel.add( box );
			}
		}
		else {
			return null;
		}
		
		return panel;
	}

	private int getCycleStateForDifficulty( Difficulty d ) {
		if ( d == null ) return ACH_LOCKED;
		return ( 1 + d.ordinal() );                // LOCKED plus 0-based enum index.
	}

	private Difficulty getDifficultyForCycleState( int cycleState ) {
		if ( cycleState == ACH_LOCKED ) return null;
		return Difficulty.values()[cycleState-1];  // Adjust array index because of LOCKED.
	}

	public void unlockAllShips() {
		for ( IconCycleButton box : shipABoxes.values() ) {
			if ( box.getSelectedState() == SHIP_LOCKED ) {
				box.setSelectedState( SHIP_UNLOCKED );
			}
		}
		for ( IconCycleButton box : shipCBoxes.values() ) {
			if ( box.getSelectedState() == SHIP_LOCKED ) {
				box.setSelectedState( SHIP_UNLOCKED );
			}
		}
	}

	public void unlockAllShipAchievements() {
		for ( IconCycleButton box : shipAchBoxes.values() ) {
			if ( box.getSelectedState() == ACH_LOCKED ) {
				box.setSelectedState( getCycleStateForDifficulty( Difficulty.EASY ) );
			}
		}
	}

	public void setProfile( Profile p ) {

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			IconCycleButton shipABox = shipABoxes.get( baseId );
			IconCycleButton shipCBox = shipCBoxes.get( baseId );

			ShipAvailability shipAvail = p.getShipUnlockMap().get( baseId );
			if ( shipAvail != null ) {
				if ( shipABox != null ) shipABox.setSelectedState( (shipAvail.isUnlockedA() ? SHIP_UNLOCKED : SHIP_LOCKED) );
				if ( shipCBox != null ) shipCBox.setSelectedState( (shipAvail.isUnlockedC() ? SHIP_UNLOCKED : SHIP_LOCKED) );
			}
			else {
				if ( shipABox != null ) shipABox.setSelectedState( SHIP_LOCKED );
				if ( shipCBox != null ) shipCBox.setSelectedState( SHIP_LOCKED );
			}
		}

		for ( Map.Entry<Achievement, IconCycleButton> entry : shipAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			IconCycleButton box = entry.getValue();

			box.setSelectedState( ACH_LOCKED );

			AchievementRecord rec = AchievementRecord.getFromListById( p.getAchievements(), achId );
			if ( rec != null ) {
				box.setSelectedState( getCycleStateForDifficulty( rec.getDifficulty() ) );
			}
		}
		this.repaint();
	}

	public void updateProfile( Profile p ) {

		ArrayList<AchievementRecord> newAchRecs = new ArrayList<AchievementRecord>();
		newAchRecs.addAll( p.getAchievements() );

		for ( Map.Entry<Achievement, IconCycleButton> entry : shipAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			IconCycleButton box = entry.getValue();

			Difficulty diff = getDifficultyForCycleState( box.getSelectedState() );

			if ( diff != null ) {
				// Add unlocked achievement recs if not already present.

				AchievementRecord rec = AchievementRecord.getFromListById( newAchRecs, achId );
				if ( rec != null ) {
					rec.setDifficulty( diff );
				}
				else {
					rec = new AchievementRecord( achId, diff );
					newAchRecs.add( rec );
				}
			}
			else {
				// Remove achievement recs that are locked.
				AchievementRecord.removeFromListById( newAchRecs, achId );
			}
		}

		Map<String, ShipAvailability> shipUnlockMap = new LinkedHashMap<String, ShipAvailability>();
		shipUnlockMap.putAll( p.getShipUnlockMap() );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			IconCycleButton shipABox = shipABoxes.get( baseId );
			IconCycleButton shipCBox = shipCBoxes.get( baseId );

			boolean unlockedA = ( shipABox != null && shipABox.getSelectedState() == SHIP_UNLOCKED );
			boolean unlockedC = ( shipCBox != null && shipCBox.getSelectedState() == SHIP_UNLOCKED );
			ShipAvailability shipAvail = shipUnlockMap.get( baseId );
			if ( shipAvail == null ) {
				shipAvail = new ShipAvailability( baseId );
				shipUnlockMap.put( baseId, shipAvail );
			}
			shipAvail.setUnlockedA( unlockedA );
			shipAvail.setUnlockedC( unlockedC );

			// Remove ship achievements for locked ships.
			// TODO: FTL:AE permits Type-B and Type-C ships in the absense of Type-A.
			// Original FTL's menus expected Type-A at least to be present.
			//if ( !unlockedA ) {
			//	List<Achievement> shipAchs = DataManager.get().getShipAchievements( DataManager.get().getPlayerShipVariant( baseId, 0 ) );
			//	if ( shipAchs != null ) {
			//		for ( Achievement shipAch : shipAchs ) {
			//			if ( shipAch.isVictory() || shipAch.isQuest() ) continue;
			//
			//			// Search for records with the doomed id.
			//			AchievementRecord.removeFromListById( newAchRecs, shipAch.getId() );
			//		}
			//	}
			//}
		}
		p.setShipUnlockMap( shipUnlockMap );

		p.setAchievements( newAchRecs );
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();

		if ( source == allShipsBtn ) {
			unlockAllShips();
		}
		else if ( source == allShipAchsBtn ) {
			unlockAllShipAchievements();
		}
	}
}
