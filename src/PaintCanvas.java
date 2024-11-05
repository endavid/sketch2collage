import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import titech.util.*;

/**
 * A Canvas used for painting in Applets, using Double Buffering.
 * 
 * Behaviour changed: left click, paint, right click, erase. There is an Undo buffer.
 * <p>
 * History: <ul>
 * <li>06/07/15: Added dispose() calls to all Graphic objects. 
 * <li>05/10/09: Branch of the blobby code
 * <li>03/12/14: Added brushes (for replacing strokes) and grid mode.
 * </ul>
 * @see titech.image.InteractiveImageDisplay if you want to use JAI.
 */
public class PaintCanvas extends Canvas implements MouseListener, MouseMotionListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int NONE = 0;
    public static final int DOTS = 1;
    public static final int SQUARE = 2;

	/** The images for Double Buffering. mImage is the layer being "painted" */
	private BufferedImage mImage = null;
	/** Undo buffer (undoes last paint) */
	private BufferedImage mUndo = null;
	/** When a brush is selected, this image is used instead of a stroke */
	private BufferedImage mBrush = null;
	/** For finding objects as we paint */
	private BufferedImage mObject = null;
	
	titech.image.dsp.ObjectImage objectImage;
	
	protected Color bgColor = Color.black;
	protected Color fgColor = Color.white;
    protected int toolMode = DOTS;	
	protected Point startP, endP;
	protected int strokeSize = 50;
	protected Stroke stroke = new BasicStroke((float)strokeSize);	
	protected boolean dragging = false;
	
	/** The size of the grid to which drawing is limited. 1x1 is maximum resolution. */
	private int gridWidth = 1;
	private int gridHeight = 1;
	
	private int width = 1;
	private int height = 1;
	
	boolean block = false;
	boolean rubber = false;
	boolean inside = false;
	Point location;
	
	/** Main Retrieval object, to pass information to other Retrieval threads */
	titech.db.Retrieval retrieval;
	titech.db.ImageRetrieval iretrieval;
	Thread iretrievalThread;
	
	public void setRetrieval(titech.db.Retrieval ret) {
		retrieval = ret;
	}

	public void setImageRetrieval(titech.db.ImageRetrieval iret) {
		iretrieval = iret;
	}
	
	public void setRubber(boolean r) {
		rubber = r;
	}
	
	public void block() {
		block = true;
	}
	
	public void release() {
		block = false;
	}
	
	/** When the component is blocked, you can not paint on it */
	public boolean isBlocked() {
		return block;
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
	
	public Color getbgColor() {
		return bgColor;
	}
	
	/** Make it square */
	public PaintCanvas(int size) {
		this(size, size);
	}
	
	public PaintCanvas(int width, int height) {
		this(width, height, Color.black);
	}
	
	public PaintCanvas(int width, int height, Color bg) {
		super();
		
		bgColor = bg;
		setBackground(bgColor);
		setForeground(fgColor);
		setSize(width, height);
		setStroke(strokeSize);
		toolMode = DOTS;
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		objectImage = new titech.image.dsp.ObjectImage();
	}

	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		this.width=width; this.height = height;
		mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics gi = mImage.getGraphics();
		gi.setColor(bgColor);
		gi.fillRect(0,0,width,height);
		gi.dispose();
		mUndo = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		mObject = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);				
	}
	
	public void setStroke(int size) {
		strokeSize = size;
        stroke = new BasicStroke((float)size, BasicStroke.CAP_ROUND, 
                                 BasicStroke.JOIN_ROUND);		
    }
	
	public void setBrush(BufferedImage buf) {
		mBrush = buf;
		toolMode = DOTS;
	}
	
	public void setMode(int mode) {
		toolMode=mode;
	}

	public void setGrid(int width, int height) {
		gridWidth = width; gridHeight = height;
	}
	
    public void setPaintColor(Color c) {
		fgColor = c;
		repaint();
	}
	
	public Color getPaintColor() {
		return fgColor;
	}
	
	/** This is not the background color of the component, but the image
	* inside this component.
	*/
	public void setBackColor(Color c) {		
		bgColor = c;
	}
	public Color getBackColor() {
		return bgColor;
	}
	

	/** Clears the Canvas with the background color (bgColor) */
	public void clear() {
		Graphics gu = mUndo.getGraphics();
		gu.drawImage(mImage, 0, 0, this);
		gu.dispose();
		
		Graphics gi = mImage.getGraphics();
		gi.setColor(bgColor);
		gi.fillRect(0,0,width,height);
		gi.dispose();
				
		repaint();
		
		// clear all regions
		objectImage.clear();
		// add the whole
		objectImage.setRegions(Utilities.resize(mImage, 100));
		// start new retrieval thread
		try {			
			if (iretrievalThread!=null) 
				iretrievalThread.join(); // wait
		} catch (Exception exc) {
			System.err.println("PaintCanvas: "+exc);
		}	
		if (iretrieval != null) {
			iretrieval.setQuery(objectImage.getHistogram());
			iretrievalThread = new Thread(iretrieval);
			iretrievalThread.start();
		}
	}
	
	/** Sets a foreground image */
	public void set(BufferedImage img) {
		setSize(img.getWidth(), img.getHeight());

		Graphics layer = mImage.getGraphics();
		
		layer.drawImage(img,0,0,this);
		layer.dispose();
		
		repaint();
	}
		

    /**
     * Draws the result of used tools over the Graphics object g.
     * The object may refer to an image, for instance.
     */
    public void drawOn(Graphics g0) {

        Graphics2D g = (Graphics2D)g0;
        if (rubber) g.setColor(bgColor);
		else g.setColor(fgColor);

		if (mBrush!=null) {
			g.drawImage(mBrush, startP.x, startP.y, this);
		} else if (startP.equals(endP)) {			
			switch (toolMode) {
			
            case DOTS:
				g.fillOval(startP.x-(strokeSize>>1), startP.y-(strokeSize>>1), strokeSize, strokeSize);
                break;
			case SQUARE:
				g.fillRect(startP.x-(strokeSize>>1), startP.y-(strokeSize>>1), strokeSize, strokeSize);
				break;

            default:
			}
		} else {
			g.setStroke(stroke);
			g.drawLine(startP.x, startP.y, endP.x, endP.y);
		}
    }
	

    public void paint(Graphics g) {
		g.drawImage(mImage,0,0,this);
		
		if (inside && !dragging) { // show the brush size
			g.setColor(fgColor);
			g.drawOval(location.x-(strokeSize>>1), location.y-(strokeSize>>1), strokeSize, strokeSize);
		}
    }	
	
	/** Redefine the method since we already clear in paint.
	* Et voila! No glitches! :D
	  */
	public void update(Graphics g) {
		paint(g);
	}
	

	
    // mouse interface
    public void mouseEntered(MouseEvent e) {
		location = e.getPoint();
		inside = true;
		repaint();
    }

    public void mouseExited(MouseEvent e) {
		inside = false;
		repaint();
    }

    public void mousePressed(MouseEvent e) {
		
		if (isBlocked()) return;
		
        if (e.getButton() == MouseEvent.BUTTON3) {
			setRubber(true);
        } else if (e.getButton() == MouseEvent.BUTTON1) {
			setRubber(false);
		} // otherwise do not change the state, since we may be dragging a rubber!
		
        Point p = e.getPoint();
		// discretize to grid
		if (gridWidth>1) { p.x /= gridWidth; p.x *= gridWidth;}
		if (gridHeight>1) {p.y /= gridHeight; p.y *=gridHeight;}
		
  		Graphics gUndo = mUndo.getGraphics();
		Graphics gImage = mImage.getGraphics();
		gUndo.drawImage(mImage, 0, 0, this);
		gUndo.dispose();
		
		if (!dragging) {
			startP = p;
			endP = p;
		}
		
		drawOn(gImage);
		gImage.dispose();
		
		// paint of the object image too
		if (!rubber) {
			Color fg = fgColor;
			fgColor = Color.WHITE;
			Graphics gObj = mObject.getGraphics();
			drawOn(gObj);
			gObj.dispose();
			fgColor = fg;
		}
		// when the mouse is released, the object will be added
		
        repaint();

  	
    }

    public void mouseReleased(MouseEvent e) {
		dragging = false;
				
		// add object
		titech.image.dsp.Region region = objectImage.addRegion(mObject, fgColor);
		//System.out.println(region);
		// start new retrieval thread
		if (retrieval != null) {
			titech.db.Retrieval ret = new titech.db.Retrieval(region);
			ret.cloneFrom(retrieval);
			Thread rt = new Thread(ret);
			rt.start();
		}
		
		objectImage.setRegions(Utilities.resize(mImage, 100));
		try {			
			if (iretrievalThread!=null) 
				iretrievalThread.join(); // wait
		} catch (Exception exc) {
			System.err.println("PaintCanvas: "+exc);
		}
		if (iretrieval != null) {
			iretrieval.setQuery(objectImage.getHistogram());
			iretrievalThread = new Thread(iretrieval);
			iretrievalThread.start();
		}
		
		// clear image
		Graphics gi = mObject.getGraphics();
		gi.setColor(Color.BLACK);
		gi.fillRect(0,0,width,height);
		gi.dispose();
    }

    public void mouseClicked(MouseEvent e) {/*
		if (e.getButton()==MouseEvent.BUTTON3) {
			// just to check the resize is working (LINEARLY!)
			mImage = Utilities.resize(mImage, 100);
			repaint();
		}*/
    }

    public void mouseMoved(MouseEvent e) {
		location = e.getPoint();
		repaint();
    }

	/** This is to draw a continuous line */
    public void mouseDragged(MouseEvent e) {
		if (isBlocked()) return;
		
		endP = e.getPoint();
		dragging = true;
		mousePressed(e);
		startP = endP;
    }	
	
}

