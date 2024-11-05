//
//  MOps2D.java
//  sketchRC
//
//  Created by David Gavilan on 11/4/06.
//  Copyright 2006 Nakajima Lab. All rights reserved.
//
package titech.image.dsp;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import titech.image.math.*;

/** Morphological Operations in BufferedImages (Java2D) */
public class MOps2D {
	
	/** Assumes that the input is an indexed image, and returns
	  * a TYPE_BYTE_BINARY image such that source == index */
	public static BufferedImage equal(BufferedImage source, int index) {
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		Raster rasta = source.getData();
		WritableRaster wrasta = out.getRaster();
		int width = source.getWidth();
		int height = source.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int pixel = rasta.getSample(i,j,0);
				if (pixel == index) wrasta.setSample(i,j,0,1);
			}
		}
		
		return out;
	}
	
	/** 
	  * a TYPE_BYTE_BINARY image such that source != color 
	  * */
	public static BufferedImage equalNot(BufferedImage source, int color) {
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		WritableRaster wrasta = out.getRaster();
		int width = source.getWidth();
		int height = source.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int pixel = source.getRGB(i, j);
				if (pixel != color) wrasta.setSample(i,j,0,1);
			}
		}
		
		return out;
	}

	/** Assumes that the input is an indexed image, and returns
	  * a TYPE_BYTE_BINARY image such that source == index
	  * The output image size is set to "size" and the source is scaled to that size.
	  */
	public static BufferedImage equal(BufferedImage source, int index, Dimension size) {
		BufferedImage out = new BufferedImage((int)size.getWidth(), (int)size.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		Raster rasta = source.getData();
		WritableRaster wrasta = out.getRaster();
		double scalex = (double)source.getWidth()/size.getWidth();
		double scaley = (double)source.getHeight()/size.getHeight();

		for (int y=0;y<size.getHeight();y++) {
			for (int x=0;x<size.getWidth();x++) {
				int i=(int)(x*scalex);
				int j=(int)(y*scaley);
				int pixel = rasta.getSample(i,j,0);
				if (pixel == index) wrasta.setSample(x,y,0,1);
			}
		}
		
		return out;
	}

	/** Assumes that the input is an indexed image, and returns
	* a TYPE_BYTE_BINARY image such that source in {index}
	* The output image size is set to "size" and the source is scaled to that size.
	*/
	public static BufferedImage equal(BufferedImage source, int[] inds, Dimension size) {
		BufferedImage out = new BufferedImage((int)size.getWidth(), (int)size.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		Raster rasta = source.getData();
		WritableRaster wrasta = out.getRaster();
		double scalex = (double)source.getWidth()/size.getWidth();
		double scaley = (double)source.getHeight()/size.getHeight();
		
		for (int y=0;y<size.getHeight();y++) {
			for (int x=0;x<size.getWidth();x++) {
				int i=(int)(x*scalex);
				int j=(int)(y*scaley);
				int pixel = rasta.getSample(i,j,0);
				if (AMath.inVector(pixel,inds)) wrasta.setSample(x,y,0,1);
			}
		}
		
		return out;
	}
	
	public static BufferedImage bw2binary(BufferedImage source) {
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		Graphics gi = out.getGraphics();
		gi.drawImage(source, 0, 0, null);
		gi.dispose();
		
		return out;
	}
	
	
	public static BufferedImage erode(BufferedImage source, int[][] strel) {
		return erode(source, strel, true);
	}

	/** Erodes the binary source image given
	  * @param strel the structuring element
	  */
	public static BufferedImage erode(BufferedImage source, int[][] strel, boolean zeroPadding) {
		
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage out = new BufferedImage(width, height, 
											  BufferedImage.TYPE_BYTE_BINARY);

		Raster rasta = source.getData();
		WritableRaster wrasta = out.getRaster();
		
		int shh = strel.length >> 1;
		int swh = strel[0].length >> 1;
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				// check if the structuring element fits in that position
				// erode otherwise
				boolean fits = true;
				for (int y=0;y<strel.length;y++)
					for (int x=0;x<strel[y].length;x++) {
						int ni = i-swh+x, nj = j-shh+y;
						// count only pixels inside the image
						if (ni>=0 && nj>=0 && ni<width && nj<height) {
							int pixel = rasta.getSample(ni,nj,0);
							if (strel[y][x]==1 && pixel==0) fits = false;
						} else if (strel[y][x]==1 && zeroPadding) { 
							// assume outside is padded with zeros
							fits = false;
						}
					}
				if (fits) wrasta.setSample(i,j,0,1);
			}
		}
		
		
		return out;
	}

	/** Erodes the binary source image given a symmetric structuring element
	* @param strel the structuring element
	*/
	public static BufferedImage erode(BufferedImage source, int[] strel) {
		
		BufferedImage aux = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		
		Raster rasta = source.getData();
		WritableRaster wrasta = aux.getRaster();
		int width = source.getWidth();
		int height = source.getHeight();
		
		int shh = strel.length >> 1;
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				// check if the structuring element fits in that position
				// erode otherwise
				boolean fits = true;
				
				for (int y=0;y<strel.length;y++) {
					int nj = j-shh+y;
					// count only pixels inside the image
					if (nj>=0 && nj<height) {
						int pixel = rasta.getSample(i,nj,0);
						if (strel[y]==1 && pixel==0) {
							fits = false;
							break;
						}
					} else { // assume outside is padded with zeros
						fits = false;
					}
				}
				if (fits) wrasta.setSample(i,j,0,1);
			}
		}
		
		rasta = aux.getData();
		wrasta = out.getRaster();
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				boolean fits = true;
				for (int x=0;x<strel.length;x++) {
					int ni = i-shh+x;					
					// count only pixels inside the image
					if (ni>=0 && ni<width) {
						int pixel = rasta.getSample(ni,j,0);
						if (strel[x]==1 && pixel==0) {
							fits = false;
							break;
						}
					} else { // assume outside is padded with zeros
						fits = false;
					}
				}
				if (fits) wrasta.setSample(i,j,0,1);
			}
		}
		
		return out;
	}
	
	
	/** Dilates the binary source image given
	* @param strel the structuring element
	*/
	public static BufferedImage dilate(BufferedImage source, int[][] strel) {
		
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		Raster rasta = source.getData();
		WritableRaster wrasta = out.getRaster();
		int width = source.getWidth();
		int height = source.getHeight();
		
		int shh = strel.length >> 1;
		int swh = strel[0].length >> 1;
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				// check if any part of the structuring element touches the object
				// if it does, add the pixel
				boolean touches = false;
				for (int y=0;y<strel.length;y++) {
					for (int x=0;x<strel[y].length;x++) {
						int ni = i-swh+x, nj = j-shh+y;
						// count only pixels inside the image
						if (ni>=0 && nj>=0 && ni<width && nj<height) {
							int pixel = rasta.getSample(ni,nj,0);
							if (strel[y][x]==1 && pixel!=0) {
								touches = true;
								break;
							}
						}
					} // for x
					if (touches) break;
				} // for y
				if (touches) wrasta.setSample(i,j,0,1);
			}
		}
				
		return out;
	}

	/** Dilates the binary source image given a symmetric structuring element
	* @param strel the structuring element
	*/
	public static BufferedImage dilate(BufferedImage source, int[] strel) {
		
		BufferedImage aux = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);

		
		Raster rasta = source.getData();
		WritableRaster wrasta = aux.getRaster();
		int width = source.getWidth();
		int height = source.getHeight();
		
		int shh = strel.length >> 1;
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				// check if any part of the structuring element touches the object
				// if it does, add the pixel
				boolean touches = false;
				
				for (int y=0;y<strel.length;y++) {
					int nj = j-shh+y;
					// count only pixels inside the image
					if (nj>=0 && nj<height) {
						int pixel = rasta.getSample(i,nj,0);
						if (strel[y]==1 && pixel!=0) {
							touches = true;
							break;
						}
					}
				}
				if (touches) wrasta.setSample(i,j,0,1);
			}
		}
		
		rasta = aux.getData();
		wrasta = out.getRaster();
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				// check if any part of the structuring element touches the object
				// if it does, add the pixel
				boolean touches = false;
				for (int x=0;x<strel.length;x++) {
					int ni = i-shh+x;					
					// count only pixels inside the image
					if (ni>=0 && ni<width) {
						int pixel = rasta.getSample(ni,j,0);
						if (strel[x]==1 && pixel!=0) {
							touches = true;
							break;
						}
					}
				}
				if (touches) wrasta.setSample(i,j,0,1);
			}
		}
		
		return out;
	}
	
	public static BufferedImage erode(BufferedImage source) {
		return erode(source, true);
	}
	
	public static BufferedImage erode(BufferedImage source, boolean zeroPadding) {
		return erode(source,new int[][]{
			new int[] {0, 1, 0},
			new int[] {1, 1, 1},
			new int[] {0, 1, 0}			
		}, zeroPadding);
	}
	
	/** Erodes the binary image given by source == index 
	* with the default structuring element 3 x 3 + */
	public static BufferedImage erode(BufferedImage source, int index) {
		return erode(equal(source,index),new int[][]{
			new int[] {0, 1, 0},
			new int[] {1, 1, 1},
			new int[] {0, 1, 0}			
		});
	}

	public static BufferedImage dilate(BufferedImage source) {
		return dilate(source,new int[][]{
			new int[] {0, 1, 0},
			new int[] {1, 1, 1},
			new int[] {0, 1, 0}			
		});
	}
	
	/** Dilates the binary image given by source == index 
	* with the default structuring element 3 x 3 + */
	public static BufferedImage dilate(BufferedImage source, int index) {
		return dilate(equal(source,index),new int[][]{
			new int[] {0, 1, 0},
			new int[] {1, 1, 1},
			new int[] {0, 1, 0}			
		});
	}
	
	/** xor(s1,s2) */
	public static BufferedImage difference(BufferedImage s1, BufferedImage s2) {
		int width = s1.getWidth();
		int height = s1.getHeight();
		BufferedImage out = new BufferedImage(width, height, 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		Raster r1 = s1.getData();
		Raster r2 = s2.getData();
		WritableRaster wrasta = out.getRaster();
		
		if (s2.getWidth()<width || s2.getHeight()<height) {
			System.err.println("MOps2D.difference: Size mismatch! "+width+"x"+height+", "+
					s2.getWidth()+"x"+s2.getHeight());
			return out;
		}
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int p1 = r1.getSample(i,j,0);
				int p2 = r2.getSample(i,j,0);
				if (p1!=p2) wrasta.setSample(i,j,0,1);
			}
		}
		
		return out;
				
	}

	/** Given an input binary image, outputs an ARGB image with value argb where binary(i,j) == 1 */
	public static BufferedImage colorMask(BufferedImage binary, int argb) {
		BufferedImage out = new BufferedImage(binary.getWidth(), binary.getHeight(), 
											  BufferedImage.TYPE_INT_ARGB);
		
		Raster r = binary.getData();
		int width = binary.getWidth();
		int height = binary.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int p1 = r.getSample(i,j,0);
				if (p1==1) out.setRGB(i,j,argb);
			}
		}
		
		return out;
	}
	
	public static BufferedImage maskImage(BufferedImage source, BufferedImage mask) {
		return maskImage(source, mask, new Point(0,0));
	}
	
	/*
	 * @param offset The offset of the mask
	 */
	public static BufferedImage maskImage(BufferedImage source, BufferedImage mask, Point offset) {
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage out = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		for (int j=0;j<height;j++)
			for (int i=0;i<width;i++) {
				int v=source.getRGB(i,j);
				int m=0x00000000;
				int im=i+offset.x,jm=j+offset.y;
				if (im>=0 && im<mask.getWidth() && jm>=0 && jm<mask.getHeight()) 
					m=mask.getRGB(im,jm);
				out.setRGB(i,j,v&m);
			}
				return out;
	}
	
	/** Changes the alpha channel of the source image.
	  * @param mask should be a gray scale image */
	public static void alphaMask(BufferedImage source, BufferedImage mask) {
		int w = source.getWidth();
		int h = source.getHeight();
		if (w!=mask.getWidth() || h!=mask.getHeight()) {
			System.err.println("MOps2D.alphaMask: Sizes differ! "+ 
					w+"x"+h+", "+mask.getWidth()+"x"+mask.getHeight());
			return;
		}
		WritableRaster wrasta = source.getAlphaRaster();
		if (wrasta == null) {
			System.err.println("MOps2D.alphaMask: no alpha channel!");
			return;
		}
		int[] samples = new int[w*h];
		mask.getData().getSamples(0,0,w,h,0,samples);
		wrasta.setSamples(0,0,w,h,0,samples);
	}
	
	public static BufferedImage plot(Vector points, int width, int height) {
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		WritableRaster wr = out.getRaster();

		for (int i=0;i<points.size();i++) {
			Point p = (Point)points.get(i);
			if (p.x>=0 && p.y>=0 && p.x<width && p.y<height) 
				wr.setSample(p.x,p.y,0,1);
			else {
				System.err.println("MOps2D.plot: Coordinate out of bounds! "+p);
				return out;
			}
		}
		return out;
	}
	
	public static void orImage(BufferedImage source, BufferedImage binary, int argb) {
		Graphics gi = source.getGraphics();
		gi.drawImage(colorMask(binary,argb),0,0,null);
		gi.dispose();
	}
	
	/** source <- source | mask */
	public static void or(BufferedImage source, BufferedImage mask) {		
		Raster r2 = mask.getData();
		WritableRaster wrasta = source.getRaster();
		int width = mask.getWidth();
		int height = mask.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int p2 = r2.getSample(i,j,0);
				if (p2==1) wrasta.setSample(i,j,0,1);
			}
		}		
	}
	
	public static void and(BufferedImage source, BufferedImage mask) {
		Raster r2 = mask.getData();
		WritableRaster wrasta = source.getRaster();
		int width = source.getWidth();
		int height = source.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int p2 = r2.getSample(i,j,0);
				if (p2==0) wrasta.setSample(i,j,0,0);
			}
		}		
	}
	
	public static BufferedImage gradient(BufferedImage source) {
		return difference(source, erode(source));
	}
	
	public static BufferedImage gradient(BufferedImage source, int index) {
		BufferedImage binary = equal(source, index);
		return difference(binary, erode(binary));		
	}
	public static BufferedImage gradient(BufferedImage source, int index, Dimension dim) {
		BufferedImage binary = equal(source, index, dim);
		return difference(binary, erode(binary));	
	}
	public static BufferedImage gradient(BufferedImage source, int[] inds, Dimension dim) {
		BufferedImage binary = equal(source, inds, dim);
		return difference(binary, erode(binary));	
	}
	
	
	/**
	  * @return An array the same size of the source image such that
	  *         = -1 if background
	  *         = 0 if contour
	  *         = 1 if object
	  */
	public static byte[][] getGradient(BufferedImage source) {
		BufferedImage contour = difference(source, erode(source));
		Raster r1 = source.getData();
		Raster r2 = contour.getData();
		int width = contour.getWidth();
		int height = contour.getHeight();
		byte[][] out = new byte[width][height];
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int p1 = r1.getSample(i,j,0);				
				int p2 = r2.getSample(i,j,0);
				out[i][j]=(p1==0?(byte)-1:(p2==0?(byte)1:(byte)0));
			}
		}		
		
		return out;
	}


	/** Gets the bounding box of an object, where the object is anything
	 *  non zero valued.
	 *  In case the image is blank, it returns a bounding box of the whole image.
	 * @param source The image containing the object
	 * @return a Rectangle object specifying the bounding box of the object.
	 */
	public static Rectangle getBB(BufferedImage source) {
		
		Raster rasta = source.getData();
		int mx = rasta.getMinX();
		int my = rasta.getMinY();
		int width = rasta.getWidth();
		int height = rasta.getHeight();
		
		int minx=width-1, miny=height-1, maxx=0, maxy=0;
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int pixel = rasta.getSample(i+mx,j+my,0);
				if (pixel!=0) {
					if (i<minx) minx=i;
					if (i>maxx) maxx=i;
					if (j<miny) miny=j;
					if (j>maxy) maxy=j;
				}
			}
		}
		if (maxx-minx+1<0 || maxy-miny+1<0) { // no object! grab all
			minx = 0; miny=0; maxx=width-1; maxy=height-1;
		}
		
		return new Rectangle(minx,miny,maxx-minx+1,maxy-miny+1);		
	}
	
	public static BufferedImage fill4(BufferedImage source, int ox, int oy, int color) {
		int target = (ox<0||oy<0)?0xFF000000:source.getRGB(ox,oy);
		return fill4(source,ox,oy,color,target);
	}

	/** 
	  * Flood-fills the source image, starting from point (ox,oy) with the desired color.
	  * The original image is not modified, but a new ARGB image is created instead.
	  * 4-connected components.
	  * The rest of the output image will be transparent.
	  * You can flood fill from (-1,-1) to fill the background.
	  */
	public static BufferedImage fill4(BufferedImage source, int ox, int oy, int color, int target) {
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		boolean[][] added = new boolean[width+1][height+1]; // false by default
		
		Vector<Point> q = new Vector<Point>(); // just a Stack
		q.add(new Point(ox,oy));
		added[ox+1][oy+1]=true;
		while (!q.isEmpty()) {
			Point p = (Point)q.remove(0);
			if (p.x>=0 && p.y>=0) out.setRGB(p.x,p.y,color);
			if (p.x>0 && p.y>=0) {
				if (source.getRGB(p.x-1,p.y)==target && out.getRGB(p.x-1,p.y)!=color) {
					Point np = new Point(p.x-1,p.y);
					//if (!q.contains(np)) q.add(np); //slow!
					if (!added[p.x][p.y+1]) {
						q.add(np);
						added[p.x][p.y+1]=true;
					}
				}	
			}
			if (p.x<width-1) {
				if (p.y>=0) {
					if (source.getRGB(p.x+1,p.y)==target && out.getRGB(p.x+1,p.y)!=color) {
						Point np = new Point(p.x+1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+2][p.y+1]) {
							q.add(np);
							added[p.x+2][p.y+1]=true;
						}
					}
				} else {
					Point np = new Point(p.x+1,p.y);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+2][p.y+1]) {
						q.add(np);
						added[p.x+2][p.y+1]=true;
					}
				}
			}
		
			if (p.y>0 && p.x>=0) {
				if (source.getRGB(p.x,p.y-1)==target && out.getRGB(p.x,p.y-1)!=color) {
					Point np = new Point(p.x,p.y-1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y]) {
						q.add(np);
						added[p.x+1][p.y]=true;
					}
				}	
			} 
			if (p.y<height-1) {
				if (p.x>=0) {
					if (source.getRGB(p.x,p.y+1)==target && out.getRGB(p.x,p.y+1)!=color) {
						Point np = new Point(p.x,p.y+1);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+1][p.y+2]) {
							q.add(np);
							added[p.x+1][p.y+2]=true;
						}
					}			
				} else {
					Point np = new Point(p.x,p.y+1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y+2]) {
						q.add(np);
						added[p.x+1][p.y+2]=true;
					}
				}
			} 
		}
		
		return out;
	}
	
	/** Applies flood fill to all the elements outside the mask */
	public static BufferedImage fill4(BufferedImage source, BufferedImage mask) {
		Raster mrasta = mask.getData();
		Raster srasta = source.getData();
		
		int width = source.getWidth();
		int height = source.getHeight();
		// just makes a copy
		// initially, all is white, and then we will fill with 0 some parts
		BufferedImage out = new BufferedImage(width, height, 
											 BufferedImage.TYPE_BYTE_BINARY);
		Graphics gi = out.getGraphics();
		gi.setColor(Color.WHITE);
		gi.fillRect(0,0,width,height);
		gi.dispose();
		
		WritableRaster wrasta = out.getRaster();
		
		boolean[][] added = new boolean[width+1][height+1]; // false by default
		Vector<Point> q = new Vector<Point>();
		TreeSet<Integer> inds = new TreeSet<Integer>();
		q.add(new Point(-1,-1));
		added[0][0]=true;
		while (!q.isEmpty()) {
			Point p = (Point)q.remove(0);
			if (p.x>=0 && p.y>=0) wrasta.setSample(p.x,p.y,0,0);
			if (p.x>0 && p.y>=0) {
				int index = srasta.getSample(p.x-1,p.y,0);
				if (wrasta.getSample(p.x-1,p.y,0)!=0) {
					if (mrasta.getSample(p.x-1,p.y,0)==0) {
						Point np = new Point(p.x-1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x][p.y+1]) {
							q.add(np);
							added[p.x][p.y+1]=true;
						}
						if (!inds.contains(index)) inds.add(index);
					} else if (inds.contains(index)) {
						Point np = new Point(p.x-1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x][p.y+1]) {
							q.add(np);
							added[p.x][p.y+1]=true;
						}
					}
				}
			}
			if (p.x<width-1) {
				if (p.y>=0) {
					int index = srasta.getSample(p.x+1,p.y,0);
					if (wrasta.getSample(p.x+1,p.y,0)!=0) {

					if (mrasta.getSample(p.x+1,p.y,0)==0) {
						Point np = new Point(p.x+1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+2][p.y+1]) {
							q.add(np);
							added[p.x+2][p.y+1]=true;
						}
						if (!inds.contains(index)) inds.add(index);
					} else if (inds.contains(index)) {
						Point np = new Point(p.x+1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+2][p.y+1]) {
							q.add(np);
							added[p.x+2][p.y+1]=true;
						}
					}}
				} else {
					Point np = new Point(p.x+1,p.y);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+2][p.y+1]) {
						q.add(np);
						added[p.x+2][p.y+1]=true;
					}
				}
			}
			
			if (p.y>0 && p.x>=0) {
				int index = srasta.getSample(p.x,p.y-1,0);
				if (wrasta.getSample(p.x,p.y-1,0)!=0) {

				if (mrasta.getSample(p.x,p.y-1,0)==0) {
					Point np = new Point(p.x,p.y-1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y]) {
						q.add(np);
						added[p.x+1][p.y]=true;
					}
					if (!inds.contains(index)) inds.add(index);
				} else if (inds.contains(index)) {
					Point np = new Point(p.x,p.y-1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y]) {
						q.add(np);
						added[p.x+1][p.y]=true;
					}
				}}
			} 
			if (p.y<height-1) {
				if (p.x>=0) {
					int index = srasta.getSample(p.x,p.y+1,0);
					if (wrasta.getSample(p.x,p.y+1,0)!=0) {

					if (mrasta.getSample(p.x,p.y+1,0)==0) {
						Point np = new Point(p.x,p.y+1);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+1][p.y+2]) {
							q.add(np);
							added[p.x+1][p.y+2]=true;
						}
						if (!inds.contains(index)) inds.add(index);
					} else if (inds.contains(index)) {
						Point np = new Point(p.x,p.y+1);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+1][p.y+2]) {
							q.add(np);
							added[p.x+1][p.y+2]=true;
						}
					}}
				} else {
					Point np = new Point(p.x,p.y+1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y+2]) {
						q.add(np);
						added[p.x+1][p.y+2]=true;
					}
				}
			} 
		}
		
		return out;
				
	} 

	
	/** Applies flood fill to all the elements outside the mask */
	public static BufferedImage fill4(int[][] source, BufferedImage mask) {
		Raster mrasta = mask.getData();
		
		int width = source.length;
		int height = source[0].length;
		
		// initially, all is white, and then we will fill with 0 some parts
		BufferedImage out = new BufferedImage(width, height, 
											  BufferedImage.TYPE_BYTE_BINARY);
		Graphics gi = out.getGraphics();
		gi.setColor(Color.WHITE);
		gi.fillRect(0,0,width,height);
		gi.dispose();
		
		WritableRaster wrasta = out.getRaster();
		
		boolean[][] added = new boolean[width+1][height+1]; // false by default
		Vector<Point> q = new Vector<Point>();
		TreeSet<Integer> inds = new TreeSet<Integer>();
		q.add(new Point(-1,-1));
		added[0][0]=true;
		while (!q.isEmpty()) {
			Point p = (Point)q.remove(0);
			if (p.x>=0 && p.y>=0) wrasta.setSample(p.x,p.y,0,0);
			if (p.x>0 && p.y>=0) {
				int index = source[p.x-1][p.y];
				if (wrasta.getSample(p.x-1,p.y,0)!=0) {
					if (mrasta.getSample(p.x-1,p.y,0)==0) {
						Point np = new Point(p.x-1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x][p.y+1]) {
							q.add(np);
							added[p.x][p.y+1]=true;
						}
						if (!inds.contains(index)) inds.add(index);
					} else if (inds.contains(index)) {
						Point np = new Point(p.x-1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x][p.y+1]) {
							q.add(np);
							added[p.x][p.y+1]=true;
						}
					}
				}
			}
			if (p.x<width-1) {
				if (p.y>=0) {
					int index = source[p.x+1][p.y];
					if (wrasta.getSample(p.x+1,p.y,0)!=0) {
						
						if (mrasta.getSample(p.x+1,p.y,0)==0) {
							Point np = new Point(p.x+1,p.y);
							//if (!q.contains(np)) q.add(np);
							if (!added[p.x+2][p.y+1]) {
								q.add(np);
								added[p.x+2][p.y+1]=true;
							}
							if (!inds.contains(index)) inds.add(index);
						} else if (inds.contains(index)) {
							Point np = new Point(p.x+1,p.y);
							//if (!q.contains(np)) q.add(np);
							if (!added[p.x+2][p.y+1]) {
								q.add(np);
								added[p.x+2][p.y+1]=true;
							}
						}}
				} else {
					Point np = new Point(p.x+1,p.y);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+2][p.y+1]) {
						q.add(np);
						added[p.x+2][p.y+1]=true;
					}
				}
			}
			
			if (p.y>0 && p.x>=0) {
				int index = source[p.x][p.y];
				if (wrasta.getSample(p.x,p.y-1,0)!=0) {
					
					if (mrasta.getSample(p.x,p.y-1,0)==0) {
						Point np = new Point(p.x,p.y-1);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+1][p.y]) {
							q.add(np);
							added[p.x+1][p.y]=true;
						}
						if (!inds.contains(index)) inds.add(index);
					} else if (inds.contains(index)) {
						Point np = new Point(p.x,p.y-1);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+1][p.y]) {
							q.add(np);
							added[p.x+1][p.y]=true;
						}
					}}
			} 
			if (p.y<height-1) {
				if (p.x>=0) {
					int index = source[p.x][p.y+1];
					if (wrasta.getSample(p.x,p.y+1,0)!=0) {
						
						if (mrasta.getSample(p.x,p.y+1,0)==0) {
							Point np = new Point(p.x,p.y+1);
							//if (!q.contains(np)) q.add(np);
							if (!added[p.x+1][p.y+2]) {
								q.add(np);
								added[p.x+1][p.y+2]=true;
							}
							if (!inds.contains(index)) inds.add(index);
						} else if (inds.contains(index)) {
							Point np = new Point(p.x,p.y+1);
							//if (!q.contains(np)) q.add(np);
							if (!added[p.x+1][p.y+2]) {
								q.add(np);
								added[p.x+1][p.y+2]=true;
							}
						}}
				} else {
					Point np = new Point(p.x,p.y+1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y+2]) {
						q.add(np);
						added[p.x+1][p.y+2]=true;
					}
				}
			} 
		}
		
		return out;
		
	} 
	
	
	/** This modifies the input binary image */
	public static void floodfill4(BufferedImage binary, int ox, int oy) {
		WritableRaster wrasta = binary.getRaster();
		int width = wrasta.getWidth();
		int height = wrasta.getHeight();
		
		boolean[][] added = new boolean[width+1][height+1]; // false by default
		Vector<Point> q = new Vector<Point>();
		q.add(new Point(ox,oy));
		added[ox+1][oy+1]=true;
		while (!q.isEmpty()) {
			Point p = (Point)q.remove(0);
			if (p.x>=0 && p.y>=0) wrasta.setSample(p.x,p.y,0,1);
			if (p.x>0 && p.y>=0) 
				if (wrasta.getSample(p.x-1,p.y,0)==0) {
					Point np = new Point(p.x-1,p.y);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x][p.y+1]) {
						q.add(np);
						added[p.x][p.y+1]=true;
					}
				} 
			if (p.x<width-1)
				if (p.y>=0) {
					if (wrasta.getSample(p.x+1,p.y,0)==0) {
						Point np = new Point(p.x+1,p.y);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+2][p.y+1]) {
							q.add(np);
							added[p.x+2][p.y+1]=true;
						}
					}
				} else {
					Point np = new Point(p.x+1,p.y);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+2][p.y+1]) {
						q.add(np);
						added[p.x+2][p.y+1]=true;
					}
				}
			if (p.y>0 && p.x>=0) 
				if (wrasta.getSample(p.x,p.y-1,0)==0) {
					Point np = new Point(p.x,p.y-1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y]) {
						q.add(np);
						added[p.x+1][p.y]=true;
					}
				} 
			if (p.y<height-1)
				if (p.x>=0) {
					if (wrasta.getSample(p.x,p.y+1,0)==0) {
						Point np = new Point(p.x,p.y+1);
						//if (!q.contains(np)) q.add(np);
						if (!added[p.x+1][p.y+2]) {
							q.add(np);
							added[p.x+1][p.y+2]=true;
						}
					}
				} else {
					Point np = new Point(p.x,p.y+1);
					//if (!q.contains(np)) q.add(np);
					if (!added[p.x+1][p.y+2]) {
						q.add(np);
						added[p.x+1][p.y+2]=true;
					}
				}
			} // end while
						
	} // end floodfill4
	
	/** NOT for a binary image */
	public static void not(BufferedImage binary) {
		WritableRaster wrasta = binary.getRaster();
		int width = binary.getWidth();
		int height = binary.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int v = wrasta.getSample(i,j,0);
				wrasta.setSample(i,j,0,v==0?1:0);
			}
		}
		
	}
			
	/** Swaps bgcolor <-> fgcolor */
	public static void not(BufferedImage rgb, int bgcolor, int fgcolor) {
		int width = rgb.getWidth();
		int height = rgb.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int c = rgb.getRGB(i,j);
				if (c==bgcolor) rgb.setRGB(i,j,fgcolor);
				else rgb.setRGB(i,j,bgcolor);
			}
		}		
	}
			
	
	/** 
	* Flood-fills the source image, starting from point (ox,oy) with the desired color.
	* The original image is not modified, but a new ARGB image is created instead.
	* 8-connected components.
	* The rest of the output image will be transparent.
	*/
	public static BufferedImage fill8(BufferedImage source, int ox, int oy, int color) {		
		int width = source.getWidth();
		int height = source.getHeight();
		int target = source.getRGB(ox,oy);
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		Vector<Point> q = new Vector<Point>();
		q.add(new Point(ox,oy));
		while (!q.isEmpty()) {
			Point p = (Point)q.remove(0);
			out.setRGB(p.x,p.y,color);
			if (p.x>0) 
				if (source.getRGB(p.x-1,p.y)==target && out.getRGB(p.x-1,p.y)!=color) {
					Point np = new Point(p.x-1,p.y);
					if (!q.contains(np)) q.add(np);
				} 
			if (p.x<width-1) 
				if (source.getRGB(p.x+1,p.y)==target && out.getRGB(p.x+1,p.y)!=color) {
					Point np = new Point(p.x+1,p.y);
					if (!q.contains(np)) q.add(np);
				} 
			if (p.y>0) 
				if (source.getRGB(p.x,p.y-1)==target && out.getRGB(p.x,p.y-1)!=color) {
					Point np = new Point(p.x,p.y-1);
					if (!q.contains(np)) q.add(np);
				} 
			if (p.y<height-1) 
				if (source.getRGB(p.x,p.y+1)==target && out.getRGB(p.x,p.y+1)!=color) {
					Point np = new Point(p.x,p.y+1);
					if (!q.contains(np)) q.add(np);
				} 
			if (p.x>0 && p.y>0) 
				if (source.getRGB(p.x-1,p.y-1)==target && out.getRGB(p.x-1,p.y-1)!=color){
					Point np = new Point(p.x-1,p.y-1);
					if (!q.contains(np)) q.add(np);
				}  
			if (p.x<width-1 && p.y>0)
				if (source.getRGB(p.x+1,p.y-1)==target && out.getRGB(p.x+1,p.y-1)!=color){
					Point np = new Point(p.x+1,p.y-1);
					if (!q.contains(np)) q.add(np);
				} 
			if (p.y<height-1 && p.x>0) 
				if (source.getRGB(p.x-1,p.y+1)==target && out.getRGB(p.x-1,p.y+1)!=color) {
					Point np = new Point(p.x-1,p.y+1);
					if (!q.contains(np)) q.add(np);
				} 
			if (p.y<height-1 && p.x<width-1) 
				if (source.getRGB(p.x+1,p.y+1)==target && out.getRGB(p.x+1,p.y+1)!=color){
					Point np = new Point(p.x+1,p.y+1);
					if (!q.contains(np)) q.add(np);
				} 
		}
		
		return out;
	}

		
	/** Makes a list of points such that index == 1 */	
	public static Vector<Point> listPoints(BufferedImage binary) {
		Vector<Point> v = new Vector<Point>();

		Raster r = binary.getRaster();
		int width = binary.getWidth();
		int height = binary.getHeight();
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int value = r.getSample(i,j,0);
				if (value==1) v.add(new Point(i,j));
			}
		}
		
		return v;
	}
		
	/** Makes a list of points such that value == rgb */	
	public static Vector<Point> listPoints(BufferedImage source, int rgb) {
		Vector<Point> v = new Vector<Point>();
		
		for (int j=0;j<source.getHeight();j++) {
			for (int i=0;i<source.getWidth();i++) {
				int value = source.getRGB(i,j);
				if (value == rgb) v.add(new Point(i,j));
			}
		}
		
		return v;
	}

	/** Makes a list of points such that value != rgb */	
	public static Vector<Point> listPointsNot(BufferedImage source, int rgb) {
		Vector<Point> v = new Vector<Point>();
			
		for (int j=0;j<source.getHeight();j++) {
			for (int i=0;i<source.getWidth();i++) {
				int value = source.getRGB(i,j);
				if (value != rgb) v.add(new Point(i,j));
			}
		}
			
		return v;
			
	}

		
	/** The greatest index in an indexed image */
	public static int maxValue(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		int max = 0;
		
		Raster r = img.getData();
		
		for (int j=0;j<h;j++) {
			for (int i=0;i<w;i++) {
				int v = r.getSample(i,j,0);
				if (v>max) max=v;
			}
		}
		
		return max;
	}
		
	public static BufferedImage maxPercentil(BufferedImage img, int size) {
		return maxPercentil(maxValue(img)+1, img, size);	
	}
		
	/**
	 *  An approximation of the Occlusion N-Sieve using 
	 *  Statistics Order Filtering.
	 *
	 * @param  n		Layers or number of colors of the indexed palette.
	 * @param  image    An indexed image.
	 * @param  size     The size of the median filter
	 * @return          An indexed image that has been filtered.
	 */	
	public static BufferedImage maxPercentil(int n, BufferedImage img, int size) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage result = new BufferedImage(w, h,
											  BufferedImage.TYPE_BYTE_INDEXED,
											  (IndexColorModel)img.getColorModel());
		Raster r = img.getData();
		WritableRaster wr = result.getRaster();
		int hf = size >> 1;
		int[] votes = new int[n];
		for (int j=0;j<h;j++) {
			for (int i=0;i<w;i++) {
				for (int v=0;v<votes.length;v++) votes[v]=0;
				for (int k=Math.max(0,i-hf);k<=Math.min(i+hf,w-1);k++) {
					for (int l=Math.max(0,j-hf);l<=Math.min(j+hf,h-1);l++) {
						int sample = r.getSample(k,l,0);
						// vote
						if (sample>=0 && sample<n) votes[sample]++;
					}
				}
				// select most voted
				wr.setSample(i,j,0,AMath.indexMax(votes));
			}
		}
		
		return result;
	}
	
		
	public static BufferedImage connectRegions(BufferedImage binary) {
		ColorLabel cl = new ColorLabel(binary);
		ObjectImage obi = new ObjectImage(cl.getLabeledMatrix(), 
										  cl.getNLabels(), binary);
		BufferedImage out = new BufferedImage(binary.getWidth(), binary.getHeight(), 
											  BufferedImage.TYPE_BYTE_BINARY);
		Graphics gi = out.getGraphics();
		gi.setColor(Color.WHITE);
		
		int w = out.getWidth();
		int h = out.getHeight();
		
		Vector regions = obi.getRegions();
		//System.out.println("Regions: "+regions.size());

		if (regions.size()==0) return out;
		
		// first element is the background, remove it
		regions.remove(regions.get(0));

		if (regions.size()==0) return out;

		Region a = (Region)regions.get(0);
		regions.remove(a);
		while (regions.size()>0) {
			Region b = a.closest(regions);
			int ax = (int) (a.cx*w);
			int ay = (int) (a.cy*h);
			int bx = (int) (b.cx*w);
			int by = (int) (b.cy*h);
			
			gi.drawLine(ax, ay, bx, by);
			a = (Region)regions.get(0);
			regions.remove(a);
		}
		gi.dispose();
		
		// when optimizing with 8-neighbors, a single line allows flooding!
		// dilate the lines!
		out = dilate(out);
		or(out, binary);
	
		return out;
	}
		
	public static BufferedImage discardRegions(BufferedImage binary, double minSize) {
		//ColorLabel cl = new ColorLabel(binary, 0x000000);
		//ObjectImage obi = new ObjectImage(cl.getLabeledMatrix(), 
		//								  cl.getNLabels(), binary, minSize);

		ObjectImage obi = new ObjectImage();
		obi.setRegions(binary);
		return bw2binary(obi.getLabeledImage());
		
		/*
		Vector regions = obi.getRegions();
		TreeSet acceptable = new TreeSet();
		for (int i=0;i<regions.size();i++) {
			Region r = (Region)regions.get(i);
			// background does not exist, so i+1 is the label
			if (r.vol>minSize) {
				acceptable.add(i+1);
				//System.out.println("vol: "+r.vol);
			}
		}
		
		
		return equal(cl.getLabeledMatrix(), acceptable);
		 */
	}
		
	public static BufferedImage equal(int[][] labels, TreeSet acceptable) {
		int w = labels.length;
		int h = labels[0].length;
		BufferedImage out = new BufferedImage(w, h, 
											  BufferedImage.TYPE_BYTE_BINARY);
		
		WritableRaster wrasta = out.getRaster();
		
		for (int j=0;j<h;j++) {
			for (int i=0;i<w;i++) {
				int v = labels[i][j];
				if (acceptable.contains(v)) wrasta.setSample(i,j,0,1);
			}
		}
		
		return out;
		
	}
	/** Counts the number of pixels==index in an indexed image */
	public static int objSize(BufferedImage source, int index) {
		Raster rasta = source.getData();
		int width = source.getWidth();
		int height = source.getHeight();
		int out = 0;
		
		for (int j=0;j<height;j++) {
			for (int i=0;i<width;i++) {
				int pixel = rasta.getSample(i,j,0);
				if (pixel == index) out++;
			}
		}
		
		return out;
	}
		
	/** Counts the number of white pixels in a binary image */	
	public static int objSize(BufferedImage source) {
		return objSize(source, 1);
	}
	
		
	
}
