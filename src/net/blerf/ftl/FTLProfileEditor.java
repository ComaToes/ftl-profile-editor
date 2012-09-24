package net.blerf.ftl;

import java.io.IOException;

public class FTLProfileEditor {
	
	private static final int VERSION = 3;

	public static void main(String[] args) throws IOException {

		FTLFrame frame = new FTLFrame(VERSION);
		
		frame.setVisible(true);
		
	}
	
}
