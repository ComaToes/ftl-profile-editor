package net.blerf.ftl.ui;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Score.Difficulty;


public class TopScorePanel extends JPanel {

	private String shipName;
	private String shipType;
	private int score;
	private int sector;
	private Difficulty difficulty;
	private boolean victory;

	public TopScorePanel(int rank, Image shipImage, Score s) {
		shipName = s.getShipName();
		shipType = s.getShipType();
		score = s.getScore();
		sector = s.getSector();
		difficulty = s.getDifficulty();
		victory = s.isVictory();

		this.setBorder( BorderFactory.createTitledBorder("#"+rank) );
		this.setLayout( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.gridwidth = 1;
		c.gridheight = 5;
		c.gridx = 0;
		c.gridy = 0;
		this.add( new JLabel( new ImageIcon(shipImage), JLabel.LEFT ), c );

		// Wrap Victory label in a panel with 0 preferred width.
		// By itself, the label's preferred width would've imbalanced weightx.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 3;
		JPanel victoryPanel = new JPanel(new BorderLayout());
		JLabel victoryLbl = new JLabel((victory ? "Victory" : ""), JLabel.RIGHT);
		victoryLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		victoryPanel.add(victoryLbl, BorderLayout.EAST);
		victoryPanel.setPreferredSize(new java.awt.Dimension(0, 0));
		this.add( victoryPanel, c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.gridx = 2;
		c.gridy = 0;
		this.add( new JLabel(shipName, JLabel.CENTER), c );

		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy++;
		this.add( new JLabel("Score: ", JLabel.RIGHT), c );
		c.gridx++;
		this.add( new JLabel(""+score), c );

		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy++;
		this.add( new JLabel("Sector: ", JLabel.RIGHT), c );
		c.gridx++;
		this.add( new JLabel(""+sector), c );

		String diffText = "Unknown";
		switch( difficulty ) {
			case EASY: diffText = "Easy"; break;
			case NORMAL: diffText = "Normal"; break;
		}

		c.gridwidth = 2;
		c.gridx = 2;
		c.gridy++;
		this.add( new JLabel(diffText, JLabel.CENTER), c );

		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 0;
		this.add( Box.createHorizontalGlue(), c );
	}

	public void setShipName( String s ) { shipName = s; }
	public void setShipType( String s ) { shipType = s; }
	public void setScore( int n ) { score = n; }
	public void setSector( int n ) { sector = n; }
	public void setDifficulty( Difficulty d ) { difficulty = d; }
	public void setVictory( boolean b ) { victory = b; }

	public String getShipName() { return shipName; }
	public String getShipType() { return shipType; }
	public int getScore() { return score; }
	public int getSector() { return sector; }
	public Difficulty getDifficulty() { return difficulty; }
	public boolean isVictory() { return victory; }
}
