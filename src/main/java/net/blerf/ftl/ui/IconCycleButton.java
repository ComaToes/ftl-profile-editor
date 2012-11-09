package net.blerf.ftl.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;


/**
 * A button that cycles throw an array of icons when clicked.
 */
public class IconCycleButton extends JButton implements ActionListener {
	private ImageIcon[] icons;
	private int state = 0;

	public IconCycleButton( ImageIcon[] icons ) {
		this.icons = icons;
		this.setBorder( null );
		setSelectedState( 0 );
		this.addActionListener( this );
	}

	public void setSelectedState( int n ) {
		state = n;
		this.setIcon( icons[state] );
	}

	public int getSelectedState() {
		return state;
	}

	public void actionPerformed(ActionEvent e) {
		setSelectedState( (state+1) % icons.length );
	}
}
