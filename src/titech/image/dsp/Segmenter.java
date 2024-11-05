//
//  Segmenter.java
//  sketchRC
//
//  Created by David Gavilan on 3/4/07.
//  Copyright 2007 NJLab. All rights reserved.
//
package titech.image.dsp;

import java.awt.image.*;
import titech.cluster.*;
import titech.util.*;
import titech.image.math.*;

/**
  * Segmenter
  *
  * The RGB-L*a*b* color conversion is computed fast by means of a 16-bit LUT
  * The same for the Color Categorization neural network.
  */
public class Segmenter {
	/**
	* This decides the size of the resulting image, thus, the resulting blobs.
	 * It should be the same size as for the ones in the Database (80x60)
	 */
	public final static int IMGLONG = 80;
	
	/** Minimum number of colors to count a category */
	public static final double COLOR_MIN_SIZE = 0.05;
	/** Default size for the statistic ordering filter */
	public final static int M_SIZE = 5;
	
	/** An indexed image */
	BufferedImage segmented;
	BufferedImage quantized;
	
	byte[] catLut;
	float[] labLut;
	byte[][] univPal;
	
	public BufferedImage getSegmentedImage() {
		return segmented;
	}
	
	public BufferedImage getQuantizedImage() {
		return quantized;
	}
	
	public Segmenter() {
		univPal = null;
		catLut = new byte[32768]; // size of catlut
		labLut = new float[32768*3]; // 3 bands
		try {
			java.io.InputStream is = this.getClass().getResourceAsStream("/resources/cc14.raw");
			is.read(catLut);
			is.close();
			
			is = this.getClass().getResourceAsStream("/resources/lab15bit.raw");
			java.io.DataInputStream din = new java.io.DataInputStream(is);
			// 15-bit palette, 32 colors per band
			for (int r=0;r<32;r++){
				for (int g=0;g<32;g++) {
					for (int b=0;b<32;b++) {						
						for (int i=0;i<3;i++) {
							float ff = din.readFloat();
							labLut[3*32*32*r+3*32*g+3*b+i] = ff;
						}
					}
				}
			} // end fors
			din.close();
						
			// read the color palette
			is = this.getClass().getResourceAsStream("/resources/universalEx.palette");
			univPal=Utilities.loadPalette(is);
			is.close();

		} catch (Exception e) {
			System.err.println("Segmenter: Couldn't load resources!");
		}
		

	}
	
	public ObjectImage getKMedianRegions(BufferedImage img) {
//										 int size, double osize, byte[][] pal) {
		
		ObjectImage oimg = null;
		try {
		    BufferedImage rop = Utilities.resize(img,IMGLONG);
			BufferedImage ccat = colorCategorization(rop);
			
			int npixels = rop.getWidth()*rop.getHeight();
			int[] initialAssignment = new int[npixels];
			int k = countPresentCategories(ccat,initialAssignment);
			k = Math.max(2,k);
			BufferedImage reg = clusterKMeans(rop,k,initialAssignment);
			quantized = reg;
			segmented = MOps2D.maxPercentil(14,reg,M_SIZE);

		    oimg = new ObjectImage();
			oimg.setOriginalImage(rop);
			oimg.setRegions(segmented);
			segmented=oimg.getLabeledImage();
			
		} catch (Exception exc) {
			System.err.println("Segmenter: "+exc);
		}
		return oimg;
	} // end getKMedianRegions

	public static BufferedImage createIndexedImage(int width, int height, byte[][] colormap) {
		IndexColorModel indexColorModel = new IndexColorModel(8, colormap[0].length, 
															  colormap[0],
															  colormap[1],
															  colormap[2]);
		BufferedImage indexed = new BufferedImage(width, 
												  height, 
												  BufferedImage.TYPE_BYTE_INDEXED,
												  indexColorModel);
		
		return indexed;
	}

	BufferedImage colorCategorization(BufferedImage source) {
		int w=source.getWidth(), h=source.getHeight();
		BufferedImage sgm = createIndexedImage(w, h, univPal);
		WritableRaster wr = sgm.getRaster();
		for (int j=0;j<h;j++) {
			for (int i=0;i<w;i++) {
				int col = source.getRGB(i,j);
				wr.setSample(i,j,0,AMath.byteToInt(colorCategory(col)));
			}
		}
		return sgm;
	}
	
	byte colorCategory(int colour) {
		int r=colour>>16 & 0x000000FF;
		int g=colour>>8 & 0x000000FF;
		int b=colour & 0x000000FF;
		// divide by 8 (15-bit color palette)
		r=r>>3; g=g>>3; b=b>>3;
		byte category = catLut[r*32*32+g*32+b];
		return category;
	}
	
	
	public BufferedImage clusterKMeans(BufferedImage image, int k, int[] initialClusters) {
		// Loop over the input, copy each pixel to the output, 
		// converting them from sRGB to L*a*b* as we go
        int bands = image.getSampleModel().getNumBands();
        int height = image.getHeight();
        int width = image.getWidth();
		// override globals
		byte[][] colormap = new byte[3][256];
				
		double[][] data=new double[height*width][bands];
		int i=0;
		Raster raster = image.getData();
        for (int samp = 0; samp < width; samp++) {
            for (int line = 0; line < height; line++) {
				int r = raster.getSample(samp, line, 0);
				int g = raster.getSample(samp, line, 1);
				int b = raster.getSample(samp, line, 2);
				// divide by 8 (15-bit color palette)
				r=r>>3; g=g>>3; b=b>>3;
				int index = r*32*32*3+g*32*3+b*3;
				for (int mi=0;mi<3;mi++) {
					data[i][mi] = (double)labLut[index+mi];					
				}
				i++;
			}
		}
		
		// apply K-Means
		KMeans cluster = new KMeans(k,data,initialClusters);
		int[] clases = cluster.getAssignments();
		
		// palette
		double[][] means = cluster.getMeans();
		// background color
		colormap[0][0]=80; colormap[1][0]=80; colormap[2][0]=80;
		double colorNorm = 1.0;
		for (i = 0; i < k; i++) {
			float[] col = new float[3];
			for (int j = 0; j < 3; j++) col[j] = (float)(colorNorm * means[i][j]);
			col = Utilities.LabtosRGB(col);
			for (int j = 0; j < 3; j++) col[j] *= 255f;
			for (int j = 0; j < 3; j++) colormap[j][i+1] = (byte) col[j];
		}
		
        BufferedImage outImage = createIndexedImage(image.getWidth(),
													image.getHeight(), colormap);
		
		i=0;
		WritableRaster wrasta = outImage.getRaster();
		for (int samp = 0; samp < width; samp++) {
            for (int line = 0; line < height; line++)
				wrasta.setSample(samp, line, 0, clases[i++]);
		}
		
		//System.out.println(AMath.showMatrix(means));
		return outImage;
		
	}
	
	
	/** Applies the MLP Color categorization and counts the number
	  * of colors that are present at least MIN_SIZE %
	  * We count the "unknown" pixels as a different category.
      */
	public int countPresentCategories(BufferedImage ccat, int[] output) {
		
        int height = ccat.getHeight();
        int width = ccat.getWidth();
		
		int ncats = 16;
		int[] votes = new int[ncats+1];
		Raster rasta = ccat.getData();
        for (int samp = 0; samp < width; samp++) {
            for (int line = 0; line < height; line++) {
				int value = rasta.getSample(samp, line, 0);
				votes[value]++;
			}
		}
		
		int r=0;
		double npixels = height*width;
		for (int i=0;i<=ncats;i++) {
			double amount = (double)votes[i]/npixels;
			//System.out.println("Amount: "+amount);
			if (amount>COLOR_MIN_SIZE) r++;
		}
		//System.out.println("Present categories: "+r);
		
		if (output!=null) {
			if (output.length>=npixels) {
				int j=0;
				for (int samp = 0; samp < width; samp++) {
					for (int line = 0; line < height; line++) {
						int cc = rasta.getSample(samp, line, 0);
						output[j++]= cc%r; 
					}
				}
			}
		}
		
		return r;
	}
	
	/**
	 * Instead of using the Graphics object to quantize a given src to the reference color
	 * palette, we compute the distance in the L*a*b* color space in this function.
	 * Assume that the index 0 is to be ignored (transparent or background)
	 * @param src An input RGB color image
	 * @param reference An indexed image.
	 * @return An indexed image, the colors of src having being quantized with the reference
	 */
	public BufferedImage quantizeWithMinLab(BufferedImage src, BufferedImage reference) {
		int w=src.getWidth();
		int h=src.getHeight();
		IndexColorModel icModel = (IndexColorModel)reference.getColorModel();
	
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, icModel);
		Raster rasta = src.getData();
		WritableRaster wrasta = out.getRaster();
	
		//int n = icModel.getMapSize();
		int n=MOps2D.maxValue(reference)+1;
		double[][] colormap = new double[n-1][3];
		for (int i=1;i<n;i++) {
			int r = icModel.getRed(i);
			int g = icModel.getGreen(i);
			int b = icModel.getBlue(i);
			// divide by 8 (15-bit color palette)
			r=r>>3; g=g>>3; b=b>>3;
			int index = r*32*32*3+g*32*3+b*3;
			for (int mi=0;mi<3;mi++) {
				colormap[i-1][mi] = (double)labLut[index+mi];					
			}
		}
		
	
		double[] lab=new double[3];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int r = rasta.getSample(x, y, 0);
				int g = rasta.getSample(x, y, 1);
				int b = rasta.getSample(x, y, 2);
				// divide by 8 (15-bit color palette)
				r=r>>3; g=g>>3; b=b>>3;
				int index = r*32*32*3+g*32*3+b*3;
				for (int mi=0;mi<3;mi++) {
					lab[mi] = (double)labLut[index+mi];					
				}
				int i=AMath.findMin(lab, colormap);
				wrasta.setSample(x, y, 0, i+1);
			}
		}

		
		return out;
	}
	
	/**
	 * Outputs a color categorized image
	 * Example: 
	 * java -classpath dist/SketchRC.jar titech.image.dsp.Segmenter ~/pix/kenkyu/cc/xyY01.png ~/pix/kenkyu/cc/xyY01-LUT.png
	 * @param args path to image
	 */
	public static void main(String args[]) {
		try {
			BufferedImage img = titech.util.Utilities.loadImage(args[0]);
			Segmenter segm = new Segmenter();
			BufferedImage ccat = segm.colorCategorization(img);
			String output = "ccat.png";
			if (args.length>1) output=args[1];
			titech.util.Utilities.saveImage(ccat, "png", output);
		} catch (Exception exc) {
			System.err.println("Segmenter: "+exc);
		}
	}
	
}
