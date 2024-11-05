package titech.image.dsp;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import titech.file.*;
import titech.image.math.*;
import titech.util.*;
import titech.db.*;

/**
 * This class differs from the one at blobby. It contains several titech.image.dsp.Region objects that 
 * can be added incrementally.
 */
public class ObjectImage {
	public static final int NFEATURES=15;
	public static final int RHISTO_SIZE=22;
	public static final int CHISTO_SIZE=52;

	/** The minimum acceptable size of a region, in % respect the size of the image.
		* Default = 1% */
	double volAcceptance = 0.01;
	
	Vector<titech.image.dsp.Region> regions;
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
	
	private int nlabels;
	private float[][] labmap;

	private int[] equivalences;
	BufferedImage labeledImage;

	/** The original source image, if exists */
	BufferedImage original;
	
	public ObjectImage() {
		regions = new Vector<titech.image.dsp.Region>();
		original = null;
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
		regions = new Vector<titech.image.dsp.Region>();
		original = null;
		
		StringTokenizer stok = new StringTokenizer(os);
		try {
			int nregions = Integer.parseInt(stok.nextToken());
			contourCount = Double.parseDouble(stok.nextToken());
			pwdistances = new double[4];
			for (int k=0;k<4;k++) pwdistances[k]=Double.parseDouble(stok.nextToken());
			
			// region 0 is the background
			for (int i=1;i<nregions;i++) {
				titech.image.dsp.Region region = new titech.image.dsp.Region();
				region.setColorCat(Integer.parseInt(stok.nextToken()));
				regions.add(region);
			}
			
			double[] features = new double[NFEATURES];
			for (int i=1;i<nregions;i++) {
				for (int f=0;f<NFEATURES;f++) {
					features[f]=Double.parseDouble(stok.nextToken());
				}
				titech.image.dsp.Region region = (titech.image.dsp.Region)regions.get(i-1);
				region.setFeatures(features);
			}
		} catch (Exception e) {
			System.err.println("ObjectImage: "+e);
		}
	}

	public ObjectImage(int[][] labeledImage, int nlabels, BufferedImage source) {
		this(labeledImage, nlabels, source, 0.01);
	}

	/** You can use ColorLabel outside this class, and pass the result here */
	public ObjectImage(int[][] labeledImage, int nlabels, BufferedImage source, double vol) {
		regions = new Vector<titech.image.dsp.Region>();
		this.nlabels = nlabels;
		this.volAcceptance = vol;
		setRegions(labeledImage, source);
	}
	
	public void setOriginalImage(BufferedImage src) {
		original = src;
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
		String path = location.substring(0,location.lastIndexOf(File.separator));
		String filename=location.substring(location.lastIndexOf(File.separator)+1);
		return path+File.separator+Retrieval.THUMBSDIR+
			File.separator+FileUtils.nameWOExtension(filename)+".png";		
	}
	
	public static String previewFromLocation(String location) {
		String path = location.substring(0,location.lastIndexOf(File.separator));
		String filename=location.substring(location.lastIndexOf(File.separator)+1);
		return path+File.separator+Retrieval.PREVIEWDIR+
			File.separator+filename;		
	}
	
	public static String ccatFromLocation(String location) {
		String path = location.substring(0,location.lastIndexOf(File.separator));
		String filename=location.substring(location.lastIndexOf(File.separator)+1);
		return path+File.separator+Retrieval.THUMBSDIR+
			File.separator+FileUtils.nameWOExtension(filename)+"-ccat.png";		
	}
	
	/**
	 * This adds a new region from an image assumed to have only one region of
	 * the given color
	 *
	 */
	public titech.image.dsp.Region addRegion(BufferedImage img, Color stroke) {
		
		// count white pixels (black are background)
		titech.image.dsp.Region region = calculateFeatures(img);
		region.setColor(stroke);

		
		regions.add(region);
		//System.out.println("titech.image.dsp.Region added: "+region);
		
		return region;
	}
	
	public titech.image.dsp.Region getRegion(int index) {
		return (titech.image.dsp.Region)regions.get(index);
	}
	
	/** The Vector can be modified! Be careful */
	public Vector getRegions() {
		return regions;
	}
	
	/** Empty the regions vector */
	public void clear() {
		regions.clear();
	}
	
	/** Assumes the image contains just one object on a black background
	  * Add a parameter, the color of the region, and call it for
	  * every color in settitech.image.dsp.Regions(BufferedImage)
	  */
	public titech.image.dsp.Region calculateFeatures(BufferedImage img) {
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

		titech.image.dsp.Region region = new titech.image.dsp.Region();
		region.setCenter(centerX, centerY);
		region.setVolume(vol, relativeVol);
		region.setOrientation(orientation);
		region.setElongation(elongation);
		
		return region;
	}

	public void setRegions(BufferedImage bim) {
		ColorLabel cl = new ColorLabel(bim);
		nlabels = cl.getNLabels();
		setRegions(cl.getLabeledMatrix(), bim);
		labeledImage = cl.getAsImage(regions, equivalences);
	}
	
	public void setRegions(int[][] img, BufferedImage bim) {
		clear(); // empty the titech.image.dsp.Regions vector		
		
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
		// in the sketch -- use titech.image.dsp.Region.setColor(RGB)
		// It will be stored in L*a*b* as the mean
		double[][] meanColor = new double[nlabels][3];
		double[][] deviation = new double[nlabels][3];
		double[][] skewness = new double[nlabels][3];
		Raster rasta = null;
		if (original != null) rasta = original.getData();
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
					
					if (rasta != null) {
						// we assume the image has 3 bands!
						meanColor[r][0] += rasta.getSample(i, j, 0);
						meanColor[r][1] += rasta.getSample(i, j, 1);
						meanColor[r][2] += rasta.getSample(i, j, 2);
					} else {
						color[r] = bim.getRGB(i,j);
					}

				}
			}
		}

		if (rasta != null) {
			for (int r = 1; r < nlabels; r++) {
				if (n[r] > 0) {
					// normalized 0..255
					for (int c = 0; c < 3; c++) {
						meanColor[r][c] /= n[r];
					}
				}
			}
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					// get index or color
					int r = img[i][j];
					if (r > 0 && r < nlabels) {
						for (int c = 0; c < 3; c++) {
							double p = (double)rasta.getSample(i, j, c) - meanColor[r][c];
							deviation[r][c] += p * p;
							skewness[r][c] += p * p * p;
						}
					}
				}
			}
		}
		
		double colorNorm = 255.;
		equivalences = new int[nlabels];
		equivalences[0]=0;
		int nobjects=0;
		for (int r = 1; r < nlabels; r++) {
			equivalences[r]=0;
			if (n[r] > 0) {
				// mass centre
				centerX[r] =  (double)momentX[r] /  (double)n[r];
				centerY[r] =  (double)momentY[r] /  (double)n[r];
							
				// normalized 0..1
				for (int c = 0; c < 3; c++) {
					// change 255 by the maximum value! L*a*b -> 100
					meanColor[r][c] /= colorNorm;
					deviation[r][c] = Math.sqrt(deviation[r][c] / n[r])/colorNorm;
					skewness[r][c] = AMath.qbic(skewness[r][c] / n[r])/colorNorm;
				}
				
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
				
				
				// create the region object if it's big enough
				if (vol[r]>volAcceptance) {
					titech.image.dsp.Region region = new titech.image.dsp.Region();
					region.setCenter(centerX[r], centerY[r]);
					region.setVolume(vol[r], relativeVol[r]);
					region.setOrientation(orientation[r]);
					region.setElongation(elongation[r]);
					if (rasta!=null) {
						region.setColor(meanColor[r], deviation[r], skewness[r]);
					} else {
						region.setColor(color[r]);					
					}
					regions.add(region);
					nobjects++;
					equivalences[r]=nobjects;
				}
				
				
			} // endif n[r]>0
		} // for all regions
		
		
		// compute the color histogram now
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		if (labmap!=null) {
			computeColorHistogram(bim,labmap);
			
			// compute also the region histogram
			// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			computeRegionHistogram();			
		}
	}
	
	/** Instead of returning a titech.image.dsp.Region, it returns an index to the titech.image.dsp.Region Vector */
	public int minDistanceRegion(titech.image.dsp.Region query) {
		minDist = Double.MAX_VALUE;
		int minR = 0;
		for (int i=0;i<regions.size();i++) {
			titech.image.dsp.Region r = (titech.image.dsp.Region)regions.get(i);
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
	
	/** This value is initialized after a call to minDistancetitech.image.dsp.Region */
	public double getMinDist() {
		return minDist;
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
			titech.image.dsp.Region rg = (titech.image.dsp.Region)regions.get(r);
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
	
	
	/**
		*  A String representation of this object, with all its features.
	 *
	 *  n cc pw[0] pw[1] pw[2] pw[3] cats[0] ... cats[n-2] feats[0][0] .. feats[0][13] ..
	 *
	 *  You can reconstruct the object with this string:
	 *
	 *  ObjectImage copy = new ObjectImage(obj.getDescriptor());
	 *
	 * @return    The String representation.
	 */
	public String getDescriptor() {		
		int n = regions.size();
		getContourSum();
		getPairwises(); // maybe they are not yet computed!
		
		String descriptor = ""+(n+1)+" "+contourCount+" ";
		for (int k=0;k<4;k++) descriptor+=pwdistances[k]+" ";
		
		for (int i=0;i<n;i++) {
			titech.image.dsp.Region r=(titech.image.dsp.Region)regions.get(i);
			descriptor+=r.ccat+" ";	
		}
		double[] ff = new double[NFEATURES];
		for (int i=0;i<n;i++) {
			titech.image.dsp.Region r=(titech.image.dsp.Region)regions.get(i);
			r.getFeatures(ff);
			for (int f=0;f<NFEATURES;f++)
				descriptor+=ff[f]+" ";			
		}
		
		return descriptor;
	}
	
	/** Gets the amount (sum) of contours in the segmented image. 
	  *  It computes it the first time the method is invoked.
	  * -NOT IMPLEMENTED-
	  */
	public double getContourSum() {
		/*if (contourCount < 0) { // not yet computed
			PlanarImage pim = COps.toBW(getRGBImage());
			EdgeImage ei = new EdgeImage(pim, EdgeImage.GRADIENT);
			pim = COps.binarize(ei.magnitude, 0.01);
			double[] means = COps.mean(pim);
			contourCount = means[0];
		}*/
		
		return contourCount;
	}
	
	public double[] getPairwises() {
		if (pwdistances == null) {
			pairwiseDistances();
		}
		
		return pwdistances;
	}
	
	private void pairwiseDistances() {
		double[] distances = new double[] {0,0,0,0};
		pwdistances = new double[] {0,0,0,0};
		int n=regions.size();
		for (int i=0;i<n;i++) {
			for (int j=0;j<n;j++) {
				titech.image.dsp.Region r1=(titech.image.dsp.Region)regions.get(i);
				titech.image.dsp.Region r2=(titech.image.dsp.Region)regions.get(j);
				r1.distance(r2,distances);
				for (int k=0;k<4;k++) pwdistances[k]+=distances[k];
			}
		}
		
		if (n>0) {
			for (int k=0;k<4;k++) pwdistances[k]/=(double)n*n;
		}
	}
	
	BufferedImage getLabeledImage() {
		return labeledImage;
	}
}

