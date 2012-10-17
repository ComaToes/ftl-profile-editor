package net.blerf.ftl;

import javax.swing.UIManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.blerf.ftl.ui.FTLFrame;


public class FTLProfileEditor {

	private static final Logger log = LogManager.getLogger(FTLProfileEditor.class);
	
	private static final int VERSION = 10;

	public static void main(String[] args) {

		// Set look and feel before the GUI.
		// Otherwise, some existing components might not notice without this.
		//   SwingUtilities.updateComponentTreeUI(someComponent);
		// Maybe risks NPE if present in a JFrame constructor?
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			log.error("Failed to set a native look and feel", e);
		}

		try {
			FTLFrame frame = new FTLFrame(VERSION);
			frame.setVisible(true);
		} catch (Exception e) {
			log.error( "Exception while creating FTLFrame" , e);
			// Required to kill Swing or process will remain active
			System.exit(0);
		}
		
	}
	
}
