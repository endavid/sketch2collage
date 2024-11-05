import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.event.*;

import titech.util.*;

/**
 *  This Tool Bar contains a canvas for displaying pictures,
 *  a color palette and a slide bar to control brush size.
 *
 *  06/07/16:  Changed default colors to some point in the SOM Palette.
 * 
 * @author     David Gavilan
 * @created    2005/10/19
 */
public class ToolBar
		 extends JPanel
		 implements MouseListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Color[] palette = {
		new Color(0, 0, 0), //black
		new Color(128, 128, 128), // gray
		new Color(255, 255, 255), // white
		new Color(112, 58, 30), // brown
		new Color(221, 160, 117), // skin
		new Color(235, 89, 149), // pink
		new Color(162, 24, 22), // red
		new Color(228, 116, 42), // orange
		new Color(155, 71, 200), // purple
		new Color(141, 170, 225), // azure
		new Color(43, 80, 124), // blue
		new Color(234, 222, 59), // yellow
		new Color(142, 178, 114), // light green
		new Color(38, 65, 43) // green
	};

	public final static int WAIT = 0;
	public final static int BLOCK = 1;
	public final static int PAINT = 2;
	
	/**
	 * Number of colors.
	 */
	public final static int PALETTE_SIZE = 14;
	/**
	 *  Description of the Field
	 */
	public final static int CANVAS_WIDTH = 160, CANVAS_HEIGHT = 160;


	byte[][] colormap;

	/** univ colors++ */
	JButton[] cbuttons = new JButton[PALETTE_SIZE];
	int selected = 0;
	
	/** color button containing the gradiation of color category */
	JButton ccButton;
	ImageIcon[] ccIcon = new ImageIcon[PALETTE_SIZE];
	private BufferedImage ccBuffer;
	

	public final static int ICON_WIDTH = 28;
	public final static int ICON_HEIGHT = 28;

	PaintCanvas paintCanvas = null;
	Window parentWindow = null;
	JTextArea log = null;
	JSlider strokeSize = null;
	JLabel image = null;


	/** 
	 * BLOCK: shows the pic, but doesn't let paint;
	 * PAINT: hides the pic, but lets paint;
	 */
	int mode;
	
	
	public BufferedImage getDrawing() {
		if (paintCanvas == null) return null;
		return paintCanvas.getImage();
	}
	
	public void setPaintCanvas(PaintCanvas pc) {
		paintCanvas = pc;
		setMode(mode);
	}

	public void setParentWindow(Window pw) {
		parentWindow = pw;
	}
	
	public void setLog(JTextArea l) {
		log = l;
	}

	public int getMode() {
		return mode;
	}
	
	public void setMode(int mode) {
		this.mode = mode;
		if (paintCanvas != null) {
			switch (mode) {
				case WAIT:
				case BLOCK:
					paintCanvas.block();
					break;
				case PAINT:
					clearCanvas();
					paintCanvas.release();
					break;
			}
		}
	}

	public void clearCanvas() {
		paintCanvas.setBackColor(palette[0]);
		paintCanvas.clear();		
	}
	
	public void setPic(Image img) {
		
		BufferedImage im = Utilities.makeBufferedImage(img);
					
		int h = (im.getHeight()*CANVAS_WIDTH)/im.getWidth();
					
		img = im.getScaledInstance(CANVAS_WIDTH, h, Image.SCALE_SMOOTH);
		image.setIcon(new ImageIcon(img));
		//getParent().validate();
		revalidate();
	}
	
	private JButton createColorButton(int number) {
		JButton boton = null;
		
		try {
		if (Class.forName("javax.swing.plaf.metal.MetalLookAndFeel").isInstance(
			UIManager.getLookAndFeel())) { // for the known style
			boton = new JButton("");
			boton.setBackground(palette[number]);
			boton.setPreferredSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));

		} else { // for others, including Mac Aqua

			BufferedImage img = new BufferedImage(ICON_WIDTH-4, ICON_HEIGHT-5, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = img.createGraphics();
			g2.setColor(palette[number]);
			g2.fillRect(0,0,ICON_WIDTH-4,ICON_HEIGHT-5);
			boton = new JButton(new ImageIcon(img));
			boton.setPreferredSize(new Dimension(ICON_WIDTH+4,ICON_HEIGHT+5));
		}
		
		if (boton != null) {
			boton.setActionCommand("" + number);
			//cbuttons[i].addActionListener(this);
			// with ActionListener is not possible to listen to right-click events
			// google: JButton right click
			boton.addMouseListener(this);
		}
		
		} catch (Exception exc) {
			System.err.println("button: "+exc);
		}
		return boton;
	}
	
	private void changeColorButton(JButton boton, Color color) {
		try {
		if (Class.forName("javax.swing.plaf.metal.MetalLookAndFeel").isInstance(
			UIManager.getLookAndFeel())) { // for the known style
			boton.setBackground(color);
		} else { // for others, including Mac Aqua
			BufferedImage img = new BufferedImage(ICON_WIDTH-4, ICON_HEIGHT-5, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = img.createGraphics();
			g2.setColor(color);
			g2.fillRect(0,0,ICON_WIDTH-4,ICON_HEIGHT-5);
			boton.setIcon(new ImageIcon(img));
		}
		} catch (Exception exc) {
			System.err.println("button: "+exc);
		}
		
	}
	
	/**
	 *Constructor for the ToolBar object
	 */
	public ToolBar() {
		
		strokeSize = new JSlider(5, 80, 50);
		strokeSize.addChangeListener(this);


		for (int i = 0; i < PALETTE_SIZE; i++) {
			cbuttons[i] = createColorButton(i);
		}

		image = new JLabel();

		setContinuousPalette();
																				
		ccButton = new JButton(ccIcon[0]);
		ccButton.addMouseListener(this);
	
		ccBuffer = new BufferedImage(ccIcon[0].getIconWidth(),ccIcon[0].getIconHeight(),
				BufferedImage.TYPE_INT_RGB);
		setccBuffer(ccIcon[0]);
		
		
		// LAYOUT
		// --------------------------------------------
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints cgb = new GridBagConstraints();
		setLayout(gridbag);
		cgb.insets = new Insets(2, 2, 2, 2);
		cgb.weightx = 1.0;
		cgb.gridheight = 1;
		cgb.gridwidth = 1;
		// -------------------------------------
		cgb.gridwidth = GridBagConstraints.REMAINDER;
		//gridbag.setConstraints(image, cgb);
		cgb.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(strokeSize, cgb);
		
		cgb.fill = GridBagConstraints.NONE;
		cgb.gridwidth = 2;
		cgb.gridheight = 2;
		cgb.weighty = 1.0;
		gridbag.setConstraints(ccButton, cgb);
		
		cgb.fill = GridBagConstraints.HORIZONTAL;
		cgb.gridwidth = 1;
		cgb.gridheight = 1;
		for (int i = 0, endRow = 0; i < PALETTE_SIZE; i++) {
			if (++endRow > 6) {
				endRow = 0;
				cgb.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(cbuttons[i], cgb);
				cgb.gridwidth = 1;
			} else {
				gridbag.setConstraints(cbuttons[i], cgb);
			}
		}
		cgb.gridwidth = GridBagConstraints.REMAINDER;
		
		//add(image);
		add(strokeSize);
		add(ccButton);
		for (int i = 0; i < PALETTE_SIZE; i++) {
			add(cbuttons[i]);
		}
		
		
		
		setMode(BLOCK);

	}
	
	public void setDiscretePalette() {
		setPalette("K");
	}
	public void setContinuousPalette() {
		setPalette("SOM-CC");
	}
	private void setPalette(String loc) {
		int[] order=new int[] {
			1, 2, 3, 4, 5, 6, 13, 7, 8, 9, 10, 11, 12, 12
		};
		
		for (int i=0;i<14;i++) {
			String rr = "/resources/"+loc+order[i]+".png";
			ccIcon[i] = new ImageIcon(this.getClass().getResource(rr));		
		}		
	}
	
	public void mouseClicked(MouseEvent me) {
		try {
			JButton jb = (JButton)me.getSource();
			if (jb == ccButton) { // color category palette
				Insets insets = ccButton.getMargin();
				
				int x = me.getX() - insets.left;
				int y = me.getY() - insets.top;
				x = (x<0)?0:(x>=ccBuffer.getWidth())?ccBuffer.getWidth()-1:x;
				y = (y<0)?0:(y>=ccBuffer.getHeight())?ccBuffer.getHeight()-1:y;
				Color ccHue = new Color(ccBuffer.getRGB(x,y));
				changeColorButton(cbuttons[selected],ccHue);
				palette[selected]=ccHue;
				revalidate();
				
				//System.out.println("Mouse ("+x+", "+y+") "+ccHue);
			} else { // color button
				
				String s = jb.getActionCommand();
				int i = Integer.parseInt(s);
				this.selected = i;
				
				// update color button
				ccButton.setIcon(ccIcon[i]);
				setccBuffer(ccIcon[i]);
				revalidate();
				
			}
			if (SwingUtilities.isRightMouseButton(me)) {
					paintCanvas.setBackColor(palette[selected]);
					paintCanvas.clear();					
			} else {
					paintCanvas.setPaintColor(palette[selected]);
					
			}		
					
		} catch (Exception e) {
			log.append("Exception " + e + "\n");
		}
	}


	public void mouseExited(MouseEvent me) {
	}	
	public void mousePressed(MouseEvent me) {
		if (!SwingUtilities.isRightMouseButton(me)) {
			mouseClicked(me);
		} // only for left click, I don't one double retrievals with right-click!
	}
	public void mouseEntered(MouseEvent me) {
	}
	public void mouseReleased(MouseEvent me) {
	}
	
	
	/**
	 * Change Listener
	 *
	 * @param  e  Description of the Parameter
	 */
	public void stateChanged(ChangeEvent e) {
		Object src = e.getSource();

		if (src == strokeSize) {
			paintCanvas.setStroke(strokeSize.getValue());
		}
	}

	public Image adjustImage(Image img) {
		
		return img;
	}
	
	/** In order to know the pixel colors in the ccButton */
	private void setccBuffer(ImageIcon ii) {
		Graphics2D g2d = ccBuffer.createGraphics();
		g2d.drawImage(ii.getImage(),0,0,this);
	}

}

