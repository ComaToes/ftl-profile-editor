package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class DumpPanel extends JPanel {

	private JTextArea dumpArea = null;


	public DumpPanel() {
		super( new BorderLayout() );

		dumpArea = new JTextArea("");
		dumpArea.setEditable( false );
		dumpArea.setFont( new Font( "Monospaced", Font.PLAIN, dumpArea.getFont().getSize() ) );
		//dumpArea.setOpaque( false );
		dumpArea.setBackground( new Color(212, 208, 200) );
		JScrollPane dumpScrollPane = new JScrollPane( dumpArea );
		this.add( dumpScrollPane, BorderLayout.CENTER );
	}

	public void setText( String s ) {
		dumpArea.setText( s );
		dumpArea.setCaretPosition(0);
		dumpArea.repaint();
	}
}
