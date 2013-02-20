package net.blerf.ftl.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.blerf.ftl.model.Score.Difficulty;


public class TopScorePanel extends JPanel {

	public TopScorePanel(int rank, Image shipImage, String shipName, int score, int sector, Difficulty diff) {
		setLayout( new BoxLayout(this, BoxLayout.X_AXIS) );
		setBorder( BorderFactory.createTitledBorder("#"+rank) );

		add( new JLabel( new ImageIcon(shipImage) ) );

		JPanel dataPanel = new JPanel();
		add(dataPanel);

		dataPanel.setLayout( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		dataPanel.add( new JLabel(shipName, JLabel.CENTER), c );

		c.gridwidth = 1;
		c.gridy = 1;
		dataPanel.add( new JLabel("Score: ", JLabel.RIGHT), c );

		c.gridx = 1;
		dataPanel.add( new JLabel(""+score), c );

		c.gridx = 0;
		c.gridy = 2;
		dataPanel.add( new JLabel("Sector: ", JLabel.RIGHT), c );

		c.gridx = 1;
		dataPanel.add( new JLabel(""+sector), c );

		String diffText = "Unknown";
		switch( diff ) {
			case EASY: diffText = "Easy"; break;
			case NORMAL: diffText = "Normal"; break;
		}

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		dataPanel.add( new JLabel(diffText, JLabel.CENTER), c );
	}
}
