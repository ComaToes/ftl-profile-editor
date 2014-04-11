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
	private static final String SHIP_ID = "Ship Id";
	private static final String VALUE = "Value";
	private static final String SECTOR = "Sector";
	private static final String DIFFICULTY = "Difficulty";
	private static final String VICTORY = "Victory";
	private static final String DLC_ENABLED = "DLC Enabled";

	private static final int maxIconWidth = 64;
	private static final int maxIconHeight = 64;

	private static final Logger log = LogManager.getLogger(TopScorePanel.class);

	private BufferedImage shipImage;
	private String shipName;
	private String shipId;
	private int value;
	private int sector;
	private Difficulty difficulty;
	private boolean victory;
	private boolean dlcEnabled;

	private JLabel shipImageLbl = new JLabel( "", JLabel.LEFT );
	private JLabel shipNameLbl = new JLabel( "", JLabel.CENTER );
	private JLabel valueLbl = new JLabel( "", JLabel.LEFT );
	private JLabel sectorLbl = new JLabel( "", JLabel.LEFT );
	private JLabel difficultyLbl = new JLabel( "", JLabel.CENTER );
	private JLabel victoryLbl = new JLabel( "", JLabel.RIGHT );
	private JLabel dlcLbl = new JLabel( "", JLabel.RIGHT );


	public TopScorePanel( int rank, Score s ) {
		shipName = s.getShipName();
		shipId = s.getShipId();
		value = s.getValue();
		sector = s.getSector();
		difficulty = s.getDifficulty();
		victory = s.isVictory();
		dlcEnabled = s.isDLCEnabled();

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
		c.gridwidth = 4;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 0;
		this.add( shipNameLbl, c );

		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy++;
		this.add( new JLabel("Score: ", JLabel.RIGHT), c );
		c.gridx++;
		this.add( valueLbl, c );

		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy++;
		this.add( new JLabel("Sector: ", JLabel.RIGHT), c );
		c.gridx++;
		this.add( sectorLbl, c );

		c.gridwidth = 2;
		c.gridx = 2;
		c.gridy = 3;
		difficultyLbl.setText(difficulty.toString());
		this.add( difficultyLbl, c );

		// Wrap DLC label in a panel with 0 preferred width.
		// By itself, the label's preferred width would've imbalanced weightx.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 4;
		c.gridy = 3;
		JPanel dlcPanel = new JPanel(new BorderLayout());
		dlcLbl.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		dlcPanel.add(dlcLbl, BorderLayout.EAST);
		dlcPanel.setPreferredSize(new java.awt.Dimension(0, 0));
		this.add( dlcPanel, c );

		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridx = 5;
		c.gridy = 3;
		this.add( Box.createHorizontalGlue(), c );

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		c.gridwidth = 1;
		c.gridheight = 4;
		c.gridx = 6;
		c.gridy = 0;
		JButton editBtn = new JButton("Edit");
		editBtn.setMargin(new Insets(0,0,0,0));
		this.add( editBtn, c );

		editBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showEditPopup();
			}
		});

		setRank( rank );
		setShipName( shipName );
		setShipId( shipId );
		setValue( value );
		setSector( sector );
		setDifficulty( difficulty );
		setVictory( victory );
		setDLCEnabled( dlcEnabled );
	}

	public void setRank( int n ) {
		this.setBorder( BorderFactory.createTitledBorder("#"+n) );
	}

	public void setShipName( String s ) {
		shipName = s;
		shipNameLbl.setText(shipName);
	}

	public void setShipId( String s ) {
		shipId = s;

		InputStream stream = null;
		try {
			ShipBlueprint ship = DataManager.get().getShip( shipId );
			stream = DataManager.get().getResourceInputStream( "img/ship/"+ ship.getGraphicsBaseName() +"_base.png" );
			shipImage = getScaledImage( stream );
			shipImageLbl.setIcon( new ImageIcon(shipImage) );
		}
		catch (IOException e) {
			log.error( "Error reading ship image for "+ shipId +"." , e );
		}
		finally {
			try {if (stream != null) stream.close();}
			catch (IOException f) {}
		}
	}

	public void setValue( int n ) {
		value = n;
		valueLbl.setText(""+value);
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

	public void setDLCEnabled( boolean b ) {
		dlcEnabled = b;
		dlcLbl.setText(dlcEnabled ? "Advanced" : "");
	}

	public String getShipName() { return shipName; }
	public String getShipId() { return shipId; }
	public int getValue() { return value; }
	public int getSector() { return sector; }
	public Difficulty getDifficulty() { return difficulty; }
	public boolean isVictory() { return victory; }
	public boolean isDLCEnabled() { return dlcEnabled; }

	private void showEditPopup() {
		Map<String, ShipBlueprint> playerShipMap = DataManager.get().getPlayerShips();
		Map<String, ShipBlueprint> autoShipMap = DataManager.get().getAutoShips();

		JPanel popupPanel = new JPanel(new BorderLayout());

		final FieldEditorPanel editorPanel = new FieldEditorPanel( true );
		editorPanel.addRow( SHIP_NAME, FieldEditorPanel.ContentType.STRING );
		editorPanel.addRow( SHIP_ID, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( VALUE, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( SECTOR, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( DIFFICULTY, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( VICTORY, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( DLC_ENABLED, FieldEditorPanel.ContentType.BOOLEAN );

		editorPanel.getCombo(SHIP_ID).addItem("");
		for ( ShipBlueprint blueprint : playerShipMap.values() )
			editorPanel.getCombo(SHIP_ID).addItem(blueprint);
		editorPanel.getCombo(SHIP_ID).addItem("");
		for ( ShipBlueprint blueprint : autoShipMap.values() )
			editorPanel.getCombo(SHIP_ID).addItem(blueprint);

		if ( playerShipMap.containsKey(shipId) )
			editorPanel.setComboAndReminder( SHIP_ID, playerShipMap.get(shipId) );
		else if ( autoShipMap.containsKey(shipId) )
			editorPanel.setComboAndReminder( SHIP_ID, autoShipMap.get(shipId) );

		for ( Difficulty d : Difficulty.values() )
			editorPanel.getCombo(DIFFICULTY).addItem(d);

		editorPanel.setStringAndReminder( SHIP_NAME, getShipName() );
		editorPanel.setIntAndReminder( VALUE, getValue() );
		editorPanel.setIntAndReminder( SECTOR, getSector() );
		editorPanel.setComboAndReminder( DIFFICULTY, getDifficulty() );
		editorPanel.setBoolAndReminder( VICTORY, isVictory() );
		editorPanel.setBoolAndReminder( DLC_ENABLED, isDLCEnabled() );
		popupPanel.add(editorPanel, BorderLayout.CENTER);

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

				setShipName( editorPanel.getString(SHIP_NAME).getText() );

				Object shipObj = editorPanel.getCombo(SHIP_ID).getSelectedItem();
				if ( shipObj instanceof ShipBlueprint ) {
					setShipId( ((ShipBlueprint)shipObj).getId() );
				}

				newString = editorPanel.getInt(VALUE).getText();
				try { setValue(Integer.parseInt(newString)); }
				catch (NumberFormatException f) {}

				newString = editorPanel.getInt(SECTOR).getText();
				try { setSector(Integer.parseInt(newString)); }
				catch (NumberFormatException f) {}

				setDifficulty( (Difficulty)editorPanel.getCombo(DIFFICULTY).getSelectedItem() );
				setVictory( editorPanel.getBoolean(VICTORY).isSelected() );
				setDLCEnabled( editorPanel.getBoolean(DLC_ENABLED).isSelected() );

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
