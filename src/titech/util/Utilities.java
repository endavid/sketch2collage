package titech.util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.URL;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.util.*;
import titech.image.math.*;
import titech.db.*;
import titech.image.dsp.*;
import titech.file.*;

public class Utilities {
  private static final Component sComponent = new Component() {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;};
  private static final MediaTracker sTracker = new MediaTracker(sComponent);
  private static int sID = 0;
  /** D65 White point (x,y)=0.31382,0.33100 */
  public static float[] D65XYZ={0.950166f, 1f, 1.087654f};

  public final static String jpeg = "jpeg";
  public final static String jpg = "jpg";
  public final static String gif = "gif";
  public final static String tiff = "tiff";
  public final static String tif = "tif";
  public final static String png = "png";

   // ------------------------------------------ FILES -----------------------------
  
    /*
     * Get the extension of a file.
     */  
    public static String getExtension(File f) {
        return getExtension(f.getName());
    }
	
    public static String getExtension(String s) {
        String ext = null;
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
				
	
	/** Returns the name without the last .XXXX */
	public static String clearExtension(String s) {
        int i = s.lastIndexOf('.');
		if (i > 0 &&  i < s.length() - 1) {
			return s.substring(0,i);
        }
		return s;
	}

	/**
	  * Returns true if the index has been created.
	  * False if the index files already existed.
	  */
	public static boolean indexDir(String currentPath) throws IOException {
			
		// load the colormap for the color histogram
		//interpret("resource /resources/histogram.palette");
			
		File indexFile = new File(currentPath+File.separator+Retrieval.INDEX_NAME);
		File histogramFile = new File(currentPath+File.separator+ImageRetrieval.INDEX_NAME);
		if (indexFile.exists() && histogramFile.exists()) {
			// give up
			// Notice that we don't check for changes in the directory!
			return false;
		}
		ImageFileFilter filter = new ImageFileFilter();
		filter.addExtension("jpg");
		filter.addExtension("png");
		filter.addExtension("jpeg");
		filter.addExtension("bmp");
		filter.addExtension("tiff");
		File[] fileList = FileUtils.ls(currentPath,filter);
		
		// create index
		if (!indexFile.exists()) indexFile.createNewFile();
		PrintWriter iWriter = new PrintWriter(new FileOutputStream(indexFile));
		if (!histogramFile.exists()) histogramFile.createNewFile();
		PrintWriter ihWriter = new PrintWriter(new FileOutputStream(histogramFile));
			
		// create directories to save thumbs
		File bdir = new File(currentPath+File.separator+Retrieval.THUMBSDIR);
		if (bdir.mkdir()) {
			System.out.println("Directory "+currentPath+File.separator+Retrieval.THUMBSDIR+" created.\n");
		}
		
		File pdir = new File(currentPath+File.separator+Retrieval.PREVIEWDIR);
		if (pdir.mkdir()) {
			System.out.println("Directory "+currentPath+File.separator+Retrieval.PREVIEWDIR+" created.\n");			
		}
			
		long totalTime = 0;
		Segmenter segmenter = new Segmenter();
		for (int i = 0; i < fileList.length; i++) {
			File f = fileList[i];
			
			BufferedImage img = loadImage(f.getAbsolutePath());
			long time = System.currentTimeMillis();
			System.out.println("indexing... "+(i+1)+"/"+fileList.length);
			ObjectImage objectImage = segmenter.getKMedianRegions(img);
			totalTime += System.currentTimeMillis() - time;
				
			// regions - separated by spaces
			String descriptor = objectImage.getDescriptor();
			//System.out.println(descriptor);
			iWriter.println(descriptor);
			iWriter.flush();
				
			// histogram - separated by commas...
			// it contains both region and color histograms
			double[] rh = objectImage.getHistogram();
			descriptor = AMath.showVector(rh);
			ihWriter.println(descriptor);
			ihWriter.flush();
				
			saveImage(segmenter.getSegmentedImage(), "png", 
					  currentPath+File.separator+Retrieval.THUMBSDIR+
					 File.separator+FileUtils.nameWOExtension(f.getName())+".png");
				
			// save also the color-clustered thumbnail for convenience
			saveImage(segmenter.getQuantizedImage(), "png", 
					  currentPath+File.separator+Retrieval.THUMBSDIR+
					 File.separator+FileUtils.nameWOExtension(f.getName())+"-ccat.png");
				
				
				//monitor.setProgress((int)(100*i/fileList.length));
		}
		//monitor.setProgress(100);
		double t = (double) totalTime / 1000.0;
			
		iWriter.close();
			
		System.out.println("Computed index for "+fileList.length
					  +" images in "+t+" secs.\n");
		System.out.println("Index saved in "+indexFile.getAbsolutePath());
		
		return true;
	}
	
	// ----------------------------------------- IMAGES  -------------------------------
	
  public static boolean waitForImage(Image image) {
    int id;
    synchronized(sComponent) { id = sID++; }
    sTracker.addImage(image, id);
    try { sTracker.waitForID(id); }
    catch (InterruptedException ie) { return false; }
    if (sTracker.isErrorID(id)) return false;
    return true;
  }    

  public static Image blockingLoad(String path) {
    Image image = Toolkit.getDefaultToolkit().getImage(path);
    if (waitForImage(image) == false) return null;
    return image;
  }
  
  public static BufferedImage loadImage(File f) throws IOException {
	  return ImageIO.read(f);
  }
	
	public static BufferedImage loadImage(String filepath) throws IOException {
		return loadImage(new File(filepath));
	}
  
	public static BufferedImage loadIndexedImage(String filepath) throws Exception {
		return loadIndexedImage(new File(filepath));
	}
	
	/**
	  * This method forces that the ColorModel of the output is: 
	  * IndexColorModel: #pixelBits = 8 numComponents = 3 color space = java.awt.color.ICC_ColorSpace
	  * 
	  * But the loadImage method above should return the same thing, hopefully.
	 */
	public static BufferedImage loadIndexedImage(File f) throws Exception {
		Iterator<ImageReader> itimr = ImageIO.getImageReadersBySuffix("png");
		
		BufferedImage out = null;
		// el primero q se presente, que debe ser com.sun.imageio.plugins.png.PNGImageReader
		if (itimr.hasNext()) {
			ImageReader imr = itimr.next();
			imr.setInput(new FileImageInputStream(f));
			ImageReadParam param = imr.getDefaultReadParam();
			param.setDestinationType(imr.getRawImageType(0)); // INDEXED if the image is indexed
			out = imr.read(0,param);  // ara viene cuando la matan
			System.out.println(out.getColorModel());
		} else { // tocate la pera, que no hay PNG reader
			System.err.println("Utilities: Can't read PNG files!");
		}
		return out;
	}
	
  public static void saveImage(BufferedImage img, String format, File f) throws IOException {
	  ImageIO.write(img, format, f);
  }

  public static void saveImage(BufferedImage img, String format, String path) throws IOException {
	  ImageIO.write(img, format, new File(path));
  }


  public static Image blockingLoad(URL url) {
    Image image = Toolkit.getDefaultToolkit().getImage(url);
    if (waitForImage(image) == false) return null;
    return image;
  }
  
  public static BufferedImage makeBufferedImage(Image image) {
    return makeBufferedImage(image, BufferedImage.TYPE_INT_RGB);
  }
  
  public static BufferedImage makeBufferedImage(Image image, int imageType) {
    if (waitForImage(image) == false) return null;

    BufferedImage bufferedImage = new BufferedImage(
        image.getWidth(null), image.getHeight(null),
        imageType);
    Graphics2D g2 = bufferedImage.createGraphics();
    g2.drawImage(image, null, null);
    return bufferedImage;
  }

	public static BufferedImage maskIndexedImage(BufferedImage image, int index, BufferedImage original) {
		return maskIndexedImage(image, index, original, true, true);
	}

	/**
	 * Returns the same image such that the pixels p == index.
	 * The result image is not indexed, but an ARGB image, with alpha=1.0 where p!=index.
	 *
	 * If the original image is provided, then return the value of original pixels.
	 * The original image will be resized to match the input.
	 */
	public static BufferedImage maskIndexedImage(BufferedImage image, int index, BufferedImage original, boolean togray, boolean rescale) {
		int transparent = 0x00000000;
		int noalpha = 0xFF000000;		
		Raster rasta = image.getData();
		int minx = rasta.getMinX();
		int miny = rasta.getMinY();
		int width = rasta.getWidth();
		int height = rasta.getHeight();
		
		BufferedImage source = image;
		if (original != null) {
			source = original;
			// resize if necessary
			if ((original.getWidth()!=image.getWidth() || original.getHeight() != image.getHeight())
				&& rescale)
				source = resize(original, image.getWidth(), image.getHeight());
		}
		BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), 
											  BufferedImage.TYPE_INT_ARGB);

		int x=0,y=0;
		double scalex = (double)width/(double)source.getWidth();
		double scaley = (double)height/(double)source.getHeight();
		//for (int j=miny;j<(miny+height);j++) {
		for (y=0;y<source.getHeight();y++) {
			//for (int i=minx;i<(minx+width);i++) {
			for (x=0;x<source.getWidth();x++) {
				int i=minx+(int)(x*scalex);
				int j=miny+(int)(y*scaley);
				int pixel = rasta.getSample(i,j,0);
				if (pixel == index) out.setRGB(x,y,source.getRGB(x,y));
				else if (togray) out.setRGB(x,y,noalpha|toGray(source.getRGB(x,y)));
				else out.setRGB(x,y,transparent);
			}
		}
		
		return out;
	}
	
	public static BufferedImage maskIndexedImage(BufferedImage image, int index) {
		return maskIndexedImage(image, index, null);
	}


	public static BufferedImage resize(BufferedImage image, int newSize) {
		double w = image.getWidth(), h=image.getHeight(), scale = 1;
		int newWidth = newSize, newHeight = newSize;
		if (w<h) {
			scale = newWidth / w;
			newHeight = (int)(h*scale);
		} else {
			scale = newHeight / h;
			newWidth = (int)(w*scale);			
		}
		BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        g.drawRenderedImage(image, xform);
        g.dispose();
        return result;
    }
	
	public static Dimension resizeDimension(BufferedImage image, int newSize) {
		double w = image.getWidth(), h=image.getHeight(), scale = 1;
		int newWidth = newSize, newHeight = newSize;
		if (w<h) {
			scale = newWidth / w;
			newHeight = (int)(h*scale);
		} else {
			scale = newHeight / h;
			newWidth = (int)(w*scale);			
		}
		return new Dimension(newWidth, newHeight);
	}
	
	public static BufferedImage resize(BufferedImage image, int newWidth, int newHeight) {
		// add alpha channel when resizing, ARGB
        BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        double scaleX = newWidth / (double) image.getWidth();
        double scaleY = newHeight / (double) image.getHeight();
        AffineTransform xform = AffineTransform.getScaleInstance(scaleX, scaleY);
        g.drawRenderedImage(image, xform);
        g.dispose();
        return result;
    }
	
	public static void colorToGray(BufferedImage image) {
		int noalpha = 0xFF000000;
		for (int y=0;y<image.getHeight();y++) {
			for (int x=0;x<image.getWidth();x++) {
				int pixel = image.getRGB(x,y);
				image.setRGB(x,y,noalpha|toGray(pixel));
			}
		}		
	}
	
	public static Kernel gaussian2Dkernel(float sigma) {
		int ksize = (int)(Math.ceil(sigma*3) * 2 + 1);
		return gaussian2Dkernel(ksize, sigma);
	}
	
	/**
		* (Computer Vision: A Modern Approach (pag. 209))
	 *
	 * @param  size  Size of the kernel
	 * @param  s     The sigma of the Gaussian
	 * @return       A matrix with a 2D gaussian. 
	 */
	public static Kernel gaussian2Dkernel(int size, float s) {
		if (size < 3) {
			return null;
		}
		if (size % 2 == 0) {
			return null;
		}
		// only odd numbers
		
		int half = size >> 1;
		double[] kernel = new double[size*size];
		for (int i = -half; i <= half; i++) {
			for (int j = -half; j <= half; j++) {
				//kernel[i + half][j + half] = gauss(i, j, s);
				// save some computations. We are gonna divide by sum anyway.
				kernel[i + half + (j + half)*size] = gaussPI(i, j, s);
			}
		}
		
		AMath.multV(kernel, (1. / AMath.sum(kernel)));
		// now it's safer to convert to float. If we operate directly in float,
		// the loss of precision results in completely wrong kernels!
		
		return new Kernel(size,size,AMath.toFloat(kernel));
	}
	
	/** Just the exponential */
	public static double gaussPI(double x, double y, double s) {
		return Math.exp(-(x * x + y * y) / (2. * s * s));
	}
	
  // ----------------------------- COLOR -----------------------------------------
	public static int toGray(int rgb) {
		int r = (rgb & 0x00FF0000)>>16;
		int g = (rgb & 0x0000FF00)>>8;
		int b = (rgb & 0x000000FF);
		int gray = (r+g+b)/3;
		return (gray<<16)|(gray<<8)|gray;
	}
	/** Expects normalized values. */
	public static float[] sRGBtoRGB(float[] srgb) {
		float[] rgb = new float[3];
		if ( srgb[0] > 0.04045 ) rgb[0] =(float)Math.pow((srgb[0]+0.055)/1.055,2.4);
		else rgb[0] = (float)(srgb[0] / 12.92);
		if ( srgb[1] > 0.04045 ) rgb[1] =(float)Math.pow((srgb[1]+0.055)/1.055,2.4);
		else rgb[1] = (float)(srgb[1] / 12.92);
		if ( srgb[2] > 0.04045 ) rgb[2] =(float)Math.pow((srgb[2]+0.055)/1.055,2.4);
		else rgb[2] = (float)(srgb[2] / 12.92);
		
		return rgb;
	}
	
	/** Converts from XYZ color space to L*a*b color space (values < 100)
		*/
	public static float[] XYZtoLab(float[] xyz) {
		
		xyz[0]=xyz[0]/D65XYZ[0];
		xyz[1]=xyz[1]/D65XYZ[1];
		xyz[2]=xyz[2]/D65XYZ[2];
		
		float e=216f/24389f; // the actual CIE standard is 0.008856
		float k=24389f/27f;  // the actual CIE standard is 903.3
		
		// compute fx, fy, fz
		for (int i=0;i<3;i++) {
			if (xyz[i]>e) {
				xyz[i]=(float)Math.pow(xyz[i],1./3.);
			} else {
				xyz[i]=(float)((k*xyz[i]+16.)/116.);
			}
		}
		
		float[] Lab = new float[] {
			116f*xyz[1]-16f,
			500f*(xyz[0]-xyz[1]),
			200f*(xyz[1]-xyz[2])};
		
		return Lab;
	}
	
	/**
		* Ref. http://www.brucelindbloom.com/index.html?Equations.html
	 * Expects RGB normalized input.
	 * 
	 */
	public static float[] RGBtoXYZ(float[] rgb) {
		// it was transposed! now (05/01/15) OK
		float[][] M= new float[][] { 
			new float[] {0.412424f, 0.357579f, 0.180464f},
			new float[] {0.212656f, 0.715158f, 0.0721856f},
			new float[] {0.0193324f, 0.119193f, 0.950444f}};
		return AMath.fmultV(rgb, M);
	}
	
	public static float[] sRGBtoLab(float[] srgb) {
		return XYZtoLab(RGBtoXYZ(sRGBtoRGB(srgb)));
	}
	
	public static float[] sRGBtoLab(int rgb) {
		float[] srgb = new float[3];
		srgb[0] = (float)((rgb & 0x00FF0000)>>16)/255f;
		srgb[1] = (float)((rgb & 0x0000FF00)>>8)/255f;
		srgb[2] = (float)((rgb & 0x000000FF))/255f;		
		return XYZtoLab(RGBtoXYZ(sRGBtoRGB(srgb)));
	}
	
	public static float[] LabtosRGB(float[] lab) {
		return RGBtosRGB(XYZtoRGB(LabtoXYZ(lab)));
	}
	
	public static float[] LabtoXYZ(float[] lab) {
		float e=216f/24389f; // the actual CIE standard is 0.008856
		float k=24389f/27f;  // the actual CIE standard is 903.3
		
		float[] xyz = new float[] {0f, 0f, 0f};
		xyz[1]=(float)(lab[0]>k*e?Math.pow((lab[0]+16f)/116f,3.):lab[0]/k);
		
		// compute fx, fy, fz
		float fx, fy, fz;
		fy = (float)(xyz[1]>e?(lab[0]+16f)/116f:(k*xyz[1]+16f)/116f);
		fx = lab[1]/500f+fy;
		fz = fy - lab[2]/200f;
		
		xyz[0] = (float)Math.pow(fx, 3);
		if (xyz[0]<=e) xyz[0] = (116f*fx-16f)/k;
		xyz[2] = (float)Math.pow(fz, 3);
		if (xyz[2]<=e) xyz[2] = (116f*fz-16f)/k;
		
		for (int i=0;i<3;i++) xyz[i]*=D65XYZ[i];
		
		return xyz;
	}
	
	public static float[] XYZtoRGB(float[] xyz) {
		// it was transposed! now OK
		float[][] M= new float[][] {
			new float[] {3.24071f, -1.53726f, -0.498571f}, 
			new float[] {-0.969258f, 1.87599f, 0.0415557f},   
			new float[] {0.0556352f, -0.203996f, 1.05707f}};
		
		return AMath.fmultV(xyz, M);
	}
	
	/** Chang */
	public static float[] RGBtosRGB(float[] rgb) {
		float[] srgb = new float[3];
		if ( rgb[0] > 0.00304f ) 
			srgb[0] =(float) (1.055 * Math.pow(rgb[0],(1.0/2.4)) - 0.055);
		else
			srgb[0] = (float)(12.92 * rgb[0]);
		if ( rgb[1] > 0.00304f )
			srgb[1] = (float)(1.055 * Math.pow(rgb[1],(1.0/2.4)) - 0.055);
		else
			srgb[1] = (float)(12.92 * rgb[1]);
		if ( rgb[2] > 0.00304f )
			srgb[2] = (float)(1.055 * Math.pow(rgb[2],(1.0/2.4)) - 0.055);
		else
			srgb[2] = (float)(12.92 * rgb[2]);
		
		return srgb;
	}
	
	
	public static float[][] loadPaletteLab(InputStream stream) 
		throws FileNotFoundException {
		float[][] labmap = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		
		Vector<float[]> map = new Vector<float[]>();
		try {
			String str=br.readLine();
			while(str!=null) { 
				StringTokenizer tzer = new StringTokenizer(str);
				float[] rgb = new float[3];
				for (int c=0;c<3;c++) {
					int ii = Integer.parseInt(tzer.nextToken());
					rgb[c]=(float)ii/255f;
				}
				float[] lab=sRGBtoLab(rgb);
				map.add(lab);
				str = br.readLine();
			}
		} catch (Exception exc) {
			System.err.println("loadPaletteLab: "+exc);
		} finally {
			
			labmap=new float[map.size()][3];
			for (int i=0;i<map.size();i++) {
				float[] lab = (float[])map.get(i);
				for (int c=0;c<3;c++) 
					labmap[i][c]=lab[c];
			}
			
		}
		
		return labmap;
	}
	
	public static byte[][] loadPalette(InputStream stream)
		throws FileNotFoundException {
			byte[][] colormap = null;
			BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			
			Vector<int[]> map = new Vector<int[]>();
			try {
				
				while(true) { // just catch the exception at the end of the file
					String str = br.readLine();
					StringTokenizer tzer = new StringTokenizer(str);
					int[] rgb = new int[3];
					for (int c=0;c<3;c++) 
						rgb[c] = Integer.parseInt(tzer.nextToken());
					map.add(rgb);
				}
			} catch (Exception exc) {
				
				colormap=new byte[3][map.size()];
				for (int i=0;i<map.size();i++) {
					int[] rgb = (int[])map.get(i);
					for (int c=0;c<3;c++) 
						colormap[c][i]=(byte)rgb[c];
				}
				
			}
			
			return colormap;
	}
	
 // ------------------------------- INTERFACE -------------------------------------
	
  public static Frame getNonClearingFrame(String name, Component c) {
    final Frame f = new Frame(name) {
      /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	public void update(Graphics g) { paint(g); }
    };
    sizeContainerToComponent(f, c);
    centerFrame(f);
    f.setLayout(new BorderLayout());
    f.add(c, BorderLayout.CENTER);
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { f.dispose(); }
    });
    return f;
  }
  
  public static void sizeContainerToComponent(Container container,
      Component component) {
    if (container.isDisplayable() == false) container.addNotify();
    Insets insets = container.getInsets();
    Dimension size = component.getPreferredSize();
    int width = insets.left + insets.right + size.width;
    int height = insets.top + insets.bottom + size.height;
    container.setSize(width, height);
  }
  
	/** Centers the window in the screen */
  public static void centerFrame(Frame f) {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension d = f.getSize();
    int x = (screen.width - d.width) / 2;
    int y = (screen.height - d.height) / 2;
    f.setLocation(x, y);
  }
	/** Places the window to the right of the reference window */
	public static void placeRightTo(Frame f, Frame reference) {
		Dimension d = reference.getSize();
		Point origin = reference.getLocation();
		f.setLocation((int)(origin.getX()+d.getWidth()),(int)(origin.getY()));
	}

	public static void fitBetween(Frame f, Frame left, Frame right) {
		fitBetween(f, left, right, 0);
	}


	public static void fitBetween(Frame f, Frame left, Frame right, int minSize) {
		Dimension d = left.getSize();
		Point origin = left.getLocation();
		Point maxr = right.getLocation();
		double size = Math.max(maxr.getX()-origin.getX()-d.getWidth(),minSize);
		f.setSize((int) size,
						Toolkit.getDefaultToolkit().getScreenSize().height);
		f.setLocation((int)(origin.getX()+d.getWidth()),(int)(origin.getY()));
	}
	
	/** Moves the window to the right-most part of the screen */
	public static void placeRight(Frame f) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = f.getSize();
		Point origin = f.getLocation();
		f.setLocation((int)(screen.width-d.getWidth()),(int)(origin.getY()));		
	}

	/** Places the window just below the reference window */
	public static void placeBelow(Frame f, Frame reference) {
		Dimension d = reference.getSize();
		Point origin = reference.getLocation();
		f.setLocation((int)(origin.getX()),(int)(origin.getY()+d.getHeight()));		
	}
	
	/** Puts the window on the bottom right of the screen */
	public static void placeBottomRight(Frame f) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = f.getSize();
		int x = (screen.width - d.width);
		int y = (screen.height - d.height);
		f.setLocation(x, y);		
	}
	/** Puts the window on the bottom left of the screen */
	public static void placeBottomLeft(Frame f) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = f.getSize();
		//int x = (screen.width - d.width);
		int y = (screen.height - d.height);
		f.setLocation(0, y);		
	}
	
	// TIME
	public static String dateStamp() {
		Calendar c = new GregorianCalendar();
		int year = c.get(Calendar.YEAR)-2000;
		int month = c.get(Calendar.MONTH)+1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		String date = "";
		date+=(year<10)?"0"+year:year;
		date+=(month<10)?"0"+month:month;
		date+=(day<10)?"0"+day:day;
		return date;
	}

}