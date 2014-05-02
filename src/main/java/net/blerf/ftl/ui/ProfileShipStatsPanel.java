package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Stats;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.ScorePanel;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ProfileShipStatsPanel extends JPanel implements ActionListener {

	private static final Logger log = LogManager.getLogger(ProfileShipStatsPanel.class);

	private static final int MAX_SCORE_PANELS = 4;

	private FTLFrame frame;

	private Map<String, BufferedImage> cacheMap = new HashMap<String, BufferedImage>();

	private Map<String, List<Score>> allBestMap = new LinkedHashMap<String, List<Score>>();
	private String currentShipId = null;

	private JComboBox bestCombo = null;
	private JPanel bestScoresPanel = null;
	private List<ScorePanel> bestScorePanels = new ArrayList<ScorePanel>();


	public ProfileShipStatsPanel( FTLFrame frame ) {
		this.frame = frame;

		this.setLayout( new GridLayout(0, 2) );

		JPanel leftPanel = new JPanel( new GridBagLayout() );
		leftPanel.setBorder( BorderFactory.createTitledBorder("Ship Best") );

		GridBagConstraints leftC = new GridBagConstraints();

		leftC.anchor = GridBagConstraints.CENTER;
		leftC.fill = GridBagConstraints.HORIZONTAL;
		leftC.weightx = 1.0;
		leftC.weighty = 0.0;
		leftC.gridx = 0;
		leftC.gridy = 0;
		JPanel bestChooserPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints chooserC = new GridBagConstraints();

		chooserC.fill = GridBagConstraints.HORIZONTAL;
		bestCombo = new JComboBox();
		bestChooserPanel.add( bestCombo );
		leftPanel.add( bestChooserPanel, leftC );

		leftC.anchor = GridBagConstraints.NORTH;
		leftC.fill = GridBagConstraints.BOTH;
		leftC.weightx = 1.0;
		leftC.weighty = 1.0;
		leftC.gridy++;
		bestScoresPanel = new JPanel();
		bestScoresPanel.setLayout( new BoxLayout(bestScoresPanel, BoxLayout.Y_AXIS ) );
		leftPanel.add( bestScoresPanel, leftC );

		this.add( leftPanel );

		JPanel rightPanel = new JPanel();
		rightPanel.setBorder( BorderFactory.createTitledBorder("...") );
		this.add( rightPanel );
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();

		if ( source == bestCombo ) {
			setShipId( getComboShipId() );
		}
	}

	@SuppressWarnings("unchecked")
	private String getComboShipId() {
		String result = null;
		Object shipObj = bestCombo.getSelectedItem();

		if ( shipObj instanceof ShipBlueprint ) {
			ShipBlueprint ship = (ShipBlueprint)shipObj;
			result = ship.getId();
		}
		else if ( shipObj instanceof String && "".equals(shipObj) == false ) {
			result = (String)shipObj;
		}
		return result;
	}

	private void setShipId( String newShipId ) {
		if ( currentShipId != null ) applyScores();

		bestScoresPanel.removeAll();
		bestScorePanels.clear();

		currentShipId = newShipId;

		if ( currentShipId != null ) {
			List<Score> shipScoreList = allBestMap.get( currentShipId );
			if ( shipScoreList != null ) {
				int i = 0;
				for ( Score s : shipScoreList ) {
					ScorePanel bsp = new ScorePanel( ++i, s );
					bsp.setCacheMap( cacheMap );
					bsp.setShipId( currentShipId );
					bsp.setShipIdEditingEnabled( false );
					bsp.setBlankable( true );
					bsp.setEditable( true );
					bestScoresPanel.add( bsp );
					bestScorePanels.add( bsp );
				}
			}
		}
		// Add blank panels to fill all four slots.
		int panelsCount = bestScorePanels.size();
		for ( int i=0; i < MAX_SCORE_PANELS - panelsCount; i++ ) {
			ScorePanel bsp = new ScorePanel( panelsCount+i+1, null );
			bsp.setCacheMap( cacheMap );
			bsp.setShipId( currentShipId );
			bsp.setShipIdEditingEnabled( false );
			bsp.setBlankable( true );
			bsp.setEditable( currentShipId != null );
			bestScoresPanel.add( bsp );
			bestScorePanels.add( bsp );
		}

		this.revalidate();
		this.repaint();
	}

	private void applyScores() {
		if ( currentShipId == null ) return;

		List<Score> shipScoreList = allBestMap.get( currentShipId );
		if ( shipScoreList == null ) {
			shipScoreList = new ArrayList<Score>(4);
			allBestMap.put( currentShipId, shipScoreList );
		}
		shipScoreList.clear();

		for ( ScorePanel bsp : bestScorePanels ) {
			if ( bsp.isBlank() || !currentShipId.equals(bsp.getShipId()) ) continue;
			// Ignore panels that changed to another ship? *shrug*
			// TODO: Add a setShipEditable(boolean) method to ScorePanel.

			Score newScore = bsp.createScore();
			if ( newScore == null ) continue;
			shipScoreList.add( newScore );
		}
	}

	private void resetCombo() {
		setShipId( null );

		bestCombo.removeActionListener( this );
		bestCombo.removeAllItems();
		currentShipId = null;

		bestCombo.addItem( "" );
		bestCombo.setSelectedItem( "" );

		bestCombo.addActionListener( this );
	}

	public void setProfile( Profile p ) throws IOException {
		resetCombo();
		allBestMap.clear();

		for ( Score s : p.getStats().getShipBest() ) {
			List<Score> shipScoreList = allBestMap.get( s.getShipId() );
			if ( shipScoreList == null ) {
				shipScoreList = new ArrayList<Score>( 4 );
				allBestMap.put( s.getShipId(), shipScoreList );
			}
			shipScoreList.add( new Score( s ) );
		}

		Map<String, ShipBlueprint> playerShipIdMap = DataManager.get().getPlayerShips();
		for ( ShipBlueprint ship : playerShipIdMap.values() ) {
			bestCombo.addItem( ship );
		}

		// Add any non-standard ships...
		boolean first = true;
		for ( String shipId : allBestMap.keySet() ) {
			if ( !playerShipIdMap.containsKey( shipId ) ) {
				if ( first ) {
					bestCombo.addItem( "" );
					first = false;
				}
				bestCombo.addItem( shipId );
			}
		}

		this.repaint();
	}

	public void updateProfile( Profile p ) {
		if ( currentShipId != null ) applyScores();

		Stats stats = p.getStats();

		List<Score> newBest = new ArrayList<Score>();
		
		for ( Map.Entry<String, List<Score>> entry : allBestMap.entrySet() ) {
			if ( entry.getValue() == null ) continue;
			for ( Score s : entry.getValue() ) {
				newBest.add( new Score( s ) );
			}
		}

		stats.setShipBest( newBest );
	}
}
