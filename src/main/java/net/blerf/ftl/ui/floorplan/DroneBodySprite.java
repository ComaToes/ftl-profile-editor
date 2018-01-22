package net.blerf.ftl.ui.floorplan;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser.BoarderDronePodInfo;
import net.blerf.ftl.parser.SavedGameParser.DroneState;
import net.blerf.ftl.parser.SavedGameParser.DronePodState;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.parser.SavedGameParser.ExtendedDroneInfo;
import net.blerf.ftl.parser.SavedGameParser.ExtendedDronePodInfo;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;
import net.blerf.ftl.xml.DroneBlueprint;


public class DroneBodySprite extends JComponent implements ReferenceSprite<DroneState> {

	private SpriteReference<DroneState> droneRef;
	private BufferedImage bodyImage;


	public DroneBodySprite( SpriteReference<DroneState> droneRef, BufferedImage bodyImage ) {
		this.droneRef = droneRef;
		this.bodyImage = bodyImage;

		this.setPreferredSize( new Dimension( bodyImage.getWidth(), bodyImage.getHeight() ) );
		this.setOpaque( false );

		droneRef.addSprite( this );
		referenceChanged();
	}

	@Override
	public SpriteReference<DroneState> getReference() {
		return droneRef;
	}

	@Override
	public void referenceChanged() {
		boolean bodyVisible = false;

		if ( droneRef.get() != null ) {
			DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneRef.get().getDroneId() );
			DroneType droneType = DroneType.findById( droneBlueprint.getType() );

			if ( DroneType.BATTLE.equals( droneType )
				|| DroneType.REPAIR.equals( droneType ) ) {

				if ( droneRef.get().getBodyRoomId() >= 0 ) {
					bodyVisible = true;
				}
			}
			else if ( DroneType.BOARDER.equals( droneType ) ) {
				ExtendedDroneInfo droneInfo = droneRef.get().getExtendedDroneInfo();
				if ( droneInfo != null ) {
					DronePodState dronePod = droneInfo.getDronePod();
					if ( dronePod != null ) {
						BoarderDronePodInfo boarderPodInfo = dronePod.getExtendedInfo( BoarderDronePodInfo.class );
						if ( boarderPodInfo != null ) {
							if ( boarderPodInfo.getBodyRoomId() >= 0 ) {
								bodyVisible = true;
							}
						}
					}
				}
			}
		}

		this.setVisible( bodyVisible );

		this.repaint();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;

		if ( bodyImage != null ) {
			g2d.drawImage( bodyImage, 0, 0, this.getWidth(), this.getHeight(), this );
		}
	}
}