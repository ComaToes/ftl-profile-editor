package net.blerf.ftl.ui;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.blerf.ftl.model.CrewRecord;
import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Stats;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ProfileStatsPanel extends JPanel {

	private static final Logger log = LogManager.getLogger(ProfileStatsPanel.class);

	private FTLFrame frame;

	private JPanel topScoresPanel;
	private StatsSubPanel sessionRecordsPanel;
	private StatsSubPanel crewRecordsPanel;
	private StatsSubPanel totalStatsPanel;

	public ProfileStatsPanel( FTLFrame frame ) {
		this.frame = frame;

		this.setLayout( new GridLayout(0, 2) );

		topScoresPanel = new JPanel();
		topScoresPanel.setLayout( new BoxLayout(topScoresPanel, BoxLayout.Y_AXIS ) );
		topScoresPanel.setBorder( BorderFactory.createTitledBorder("Top Scores") );
		this.add( topScoresPanel );

		JPanel statsSubPanelsHolder = new JPanel();
		statsSubPanelsHolder.setLayout( new BoxLayout(statsSubPanelsHolder, BoxLayout.Y_AXIS) );
		this.add( statsSubPanelsHolder );
		
		sessionRecordsPanel = new StatsSubPanel();
		sessionRecordsPanel.addFillRow();
		sessionRecordsPanel.setBorder( BorderFactory.createTitledBorder("Session Records") );
		statsSubPanelsHolder.add( sessionRecordsPanel );
		
		crewRecordsPanel = new StatsSubPanel();
		crewRecordsPanel.addFillRow();
		crewRecordsPanel.setBorder( BorderFactory.createTitledBorder("Crew Records") );
		statsSubPanelsHolder.add( crewRecordsPanel );

		totalStatsPanel = new StatsSubPanel();
		totalStatsPanel.addFillRow();
		totalStatsPanel.setBorder( BorderFactory.createTitledBorder("Totals") );
		statsSubPanelsHolder.add( totalStatsPanel );
	}

	public void setProfile( Profile p ) throws IOException {

		topScoresPanel.removeAll();
		int i = 0;
		for ( Score s : p.getStats().getTopScores() ) {
			InputStream stream = null;
			try {
				ShipBlueprint ship = DataManager.get().getShip( s.getShipType() );
				stream = DataManager.get().getResourceInputStream("img/ship/"+ship.getImg()+"_base.png");
				Image img = frame.getScaledImage( stream );
				TopScorePanel tsp = new TopScorePanel( ++i, img, s.getShipName(), s.getScore(), s.getSector(), s.getDifficulty() );
				topScoresPanel.add( tsp );

			}	finally {
				try {if (stream != null) stream.close();}
				catch (IOException f) {}
			}
		}

		Stats stats = p.getStats();

		sessionRecordsPanel.removeAll();
		sessionRecordsPanel.addRow("Most Ships Defeated", null, null, stats.getMostShipsDefeated());
		sessionRecordsPanel.addRow("Most Beacons Explored", null, null, stats.getMostBeaconsExplored());
		sessionRecordsPanel.addRow("Most Scrap Collected", null, null, stats.getMostScrapCollected());
		sessionRecordsPanel.addRow("Most Crew Hired", null, null, stats.getMostCrewHired());
		sessionRecordsPanel.addFillRow();

		crewRecordsPanel.removeAll();
		CrewRecord repairCrewRecord = stats.getMostRepairs();
		CrewRecord killsCrewRecord = stats.getMostKills();
		CrewRecord evasionsCrewRecord = stats.getMostEvasions();
		CrewRecord jumpsCrewRecord = stats.getMostJumps();
		CrewRecord skillsCrewRecord = stats.getMostSkills();

		crewRecordsPanel.addRow("Most Repairs", frame.getCrewIcon(repairCrewRecord.getRace()), repairCrewRecord.getName(), repairCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Combat Kills", frame.getCrewIcon(killsCrewRecord.getRace()), killsCrewRecord.getName(), killsCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Piloted Evasions", frame.getCrewIcon(evasionsCrewRecord.getRace()), evasionsCrewRecord.getName(), evasionsCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Jumps Survived", frame.getCrewIcon(jumpsCrewRecord.getRace()), jumpsCrewRecord.getName(), jumpsCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Skill Masteries", frame.getCrewIcon(skillsCrewRecord.getRace()), skillsCrewRecord.getName(), skillsCrewRecord.getScore());
		crewRecordsPanel.addFillRow();

		totalStatsPanel.removeAll();
		totalStatsPanel.addRow("Total Ships Defeated", null, null, stats.getTotalShipsDefeated());
		totalStatsPanel.addRow("Total Beacons Explored", null, null, stats.getTotalBeaconsExplored());
		totalStatsPanel.addRow("Total Scrap Collected", null, null, stats.getTotalScrapCollected());
		totalStatsPanel.addRow("Total Crew Hired", null, null, stats.getTotalCrewHired());
		totalStatsPanel.addBlankRow();
		totalStatsPanel.addRow("Total Games Played", null, null, stats.getTotalGamesPlayed());
		totalStatsPanel.addRow("Total Victories", null, null, stats.getTotalVictories());
		totalStatsPanel.addFillRow();

		this.repaint();
	}



	private class StatsSubPanel extends JPanel {
		private int COLUMN_COUNT = 0;
		private final int NAME_COL = COLUMN_COUNT++;
		private final int RECIPIENT_COL = COLUMN_COUNT++;
		private final int VALUE_COL = COLUMN_COUNT++;

		GridBagConstraints gridC = null;

		public StatsSubPanel() {
			super(new GridBagLayout());
			removeAll();
		}

		@Override
		public void removeAll() {
			super.removeAll();
			gridC = new GridBagConstraints();
			gridC.anchor = GridBagConstraints.WEST;
			gridC.fill = GridBagConstraints.NONE;
			gridC.weightx = 1.0;
			gridC.weighty = 0.0;
			gridC.insets = new Insets(2, 4, 2, 4);
			gridC.gridwidth = 1;
			gridC.gridx = 0;
			gridC.gridy = 0;
		}

		public void addRow(String name, ImageIcon icon, String recipient, int value) {
			gridC.gridx = NAME_COL;
			gridC.anchor = GridBagConstraints.WEST;
			gridC.fill = GridBagConstraints.NONE;
			gridC.weightx = 1.0;
			JLabel nameLbl = new JLabel(name);
			this.add(nameLbl, gridC);

			gridC.gridx = RECIPIENT_COL;
			gridC.anchor = GridBagConstraints.CENTER;
			gridC.fill = GridBagConstraints.NONE;
			gridC.weightx = 1.0;
			JLabel recipientLbl = new JLabel();
			recipientLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
			if (recipient != null)
				recipientLbl.setText(recipient);
			if (icon != null)
				recipientLbl.setIcon(icon);
			this.add(recipientLbl, gridC);

			gridC.gridx = VALUE_COL;
			gridC.anchor = GridBagConstraints.CENTER;
			gridC.weightx = 0.0;
			JLabel valueLbl = new JLabel(Integer.toString(value));
			this.add(valueLbl, gridC);

			gridC.gridy++;
		}

		public void addBlankRow() {
			gridC.fill = GridBagConstraints.NONE;
			gridC.weighty = 0.0;
			gridC.gridwidth = GridBagConstraints.REMAINDER;
			gridC.gridx = 0;

			this.add(Box.createVerticalStrut(12), gridC);
			gridC.gridy++;
		}

		public void addFillRow() {
			gridC.fill = GridBagConstraints.VERTICAL;
			gridC.weighty = 1.0;
			gridC.gridwidth = GridBagConstraints.REMAINDER;
			gridC.gridx = 0;

			this.add(Box.createVerticalGlue(), gridC);
			gridC.gridy++;
		}
	}
}
