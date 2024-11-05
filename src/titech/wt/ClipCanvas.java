//
//  ClipCanvas.java
//  sketchRC
//
//  Created by David Gavilan on 11/3/06.
//  Copyright 2006 Nakajima Lab. All rights reserved.
//
package titech.wt;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Vector;

import titech.util.*;
import titech.image.matting.*;



public class ClipCanvas extends Canvas implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int pressedButton = MouseEvent.NOBUTTON;
	
	/** The REAL images for Double Buffering. mRealBufferImage is the layer being blited to screen */
	private BufferedImage mRealBufferImage = null;
	private Graphics realBufferGraphics; 
	
	/** This is the layer that contains the resized image. Its a bit bigger than the window. */
	private BufferedImage mImage = null;
	
	/** For lines */
	private BufferedImage lineImage = null;
	/** For drawing a contour */
	private BufferedImage contourImage = null;
	/** For resizes and stuff, keep the original */
	private BufferedImage originalImage = null;
	
	protected int strokeSize = 50;
	protected Stroke stroke = new BasicStroke((float)strokeSize);	
	
	private Dimension maxSize;
	
	private int width = 1;
	private int height = 1;
	
	private Point offset;
	Point location;
	
	protected Color bgColor = Color.black;
	protected Color fgColor = Color.green;
	protected Color transparent = new Color(0x00000000, true);
	Color strokeColor = new Color(0x30FFFF00, true);
	
	/** Signal for the Boundary object */
	public boolean moved;
	
	// parent Window
	private ModListener modListener;
	
	private Point previousPoint;
	private Point firstPoint;
	private boolean freehand;
	private boolean contourExists;
	boolean inside = false;
	boolean dragging = false;
	
	public boolean theresContour() {
		return contourExists;
	}
	
	
	public void setStroke(int size) {
		strokeSize = size;
        stroke = new BasicStroke((float)size, BasicStroke.CAP_ROUND, 
                                 BasicStroke.JOIN_ROUND);		
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
	
	public Dimension getMaxSize() {
		return maxSize;
	}
	
	public BufferedImage getImage() {
		return mImage;
	}
	
	public BufferedImage getContour() {
		if (contourExists) return contourImage;
		else return null;
	}
	
	public BufferedImage getClippedImage() {
		BufferedImage bim = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics gi = bim.getGraphics();
		gi.drawImage(mImage, offset.x, offset.y, this);
		gi.dispose();
		
		return bim;
	}
	
	public Rectangle getClipArea() {
		int w = this.width;
		int h = this.height;
		int x = 0, y=0;
		if (offset.x<0) {
			x=-offset.x;
			w=(int)Math.min(w,mImage.getWidth()-x);
		} else {
			w=w-offset.x;
		}
		if (offset.y<0) {
			y=-offset.y;
			h=(int)Math.min(h,mImage.getHeight()-y);
		} else {
			h=h-offset.y;
		}
		Rectangle r = new Rectangle(x,y,w,h);		
		return r;
	}
	
	public Point getOffset() {
		return offset;
	}
	
	/** Make it square */
	public ClipCanvas(int size) {
		this(size, size);
	}
	
	public ClipCanvas(int width, int height) {
		this(width, height, Color.black);
	}
	
	public ClipCanvas(int width, int height, Color bg) {
		super();
		
		bgColor = bg;
		setBackground(bgColor);
		setSize(width, height);
		offset = new Point(0,0);
		moved = true;
		previousPoint = new Point(-1,-1);
		firstPoint = new Point(-1,-1);
		freehand = true;
		contourExists = false;
		maxSize = new Dimension(width, width);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
	}
	
	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		this.width=width; this.height = height;
		mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics gi = mImage.getGraphics();
		gi.setColor(bgColor);
		gi.fillRect(0,0,width,height);
		gi.dispose();
		
		// transparent by default (all pixels 0x00000000)
		lineImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		contourImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
				
		mRealBufferImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		realBufferGraphics = mRealBufferImage.getGraphics();
		realBufferGraphics.setColor(bgColor);
		realBufferGraphics.fillRect(0,0,width,height);
	}
	
	public void setMaxSize(Dimension dim) {
		maxSize.width = dim.width;
		maxSize.height = dim.height;
	}
	
	public void setModListener(ModListener modListener) {
		this.modListener = modListener;
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
		realBufferGraphics.drawImage(mImage,offset.x,offset.y,this);
		// contour line
		realBufferGraphics.drawImage(contourImage,0,0,this);
		// lines
		realBufferGraphics.drawImage(lineImage,0,0,this);
		if (inside && !dragging) { // show the brush size
			realBufferGraphics.setColor(fgColor);
			realBufferGraphics.drawOval(location.x-(strokeSize>>1), location.y-(strokeSize>>1), strokeSize, strokeSize);
		}
		g.drawImage(mRealBufferImage,0, 0, this);
	}
	
	public void add(BufferedImage bim) {
		Graphics gi = mImage.getGraphics();
		gi.drawImage(bim,0,0,this);
		repaint();		
	}
	
	public void setImage(BufferedImage bim) {
		originalImage = bim;
		BufferedImage img = Utilities.resize(originalImage, maxSize.width, maxSize.height);
		setSize(img.getWidth(), img.getHeight());
		mImage = img;
		
		resetOffset();
		clearLines();
		clearContour();
		repaint();
	}
	
	public void addL(BufferedImage bim) {
		addL(bim, 0, 0);
	}
	
	public void addL(BufferedImage bim, int x, int y) {
		Graphics gi = lineImage.getGraphics();
		gi.drawImage(bim,x,y,this);
		repaint();		
	}
	
	public void add(Boundary boundary) {
		add(boundary, 0, 0);
	}

	public void add(Boundary boundary, int x, int y) {
		Graphics2D gi = (Graphics2D) lineImage.getGraphics();
		gi.drawImage(boundary.getContour(),x,y,this);
		repaint();
	}

	public void add(Vector points, int color) {
		add(points, color, 0, 0);
	}
	public void add(Vector points, int color, int x, int y) {
		int w = lineImage.getWidth();
		int h = lineImage.getHeight();
		for (int i=0;i<points.size();i++) {
			Point p = (Point)points.get(i);
			int xx = p.x+x;
			int yy = p.y+y;
			if (xx>=0 && xx<w && yy>=0 && yy<h)
				lineImage.setRGB(xx,yy,color);
		}
		repaint();
	}

	public void add(Point[] points, int color) {
		add(points, color, 0, 0);
	}

	public void add(Point[] points, int color, int x, int y) {
		int w = lineImage.getWidth();
		int h = lineImage.getHeight();
		for (int i=0;i<points.length;i++) {
			Point p = points[i];
			if (p!=null) {
				int xx = p.x+x;
				int yy = p.y+y;
				if (xx>=0 && xx<w && yy>=0 && yy<h)
					lineImage.setRGB(xx,yy,color);
			}
		}
		repaint();
	}

	public void add(Point p, int color) {
		add(p, color, 0, 0);
	}
	
	public void add(Point p, int color, int x, int y) {
		int w = lineImage.getWidth();
		int h = lineImage.getHeight();

		int xx = p.x+x;
		int yy = p.y+y;
		if (xx>=0 && xx<w && yy>=0 && yy<h)
			lineImage.setRGB(xx,yy,color);
		repaint();
	}
	
	public void clearLines() {
		Graphics2D gi = (Graphics2D)lineImage.getGraphics();
		gi.setBackground(transparent);
		gi.clearRect(0,0,width,height);
		gi.dispose();		
	}
	
	public void clearContour() {
		Graphics2D gi = (Graphics2D)contourImage.getGraphics();
		gi.setBackground(transparent);
		gi.clearRect(0,0,width,height);
		gi.dispose();
		contourExists = false;
	}
	
	
	public void resetOffset() {
		offset.x = 0; offset.y = 0;
	}
	
	
	
	// MouseMotionListener 
	// -------------------------------------------------------------
	public void mouseDragged(MouseEvent e) {
		dragging = true;
		if (pressedButton == MouseEvent.BUTTON2 || e.isMetaDown()) { 
			// drag with right button or using APPLE (Meta) 
			int x = e.getX();
			int y = e.getY();
			int wh = width >> 1;
			int hh = height >> 1;
			
			offset.x = x - wh;
			offset.y = y - hh;
			
			repaint();
		} else if (pressedButton == MouseEvent.BUTTON1) {
			int x = e.getX();
			int y = e.getY();
			int minx=offset.x>0?offset.x:0;
			int miny=offset.y>0?offset.y:0;
			int maxx=mImage.getWidth()+offset.x<width?mImage.getWidth()+offset.x:width;
			int maxy=mImage.getHeight()+offset.y<height?
				mImage.getHeight()+offset.y:height;
			x = x<minx?minx:(x>=maxx?maxx-1:x);
			y = y<miny?miny:(y>=maxy?maxy-1:y);			
			if (previousPoint.x>=0) {
				Graphics2D gi = (Graphics2D)contourImage.getGraphics();
				if (e.isShiftDown()) {
					freehand = false;
					clearContour();
					int xx=(x<firstPoint.x?x:firstPoint.x);
					int yy=(y<firstPoint.y?y:firstPoint.y);
					int w = (int)Math.abs(firstPoint.x-x); //+1?
					int h = (int)Math.abs(firstPoint.y-y); //+1?		
					gi.drawRect(xx, yy, w, h); 
				} else {
					freehand = true;
					gi.setStroke(stroke);
					gi.setColor(strokeColor);
					//gi.drawLine(previousPoint.x,previousPoint.y,x,y);	
					gi.fillOval(x-(strokeSize>>1), y-(strokeSize>>1), strokeSize, strokeSize);
				}
				repaint();
			}  else {
				clearContour();
				firstPoint.x = x;
				firstPoint.y = y;
			}
			previousPoint.x = x;
			previousPoint.y = y;
		}
	}
	
	public void mouseMoved(MouseEvent e) {
		location = e.getPoint();
		repaint();	
	}
	
	public void	mouseClicked(MouseEvent e) {
		
	} 
	public void	mouseEntered(MouseEvent e) {
		location = e.getPoint();
		inside = true;
		repaint();		
	}
	public void	mouseExited(MouseEvent e) {
		inside = false;
		repaint();		
	}
	public void	mousePressed(MouseEvent e) {
		this.pressedButton = e.getButton();
	}
	public void	mouseReleased(MouseEvent e) {
		this.pressedButton = MouseEvent.NOBUTTON;
		if (previousPoint.x<0) {
			moved = true;
			clearLines();
			clearContour();
			repaint();
			if (modListener!=null) modListener.shifted();
		} else {
			moved = true;
//			if (freehand) {
//				Graphics2D gi = (Graphics2D)contourImage.getGraphics();
//				gi.setStroke(stroke);
//				gi.setColor(strokeColor);
//				gi.drawLine(previousPoint.x,previousPoint.y,firstPoint.x,firstPoint.y);
//			}
			previousPoint.x=-1; previousPoint.y=-1;	
			firstPoint.x=-1; firstPoint.y=-1;
			contourExists = true;
			repaint();
			if (modListener!=null) modListener.clip();
		}
		dragging = false;
	}
	
	// MouseWheelListener
	// ----------------------------------------------
	public void mouseWheelMoved(MouseWheelEvent event) {
		if (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
			int totalScrollAmount = event.getUnitsToScroll();
			double ratio = (double)originalImage.getWidth()/(double)originalImage.getHeight();
			maxSize.height += totalScrollAmount;
			maxSize.width = (int)((double)maxSize.height*ratio);
			mImage = Utilities.resize(originalImage, maxSize.width, maxSize.height);
			repaint();
		}
	}
	
	// KeyListener
	// ----------------------------------------------
	public void keyPressed(KeyEvent e) {
		int c = e.getKeyCode();
		switch(c) {
			case KeyEvent.VK_G: // convert to gray
				Utilities.colorToGray(mImage);
				repaint();
				System.out.println("g");
				break;
			case KeyEvent.VK_W:
				// transfer the WHOLE image to the composite
				// --generate event
				if (modListener!=null) modListener.fullclip();
			default:
		}		
	}
	public void keyReleased(KeyEvent e) {
		
	}
	public void keyTyped(KeyEvent e) {
	}
	
	
}
