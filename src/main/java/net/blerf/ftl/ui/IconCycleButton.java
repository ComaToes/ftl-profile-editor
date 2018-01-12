package net.blerf.ftl.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;


/**
 * A button that cycles through an array of icons when clicked.
 */
public class IconCycleButton extends JButton implements ActionListener {

	private Icon[] icons;
	private int state = 0;


	public IconCycleButton( Icon[] icons ) {
		this.icons = icons;
		this.setBorder( BorderFactory.createEmptyBorder( 2, 4, 2, 4 ) );
		this.setFocusPainted( true );
		this.setContentAreaFilled( false );
		this.setHorizontalAlignment( SwingConstants.LEADING );
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

	@Override
	public void actionPerformed( ActionEvent e ) {
		setSelectedState( (state+1) % icons.length );
	}
}
