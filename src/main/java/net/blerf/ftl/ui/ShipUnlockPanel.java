package net.blerf.ftl.ui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.Score.Difficulty;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ShipUnlockPanel extends JPanel {

	private static final Logger log = LogManager.getLogger(ShipUnlockPanel.class);

	private FTLFrame frame;

	private ArrayList<JCheckBox> shipBoxes = new ArrayList<JCheckBox>();
	private HashMap<Achievement, JCheckBox> shipAchBoxes = new HashMap<Achievement, JCheckBox>();

	// TODO: allow user selection of difficulty.
	private Difficulty difficulty = Difficulty.EASY;

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
			JCheckBox shipBox = new JCheckBox( ship.getShipClass() );
			frame.setCheckboxIcons(shipBox, "img/ship/" + ship.getImg() + "_base.png");
			shipBoxes.add(shipBox);
			shipPanel.add(shipBox);
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
			JCheckBox box = new JCheckBox();
			frame.setCheckboxIcons(box, "img/" + shipAch.getImagePath() );
			box.setToolTipText( shipAch.getName() );

			String achDesc = shipAch.getDescription().replaceAll("(\r\n|\r|\n)+", " ");
			box.addMouseListener( new StatusbarMouseListener(frame, achDesc) );

			shipAchBoxes.put(shipAch, box);
			panel.add( box );
		}
		
		return panel;
	}

	public void unlockAllShips() {
		for ( JCheckBox box : shipBoxes )
			box.setSelected(true);
	}

	public void unlockAllShipAchievements() {
		for ( JCheckBox box : shipAchBoxes.values() )
			box.setSelected(true);
	}

	public void setProfile( Profile p ) {

		boolean[] unlocks = p.getShipUnlocks();
		for (int i = 0; i < shipBoxes.size(); i++) {
			shipBoxes.get(i).setSelected( unlocks[i] );
		}

		for ( Map.Entry<Achievement, JCheckBox> entry : shipAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			JCheckBox box = entry.getValue();

			box.setSelected(false);

			for ( AchievementRecord rec : p.getAchievements() )
				if ( rec.getAchievementId().equals( achId ) )
					box.setSelected(true);
		}
		this.repaint();
	}

	public void updateProfile( Profile p ) {

		ArrayList<AchievementRecord> newAchRecs = new ArrayList<AchievementRecord>();
		newAchRecs.addAll( p.getAchievements() );

		for ( Map.Entry<Achievement, JCheckBox> entry : shipAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			JCheckBox box = entry.getValue();
			if ( box.isSelected() ) {
				// Add selected achievement recs if not already present.
				if ( !AchievementRecord.listContainsId( newAchRecs, achId ) ) {
					newAchRecs.add( new AchievementRecord(achId, difficulty) );
				}
			} else {
				// Remove achievement recs that are not selected.
				AchievementRecord.removeFromListById( newAchRecs, achId );
			}
		}

		boolean[] unlocks = p.getShipUnlocks();
		for (int i=0; i < shipBoxes.size(); i++) {
			unlocks[i] = shipBoxes.get(i).isSelected();

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
