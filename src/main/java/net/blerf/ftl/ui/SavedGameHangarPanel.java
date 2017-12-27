package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.SystemBlueprint;


public class SavedGameHangarPanel extends JPanel {

	private static final Logger log = LoggerFactory.getLogger( SavedGameHangarPanel.class );

	private FTLFrame frame;
	private JComboBox shipCombo = null;
	private JButton createShipBtn = null;
	private JComboBox autoCombo = null;
	private JButton createAutoBtn = null;
	private JButton stealNearbyShipBtn = null;


	public SavedGameHangarPanel( FTLFrame frame ) {
		this.setLayout( new GridBagLayout() );

		this.frame = frame;

		Map<String, ShipBlueprint> playerShipMap = DataManager.get().getPlayerShips();
		Map<String, ShipBlueprint> autoShipMap = DataManager.get().getAutoShips();

		GridBagConstraints hangarC = new GridBagConstraints();
		hangarC.fill = GridBagConstraints.NONE;
		hangarC.weightx = 0.0;
		hangarC.weighty = 0.0;
		hangarC.insets = new Insets( 4, 4, 4, 4 );
		hangarC.gridx = 0;
		hangarC.gridy = 0;

		JPanel borderPanel = new JPanel( new BorderLayout() );
		borderPanel.setBorder( BorderFactory.createTitledBorder( "Change Ship" ) );
		JPanel hangarPanel = new JPanel( new GridBagLayout() );
		hangarPanel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

		hangarC.gridwidth = 2;
		hangarC.gridx = 0;
		JLabel noticeALbl = new JLabel( "Use one of the following to get a new ship." );
		hangarPanel.add( noticeALbl, hangarC );

		hangarC.gridwidth = 2;
		hangarC.gridx = 0;
		hangarC.gridy++;
		JLabel noticeBLbl = new JLabel( "Then manually add crew and equipment." );
		hangarPanel.add( noticeBLbl, hangarC );

		hangarC.fill = GridBagConstraints.HORIZONTAL;
		hangarC.insets = new Insets( 8, 4, 8, 4 );
		hangarC.gridwidth = 1;
		hangarC.gridx = 0;
		hangarC.gridy++;
		shipCombo = new JComboBox();
		for ( ShipBlueprint blueprint : playerShipMap.values() )
			shipCombo.addItem( blueprint );
		hangarPanel.add( shipCombo, hangarC );

		hangarC.fill = GridBagConstraints.NONE;
		hangarC.gridx++;
		createShipBtn = new JButton( "Create" );
		hangarPanel.add( createShipBtn, hangarC );

		hangarC.fill = GridBagConstraints.HORIZONTAL;
		hangarC.gridx = 0;
		hangarC.gridy++;
		autoCombo = new JComboBox();
		for ( ShipBlueprint blueprint : autoShipMap.values() )
			autoCombo.addItem( blueprint );
		hangarPanel.add( autoCombo, hangarC );

		hangarC.fill = GridBagConstraints.NONE;
		hangarC.gridx++;
		createAutoBtn = new JButton( "Create" );
		hangarPanel.add( createAutoBtn, hangarC );

		hangarC.fill = GridBagConstraints.NONE;
		hangarC.gridwidth = 2;
		hangarC.gridx = 0;
		hangarC.gridy++;
		stealNearbyShipBtn = new JButton( "Steal Nearby Ship" );
		stealNearbyShipBtn.addMouseListener( new StatusbarMouseListener( frame, "Abandon the player ship and commandeer one nearby." ) );
		hangarPanel.add( stealNearbyShipBtn, hangarC );

		borderPanel.add( hangarPanel, BorderLayout.CENTER );

		GridBagConstraints thisC = new GridBagConstraints();
		thisC.fill = GridBagConstraints.NONE;
		thisC.weightx = 0.0;
		thisC.weighty = 0.0;
		thisC.gridx = 0;
		thisC.gridy = 0;
		this.add( borderPanel, thisC );

		thisC.fill = GridBagConstraints.BOTH;
		thisC.weighty = 1.0;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( Box.createVerticalGlue(), thisC );

		createShipBtn.addActionListener(new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				createShip( (ShipBlueprint)shipCombo.getSelectedItem(), false );
			}
		});
		createAutoBtn.addActionListener(new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				createShip( (ShipBlueprint)autoCombo.getSelectedItem(), true );
			}
		});
		stealNearbyShipBtn.addActionListener(new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				stealNearbyShip();
			}
		});
	}

	/**
	 * Create a replacement player ship.
	 *
	 * Some initial values are set, but it will
	 * still need to be outfitted manually.
	 */
	private void createShip( ShipBlueprint shipBlueprint, boolean auto ) {
		SavedGameParser.SavedGameState gameState = frame.getGameState();
		if ( gameState == null ) return;

		// Apply all other pending changes.
		frame.updateGameState( gameState );

		String nag = "";
		nag += "The player ship is about to be replaced with a new one.\n";
		nag += "Some ships lack shield oval and floor outline images.\n";
		nag += "Are you sure you want to do this?";
		int response = JOptionPane.showConfirmDialog( frame, nag, "Change Player Ship", JOptionPane.YES_NO_OPTION );
		if ( response != JOptionPane.YES_OPTION ) return;

		SavedGameParser.ShipState shipState = new SavedGameParser.ShipState( "The Nameless One", shipBlueprint, auto );
		shipState.refit();
		gameState.setPlayerShipState( shipState );

		// Sync session's redundant ship info with player ship.
		gameState.setPlayerShipName( gameState.getPlayerShipState().getShipName() );
		gameState.setPlayerShipBlueprintId( gameState.getPlayerShipState().getShipBlueprintId() );

		frame.loadGameState( gameState );
	}

	private void stealNearbyShip() {
		SavedGameParser.SavedGameState gameState = frame.getGameState();
		if ( gameState == null ) return;

		StringBuilder nagBuf = new StringBuilder();
		nagBuf.append( "The player ship is about to be replaced with the nearby one.\n" );
		nagBuf.append( "\n" );
		nagBuf.append( "Some ships lack shield oval and floor outline images.\n" );
		nagBuf.append( "\n" );
		nagBuf.append( "Are you sure you want to do this?" );
		int response = JOptionPane.showConfirmDialog( frame, nagBuf.toString(), "Steal Nearby Ship", JOptionPane.YES_NO_OPTION );
		if ( response != JOptionPane.YES_OPTION ) return;

		// Apply all other pending changes.
		frame.updateGameState( gameState );

		if ( gameState.getNearbyShipState() == null ) {
			JOptionPane.showMessageDialog( frame, "There is no nearby ship to steal.", "Steal Nearby Ship", JOptionPane.WARNING_MESSAGE );
			return;
		}

		SavedGameParser.ShipState shipState = gameState.getNearbyShipState();
		gameState.setNearbyShipState( null );
		gameState.setNearbyShipAI( null );
		shipState.commandeer();
		gameState.setPlayerShipState( shipState );

		gameState.getProjectileList().clear();

		if ( gameState.isRebelFlagshipNearby() ) {
			// Stole the flagship!? Have the enemy approach this beacon again.
			gameState.setRebelFlagshipNearby( false );

			if ( gameState.getRebelFlagshipHop() > 0 ) {
				gameState.setRebelFlagshipHop( gameState.getRebelFlagshipHop()-1 );
			}

			gameState.setRebelFlagshipMoving( true );
		}

		// Sync session's redundant ship info with player ship.
		gameState.setPlayerShipName( gameState.getPlayerShipState().getShipName() );
		gameState.setPlayerShipBlueprintId( gameState.getPlayerShipState().getShipBlueprintId() );

		frame.loadGameState( gameState );
	}


	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		// This panel doesn't cache anything of the state. This just toggles the UI.

		if ( gameState != null && gameState.getFileFormat() == 2 ) {
			shipCombo.setEnabled( true );
			createShipBtn.setEnabled( true );
			autoCombo.setEnabled( true );
			createAutoBtn.setEnabled( true );

			stealNearbyShipBtn.setEnabled( (gameState.getNearbyShipState() != null) );
		}
		else if ( gameState != null && ( gameState.getFileFormat() == 7 || gameState.getFileFormat() == 8 || gameState.getFileFormat() == 9 ) ) {
			// FTL 1.5.4 is only partially editable.

			shipCombo.setEnabled( false );
			createShipBtn.setEnabled( false );
			autoCombo.setEnabled( false );
			createAutoBtn.setEnabled( false );

			stealNearbyShipBtn.setEnabled( (gameState.getNearbyShipState() != null) );
		}
		else {
			shipCombo.setEnabled( false );
			createShipBtn.setEnabled( false );
			autoCombo.setEnabled( false );
			createAutoBtn.setEnabled( false );

			stealNearbyShipBtn.setEnabled( false );
		}
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		// This panel makes its changes immediately. Nothing to do here.
	}
}
