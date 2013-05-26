package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Score.Difficulty;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class TopScorePanel extends JPanel {

	private static final String SHIP_NAME = "Ship Name";
	private static final String SHIP_TYPE = "Ship Type";
	private static final String SCORE = "Score";
	private static final String SECTOR = "Sector";
	private static final String DIFFICULTY = "Difficulty";
	private static final String VICTORY = "Victory";

	private static final int maxIconWidth = 64;
	private static final int maxIconHeight = 64;

	private static final Logger log = LogManager.getLogger(TopScorePanel.class);

	private BufferedImage shipImage;
	private String shipName;
	private String shipType;
	private int score;
	private int sector;
	private Difficulty difficulty;
	private boolean victory;

	private JLabel shipImageLbl = new JLabel("", JLabel.LEFT);
	private JLabel shipNameLbl = new JLabel("", JLabel.CENTER);
	private JLabel scoreLbl = new JLabel("", JLabel.LEFT);
	private JLabel sectorLbl = new JLabel("", JLabel.LEFT);
	private JLabel difficultyLbl = new JLabel("", JLabel.CENTER);
	private JLabel victoryLbl = new JLabel("", JLabel.RIGHT);

	public TopScorePanel(int rank, Score s) {
		shipName = s.getShipName();
		shipType = s.getShipType();
		score = s.getScore();
		sector = s.getSector();
		difficulty = s.getDifficulty();
		victory = s.isVictory();

		this.setBorder( BorderFactory.createTitledBorder("") );
		this.setLayout( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.gridwidth = 1;
		c.gridheight = 4;
		c.gridx = 0;
		c.gridy = 0;
		this.add( shipImageLbl, c );

		// Wrap Victory label in a panel with 0 preferred width.
		// By itself, the label's preferred width would've imbalanced weightx.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 3;
		JPanel victoryPanel = new JPanel(new BorderLayout());
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
		this.add( shipNameLbl, c );

		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy++;
		this.add( new JLabel("Score: ", JLabel.RIGHT), c );
		c.gridx++;
		this.add( scoreLbl, c );

		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy++;
		this.add( new JLabel("Sector: ", JLabel.RIGHT), c );
		c.gridx++;
		this.add( sectorLbl, c );

		c.gridwidth = 2;
		c.gridx = 2;
		c.gridy++;
		difficultyLbl.setText(difficulty.toString());
		this.add( difficultyLbl, c );

		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 3;
		this.add( Box.createHorizontalGlue(), c );

		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.0;
		c.gridwidth = 1;
		c.gridheight = 4;
		c.gridx = 5;
		c.gridy = 0;
		JButton editBtn = new JButton("Edit");
		editBtn.setMargin(new Insets(0,0,0,0));
		this.add( editBtn, c );

		editBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showEditPopup();
			}
		});

		setRank(rank);
		setShipName(shipName);
		setShipType(shipType);
		setScore(score);
		setSector(sector);
		setDifficulty(difficulty);
		setVictory(victory);
	}

	public void setRank( int n ) {
		this.setBorder( BorderFactory.createTitledBorder("#"+n) );
	}

	public void setShipName( String s ) {
		shipName = s;
		shipNameLbl.setText(shipName);
	}

	public void setShipType( String s ) {
		shipType = s;

		InputStream stream = null;
		try {
			ShipBlueprint ship = DataManager.get().getShip( shipType );
			stream = DataManager.get().getResourceInputStream( "img/ship/"+ ship.getGraphicsBaseName() +"_base.png" );
			shipImage = getScaledImage( stream );
			shipImageLbl.setIcon( new ImageIcon(shipImage) );
		}
		catch (IOException e) {
			log.error( "Error reading ship image for "+ shipType +"." , e );
		}
		finally {
			try {if (stream != null) stream.close();}
			catch (IOException f) {}
		}
	}

	public void setScore( int n ) {
		score = n;
		scoreLbl.setText(""+score);
	}

	public void setSector( int n ) {
		sector = n;
		sectorLbl.setText(""+sector);
	}

	public void setDifficulty( Difficulty d ) {
		difficulty = d;
		difficultyLbl.setText(d.toString());
	}

	public void setVictory( boolean b ) {
		victory = b;
		victoryLbl.setText(victory ? "Victory" : "");
	}

	public String getShipName() { return shipName; }
	public String getShipType() { return shipType; }
	public int getScore() { return score; }
	public int getSector() { return sector; }
	public Difficulty getDifficulty() { return difficulty; }
	public boolean isVictory() { return victory; }

	private void showEditPopup() {
		Map<String, ShipBlueprint> shipMap = DataManager.get().getShips();
		Map<String, ShipBlueprint> autoMap = DataManager.get().getAutoShips();

		JPanel popupPanel = new JPanel(new BorderLayout());

		final FieldEditorPanel editPanel = new FieldEditorPanel( true );
		editPanel.addRow( SHIP_NAME, FieldEditorPanel.ContentType.STRING );
		editPanel.addRow( SHIP_TYPE, FieldEditorPanel.ContentType.COMBO );
		editPanel.addRow( SCORE, FieldEditorPanel.ContentType.INTEGER );
		editPanel.addRow( SECTOR, FieldEditorPanel.ContentType.INTEGER );
		editPanel.addRow( DIFFICULTY, FieldEditorPanel.ContentType.COMBO );
		editPanel.addRow( VICTORY, FieldEditorPanel.ContentType.BOOLEAN );

		editPanel.getCombo(SHIP_TYPE).addItem("");
		for ( ShipBlueprint blueprint : shipMap.values() )
			editPanel.getCombo(SHIP_TYPE).addItem(blueprint);
		editPanel.getCombo(SHIP_TYPE).addItem("");
		for ( ShipBlueprint blueprint : autoMap.values() )
			editPanel.getCombo(SHIP_TYPE).addItem(blueprint);

		if ( shipMap.containsKey(shipType) )
			editPanel.setComboAndReminder( SHIP_TYPE, shipMap.get(shipType) );
		else if ( autoMap.containsKey(shipType) )
			editPanel.setComboAndReminder( SHIP_TYPE, autoMap.get(shipType) );

		for ( Difficulty d : Difficulty.values() )
			editPanel.getCombo(DIFFICULTY).addItem(d);

		editPanel.setStringAndReminder( SHIP_NAME, getShipName() );
		editPanel.setIntAndReminder( SCORE, getScore() );
		editPanel.setIntAndReminder( SECTOR, getSector() );
		editPanel.setComboAndReminder( DIFFICULTY, getDifficulty() );
		editPanel.setBoolAndReminder( VICTORY, isVictory() );
		popupPanel.add(editPanel, BorderLayout.CENTER);

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.X_AXIS));
		ctrlPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		ctrlPanel.add(Box.createHorizontalGlue());
		JButton popupOkBtn = new JButton("OK");
		ctrlPanel.add(popupOkBtn);
		ctrlPanel.add(Box.createHorizontalGlue());
		JButton popupCancelBtn = new JButton("Cancel");
		ctrlPanel.add(popupCancelBtn);
		ctrlPanel.add(Box.createHorizontalGlue());
		popupPanel.add(ctrlPanel, BorderLayout.SOUTH);
		popupOkBtn.setPreferredSize(popupCancelBtn.getPreferredSize());

		final JDialog popup = new JDialog((java.awt.Frame)this.getTopLevelAncestor(), "Edit Score", true);
		popup.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		popup.getContentPane().add(popupPanel);
		popup.pack();
		popup.setLocationRelativeTo(null);

		popupCancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				popup.setVisible(false);
				popup.dispose();
			}
		});

		popupOkBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String newString = null;

				setShipName( editPanel.getString(SHIP_NAME).getText() );

				Object shipObj = editPanel.getCombo(SHIP_TYPE).getSelectedItem();
				if ( shipObj instanceof ShipBlueprint ) {
					setShipType( ((ShipBlueprint)shipObj).getId() );
				}

				newString = editPanel.getInt(SCORE).getText();
				try { setScore(Integer.parseInt(newString)); }
				catch (NumberFormatException f) {}

				newString = editPanel.getInt(SECTOR).getText();
				try { setSector(Integer.parseInt(newString)); }
				catch (NumberFormatException f) {}

				setDifficulty( (Difficulty)editPanel.getCombo(DIFFICULTY).getSelectedItem() );
				setVictory( editPanel.getBoolean(VICTORY).isSelected() );

				popup.setVisible(false);
				popup.dispose();
			}
		});

		popup.setVisible(true);
	}

	public BufferedImage getScaledImage( InputStream in ) throws IOException {
		BufferedImage origImage = ImageIO.read( in );
		int width = origImage.getWidth();
		int height = origImage.getHeight();
		
		if ( width <= maxIconWidth && height < maxIconHeight )
			return origImage;
		
		if ( width > height ) {
			height /= width / maxIconWidth;
			width = maxIconWidth;
		} else {
			width /= height / maxIconHeight;
			height = maxIconHeight;
		}
		BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = scaledImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.drawImage(origImage, 0, 0, width, height, null);
		g2d.dispose();

		return scaledImage;
	}
}
