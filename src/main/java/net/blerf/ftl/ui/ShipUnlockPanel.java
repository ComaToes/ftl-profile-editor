package net.blerf.ftl.ui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.Score.Difficulty;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.IconCycleButton;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ShipUnlockPanel extends JPanel {

	private static final int ACH_LOCKED = 0;
	private static final int ACH_EASY = 1;
	private static final int ACH_NORMAL = 2;
	private static final int SHIP_LOCKED = 0;
	private static final int SHIP_UNLOCKED = 1;

	private static final Logger log = LogManager.getLogger(ShipUnlockPanel.class);

	private FTLFrame frame;

	private ArrayList<IconCycleButton> shipBoxes = new ArrayList<IconCycleButton>();
	private HashMap<Achievement, IconCycleButton> shipAchBoxes = new HashMap<Achievement, IconCycleButton>();

	public ShipUnlockPanel( FTLFrame frame ) {
		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
		this.frame = frame;

		log.trace( "Creating Ship Unlock panel" );

		log.trace("Adding ship unlocks");

		JPanel shipPanel = new JPanel();
		shipPanel.setLayout( new GridLayout(0, 3) );
		shipPanel.setBorder( BorderFactory.createTitledBorder("Ship Unlocks") );
		this.add( shipPanel );

		for ( ShipBlueprint ship : DataManager.get().getPlayerShips() ) {
			JPanel boxPanel = new JPanel();
			boxPanel.setLayout( new BoxLayout(boxPanel, BoxLayout.X_AXIS) );

			boxPanel.add( Box.createHorizontalStrut(5) );
			IconCycleButton shipBox = frame.createCycleButton( "img/ship/" + ship.getImg() + "_base.png", false );
			boxPanel.add( shipBox );
			boxPanel.add( Box.createHorizontalStrut(10) );
			JLabel shipLbl = new JLabel();
			shipLbl.setText( ship.getShipClass() );
			boxPanel.add( shipLbl );
			boxPanel.add( Box.createHorizontalGlue() );
			boxPanel.add( Box.createHorizontalStrut(5) );

			shipBoxes.add(shipBox);
			shipPanel.add(boxPanel);
		}

		log.trace("Adding ship achievements");

		JPanel shipAchPanel = new JPanel();
		shipAchPanel.setLayout( new GridLayout(0, 3) );
		shipAchPanel.setBorder( BorderFactory.createTitledBorder("Ship Achievements") );
		this.add( shipAchPanel );

		for ( ShipBlueprint ship : DataManager.get().getPlayerShips() )
			shipAchPanel.add( createShipPanel( ship ) );
	}

	private JPanel createShipPanel( ShipBlueprint ship ) {

		log.trace("Creating ship panel for: " + ship.getId());
		
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		panel.setBorder( BorderFactory.createTitledBorder( ship.getShipClass() ) );
		
		for ( Achievement shipAch : DataManager.get().getShipAchievements(ship) ) {
			IconCycleButton box = frame.createCycleButton( "img/" + shipAch.getImagePath(), true );
			box.setToolTipText( shipAch.getName() );

			String achDesc = shipAch.getDescription().replaceAll("(\r\n|\r|\n)+", " ");
			box.addMouseListener( new StatusbarMouseListener(frame, achDesc) );

			shipAchBoxes.put( shipAch, box );
			panel.add( box );
		}
		
		return panel;
	}

	public void unlockAllShips() {
		for ( IconCycleButton box : shipBoxes ) {
			if ( box.getSelectedState() == SHIP_LOCKED )
				box.setSelectedState( SHIP_UNLOCKED );
		}
	}

	public void unlockAllShipAchievements() {
		for ( IconCycleButton box : shipAchBoxes.values() ) {
			if ( box.getSelectedState() == ACH_LOCKED )
				box.setSelectedState( ACH_EASY );
		}
	}

	public void setProfile( Profile p ) {

		boolean[] unlocks = p.getShipUnlocks();
		for (int i = 0; i < shipBoxes.size(); i++) {
			shipBoxes.get(i).setSelectedState( (unlocks[i] ? SHIP_UNLOCKED : SHIP_LOCKED) );
		}

		for ( Map.Entry<Achievement, IconCycleButton> entry : shipAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			IconCycleButton box = entry.getValue();

			box.setSelectedState( ACH_LOCKED );

			for ( AchievementRecord rec : p.getAchievements() ) {
				if ( rec.getAchievementId().equals( achId ) ) {
					if ( rec.getDifficulty() == Difficulty.NORMAL )
						box.setSelectedState( ACH_NORMAL );
					else
						box.setSelectedState( ACH_EASY );
				}
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
			if ( box.getSelectedState() != ACH_LOCKED ) {
				// Add selected achievement recs if not already present.

				Difficulty difficulty = null;
				if ( box.getSelectedState() == ACH_NORMAL )
					difficulty = Difficulty.NORMAL;
				else
					difficulty = Difficulty.EASY;

				AchievementRecord newAch = AchievementRecord.getFromListById( newAchRecs, achId );
				if ( newAch != null ) {
					newAch.setDifficulty( difficulty );
				} else {
					newAch = new AchievementRecord( achId, difficulty );
					newAchRecs.add( newAch );
				}

			} else {
				// Remove achievement recs that are not selected.
				AchievementRecord.removeFromListById( newAchRecs, achId );
			}
		}

		boolean[] unlocks = p.getShipUnlocks();
		for (int i=0; i < shipBoxes.size(); i++) {
			unlocks[i] = ( shipBoxes.get(i).getSelectedState() == SHIP_UNLOCKED );

			// Remove ship achievements for locked ships
			if ( !unlocks[i] ) {
				for ( Achievement shipAch : DataManager.get().getShipAchievements( DataManager.get().getPlayerShips().get(i) ) ) {
					// Search for records with the doomed id.
					AchievementRecord.removeFromListById( newAchRecs, shipAch.getId() );
				}
			}
		}
		p.setShipUnlocks(unlocks);

		p.setAchievements(newAchRecs);
	}
}
