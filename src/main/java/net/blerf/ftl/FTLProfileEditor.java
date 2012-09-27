package net.blerf.ftl;


public class FTLProfileEditor {
	
	private static final int VERSION = 6;

	public static void main(String[] args) {

		try {
			FTLFrame frame = new FTLFrame(VERSION);
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
}
