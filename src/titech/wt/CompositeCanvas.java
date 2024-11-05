//
//  CompositeCanvas.java
//  sketchRC
//
//  Created by David Gavilan on 10/6/06.
//  Copyright 2006 Nakajima Lab. All rights reserved.
//
package titech.wt;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class CompositeCanvas extends Canvas implements MouseMotionListener, MouseListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** The REAL images for Double Buffering. mRealBufferImage is the layer being blited to screen */
	private BufferedImage mRealBufferImage = null;
	private Graphics realBufferGraphics; 
	
	/** The images for Double Buffering. mImage is the layer being "painted" */
	private BufferedImage mImage = null;
	/** The last object added can be moved */
	private BufferedImage mObject = null;
	
	private int width = 1;
	private int height = 1;
	
	private Point offset;
	private Point draggingPoint;
	private ModListener modListener;
	
	protected Color bgColor;
	protected Color transparent = new Color(0x00000000, true);
	
	protected boolean objectExists;
	protected boolean dragging;
	
	public int maxObjectWidth;
	public int maxObjectHeight;
	
	public boolean theresObject() {
		return objectExists;
	}
	
	public void setModListener(ModListener modListener) {
		this.modListener = modListener;
	}
	public Point getOffset() {
		return offset;
	}
	
	/**
		* This method is important for the pack() call.
	 * super.getPreferredSize() returns negative dimensions and the window is
	 * not properly packed (just on Windows, on Mac OS X works fine - jdk1.4.2).
	 */
	public Dimension getPreferredSize() {
		Dimension size = super.getPreferredSize();
		//System.out.println(size);
		size.width = width;
		size.height = height;
		return size;
	}
	
	public BufferedImage getImage() {
		return mImage;
	}
	/** Fixes current object to the background, and returns the resulting collage.
		* This method is called by the Save command.
		*/
	public BufferedImage getCollage() {
		Graphics gi = mImage.getGraphics();
		// send old object to the background
		gi.drawImage(mObject,offset.x,offset.y,this);
		return mImage;
	}
	
	/** Make it square */
	public CompositeCanvas(int size) {
		this(size, size);
	}
	
	public CompositeCanvas(int width, int height) {
		this(width, height, Color.white);
	}
	
	public CompositeCanvas(int width, int height, Color bg) {
		super();
		maxObjectWidth = width;
		maxObjectHeight = height;
		
		bgColor = bg;
		setBackground(bgColor);
		setSize(width, height);
		offset = new Point(0,0);
		draggingPoint = new Point(0,0);
		modListener = null;
		objectExists = false;
		dragging = false;
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}
	
	public void setbgColor(Color bgColor) {
		this.bgColor = bgColor;
	}
	
	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		this.width=width; this.height = height;
		mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics gi = mImage.getGraphics();
		gi.setColor(bgColor);
		gi.fillRect(0,0,width,height);
		gi.dispose();
		
		mRealBufferImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		realBufferGraphics = mRealBufferImage.getGraphics();
		realBufferGraphics.setColor(bgColor);
		realBufferGraphics.fillRect(0,0,width,height);
		
		//transparent object
		mObject = new BufferedImage(maxObjectWidth, maxObjectHeight, BufferedImage.TYPE_INT_ARGB);
	}
	
	//	 Always required for good double-buffering.
    // This will cause the applet not to first wipe off
    // previous drawings but to immediately repaint.
    // the wiping off also causes flickering.
    // Update is called automatically when repaint() is called.
    public void update(Graphics g)
    {
		paint(g);
    } 
	
	public void paint(Graphics g) {		
		realBufferGraphics.drawImage(mImage,0,0,this);
		realBufferGraphics.drawImage(mObject,offset.x,offset.y,this);
		g.drawImage(mRealBufferImage, 0, 0, this);
	}
	
	public void clearObject() {
		Graphics2D gi = (Graphics2D)mObject.getGraphics();
		gi.setBackground(transparent);
		gi.clearRect(0,0,width,height);
		gi.dispose();
		objectExists = false;
	}
	
	public void clearObject(int width, int height) {
		mObject = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		objectExists = false;
	}
	
	public void add(BufferedImage bim) {
		Graphics gi = mImage.getGraphics();
		// send old object to the background
		gi.drawImage(mObject,offset.x,offset.y,this);
		// new object
		resetOffset();
		updateObject(bim);
		objectExists=true;
	}
	
	public void setBackground(BufferedImage bim) {
		Graphics gi = mImage.getGraphics();
		gi.clearRect(0,0,width,height);
		gi.drawImage(bim,0,0,this);
		gi.dispose();
		repaint();
	}
	
	public void updateObject(BufferedImage bim) {
		//clearObject();
		clearObject(bim.getWidth(), bim.getHeight());
		Graphics gi = mObject.getGraphics();
		gi.drawImage(bim,0,0,this);
		repaint();
		objectExists=true;
	}
	
	/** Adds some offset to the current one */
	public void addOffset(int x, int y) {
		offset.x +=x;
		offset.y +=y;
	}
	
	public void resetOffset() {
		offset.x = 0; offset.y = 0;
	}
	
	// MouseMotionListener 
	// -------------------------------------------------------------
	public void mouseDragged(MouseEvent e) {
		if (objectExists && dragging) {
			int x = e.getX();
			int y = e.getY();
							
			offset.x = x - draggingPoint.x;
			offset.y = y - draggingPoint.y;
			
			repaint();			
		}
	}
	
	public static boolean isTransparent(BufferedImage img, int x, int y) {
		if (x<0 || x>=img.getWidth()) return false;
		if (y<0 || y>=img.getHeight()) return false;
		if ((img.getRGB(x,y)>>24)==0) return true;
		return false;
	}
	
	public void mouseMoved(MouseEvent e) {
		
	}
	
	// MouseListener
	// -------------------------------------------------------------
	public void	mouseClicked(MouseEvent e) {
	} 
	public void	mouseEntered(MouseEvent e) {
		requestFocusInWindow();		
	}
	public void	mouseExited(MouseEvent e) {
		
	}
	public void	mousePressed(MouseEvent e) {
		if (!dragging) {
			int x = e.getX();
			int y = e.getY();

			// check if inside object
			boolean insideObject = (x>=offset.x && x<(mObject.getWidth()+offset.x) &&
					y>=offset.y && y<(mObject.getHeight()+offset.y));
			draggingPoint.x = x - offset.x; 
			draggingPoint.y = y - offset.y;
			if (insideObject) {
				if (!isTransparent(mObject, draggingPoint.x, draggingPoint.y)) {
					dragging = true;
				}
			}
		}
		
	}
	public void	mouseReleased(MouseEvent e) {
		if (modListener!=null && objectExists && dragging) {
			modListener.moved();
			dragging = false;
		}
	}
	
	// KeyListener
	// ----------------------------------------------
	public void keyPressed(KeyEvent e) {
		int c = e.getKeyCode();
		switch(c) {
			case KeyEvent.VK_BACK_SPACE:
			case KeyEvent.VK_DELETE: // clear current object
				clearObject();
				repaint();
				break;
			default:
		}		
	}
	public void keyReleased(KeyEvent e) {
		
	}
	public void keyTyped(KeyEvent e) {
	}
	
}
