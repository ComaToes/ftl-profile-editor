package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameDumpPanel extends JPanel {

	private static final Logger log = LogManager.getLogger(SavedGameDumpPanel.class);

	private FTLFrame frame;

	private JTextArea dumpArea = null;

	public SavedGameDumpPanel( FTLFrame frame ) {
		super( new BorderLayout() );

		this.frame = frame;

		dumpArea = new JTextArea("");
		dumpArea.setEditable(false);
		//dumpArea.setOpaque(false);
		dumpArea.setBackground( new Color(212, 208, 200) );
		JScrollPane dumpScrollPane = new JScrollPane( dumpArea );
		this.add( dumpScrollPane, BorderLayout.CENTER );
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		dumpArea.setText( gameState.toString() );
		dumpArea.setCaretPosition(0);
		dumpArea.repaint();
	}
}
