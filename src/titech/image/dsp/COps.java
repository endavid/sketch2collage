/**
 * 
 */
package titech.image.dsp;

import java.awt.image.*;

/**
 * @author david
 *
 */
public class COps {
	/** Computes s1 - s2, while maximizing contrast */
	public static BufferedImage subtract(BufferedImage s1, BufferedImage s2) {
		int w = s1.getWidth()<s2.getWidth()?s1.getWidth():s2.getWidth();
		int h = s1.getHeight()<s2.getHeight()?s1.getHeight():s2.getHeight();
		Raster r1 = s1.getData(), r2=s2.getData();
		int b = r1.getNumBands()<r2.getNumBands()?r1.getNumBands():r2.getNumBands();
		b = b>3?3:b;
		BufferedImage t = new BufferedImage(w, h, 
				b==1?BufferedImage.TYPE_BYTE_GRAY:BufferedImage.TYPE_INT_RGB);
		
		WritableRaster wrasta = t.getRaster();

		double minDiff = 255;
		double maxVal = 0;
		for (int y=0;y<h;y++) {
			for (int x=0;x<w;x++) {
				for (int c=0;c<b;c++) {
				int sam1 = r1.getSample(x, y, c);
				int sam2 = r2.getSample(x, y, c);
				double d = sam1 - sam2;
				if (d<minDiff) minDiff = d;
				if (d>maxVal) maxVal = d;
				}
			}
		}
		double haba = maxVal - minDiff;
		for (int y=0;y<h;y++) {
			for (int x=0;x<w;x++) {
				for (int c=0;c<b;c++) {
				int sam1 = r1.getSample(x, y, c);
				int sam2 = r2.getSample(x, y, c);
				double d = sam1 - sam2;
				double dmax = 255.*(d - minDiff)/haba;
				wrasta.setSample(x,y,c,(int)dmax); // sign is lost, so pack maximizing contrast
				}
			}
		}
		return t;
	}
	
	/** Computes the image distance between each pair of pixels, returning a gray image */
	public static BufferedImage difference(BufferedImage s1, BufferedImage s2) {
		int w = s1.getWidth()<s2.getWidth()?s1.getWidth():s2.getWidth();
		int h = s1.getHeight()<s2.getHeight()?s1.getHeight():s2.getHeight();
		Raster r1 = s1.getData(), r2=s2.getData();
		int b = r1.getNumBands()<r2.getNumBands()?r1.getNumBands():r2.getNumBands();
		BufferedImage t = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster wrasta = t.getRaster();

		for (int y=0;y<h;y++) {
			for (int x=0;x<w;x++) {
				double diff = 0;
				for (int c=0;c<b;c++) {
					int sam1 = r1.getSample(x, y, c);
					int sam2 = r2.getSample(x, y, c);
					double d = sam1 - sam2;
					diff+=d*d;
				}
				wrasta.setSample(x,y,0,(int)Math.sqrt(diff)); 
			}
		}
		return t;
	}
	
	public static float[][] differentiateX(BufferedImage source) {
		int w = source.getWidth();
		int h = source.getHeight();
		Raster r = source.getData();
		int b = r.getNumBands();
		float[][] out = new float[w*h][b];
		for (int c=0;c<b;c++) {
			for (int j=0;j<h;j++) {
				out[j*w][c]=(float)(r.getSample(1,j,c)-r.getSample(0,j,c))/2f;
				for (int i=1;i<w-1;i++) {
					out[j*w+i][c]=(float)(r.getSample(i+1,j,c)-r.getSample(i-1,j,c))/2f;
				}
				out[j*w+(w-1)][c]=(float)(r.getSample(w-1,j,c)-r.getSample(w-2,j,c))/2f;
			}
		}
		return out;
	}

	public static float[][] differentiateY(BufferedImage source) {
		int w = source.getWidth();
		int h = source.getHeight();
		Raster r = source.getData();
		int b = r.getNumBands();
		float[][] out = new float[w*h][b];
		for (int c=0;c<b;c++) {
			for (int i=0;i<w;i++) {
				out[i][c]=(float)(r.getSample(i,1,c)-r.getSample(i,0,c))/2f;
				for (int j=1;j<h-1;j++) {
					out[j*w+i][c]=(float)(r.getSample(i,j+1,c)-r.getSample(i,j-1,c))/2f;
				}
				out[(h-1)*w+i][c]=(float)(r.getSample(i,h-1,c)-r.getSample(i,h-2,c))/2f;
			}
		}
		return out;
	}

	public static BufferedImage gradientMagnitude(BufferedImage source) {
		int w = source.getWidth();
		int h = source.getHeight();
		Raster r = source.getData();
		int b = r.getNumBands()>3?3:r.getNumBands();
		BufferedImage t = new BufferedImage(w, h, 
				b==1?BufferedImage.TYPE_BYTE_GRAY:BufferedImage.TYPE_INT_RGB);
		float[][] xx=differentiateX(source);
		float[][] yy=differentiateY(source);
		
		WritableRaster wrasta = t.getRaster();
		for (int y=0;y<h;y++) {
			for (int x=0;x<w;x++) {
				for (int c=0;c<b;c++) {
				double d = Math.sqrt(xx[y*w+x][c]*xx[y*w+x][c]+yy[y*w+x][c]*yy[y*w+x][c]);
				d=d/Math.sqrt(2); // max value = sqrt(255^2+255^2)
				wrasta.setSample(x,y,c,(int)d); 
				}
			}
		}
		
		return t;
	}
}
