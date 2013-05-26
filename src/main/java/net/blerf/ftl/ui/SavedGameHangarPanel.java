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

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.SystemBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameHangarPanel extends JPanel {

	private static final Logger log = LogManager.getLogger(SavedGameHangarPanel.class);

	private FTLFrame frame;

	public SavedGameHangarPanel( FTLFrame frame ) {
		this.setLayout( new GridBagLayout() );

		this.frame = frame;

		Map<String, ShipBlueprint> shipMap = DataManager.get().getShips();
		Map<String, ShipBlueprint> autoMap = DataManager.get().getAutoShips();

		GridBagConstraints hangarC = new GridBagConstraints();
		hangarC.fill = GridBagConstraints.NONE;
		hangarC.weightx = 0.0;
		hangarC.weighty = 0.0;
		hangarC.insets = new Insets(4, 4, 4, 4);
		hangarC.gridx = 0;
		hangarC.gridy = 0;

		JPanel borderPanel = new JPanel( new BorderLayout() );
		borderPanel.setBorder( BorderFactory.createTitledBorder("Change Ship") );
		JPanel hangarPanel = new JPanel(new GridBagLayout());
		hangarPanel.setBorder( BorderFactory.createEmptyBorder(4, 4, 4, 4) );

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
		hangarC.insets = new Insets(8, 4, 8, 4);
		hangarC.gridwidth = 1;
		hangarC.gridx = 0;
		hangarC.gridy++;
		final JComboBox shipCombo = new JComboBox();
		for ( ShipBlueprint blueprint : shipMap.values() )
			shipCombo.addItem( blueprint );
		hangarPanel.add( shipCombo, hangarC );

		hangarC.fill = GridBagConstraints.NONE;
		hangarC.gridx++;
		JButton createShipBtn = new JButton( "Create" );
		hangarPanel.add( createShipBtn, hangarC );

		hangarC.fill = GridBagConstraints.HORIZONTAL;
		hangarC.gridx = 0;
		hangarC.gridy++;
		final JComboBox autoCombo = new JComboBox();
		for ( ShipBlueprint blueprint : autoMap.values() )
			autoCombo.addItem( blueprint );
		hangarPanel.add( autoCombo, hangarC );

		hangarC.fill = GridBagConstraints.NONE;
		hangarC.gridx++;
		JButton createAutoBtn = new JButton( "Create" );
		hangarPanel.add( createAutoBtn, hangarC );

		hangarC.fill = GridBagConstraints.NONE;
		hangarC.gridwidth = 2;
		hangarC.gridx = 0;
		hangarC.gridy++;
		JButton stealNearbyShipBtn = new JButton( "Steal Nearby Ship" );
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
			public void actionPerformed(ActionEvent e) {
				createShip( ((ShipBlueprint)shipCombo.getSelectedItem()), false );
			}
		});
		createAutoBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createShip( ((ShipBlueprint)autoCombo.getSelectedItem()), true );
			}
		});
		stealNearbyShipBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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
		int response = JOptionPane.showConfirmDialog(frame, nag, "Change Player Ship", JOptionPane.YES_NO_OPTION);
		if ( response != JOptionPane.YES_OPTION ) return;

		ShipLayout shipLayout = DataManager.get().getShipLayout( shipBlueprint.getLayout() );

		SavedGameParser.ShipState shipState = new SavedGameParser.ShipState( "The Nameless One", shipBlueprint, auto );

		// Systems.
		int reservePowerCapacity = 0;
		for ( SystemType systemType : SystemType.values() ) {
			SavedGameParser.SystemState systemState = new SavedGameParser.SystemState( systemType );

			// Set capacity for systems that're initially present.
			ShipBlueprint.SystemList.SystemRoom[] systemRoom = shipBlueprint.getSystemList().getSystemRoom( systemType );
			if ( systemRoom != null ) {
				Boolean start = systemRoom[0].getStart();
				if ( start == null || start.booleanValue() == true ) {
					SystemBlueprint systemBlueprint = DataManager.get().getSystem( systemType.getId() );
					systemState.setCapacity( systemBlueprint.getStartPower() );

					if ( systemType.isSubsystem() ) {
						systemState.setPower( systemState.getCapacity() );
					} else {
						reservePowerCapacity += systemState.getCapacity();
					}
				}
			}
			shipState.addSystem( systemState );
		}
		reservePowerCapacity = Math.max( reservePowerCapacity, shipBlueprint.getMaxPower().amount );
		shipState.setReservePowerCapacity( reservePowerCapacity );

		// Rooms.
		for (int r=0; r < shipLayout.getRoomCount(); r++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(r);
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			SavedGameParser.RoomState roomState = new SavedGameParser.RoomState();
			for (int s=0; s < squaresH*squaresV; s++) {
				roomState.addSquare( 0, 0, -1);
			}
			shipState.addRoom( roomState );
		}

		// Doors.
		Map<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> layoutDoorMap = shipLayout.getDoorMap();
		for ( Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : layoutDoorMap.entrySet() ) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();
			EnumMap<ShipLayout.DoorInfo,Integer> doorInfo = entry.getValue();

			shipState.setDoor( doorCoord.x, doorCoord.y, doorCoord.v, new SavedGameParser.DoorState() );
		}

		// Augments.
		if ( shipBlueprint.getAugments() != null ) {
			for ( ShipBlueprint.AugmentId augId : shipBlueprint.getAugments() )
				shipState.addAugmentId( augId.name );
		}

		// Supplies.
		shipState.setHullAmt( shipBlueprint.getHealth().amount );
		shipState.setFuelAmt( 20 );
		if ( shipBlueprint.getDroneList() != null )
			shipState.setDronePartsAmt( shipBlueprint.getDroneList().drones );
		if ( shipBlueprint.getWeaponList() != null )
			shipState.setMissilesAmt( shipBlueprint.getWeaponList().missiles );

		gameState.setPlayerShipState( shipState );

		// Sync session's redundant ship info with player ship.
		gameState.setPlayerShipName( gameState.getPlayerShipState().getShipName() );
		gameState.setPlayerShipBlueprintId( gameState.getPlayerShipState().getShipBlueprintId() );

		frame.loadGameState( gameState );
	}

	private void stealNearbyShip() {
		SavedGameParser.SavedGameState gameState = frame.getGameState();
		if ( gameState == null ) return;

		// Apply all other pending changes.
		frame.updateGameState( gameState );

		if ( gameState.getNearbyShipState() == null ) {
			JOptionPane.showMessageDialog(frame, "There is no nearby ship to steal.", "Steal Nearby Ship", JOptionPane.WARNING_MESSAGE);
			return;
		}

		String nag = "";
		nag += "The player ship is about to be replaced with the nearby one.\n";
		nag += "Some ships lack shield oval and floor outline images.\n";
		nag += "Are you sure you want to do this?";
		int response = JOptionPane.showConfirmDialog(frame, nag, "Steal Nearby Ship", JOptionPane.YES_NO_OPTION);
		if ( response != JOptionPane.YES_OPTION ) return;

		gameState.setPlayerShipState( gameState.getNearbyShipState() );
		gameState.setNearbyShipState( null );

		// Sync session's redundant ship info with player ship.
		gameState.setPlayerShipName( gameState.getPlayerShipState().getShipName() );
		gameState.setPlayerShipBlueprintId( gameState.getPlayerShipState().getShipBlueprintId() );

		frame.loadGameState( gameState );
	}
}
