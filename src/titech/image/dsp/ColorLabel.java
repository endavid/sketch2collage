//
//  ColorLabel.java
//  sketchRC
//
//  Created by David Gavilan on 1/13/07.
//  Copyright 2007 NakajimaLab. All rights reserved.
//
package titech.image.dsp;

import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import titech.util.*;

public class ColorLabel {
	
	private int nlabels;
	private int[] equivalences = null;
	int[][] labeled;
	int width;
	int height;
	
	/**
	 * Source doesn't need to be an indexedImage, since we use getRGB method to compare colors.
	 */
	public ColorLabel(BufferedImage indexedImage) {
		labeled=colorLabel(indexedImage);
	}
	
	public ColorLabel(BufferedImage indexedImage, int color) {
		labeled=colorLabel(indexedImage, new Color(color));
	}
	
	public int getNLabels() {
		return nlabels;
	}
	
	public int[][] getLabeledMatrix() {
		return labeled;
	}
	
	private int[][] colorLabel(BufferedImage src) {
		return colorLabel(src, null);
	}

	
	/**
	 * Labels an indexed image.
	 * <p>
	 * The labeling of a pixel <code>p</code>, which <code>index(p)</code> is not 0 (background color index) occurs as follows:
	 * <ul>
	 * <li>If all four neighbors are different from <code>index(p)</code>, assign a new label to <code>p</code>, else
	 * <li>if only one neighbor has the same color, assign its label to <code>p</code>, else
	 * <li>if one or more of the neighbors have the same color, assign one of the labels to <code>p</code> and make a note of the equivalences.
	 *</ul>
	 * <p>
	 * The original algorithm for binary images can be found in: <a href="http://www.dai.ed.ac.uk/HIPR2/label.htm">Connected Components Algorithm</a>
	 *
	 * @param  img  Input image, supposed to be indexed. Otherwise, only red channel is used.
	 * @param  bgColor  If not null, it doesn't label pixels of color == bgColor
	 * @return      dst.
	 */
	private int[][] colorLabel(BufferedImage src, Color background) {
		width = src.getWidth();
		height = src.getHeight();
		
		
		int[][] dst = new int[width][height];
		// neighbours
		int[] nb = new int[4];
		// neighbours' labels
		int[] nbl = new int[4];
		
		equivalences = new int[width * height];
		// no more groups!
		// equivalents to themselves
		for (int i = 0; i < equivalences.length; i++) {
			equivalences[i] = i;
		}
		
		int bgColor = 0;
		if (background!=null) {
			bgColor = background.getRGB();
		}
		// the first label
		int label = 1;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = src.getRGB(x,y);
				if (background!=null && bgColor == color) {
					dst[x][y]=0;
				} else {
					// for all colors (including black) -- no background!
					nb[0] = getSample(src, x - 1, y);
					nb[1] = getSample(src, x, y - 1);
					nb[2] = getSample(src, x - 1, y - 1);
					nb[3] = getSample(src, x + 1, y - 1);
					
					nbl[0] = getSample(dst, x - 1, y);
					nbl[1] = getSample(dst, x, y - 1);
					nbl[2] = getSample(dst, x - 1, y - 1);
					nbl[3] = getSample(dst, x + 1, y - 1);
					
					if (nb[0] != color && nb[1] != color && nb[2] != color && nb[3] != color) {
						dst[x][y] = label++;
					} else {
						// count neighbours with the same color
						int count = 0;
						int found = -1;
						for (int i = 0; i < 4; i++) {
							if (nb[i] == color) {
								count++;
								found = i;
							}
						}
						dst[x][y] = nbl[found];
						if (count > 1) {
							for (int i = 0; i < 4; i++) {
								if (nb[i] == color && nbl[i] != dst[x][y]) {
									//System.out.println("("+x+","+y+")="+nbl[i]+"="+dst[x][y]);
									associate(nbl[i], dst[x][y]);
								}
							}
						}
					}					
				} // end if background 
				
			}
		}
		
		//reduce labels ie 76=23=22=3 -> 76=3
		//done in reverse order to preserve sorting
		for (int i = label - 1; i > 0; i--) {
			equivalences[i] = reduce(i);
			//System.out.println("equiv: "+i+"="+equivalences[i]);
		}
		
		/*
		 *  now labels will look something like 1=1 2=2 3=2 4=2 5=5.. 76=5 77=5
		 *  this needs to be condensed down again, so that there is no wasted
		 *  space eg in the above, the labels 3 and 4 are not used instead it jumps
		 *  to 5.
		 */
		int condensed[] = new int[label];
		// cant be more labels
		//System.err.println("labels: "+label);
		int count = 0;
		for (int i = 0; i < label; i++) {
			if (i == equivalences[i]) {
				condensed[i] = count++;
			}
		}
		// Record the number of labels
		nlabels = count;
		// that includes the background
		
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int i = condensed[equivalences[dst[x][y]]];
				dst[x][y]=i;
			}
		}
		
		equivalences = null;
		
		return dst;
	}
	
	private int getSample(BufferedImage src, int x, int y) {
		int w = src.getWidth();
		int h = src.getHeight();
		return ((x < 0) || (x >= w) || (y < 0) || (y >= h)) ? 0 : src.getRGB(x,y);
	}
	
	private int getSample(int[][] src, int x, int y) {
		int w = src.length;
		int h = src[0].length;
		return ((x < 0) || (x >= w) || (y < 0) || (y >= h)) ? 0 : src[x][y];
	}
	
	
	private int reduce(int a) {
		
		if (equivalences[a] == a) {
			return a;
		} else {
			return reduce(equivalences[a]);
		}
	}
	
	
	private void associate(int a, int b) {
		if (a > b) {
			associate(b, a);
			return;
		}
		if (a == b || equivalences[b] == a) {
			return;
		}
		if (equivalences[b] == b) {
			equivalences[b] = a;
		} else {
			associate(equivalences[b], a);
			equivalences[b] = a;
		}
	}
	
	BufferedImage getAsImage(Vector regions, int[] equiv) {
		byte[][] colormap = new byte[3][256];
		int n = regions.size();
		colormap[0][0]=80; colormap[1][0]=80; colormap[2][0]=80;
		double colorNorm = 100.;
		for (int i=0;i<n;i++) {
			Region r=(Region)regions.get(i);
			float[] col = new float[3];
			for (int j = 0; j < 3; j++) col[j] = (float)(colorNorm * r.color[j]);
			col = Utilities.LabtosRGB(col);
			for (int j = 0; j < 3; j++) {
				col[j] *= 255f;
				colormap[j][i+1] = (byte) col[j];
			}
		}
		
        BufferedImage outImage = Segmenter.createIndexedImage(width,
													height, colormap);
		
		WritableRaster wrasta = outImage.getRaster();
		for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++)
				wrasta.setSample(i, j, 0, equiv[labeled[i][j]]);
		}
		
		return outImage;
	}
	
	/**
	  * With a random palette
	  */
	public BufferedImage getAsImage() {
		byte[][] colormap = new byte[3][256];
		colormap[0][255]=0; colormap[1][255]=0; colormap[2][255]=0;
		for (int i=1;i<255;i++) {
			colormap[0][i]=(byte)(256.*Math.random()); 
			colormap[1][i]=(byte)(256.*Math.random()); 
			colormap[2][i]=(byte)(256.*Math.random()); 			
		}
        BufferedImage outImage = Segmenter.createIndexedImage(width,
															  height, colormap);
		
		WritableRaster wrasta = outImage.getRaster();
		for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
				int l = labeled[i][j];
				wrasta.setSample(i, j, 0, l>=255?255:l);
			}
		}
		
		return outImage;
		
	}
}
