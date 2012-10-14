package net.blerf.ftl;

import net.blerf.ftl.ui.FTLFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class FTLProfileEditor {

	private static final Logger log = LogManager.getLogger(FTLProfileEditor.class);
	
	private static final int VERSION = 10;

	public static void main(String[] args) {

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
