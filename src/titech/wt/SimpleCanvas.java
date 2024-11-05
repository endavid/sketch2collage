//
//  SimpleCanvas.java
//  sketchRC
//
//  Created by David Gavilan on 1/13/07.
//  Copyright 2007 __MyCompanyName__. All rights reserved.
//
package titech.wt;

import java.awt.*;
import java.awt.image.*;

public class SimpleCanvas extends Canvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The images for Double Buffering. mImage is the layer being "painted" */
	private BufferedImage mImage = null;

	private int width = 1;
	private int height = 1;
		
	protected Color bgColor = Color.black;

	
	/** Make it square */
	public SimpleCanvas(int size) {
		this(size, size);
	}
	
	public SimpleCanvas(int width, int height) {
		this(width, height, Color.black);
	}
	
	public SimpleCanvas(int width, int height, Color bg) {
		super();
		
		bgColor = bg;
		setBackground(bgColor);
		setSize(width, height);		
	}
	
	
	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		this.width=width; this.height = height;
		mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics gi = mImage.getGraphics();
		gi.setColor(bgColor);
		gi.fillRect(0,0,width,height);
		gi.dispose();
		
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
	
	public void add(BufferedImage bim) {
		Graphics gi = mImage.getGraphics();
		gi.drawImage(bim,0,0,this);
		repaint();		
	}
	
	public void set(BufferedImage bim) {
		setSize(bim.getWidth(), bim.getHeight());
		add(bim);
	}
	
	public void paint(Graphics g) {
		g.drawImage(mImage,0,0,this);
	}
}
