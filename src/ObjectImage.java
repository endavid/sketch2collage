import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import titech.file.*;
import titech.image.math.*;

/**
 * This class differs from the one at blobby. It contains several Region objects that 
 * can be added incrementally.
 */
public class ObjectImage {
	public static final int NFEATURES=15;
	public static final int RHISTO_SIZE=22;
	public static final int CHISTO_SIZE=52;
	
	Vector regions;
	/** The contour count */
	double contourCount = -1;
	
	/** Mean pairwise distance (position, volume, color, orientation) */
	double[] pwdistances = null;

	/** In case you know where the original images is */
	String location;
	/** Some keywords associated with the image (useful for the database) */
	String keywords;
	/** Histogram. See getHistogram() */
	double[] histogram = null;

	/** Weights of each attribute (position, volume, color, orientation) */
	double[] weights = new double[] {0.25, 0.25, 0.25, 0.25};
	
	/** Holds current minimum distance in Retrieval */
	double minDist;
	
	private int[] equivalences = null;
	private int nlabels;
	private float[][] labmap;
	
	public ObjectImage() {
		regions = new Vector();
		histogram = new double[RHISTO_SIZE+CHISTO_SIZE];
		
		try {
			InputStream stream = this.getClass().getResourceAsStream("/resources/histogram.palette");
			labmap = Utilities.loadPaletteLab(stream);
		} catch (Exception ex) {
			System.err.println("ObjectImage: "+ex);
		}
	}

	/**
		* A string representing an ObjectImage is of the form:
	 * 
	 * n cc pw[0] pw[1] pw[2] pw[3] cats[0] ... cats[n-2] feats[0][0] .. feats[0][13] ..
	 */
	public ObjectImage(String os) {
		regions = new Vector();
		
		StringTokenizer stok = new StringTokenizer(os);
		try {
			int nregions = Integer.parseInt(stok.nextToken());
			contourCount = Double.parseDouble(stok.nextToken());
			pwdistances = new double[4];
			for (int k=0;k<4;k++) pwdistances[k]=Double.parseDouble(stok.nextToken());
			
			// region 0 is the background
			for (int i=1;i<nregions;i++) {
				Region region = new Region();
				region.setColorCat(Integer.parseInt(stok.nextToken()));
				regions.add(region);
			}
			
			double[] features = new double[NFEATURES];
			for (int i=1;i<nregions;i++) {
				for (int f=0;f<NFEATURES;f++) {
					features[f]=Double.parseDouble(stok.nextToken());
				}
				Region region = (Region)regions.get(i-1);
				region.setFeatures(features);
			}
		} catch (Exception e) {
			System.err.println("ObjectImage: "+e);
		}
	}

	public void setLocation(String location) {
		this.location = location;	
	}
	
	public String getLocation() {
		return location;
	}
	
	public String getBThumbLocation() {
		return thumbFromLocation(location);
	}
	
	public static String thumbFromLocation(String location) {
		String path = location.substring(0,location.lastIndexOf("/"));
		String filename=location.substring(location.lastIndexOf("/")+1);
		return path+File.separator+Retrieval.THUMBSDIR+
			File.separator+FileUtils.nameWOExtension(filename)+".png";		
	}
	
	/**
	 * This adds a new region from an image assumed to have only one region of
	 * the given color
	 *
	 */
	public Region addRegion(BufferedImage img, Color stroke) {
		
		// count white pixels (black are background)
		Region region = calculateFeatures(img);
		region.setColor(stroke);

		
		regions.add(region);
		//System.out.println("Region added: "+region);
		
		return region;
	}
	
	public Region getRegion(int index) {
		return (Region)regions.get(index);
	}
	
	/** Empty the regions vector */
	public void clear() {
		regions.clear();
	}
	
	/** Assumes the image contains just one object on a black background
	  * Add a parameter, the color of the region, and call it for
	  * every color in setRegions(BufferedImage)
	  */
	public Region calculateFeatures(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();

		int n=0; // number of object pixels
		double vol=.0; // normalized volume
		// volume
		int minx = width;
		int maxx = 0;
		int miny = height;
		int maxy = 0;
		int momentX = 0;
		int momentY = 0;
		double momentXX=.0;
		double momentXY=.0;
		double momentYY=.0;
		double centerX=.0;
		double centerY=.0;
		double relativeVol=.0; // volume relative to its BB
		double orientation=.0;
		double elongation=.0; 
		

		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				int r = img.getRGB(i, j) & 0x00FFFFFF;
				if (r!=0) { 
					n++; // count all non-black pixels
					if (i < minx) {
						minx = i;
					}
					if (i > maxx) {
						maxx = i;
					}
					if (j < miny) {
						miny = j;
					}
					if (j > maxy) {
						maxy = j;
					}
					momentX += i;
					momentY += j;
				} 
			} 
		}

		if (n > 0) {
			// mass centre
			centerX = (double) momentX / (double) n;
			centerY = (double) momentY / (double) n;

			// calculate 2nd momentums inside bounding box
			for (int j = miny; j < maxy; j++) {
				for (int i = minx; i < maxx; i++) {
					int r = img.getRGB(i, j)& 0x00FFFFFF;
					if (r != 0) {
						double pX = (double) i - centerX;
						double pY = (double) j - centerY;
						momentXX += pX * pX;
						momentXY += pX * pY;
						momentYY += pY * pY;
					}
				}
			}

			momentXX /= n;
			momentXY *= 2./n;
			momentYY /= n;
			orientation = Math.atan(
				(momentYY-momentXY)/(momentXX-momentXY));
			if (momentYY<momentXY) orientation=Math.abs(orientation)-Math.PI/2;

			// a simple measure for elongation
			// multiply the angular distance by this 
			elongation=(Math.abs(momentXX-momentYY)+Math.abs(momentXY))/
						AMath.max(momentXX,Math.abs(momentXY),momentYY);
			elongation=Math.min(elongation,1.);


			//normalize 0..1	
			int w = maxx - minx + 1;
			int h = maxy - miny + 1;
				
			if (w * h > 0) {
				relativeVol = (double) n / (double) (w * h);
			}
		
       
			// normalized characteristics 0..1
			vol = (double) n / (double) (width * height);
    		
			//feat.color = feat.color / 255;
			// mapped to a 1x1 image
			centerX /= (double) width;
			centerY /= (double) height;
		}

		Region region = new Region();
		region.setCenter(centerX, centerY);
		region.setVolume(vol, relativeVol);
		region.setOrientation(orientation);
		region.setElongation(elongation);
		
		return region;
	}

	public void setRegions(BufferedImage bim) {
		setRegions(colorLabel(bim), bim);
	}
	
	public void setRegions(int[][] img, BufferedImage bim) {
		clear(); // empty the Regions vector		
		
		int width = img.length;
		int height = img[0].length;
		
		int[] n = new int[nlabels];
		// numero de punts per regio
		double[] vol = new double[nlabels];
		// volume
		int[] minx = new int[nlabels];
		for (int i = 0; i < nlabels; i++) {
			minx[i] = width;
		}
		int[] maxx = new int[nlabels];
		// ini 0
		int[] miny = new int[nlabels];
		for (int i = 0; i < nlabels; i++) {
			miny[i] = height;
		}
		int[] maxy = new int[nlabels];
		// ini 0
		int[] momentX = new int[nlabels];
		int[] momentY = new int[nlabels];
		double[] momentXX=new double[nlabels];
		double[] momentYY=new double[nlabels];
		double[] momentXY=new double[nlabels];
		double[] centerX = new double[nlabels];
		double[] centerY = new double[nlabels];
		double[] relativeVol=new double[nlabels]; // volume relative to its BB
		double[] orientation=new double[nlabels];
		double[] elongation=new double[nlabels]; 
		// no need to compute these because there is only one color per region
		// in the sketch -- use Region.setColor(RGB)
		// It will be stored in L*a*b* as the mean
		//int[] meanColor = new int[nlabels][3];
		//double[] deviation = new int[nlabels][3];
		//double[] skewness = new int[nlabels][3];
		int[] color=new int[nlabels];
		
		
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				// get index or color
				int r = img[i][j];
				n[r]++; // we count even the pixels of the background
						//System.err.println("i "+i+" j "+j+" r "+r);
				if (r > 0 && r < nlabels) {
					// if r>= regions it means it has badly labeled!
					// 0 is the background (unknown regions)
					// although there is no background region in our colorlabel
					if (i < minx[r]) {
						minx[r] = i;
					}
					if (i > maxx[r]) {
						maxx[r] = i;
					}
					if (j < miny[r]) {
						miny[r] = j;
					}
					if (j > maxy[r]) {
						maxy[r] = j;
					}
					momentX[r] += i;
					momentY[r] += j;
					
					color[r] = bim.getRGB(i,j);

				}
			}
		}
		
		
		for (int r = 1; r < nlabels; r++) {
			if (n[r] > 0) {
				// mass centre
				centerX[r] =  (double)momentX[r] /  (double)n[r];
				centerY[r] =  (double)momentY[r] /  (double)n[r];
								
				// calculate 2nd momentums inside bounding box
				for (int j = miny[r]; j < maxy[r]; j++) {
					for (int i = minx[r]; i < maxx[r]; i++) {
						if (img[i][j] == r) {
							double pX = (double) i - centerX[r];
							double pY = (double) j - centerY[r];
							momentXX[r] += pX * pX;
							momentXY[r] += pX * pY;
							momentYY[r] += pY * pY;
						}
					}
				}
				
				momentXX[r] /= (double)n[r];
				momentXY[r] *= 2./(double)n[r];
				momentYY[r] /= (double)n[r];
				orientation[r] = Math.atan(
										(momentYY[r]-momentXY[r])/(momentXX[r]-momentXY[r]));
				if (momentYY[r]<momentXY[r]) orientation[r]=Math.abs(orientation[r])-Math.PI/2;
				
				// a simple measure for elongation
				// multiply the angular distance by this 
				elongation[r]=(Math.abs(momentXX[r]-momentYY[r])+Math.abs(momentXY[r]))/
					AMath.max(momentXX[r],Math.abs(momentXY[r]),momentYY[r]);
				elongation[r]=Math.min(elongation[r],1.);
				
				
				//normalize 0..1	
				int w = maxx[r]- minx[r] + 1;
				int h = maxy[r] - miny[r] + 1;
				
				if (w * h > 0) {
					relativeVol[r] = (double) n[r] / (double) (w * h);
				}
				
				
				// normalized characteristics 0..1
				vol[r] = (double) n[r] / (double) (width * height);
				
				//feat.color = feat.color / 255;
				// mapped to a 1x1 image
				centerX[r] /= (double) width;
				centerY[r] /= (double) height;
				
				
				// create the region object
				Region region = new Region();
				region.setCenter(centerX[r], centerY[r]);
				region.setVolume(vol[r], relativeVol[r]);
				region.setOrientation(orientation[r]);
				region.setElongation(elongation[r]);
				region.setColor(color[r]);
				regions.add(region);
				
				
			} // endif n[r]>0
		} // for all regions
		
		
		// compute the color histogram now
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		computeColorHistogram(bim,labmap);
		
		// compute also the region histogram
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		computeRegionHistogram();
	}
	
	/** Instead of returning a Region, it returns an index to the Region Vector */
	public int minDistanceRegion(Region query) {
		minDist = Double.MAX_VALUE;
		int minR = 0;
		for (int i=0;i<regions.size();i++) {
			Region r = (Region)regions.get(i);
			double d = r.distance(query, weights, null);
			if (d < minDist) {
				minR = i;
				minDist = d;
			}
		}
		return minR;
	}
	
	public void setWeights(double position, double volume, double color, double orientation) {
		weights[0]=position;
		weights[1]=volume;
		weights[2]=color;
		weights[3]=orientation;
	} 
	
	/** This value is initialized after a call to minDistanceRegion */
	public double getMinDist() {
		return minDist;
	}
	
	
	/**
	 * Labels Color regions in a RGB image to an indexed image.
	 * <p>
	 * The labeling of a pixed <code>p</code>, which <code>index(p)</code> is not 0 (background color index) occurs as follows:
	 * <ul>
	 * <li>If all four neighbors are different from <code>index(p)</code>, assign a new label to <code>p</code>, else
	 * <li>if only one neighbor has the same color, assign its label to <code>p</code>, else
	 * <li>if one or more of the neighbors have the same color, assign one of the labels to <code>p</code> and make a note of the equivalences.
	 *</ul>
	 * <p>
	 * The original algorithm for binary images can be found in: <a href="http://www.dai.ed.ac.uk/HIPR2/label.htm">Connected Components Algorithm</a>
	 *
	 * @param  img  Input image, supposed to be indexed. Otherwise, only red channel is used.
	 * @return      dst.
	 */
	private int[][] colorLabel(BufferedImage src) {
		int width = src.getWidth();
		int height = src.getHeight();
		
		
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
		
		// the first label
		int label = 1;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = src.getRGB(x,y);
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
	
	
	public double[] getHistogram() {
		return histogram;
	}
	
	private void computeRegionHistogram() {
		
		for (int i=0;i<RHISTO_SIZE;i++) histogram[i]=0;
		// reference objects
		double[][] refPos = new double[][] { // 9 central points of a 3 x 3 square
			new double[] { 1./6., 1./6. },
			new double[] { 1./2., 1./6. },
			new double[] { 5./6., 1./6. },
			new double[] { 1./6., 1./2. },
			new double[] { 1./2., 1./2. },
			new double[] { 5./6., 1./2. },
			new double[] { 1./6., 5./6. },
			new double[] { 1./2., 5./6. },
			new double[] { 5./6., 5./6. },
		};			
		
		double[] refVols = new double[] {
			//	4./5., 3./5., 2./5., 1./5., 1./20.};
			0.60, 0.40, 0.20, 0.10, 0.05};
		
		double refRVol = 0.5; // <=0.5 or >0.5
		
		double[] refAngles = new double[] { // shapes: -, |, o
			0, Math.PI/2., Math.PI/4.};
		
		double[] refElongs = new double[] {
			1., 1., 0.};
		
		int nregions = regions.size();
		for (int r=0;r<nregions;r++) { 
			Region rg = (Region)regions.get(r);
			int i=AMath.findMin(new double[] {rg.cx, rg.cy}, refPos);
			histogram[i]+=rg.vol;
			
			// volumes (abs)
			i=AMath.findMin(rg.vol, refVols);
			int j=(rg.rvol<=refRVol?0:1);
			histogram[9+2*i+j]+=1.;
			
			// shapes (elongation*angular)
			i=AMath.findMinAngular(rg.orientation, refAngles, rg.elongation, refElongs);
			histogram[19+i]+=rg.vol;
		}
		
		for (int i=0;i<10;i++) histogram[9+i]/=(double)nregions;
		
	}
	
	private void computeColorHistogram(BufferedImage image, float[][] labmap) {
		
        int height = image.getHeight();
        int width = image.getWidth();
		int bins = labmap.length;
		
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
				int rgb = image.getRGB(i,j);
				float[] lab = Utilities.sRGBtoLab(rgb);
				
				int bin = AMath.findMin(lab,labmap);
				histogram[RHISTO_SIZE+bin]+=1.0;
            }
        }
		
		// normalize histogram
		for (int i=0;i<bins;i++)
			histogram[RHISTO_SIZE+i]/=(double)(height*width);
		
    }
}

