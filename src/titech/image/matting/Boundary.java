//
//  Boundary.java
//  sketchRC
//
//  Created by David Gavilan on 11/3/06.
//  Copyright 2006 Nakajima Lab. All rights reserved.
//
package titech.image.matting;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

import titech.image.dsp.*;
import titech.util.*;
import titech.wt.*;

/**
  * This class implements several of the algorithms found in
  * Jia et al., "Drag-and-drop pasting", SigGraph 2006
  *
  * Algorithms: 
  * Boundary energy minimization:
  *   1. initialization -> constructor
  *   2. Average color difference of the boundary
  *   3. shortest closed-path 
  * 
  * Fractional boundary:
  *
  *
  */
public class Boundary implements Runnable {

	public final static int NO_OPTIMIZATION = 0;
	public final static int HARD_SEGMENTATION = 1;
	public final static int INTELLIGENT_SCISSORS = 2;
	public final static int INTELLIGENT_ERODE = 3;
	
	public final static int DILATION = 25;
	/** For the alpha ring. The size of the ring is given by SIGMA */
	public final static float SIGMA = 4;
	/** Number of zero pixels to add for the convolution */
	public final static int PADDING = 50; 
	public final static Kernel KERNEL = Utilities.gaussian2Dkernel(SIGMA);

	/** Minimum boundary length allowed */
	public final static int MIN_LENGTH = 50;
	
	/** Keep it here so we can easily add new regions and locate interior pixels */
	BufferedImage indexedImage;
	/** This is the indexedImage but scaled to the same size as the source image */
	BufferedImage segmented;
	/** These are the pixels of the contour of the object */
	BufferedImage contour;
	/** These are all the pixels of the object */
	BufferedImage mask;
	BufferedImage resultMask;
	BufferedImage sourceImage;
	BufferedImage targetImage;
	BufferedImage externalContour;
	/** The points of the contour of the object */
	Vector<Point> contourPoints;
	Rectangle clipArea;
	/** The bounding box around the mask */
	Rectangle maskBB;
	Dimension dim;
	
	int index;
	int poissonType;
	int method;
	double sGradient;
	double poissonAccuracy;
	public boolean alphaMatting;
	public boolean gradientDifference;
	boolean debugging = false; 
	String debuggingDirectory = "/Users/david/Desktop/debug"; 
	
	/** To update visuals while processing */
	ClipCanvas canvas = null;
	CompositeCanvas compositeCanvas = null;
	SimpleCanvas debugCanvas = null;
	
	/** To delay the process of refined segmentation */
	private BufferedImage roughSegmentation = null;
	
	/** Just used to convert to L*a*b* color space */
	Segmenter segmenter;
	
	void debug(BufferedImage img, String name) {
		if (debugging) {
			try {
				name=Utilities.dateStamp()+"-"+name;
				Utilities.saveImage(img, "png", debuggingDirectory+"/"+name+".png");
			} catch (Exception e) {
				System.err.println("Boundary debug: "+e);
			}
		}
	}
	
	public void setMethod(int method) {
		this.method = method;
	}
	public void setGradientMethod(int method) {
		if (method==0) 
			gradientDifference = false;
		else
			gradientDifference = true;
	}
	public void setExternalContour(BufferedImage lineImage) {
		if (lineImage==null) {
			this.externalContour = null;
			return;
		}
		// the line image is actually a very thick line
		//this.externalContour = MOps2D.bw2binary(lineImage);
		
		// update the object contour too, to contain 
		// all the strictly contained regions
		BufferedImage mask = MOps2D.equalNot(lineImage,0x00000000);
		BufferedImage inner = MOps2D.bw2binary(mask);
		debug(mask,"contour01");
		MOps2D.floodfill4(inner,-1,-1);
		MOps2D.not(inner); 
		MOps2D.or(mask, inner); 
		this.externalContour = MOps2D.gradient(mask);
		
		BufferedImage out = null;
		if (method==HARD_SEGMENTATION || method==INTELLIGENT_SCISSORS) {
			// we'll just explore a few pixels. Otherwise when labeling, too many labels!
			BufferedImage outside = MOps2D.dilate(mask);
			
			//BufferedImage resized = MOps2D.maskImage(canvas.getClippedImage(),outside);
			BufferedImage resized = canvas.getImage(); // this image is NOT square
			
			maskBB = MOps2D.getBB(outside);
			
			BufferedImage sgm = createClipped(resized, BufferedImage.TYPE_INT_RGB);
			// quantize (using L*a*b* distance) and turn it to indexed image
			sgm = segmenter.quantizeWithMinLab(sgm, segmented);

			sgm = MOps2D.maxPercentil(sgm, 3);
			
			BufferedImage outclipped = createClipped(outside, BufferedImage.TYPE_BYTE_BINARY);			
			MOps2D.and(sgm, outclipped);
			
			debug(sgm);
			debug(sgm,"CQ");
			
			ColorLabel cl = new ColorLabel(sgm);
			
			out = createClipped(mask, BufferedImage.TYPE_BYTE_BINARY);
			out = MOps2D.fill4(cl.getLabeledMatrix(), out);
						
			out = MOps2D.discardRegions(out, 0.01);		
			if (method == INTELLIGENT_SCISSORS) // convert to genus-1 so can optimize
				out = MOps2D.connectRegions(out);
			
			// clip also the line contour
			externalContour = createClipped(externalContour, BufferedImage.TYPE_BYTE_BINARY);
			
		} else if (method == INTELLIGENT_ERODE) {
			maskBB = MOps2D.getBB(MOps2D.dilate(mask));//make the bounding box a bit bigger, to avoid wrong fills
			externalContour = createClipped(externalContour, BufferedImage.TYPE_BYTE_BINARY);
			//BufferedImage maskClipped = createClipped(mask, BufferedImage.TYPE_BYTE_BINARY);
			BufferedImage lineMask = createClipped(
					MOps2D.equalNot(lineImage,0x00000000), BufferedImage.TYPE_BYTE_BINARY);
			int[] strel = new int[31];
			for (int i=0;i<31;i++)
				strel[i]=1;
			
			// inside = not(outside + lineMask)
			//BufferedImage lineMask = MOps2D.bw2binary(lineImage);
			BufferedImage inside = MOps2D.bw2binary(lineMask);
			MOps2D.floodfill4(inside,-1,-1); // +outside
			MOps2D.not(inside);
			//BufferedImage inside = MOps2D.erode(maskClipped,strel);

			//debug(maskClipped);
			debug(inside);
			out = inside;
		} else { // else NO_OPTIMIZATION, do nothing
			maskBB = MOps2D.getBB(MOps2D.dilate(mask));//make the bounding box a bit bigger, to avoid wrong fills
			externalContour = createClipped(externalContour, BufferedImage.TYPE_BYTE_BINARY);
			mask = createClipped(mask, BufferedImage.TYPE_BYTE_BINARY);
			out = mask;
		}
		debug(mask,"contour02");
		debug(out,"contour03");
		debug(externalContour, "contour04");
		this.dim = null;
		update(null, out, 1);
		updateGraphics(contour);
	}
	public void clearExternalContour() {
		externalContour = null;
	}
	
	public void setCanvas(ClipCanvas canvas) {
		this.canvas = canvas;
	}
	
	public void setDebugCanvas(SimpleCanvas dbug) {
		debugCanvas = dbug;
	}

	public void setComposite(CompositeCanvas canvas) {
		this.compositeCanvas = canvas;
	}
	
	/** Sets the source image */
	public void setSource(BufferedImage source) {
		this.sourceImage = source;
	}
	/** Sets the "segmented" image. It's the direct result of k-means.
	  * Useful to resize images given this palette
	  * (although it should be in the L*a*b* color space).
	  * Updates the boundary.
	  */
	public void setSegmented(BufferedImage ccat) {
		segmented = ccat;
		BufferedImage inside = MOps2D.equal(indexedImage, index, canvas.getMaxSize());
		roughSegmentation = inside;
		// for now, just show this rough contour
		update(null, inside, 1);
		// the rest will appear in the segmentation thread
	}
	
	public void setSegmentedThreaded() {
		
		BufferedImage inside = roughSegmentation;
		debug(inside, "sst01");
		int[] strel = new int[31];
		for (int i=0;i<31;i++)
			strel[i]=1;
		
		BufferedImage outside = MOps2D.dilate(inside,strel);		
		// to avoid adding a black line surrounding regions like the sky,
		// make the erosion padding the outside with ones -> zeroPadding = false
		inside = MOps2D.erode(outside, false); 
		
		BufferedImage resized = canvas.getImage();		
		maskBB = MOps2D.getBB(outside);
		
		int w = (int)maskBB.getWidth();
		int h = (int)maskBB.getHeight();
		
		BufferedImage sgm = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics gi = sgm.getGraphics();
		gi.drawImage(resized, (int)-maskBB.getMinX(), (int)-maskBB.getMinY(), null);
		gi.dispose();
		// quantize (using L*a*b* distance) and turn it to indexed image
		sgm = segmenter.quantizeWithMinLab(sgm, segmented);
		//debug(sgm);
		// el orden de los factores altera el producto (!)
		sgm = MOps2D.maxPercentil(sgm, 3);
		//debug(sgm);

		BufferedImage outclipped = new BufferedImage(w, h,
													 BufferedImage.TYPE_BYTE_BINARY);
		gi=outclipped.getGraphics();
		gi.drawImage(outside, (int)-maskBB.getMinX(), (int)-maskBB.getMinY(), null);
		gi.dispose();
		
		MOps2D.and(sgm, outclipped);
		
		debug(sgm);
		
		ColorLabel cl = new ColorLabel(sgm);
		System.out.println("setSegmented: "+cl.getNLabels()+" labels");
		//debug(cl.getAsImage());
		
		BufferedImage out = new BufferedImage(w, h, 
											  BufferedImage.TYPE_BYTE_BINARY);
		gi = out.getGraphics();
		// BUG#01 added offs! it gave wrong offsets b4
		gi.drawImage(inside, (int)-maskBB.getMinX(), (int)-maskBB.getMinY(), null);
		gi.dispose();
		//debug(out);
		//try {
		//	Utilities.saveImage(out, "png", "/Users/david/Desktop/debug02.png");
		//} catch (Exception e){
		//}
		out = MOps2D.fill4(cl.getLabeledMatrix(), out);
		//cl = new ColorLabel(out, 0x000000); debug(cl.getAsImage());
		out = MOps2D.discardRegions(out, 0.01);
		//debug(out);
		// don't connect in hard segmentation
		//out = MOps2D.connectRegions(out);
		
		
		// check for wrong refinements
		double newsize = MOps2D.objSize(out) + 1;
		double oldsize = MOps2D.objSize(inside) + 1;
		if (newsize/oldsize < 0.5) { // we lost half of the object!
			// rather give the rough aproximation
			gi = out.getGraphics();
			gi.clearRect(0, 0, w, h);
			gi.drawImage(roughSegmentation, (int)-maskBB.getMinX(), (int)-maskBB.getMinY(), null);
			gi.dispose();
			// BUG#01 THIS WAS GIVING WRONG OFFSETS WHEN FAILURE
			//out = roughSegmentation;
		}

		if (compositeCanvas!=null)
			compositeCanvas.addOffset((int)maskBB.getMinX(), (int)maskBB.getMinY());
		// notify that the refinement of the segmentation is done by setting this to null 
		roughSegmentation = null;
		//debug(out);
		dim = null;
		//debug(out);
		update(null, out,1);
	}
	public void setTarget(BufferedImage target) {
		this.targetImage = target;
	}
	
	public BufferedImage getContour() {
		return contour;
	}

	
	public Boundary(BufferedImage indexedImage, int index) {
		this(indexedImage, index, null);
	}
	
	public Boundary(BufferedImage indexedImage, int index, Dimension dim) {
		this(indexedImage, index, dim, null);
	}

	/** Initializes a contour by sampling the boundary of an object in
	  * a indexed image.
	  * The clip is the viewport from the clipcanvas.
	  */
	public Boundary(BufferedImage indexedImage, int index, Dimension dim, Rectangle clip) {
		
		this.dim = dim;
		this.indexedImage = indexedImage;
		this.index = index;
		poissonType = 0;
		poissonAccuracy = 0.05;
		sGradient = 0;
		method = 0;
		this.externalContour = null;
		alphaMatting = false;
		gradientDifference = false;
		
		this.clipArea = clip;
		// we don't know yet "segmented" image! Until "source" it's initialized
		// call "update" in setSource
		//update(clip);

		segmenter = new Segmenter();

	} // end Constructor

	public void update() {
		update(this.clipArea);
	}

	public void update(Rectangle clip) {
		this.clipArea = clip;
		setSegmented(segmented);
	}

	/** Updates the object's contour, "contour" and "resultMask" */
	public void update(Rectangle clip, BufferedImage indexedImage, int index) {
		this.clipArea = clip;

		// gets the scaled up version of the object boundary
		// Since the indexedImage is usually smaller, it will look very jaggy...
		// use the "segmented" image to avoid that
		BufferedImage gr;
		if (dim!=null) gr = MOps2D.gradient(indexedImage, index, dim);
		else {
			gr = MOps2D.gradient(indexedImage, index);
			dim = new Dimension(indexedImage.getWidth(),indexedImage.getHeight());
		}
		
		if (clip!=null && canvas==null) {
			gr = gr.getSubimage(clip.x,clip.y,clip.width,clip.height);
		}
		
		debug(gr, "update01");
		BufferedImage ccaux = MOps2D.bw2binary(gr);
		MOps2D.floodfill4(ccaux,-1,-1);
		MOps2D.not(ccaux);
		debug(ccaux, "update02");
		
		MOps2D.or(ccaux, gr);
		mask = MOps2D.colorMask(ccaux,0xFFFFFFFF);
		debug(mask, "update03");
		contour = MOps2D.gradient(ccaux);
		contour = MOps2D.colorMask(contour,0xFF00FF00);
		debug(contour, "update04");
		contourPoints = MOps2D.listPoints(contour, 0xFF00FF00);
				
		resultMask = mask;
		debug(resultMask, "update05");
	}


	public BufferedImage maskImage() {
		return maskImage(sourceImage);
	}
	public BufferedImage maskImage(BufferedImage source) {
		return MOps2D.maskImage(source, resultMask);
	}


	public double colorDifference(BufferedImage gradient) {
		return colorDifference(contourPoints, gradient);
	}
	
	/** Computes the average color difference along the boundary of this object */
	public double colorDifference(BufferedImage source, BufferedImage target) {
		return colorDifference(contourPoints, source, target);
	}
	
	/** Computes the average color difference along a boundary given by a Vector of Points.
	  * The offset in the target image (compositeCanvas) should be taken into account!
	  * Assume that source and target are already aligned by the caller of this function
	  * (clipSource, clipTarget)
	  */
	public double colorDifference(Vector<Point> boundary, BufferedImage source, BufferedImage target) {
		double k = 0;
		for (int i=0;i<boundary.size();i++) {
			Point p = boundary.get(i);
			if (p.x>=0 && p.x<target.getWidth() && p.y>=0 && p.y<target.getHeight()) 
				k +=colorDistance(target.getRGB(p.x,p.y),source.getRGB(p.x,p.y));
		}
		k/=(double)boundary.size();
		
		return k;
	}

	/** Given the gradient of the image difference between source and target, compute
	 * the difference just as the sum of the gradient points. 
	 * @param boundary
	 * @param gradient
	 * @return
	 */
	public double colorDifference(Vector<Point> boundary, BufferedImage gradient) {
		double k = 0;
		for (int i=0;i<boundary.size();i++) {
			Point p = boundary.get(i);
			if (p.x>=0 && p.x<gradient.getWidth() && p.y>=0 && p.y<gradient.getHeight()) 
				k +=colorToGray(gradient.getRGB(p.x,p.y));
		}
		k/=(double)boundary.size();
		
		return k;
	}
	
	
	/** Computes the energy along a boundary given by a Vector of Points
	 * 	Assume that source and target are already aligned by the caller of this function
	 */
	public double colorDifference(Vector<Point> boundary, double ka, BufferedImage source, BufferedImage target) {
		double en = 0;
		for (int i=0;i<boundary.size();i++) {
			Point p = boundary.get(i);
			if (p.x>=0 && p.x<target.getWidth() && p.y>=0 && p.y<target.getHeight()) {
				double d=colorDistance(target.getRGB(p.x,p.y),source.getRGB(p.x,p.y))-ka;
				en +=(d*d);
			}
		}
		
		return en;
	}
	
	public double colorDifference(Vector<Point> boundary, double ka, BufferedImage gradient) {
		double en = 0;
		for (int i=0;i<boundary.size();i++) {
			Point p = boundary.get(i);
			if (p.x>=0 && p.x<gradient.getWidth() && p.y>=0 && p.y<gradient.getHeight()) {
				double d=colorToGray(gradient.getRGB(p.x,p.y))-ka;
				en +=(d*d);				
			}
		}
		
		return en;
	}

	
	/**
	 * 	Assume that source and target are already aligned by the caller of this function
	 */
	public double colorDifference(ValuePair bStart, double ka,
								  BufferedImage source, BufferedImage target) {
		Point p = (Point) bStart.getObject();
		double d=0;
		if (p.x>=0 && p.x<target.getWidth() && p.y>=0 && p.y<target.getHeight()) {
			d=colorDistance(target.getRGB(p.x,p.y),source.getRGB(p.x,p.y))-ka;
		}
		double en =(d*d);
		while (bStart.pointer != null) {
			bStart = bStart.pointer;
			p = (Point) bStart.getObject();			
			if (p.x>=0 && p.x<target.getWidth() && p.y>=0 && p.y<target.getHeight()) {
				d=colorDistance(target.getRGB(p.x,p.y),source.getRGB(p.x,p.y))-ka;
				en +=(d*d);
			}
		}
		
		return en;
	}

	public double colorDifference(ValuePair bStart, double ka, BufferedImage gradient) {
		Point p = (Point) bStart.getObject();
		double d=0;
		if (p.x>=0 && p.x<gradient.getWidth() && p.y>=0 && p.y<gradient.getHeight()) {
			d=colorToGray(gradient.getRGB(p.x,p.y))-ka;
		}
		double en =(d*d);
		while (bStart.pointer != null) {
			bStart = bStart.pointer;
			p = (Point) bStart.getObject();			
			if (p.x>=0 && p.x<gradient.getWidth() && p.y>=0 && p.y<gradient.getHeight()) {
				d=colorToGray(gradient.getRGB(p.x,p.y))-ka;
				en +=(d*d);
			}
		}

		return en;
	}
	
	/**
	 * 	Assume that source and target are already aligned by the caller of this function
	 */
	public double colorDifference(Point start, byte[][] pointers, 
								  double ka, BufferedImage source, BufferedImage target) {
		int i=start.x, j=start.y;
		byte p = pointers[i][j];
		double d=0;
		if (i>=0 && i<target.getWidth() && j>=0 && j<target.getHeight()) {
			d=colorDistance(target.getRGB(i,j),source.getRGB(i,j))-ka;
		}
		double en =(d*d);
		while (p>0) {
			switch(p) {
				case 1: i--; break;
				case 2: i++; break;
				case 3: j--; break;
				case 4: j++; break;
				case 5: i--; j--; break;
				case 6: i--; j++; break;
				case 7: i++; j--; break;
				case 8: i++; j++; break;					
			}
			if (i>=0 && i<target.getWidth() && j>=0 && j<target.getHeight()) {
				d=colorDistance(target.getRGB(i,j),source.getRGB(i,j))-ka;
			}
			en +=(d*d);
			p = pointers[i][j];
		}
		
		return en;
	}
	
	public double colorDifference(Point start, byte[][] pointers, 
			double ka, BufferedImage gradient) {
		int i=start.x, j=start.y;
		byte p = pointers[i][j];
		double d=0;
		if (i>=0 && i<gradient.getWidth() && j>=0 && j<gradient.getHeight()) {
			d=colorToGray(gradient.getRGB(i,j))-ka;
		}
		double en =(d*d);
		while (p>0) {
			switch(p) {
			case 1: i--; break;
			case 2: i++; break;
			case 3: j--; break;
			case 4: j++; break;
			case 5: i--; j--; break;
			case 6: i--; j++; break;
			case 7: i++; j--; break;
			case 8: i++; j++; break;					
			}
			if (i>=0 && i<gradient.getWidth() && j>=0 && j<gradient.getHeight()) {
				d=colorToGray(gradient.getRGB(i,j))-ka;
			}
			en +=(d*d);
			p = pointers[i][j];
		}

		return en;
	}

	/** Color distance between two points, computed as the L2-norm in the RGB color space */
	public static double colorDistance(int rgb1, int rgb2) {
		double r=(double)((rgb1>>16)&0x000000FF)-(double)((rgb2>>16)&0x000000FF);
		double g=(double)((rgb1>>8)&0x000000FF)-(double)((rgb2>>8)&0x000000FF);
		double b=(double)(rgb1&0x000000FF)-(double)(rgb2&0x000000FF);
		
		return Math.sqrt(r*r+g*g+b*b);
	}

	public static double colorToGray(int rgb) {
		double r=(double)((rgb>>16)&0x000000FF);
		double g=(double)((rgb>>8)&0x000000FF);
		double b=(double)(rgb&0x000000FF);
		
		return (r+g+b)/3.;		
	}
	/*
	 * The offset in the target image (compositeCanvas) should be taken into account!
	 */
	public static double colorDistance(Point p, BufferedImage s, BufferedImage t) {
		int rgb1 = s.getRGB(p.x,p.y);
		int rgb2 = 0;
		if (p.x>=0 && p.x<t.getWidth() && p.y>=0 && p.y<t.getHeight()) {
			t.getRGB(p.x,p.y);
		}
		return colorDistance(rgb1, rgb2);
	}
	
	
	/** This applies Dynamic Programming to optimize the Boundary 
	  * Speed test for the "contains" method
	  *  Results:
	  *  Vector: 9.172 s    (1834.4 times slower, 27.87 times slower than Image)
	  *  ---- without creating the object: 3.767 s
	  *  ArrayList: 9.139 s (1827.8 times slower)
	  *  ---- without creating the object: 3.767 s
	  *  Image: 0.329 s     (65.8 times slower)
	  *  Array: 0.0050 s
	  */
	public Vector<Point> optimizeBoundary(BufferedImage extContour, 
								   BufferedImage source, BufferedImage target) {
		int width = source.getWidth(), height = source.getHeight();
		if (width != target.getWidth() || height!=target.getHeight()) {
			System.err.println("[WARNING] Boundary: Source and target differ in size "+
					width+"x"+height+", "+target.getWidth()+"x"+target.getHeight());
		}
		Vector<Point> omega = MOps2D.listPointsNot(extContour, 0xFF000000);
		if (canvas!=null) canvas.add(omega, 0xFFFF0000, (int)maskBB.getMinX(), (int)maskBB.getMinY());
		
		MOps2D.floodfill4(extContour,-1,-1);
		MOps2D.not(extContour);
		BufferedImage extMask = MOps2D.colorMask(extContour,0xFFFFFFFF);
		BufferedImage binaryMask = MOps2D.plot(omega,mask.getWidth(),mask.getHeight());
		MOps2D.orImage(extMask,binaryMask,0xFFFFFFFF);
		debug(extMask, "opt01");
		
		BufferedImage diffOmega = MOps2D.difference(extMask, mask);
		// add the contour, cos it may be possible we erased it when doing the difference
		MOps2D.or(diffOmega, binaryMask);
		debug(diffOmega, "opt02");
		
		BufferedImage diff = COps.difference(source, target);
		BufferedImage gmag = COps.gradientMagnitude(diff);
		debug(source, "diff01");
		debug(target, "diff02");
		debug(diff, "diff03");
		debug(gmag, "diff04");
		
		double ka = gradientDifference?colorDifference(omega, gmag):
			colorDifference(omega, source, target);
		double energy = gradientDifference?colorDifference(omega, ka, gmag):
			colorDifference(omega, ka, source, target);
		double oldEnergy;
		
		byte[][] pointers = new byte[width][height];
		double[][] costs = new double[width][height];
		boolean[][] inSet = new boolean[width][height];
		byte[] assignments=new byte[] {
			2, 1, 4, 3, 8, 7, 6, 5
		};
		
		int count = 0;
		do {
			//System.out.println("[BOUNDARY] energy: "+energy);
			if (canvas.moved) break;
			// find shortest closed-path
			//---------------------------
			// find shortest cut C, cut[0] is the source, and cut[1] is the destination
			Vector[] cut = shortestCut(omega, contourPoints);
			
			updateGraphics(MOps2D.colorMask(diffOmega, 0x66FFFFFF), cut, omega);
			updateGraphics(contour);
			if (cut[0].size()<1 || cut[1].size()<1) // empty cut
				break;
			
			//updateGraphics(MOps2D.colorMask(diffOmega, 0x66FFFFFF), cut, null);
			//updateGraphics(null, cut, omega);
			//try { Thread.sleep(4000); } catch (Exception exc) {};
			TreeSet<ValuePair> ele = new TreeSet<ValuePair>();
			//Vector processed = new Vector();
			// Pointers expressed as a neighborhood relation like this:
			// |5|3|7|
			// |1|0|2|
			// |6|4|8|
			// So, if pointer[x][y]=2, it means it points to pointer[x][y-1]
			// ==0 means NULL
			// ==-1 means not processed
			for (int i=0;i<width;i++)
				for (int j=0;j<height;j++) {
					pointers[i][j]=-1;
					costs[i][j]=Double.MAX_VALUE;
					inSet[i][j]=false;
				}
			
			double cost;
			for (int m=0;m<cut[0].size();m++) {
				Point p = (Point) cut[0].get(m);
				// assume images aligned!
				cost = gradientDifference?colorToGray(gmag.getRGB(p.x, p.y)):
					colorDistance(p, source, target)-ka; 
				cost*=cost;
				ValuePair vp = new ValuePair(m,cost);
				vp.setObject(p);
				ele.add(vp);
				pointers[p.x][p.y]=0;
				inSet[p.x][p.y]=true;
			} // added all the points from the cut
			
			while (!ele.isEmpty()) {
				if (canvas.moved) break;
				ValuePair q = ele.first(); // min value
				ele.remove(q);
				//processed.add(q);
				Point pq = (Point) q.getObject();
				costs[pq.x][pq.y]=q.value;
				inSet[pq.x][pq.y]=false;
				
				// draw points in magenta
				//if (canvas != null) canvas.add(pq,0xFFFF00FF);
				Point[] neighbors = getNeighbors8(pq, cut, diffOmega, pointers);
				//if (canvas != null) canvas.add(neighbors,0xFF990099);
				//try { Thread.sleep(1000); } catch (Exception exc) {};
				for(int i=0;i<neighbors.length;i++) {
					Point pr = neighbors[i];
					if (pr!=null) {
						boolean arrived = cut[1].contains(pr);
						cost = gradientDifference?colorToGray(gmag.getRGB(pr.x, pr.y)):
							colorDistance(pr, source, target)-ka;
						cost*=cost;
						cost += q.value;
								// + local cost for linking pixels?
						if (arrived) { // add +++ cost to open paths, to make them close
							Point fp = getFirstPoint(pq,pointers);
							double geomDist = fp.distanceSq(pr);
							if (geomDist>2) { 
								// infinite cost for open paths!
								cost = Double.MAX_VALUE;
							}
						}	
						
						ValuePair r = new ValuePair(cost, pr);
						// ele.contains(r) won't work, since equals != ordering
						if (inSet[pr.x][pr.y]) {
							if (cost < costs[pr.x][pr.y]) {
								ValuePair rr=new ValuePair(costs[pr.x][pr.y],pr);
								// since it's sorted by order, for proper removal
								// we need to create an object of the same cost!!!!
								ele.remove(rr);
								inSet[pr.x][pr.y]=false;
							}
						}
						if (!inSet[pr.x][pr.y]) {
							r.pointer = q;
							pointers[pr.x][pr.y]=assignments[i];
							ele.add(r);
							inSet[pr.x][pr.y]=true;
						}
					}
				} // for all neightbors
			} // while (!ele.isEmpty)
			if (canvas.moved) break;
			//try { Thread.sleep(4000); } catch (Exception exc) {};
			
			
			int ii = 0;
			double energia = Double.MAX_VALUE;
			Point start = null;
			Vector<Point> pointList = null;
			do { // select the closed path with minimum cost
				
				// check all the blue points
				Point pp = (Point) cut[1].get(ii++);
				//ValuePair vpp = new ValuePair(pp);
				// nowhere to go?
				if (ii>=cut[1].size() && pointers[pp.x][pp.y]<0) break;
				while (pointers[pp.x][pp.y]<0) {
					pp = (Point) cut[1].get(ii++);
					if (ii>=cut[1].size()) break;
				}
				// nowhere to go in all the rest of points?? STOP!
				if (ii>=cut[1].size() && pointers[pp.x][pp.y]<0) {
					//System.out.println("-- Boundary: No path??");
					break;					
				}
				
				canvas.add(pp,0xFF00AAFF, (int)maskBB.getMinX(), (int)maskBB.getMinY());
				//try { Thread.sleep(4000); } catch (Exception exc) {};
				
				//ii = processed.indexOf(vpp);
				//ValuePair svpp = (ValuePair)processed.elementAt(ii);
				//Point start = (Point) svpp.getFirstObject();
				// start of the path (in the yellow line)
				Point firstPoint = getFirstPoint(pp, pointers);
				//canvas.add(firstPoint,0xFFFFAA00);
				if (firstPoint.distanceSq(pp)>2) {
					// WTF? the path is not closed...
					//System.out.println("... open path ... "+pp+" to "+firstPoint);
				} else {
					//double enar = colorDifference(firstPoint, pointers, ka, source, target);
					double enar = gradientDifference?colorDifference(pp, pointers, ka, gmag):
						colorDifference(pp, pointers, ka, source, target);
					//System.out.println("-- path from: "+pp+" to "+firstPoint+"; enar="+enar);
					
					if (enar < energia) {
						//pointList = getPointList(firstPoint, pointers);
						pointList = getPointList(pp, pointers);					
						//System.out.println("-- Boundary size: "+pointList.size());
						if (pointList.size()>=MIN_LENGTH) { //avoid bad loops!
							canvas.add(pointList, 0x88FFFFFF, (int)maskBB.getMinX(), (int)maskBB.getMinY());
							//try { Thread.sleep(2000); } catch (Exception exc) {};
							//start = firstPoint;
							start = pp;
							energia = enar;									
						}
					}					
				}
				
				
			} while (ii<cut[1].size()); 
			// we probably looped back if the new contour is too short!
			// new omega
			omega = getPointList(start, pointers);
			if (omega.size()<MIN_LENGTH) {
				System.out.println("[WARNING] Boundary: Path length = "+omega.size()+" ??!");
				break;
			}
			
			
			// update masks and stuff
			//----------------------------------
			extContour = MOps2D.plot(omega,mask.getWidth(),mask.getHeight());
			BufferedImage ccaux = MOps2D.plot(omega,mask.getWidth(),mask.getHeight());
			//canvas.addL(ccaux); try { Thread.sleep(2000); } catch (Exception exc) {};
			MOps2D.floodfill4(ccaux,-1,-1);
			//canvas.addL(ccaux); try { Thread.sleep(2000); } catch (Exception exc) {};

			MOps2D.not(ccaux);
			//canvas.addL(ccaux); try { Thread.sleep(2000); } catch (Exception exc) {};

			MOps2D.or(ccaux, extContour);
			//canvas.addL(ccaux); try { Thread.sleep(2000); } catch (Exception exc) {};
			extMask = MOps2D.colorMask(ccaux,0xFFFFFFFF);

			extContour = MOps2D.gradient(ccaux);
			//canvas.addL(extContour); try { Thread.sleep(2000); } catch (Exception exc) {};

			//MOps2D.orImage(extMask,extContour,0xFFFFFFFF);
			diffOmega = MOps2D.difference(extMask, mask);
			MOps2D.or(diffOmega, MOps2D.plot(omega,mask.getWidth(),mask.getHeight()));
			// get omega from this operation, to eliminate 3-connected pixels
			omega = MOps2D.listPoints(extContour);
			
			// compute K and energy again
			//---------------------------
			ka = gradientDifference?colorDifference(omega, gmag):
				colorDifference(omega, source, target);
			oldEnergy = energy;
			energy = gradientDifference?colorDifference(omega, ka, gmag):
				colorDifference(omega, ka, source, target);
			if (oldEnergy - energy <= 0 || oldEnergy/energy < 1.05)
				count++; // if it doesn't decrease, or too slow
			else count=0;
			
			
		} while (count<2); // two iterations without decreasing
		
		resultMask = extMask;
		updateGraphics(MOps2D.colorMask(diffOmega, 0x66FFFFFF), null, omega);
		updateGraphics(contour);
		
		return omega;
	} 
	
	/**
	  * Follows the pointers to retrieve the start of the path.
	  *
		 Pointers expressed as a neighborhood relation like this:
		 |5|3|7|
		 |1|0|2|
		 |6|4|8|
		 So, if pointer[x][y]=2, it means it points to pointer[x][y-1]
		 ==0 means NULL
		 ==-1 means not processed
	 */	 
	Point getFirstPoint(Point start, byte[][] pointers) {
		int i=start.x, j=start.y;
		byte p = pointers[i][j];
		int length = 0;
		while (p>0) {
			length++;
			switch(p) {
				case 1: i--; break;
				case 2: i++; break;
				case 3: j--; break;
				case 4: j++; break;
				case 5: i--; j--; break;
				case 6: i--; j++; break;
				case 7: i++; j--; break;
				case 8: i++; j++; break;					
			}
			p = pointers[i][j];
		}
		//System.out.println("-- distance to first: "+length);
		return new Point(i,j);
	}
	
	boolean isLongPath(Point start, byte[][] pointers) {
		int i=start.x, j=start.y;
		byte p = pointers[i][j];
		int l=1;
		while (p>0) {
			l++;
			if (l>=MIN_LENGTH) return true;
			switch(p) {
				case 1: i--; break;
				case 2: i++; break;
				case 3: j--; break;
				case 4: j++; break;
				case 5: i--; j--; break;
				case 6: i--; j++; break;
				case 7: i++; j--; break;
				case 8: i++; j++; break;					
			}
			p = pointers[i][j];
		}
		
		return false;
	}
	
	Vector<Point> getPointList(Point start, byte[][] pointers) {
		Vector<Point> v = new Vector<Point>();
		if (start==null) return v;
		v.add(start);
		int i=start.x, j=start.y;
		byte p = pointers[i][j];
		while (p>0) {
			switch(p) {
				case 1: i--; break;
				case 2: i++; break;
				case 3: j--; break;
				case 4: j++; break;
				case 5: i--; j--; break;
				case 6: i--; j++; break;
				case 7: i++; j--; break;
				case 8: i++; j++; break;					
			}
			p = pointers[i][j];
			v.add(new Point(i,j));
		}
		
		return v;
	}
	
	/**
      * Check the 8 adjacents pixels to p and check if they can be used.
	  * @param p the point whose neighbors we are gonna get
	  * @param cut Don't let it cross cut C, don't twist
	  * @param band The only are we can visit
	  * @param processed A list of already processed pixels.
	  * @return a list of possible neighbors
	  */
	Point[] getNeighbors8(Point p, Vector[] cut,BufferedImage band, byte[][] pointers) {
		int w = band.getWidth(), h = band.getHeight();
		Point[] nn = new Point[] {
			new Point(p.x-1,p.y),
			new Point(p.x+1,p.y),
			new Point(p.x,p.y-1),
			new Point(p.x,p.y+1),
			new Point(p.x-1,p.y-1),
			new Point(p.x-1,p.y+1),
			new Point(p.x+1,p.y-1),
			new Point(p.x+1,p.y+1)
		};
		Point[] ref=new Point[nn.length];
		for (int i=0;i<nn.length;i++) ref[i]=nn[i];
		boolean inYellow = cut[0].contains(p);
		for (int i=0;i<nn.length;i++) inYellow = inYellow || cut[0].contains(nn[i]);
		boolean twist = false;
		boolean visited = false;

		for (int i=0;i<nn.length;i++) {
			Point p0=nn[i];
			//if (inYellow) {
				twist=(cut[1].contains(p0)&&!isLongPath(p,pointers));
				// evitar filtraciones en la diagonal
				// use ref since nn is becoming null at some points!
				/* se supone no necesario con el nuevo cut azul 4-connected
				twist=twist||(i==4 && (cut[1].contains(ref[0])||cut[1].contains(ref[2])));
				twist=twist||(i==5 && (cut[1].contains(ref[0])||cut[1].contains(ref[3])));
				twist=twist||(i==6 && (cut[1].contains(ref[2])||cut[1].contains(ref[1])));
				twist=twist||(i==7 && (cut[1].contains(ref[1])||cut[1].contains(ref[3])));
				 */
			//} 
			//visited = processed.contains(new ValuePair(p0));
			if (p0.x>=0 && p0.x<w && p0.y>=0 && p0.y<h) {
				visited = (pointers[p0.x][p0.y]>=0);
				if (band.getRGB(p0.x,p0.y)==0xFF000000 || twist || visited)
					nn[i]=null;
			} else {
				nn[i]=null;
			}
		}
		
		return nn;
	}
	
	Point[] getNeighbors4(Point p, Vector<?> destinationCut) {
		int w=targetImage.getWidth(), h=targetImage.getHeight();
		Point[] nn = new Point[] {
			new Point(p.x-1,p.y),
			new Point(p.x+1,p.y),
			new Point(p.x,p.y-1),
			new Point(p.x,p.y+1),
		};
		for (int i=0;i<nn.length;i++) {
			Point p0=nn[i];
			if (!destinationCut.contains(p0)) nn[i]=null;
			if (p0!=null)
				if (p0.x<0 || p0.x>=w || p0.y<0 || p0.y>=h)
					nn[i]=null;
		}
		
		return nn;		
	}
	
	Vector<Point>[] shortestCut(Vector<Point> outside, Vector<Point> inside) {
		//int w = targetImage.getWidth(), h = targetImage.getHeight();
		int w=(int)maskBB.getWidth(), h=(int)maskBB.getHeight();
		Vector[] vectors = new Vector[] {
					new Vector<Point>(), new Vector<Point>()
				};
		Vector<Point>[] cut = vectors;
		
		if (outside.size()<1 || inside.size()<1) return cut;
		Point mina = outside.get(0);
		Point minb = inside.get(0);
		double distance = mina.distanceSq(minb);
		for (int i=0;i<outside.size();i++) {
			Point a = outside.get(i);
			for (int j=0;j<inside.size();j++){
				Point b=inside.get(j);
				double d=a.distanceSq(b);
				if (d<distance) {
					mina = a; minb = b; distance = d;
				}
			} // end for j
		} // end for i
		
		
		if (mina.equals(minb)) { 
			// only one point!
			// close all the neighborhood. Open just one point of the contour
			cut[0].add(mina);
			Point p = mina;
			Point[] nn = new Point[] {
				new Point(p.x-1,p.y),
				new Point(p.x+1,p.y),
				new Point(p.x,p.y-1),
				new Point(p.x,p.y+1),
				new Point(p.x-1,p.y-1),
				new Point(p.x-1,p.y+1),
				new Point(p.x+1,p.y-1),
				new Point(p.x+1,p.y+1)
			};
			for (int j=0;j<nn.length;j++) {
				Point pp=nn[j];
				if (pp.x<w && pp.y<h && pp.x>=0 && pp.y>=0)
					cut[1].add(pp);
			}
			for (int j=0;j<nn.length;j++) {
				Point p0 = nn[j];
				//if (outside.contains(p0)||inside.contains(p0)) {
				if (outside.contains(p0)) {					
					cut[1].remove(p0);
					break;
				}
			}
			return cut;
		}
		
		
		// now, apply Bresenham line algorithm!
		int x0=mina.x,y0=mina.y,x1=minb.x,y1=minb.y;
		boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
		if (steep) { // swap if too steep
			x0=mina.y;y0=mina.x;
			x1=minb.y;y1=minb.x;
		}
		if (x0 > x1) {
			int swap=x0;
			x0=x1;x1=swap;
			swap=y0;y0=y1;y1=swap;
		}
		int deltax = x1 - x0;
		int deltay = Math.abs(y1 - y0);
		int error = 0;
		int ystep = 0;
		int y = y0;
		if (y0 < y1) ystep = 1;
		else ystep = -1;
		int x=x0;
		// start the blue line first
		
		if (steep) { //plot(y,x);
			if (x0-1>=0) {
				if (y0<w) cut[1].add(new Point(y0,x0-1));
				int yb=y0-ystep;
				if (yb<w && yb>=0) cut[1].add(new Point(yb,x0-1));
			}
		} else { //plot(x,y);
			if (x0-1>=0) {
				if (y0<h) cut[1].add(new Point(x0-1,y0));
				int yb=y0-ystep;
				if (yb<h && yb>=0) cut[1].add(new Point(x0-1,yb));
			}
		} 
		// add one extra point if possible (for cases of length=1)
		/*
		if (steep) {
			if (x1+1<h) x1++;
		} else {
			if (x1+1<w) x1++;
		}*/
		boolean displaced = false;
		for (x=x0;x<=x1;x++) {
			if (steep) { //plot(y,x);
				if (y<w && x<h && x>=0 && y>=0) {
					cut[0].add(new Point(y,x));
					int yb=y-ystep;
					if (yb<w && yb>=0) {
						cut[1].add(new Point(yb,x));
						yb-=ystep;
						if (displaced && yb<w && yb>=0) cut[1].add(new Point(yb,x));
					}
				}
			} else { //plot(x,y);
				if (x<w && y<h && x>=0 && y>=0) {
					cut[0].add(new Point(x,y));
					int yb=y-ystep;
					if (yb<h && yb>=0) {
						cut[1].add(new Point(x,yb));
						// connect completely (4-connect)
						yb-=ystep;
						if (displaced && yb<h && yb>=0) cut[1].add(new Point(x,yb));
					} 
				}
			} 
				
			error += deltay;
			if ((error<<1) >= deltax) {
				displaced = true;
				y += ystep;
				error = error - deltax;
			} else {
				displaced = false;
			}
		} // for x
		// add one more blue point
		
		
		if (steep) { //plot(y,x);
			if (x<h && y>=0) {
				int yb=y-ystep;
				if (yb<w && yb>=0) cut[1].add(new Point(yb,x));
				yb-=ystep;
				if (yb<w && yb>=0) cut[1].add(new Point(yb,x));
			}
		} else { //plot(x,y);
			if (x<w && y>=0) {
				int yb=y-ystep;
				if (yb<h && yb>=0) cut[1].add(new Point(x,yb));
				yb-=ystep;
				if (yb<h && yb>=0) cut[1].add(new Point(x,yb));
			}
		} 
		

		
		return cut;
	} //end shortestCut
	
	private void updateGraphics(BufferedImage band, Vector[] cut, Vector<Point> list) {
		canvas.clearLines();
		if (canvas != null) {
			if (band!=null) canvas.addL(band, (int)maskBB.getMinX(), (int)maskBB.getMinY());
			if (list!=null) canvas.add(list,0xFFFF0000, (int)maskBB.getMinX(), (int)maskBB.getMinY());
			if (cut != null) {
				canvas.add(cut[0],0xFFFFFF00, (int)maskBB.getMinX(), (int)maskBB.getMinY());
				canvas.add(cut[1],0xFF00FFFF, (int)maskBB.getMinX(), (int)maskBB.getMinY());
			}
		}
	}
	
	private void updateGraphics(BufferedImage border) {
		if (canvas != null) {
			canvas.addL(border, (int)maskBB.getMinX(), (int)maskBB.getMinY());
		}
	}
	
	public void setPoissonType(int type) {
		poissonType = type;
	}

	public void setPoissonParameters(double sGradient, double poissonAccuracy) {
		this.sGradient = sGradient;
		this.poissonAccuracy = poissonAccuracy;
	}
	
	void debug(BufferedImage bim) {
		if (debugCanvas!=null) {
			debugCanvas.set(bim);
		}
	}
	
	BufferedImage createClipped(BufferedImage source) {
		return createClipped(source, BufferedImage.TYPE_INT_ARGB);
	}
	/** Creates a clipped image from the source with the current bounding box maskBB */
	BufferedImage createClipped(BufferedImage source, int type) {
		return createClipped(source,type, 
				(int)maskBB.getWidth(), (int)maskBB.getHeight(), 
				(int)-maskBB.getMinX(), (int)-maskBB.getMinY());
	}
	 
	BufferedImage createClipped(BufferedImage source, int type, int width, int height, int xoff, int yoff) {
		BufferedImage clipped = new BufferedImage(width, height, type);
		Graphics gi = clipped.getGraphics();
		gi.drawImage(source, xoff, yoff, null);
		gi.dispose();
		return clipped;
	}
	
	/** The thread will find the optimum cut and update the display, when available */
	public void run() {
		try {
						
			while(sourceImage==null || targetImage==null || canvas==null) {
				Thread.sleep(500); // half second
			}			
			
			//sourceImage = canvas.getClippedImage();
			// don't clip it! I want full objects
			sourceImage = canvas.getImage();
			Point targetOffset = compositeCanvas.getOffset();
			
			// don't alter the real buffer of sourceImage, since it belongs to ClipCanvas!
			// These will be really initialized when we get a bounding box maskBB
			BufferedImage clippedSource = sourceImage;
			BufferedImage clippedTarget = targetImage;

			if (roughSegmentation!=null) {
				setSegmentedThreaded();
			}
			canvas.clearLines();
			switch (method) {
				case NO_OPTIMIZATION:
					// TODO  This stopped working!!!
//					if (externalContour!=null) {
//						MOps2D.floodfill4(externalContour,-1,-1);
//						MOps2D.not(externalContour);
//						resultMask = externalContour;
//					}
					updateGraphics(contour);
					clippedSource = createClipped(sourceImage);
					clippedTarget = createClipped(targetImage, BufferedImage.TYPE_INT_ARGB, 
							(int)maskBB.getWidth(), (int)maskBB.getHeight(), 
							-targetOffset.x, -targetOffset.y);
					break;
				
				case HARD_SEGMENTATION:	// it's the default, where resultMask = masked object
//					if (roughSegmentation!=null) {
//						setSegmentedThreaded();
//					}
					updateGraphics(contour);
					clippedSource = createClipped(sourceImage);
					clippedTarget = createClipped(targetImage, BufferedImage.TYPE_INT_ARGB, 
							(int)maskBB.getWidth(), (int)maskBB.getHeight(), 
							-targetOffset.x, -targetOffset.y);
					break;
					
				case INTELLIGENT_SCISSORS:
//					if (roughSegmentation!=null) {
//						setSegmentedThreaded();
//					}					
					updateGraphics(contour);
				case INTELLIGENT_ERODE:
					if (externalContour!=null) {
						clippedSource = createClipped(sourceImage);
						clippedTarget = createClipped(targetImage, BufferedImage.TYPE_INT_ARGB, 
								(int)maskBB.getWidth(), (int)maskBB.getHeight(), 
								-targetOffset.x, -targetOffset.y);

						// optimize with the external contour drawn by the user
						optimizeBoundary(externalContour, clippedSource, clippedTarget);
					}
					break;
			}
				
				
				/*else {
						
						int[] strel = new int[DILATION];
						for (int i=0;i<DILATION;i++)
							strel[i]=1;
						
						//Thread.sleep(2000);
						BufferedImage extContour = MOps2D.gradient(MOps2D.dilate(mask,strel));
						//updateGraphics(mask);
						//Thread.sleep(2000);
						//get a new boundary (resultMask)
						optimizeBoundary(extContour, sourceImage, targetImage);						
				} */
			
			// wrong offset when moved!!!
			//compositeCanvas.updateObject(MOps2D.maskImage(sourceImage, resultMask));	

			BufferedImage alphaMask = new BufferedImage(resultMask.getWidth()+PADDING*2, 
														resultMask.getHeight()+PADDING*2, 
														BufferedImage.TYPE_BYTE_GRAY);
			Graphics gi = alphaMask.getGraphics();
			gi.drawImage(resultMask, PADDING, PADDING, null);
			gi.dispose();
			
			if (alphaMatting) {
				// Gaussian ring
				ConvolveOp convolve = new ConvolveOp(KERNEL);
				alphaMask = convolve.filter(alphaMask, null);
			}
			alphaMask = alphaMask.getSubimage(PADDING,PADDING,
												resultMask.getWidth(), 
												resultMask.getHeight());
			
			clippedTarget = createClipped(targetImage, BufferedImage.TYPE_INT_ARGB, 
					(int)maskBB.getWidth(), (int)maskBB.getHeight(), 
					-targetOffset.x, -targetOffset.y);
			clippedSource = createClipped(sourceImage); // why do I have to do it again for hard segmentation??
			MOps2D.alphaMask(clippedSource, alphaMask);
			compositeCanvas.updateObject(clippedSource);				
		
			
			
			if (poissonType > 0 && !canvas.moved) {
				BufferedImage binaryMask = MOps2D.bw2binary(resultMask);
				debug(clippedSource, "poisson01");
				debug(clippedTarget, "poisson02");
				debug(binaryMask, "poisson03");
				Poisson poisson = new Poisson(clippedSource, clippedTarget, binaryMask);
				poisson.setComposite(compositeCanvas);
				poisson.setMethod(poissonType);
				poisson.accuracy=poissonAccuracy;
				poisson.setMixture(sGradient);
				BufferedImage result = poisson.optimize();
				//compositeCanvas.updateObject(result);
				//Thread.sleep(2000);
				// get rid of the contour
				BufferedImage maskNoContour = MOps2D.colorMask(MOps2D.erode(binaryMask),0xFFFFFFFF);
				//compositeCanvas.updateObject(maskNoContour);
				//Thread.sleep(2000);
				
				compositeCanvas.updateObject(MOps2D.maskImage(result,maskNoContour));
				//MOps2D.alphaMask(result, alphaMask);
				//compositeCanvas.updateObject(result);
			}

		
		} catch (Exception exc) {
			System.err.println("Boundary thread: "+exc);
		}

	}
	
} // end Boundary Class
