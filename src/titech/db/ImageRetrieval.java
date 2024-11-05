//
//  Retrieval.java
//  sketchRC
//
//  Created by David Gavilan on 14/12/06.
//  Copyright 2006 Tokyo Tech. All rights reserved.
//
package titech.db;


import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.swing.*;
import titech.file.*;
import titech.util.*;
import titech.image.*;
import titech.image.math.*;
import titech.wt.*;
import titech.image.dsp.*;

/**
 * ImageRetrieval.
 *
 * This class should contain methods to read and access DB (local, remote..)
 * and thread to update the result components.
 */
public class ImageRetrieval implements Runnable {
	public static final String INDEX_NAME = ".histogram";
	public static final String THUMBSDIR = ".bthumbs";
	public static final double[] DEFAULT_WEIGHTS=new double[] {
		   0.840067, 0.438318, 0.015638, 1.125841
	};
	
	private int[] equivalences = new int[] {
		0, 1, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6,
		7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10
	};
	
	public static final int TIMEOUT = 10;
	
	/** Distance threshold for retrieval. If distance > th, not retrieved */
	public double threshold = 1.0;
	/** Maximum number of images to display */
	public int maxImages = 10;
	
	ImageFileFilter filter;

	
	double[][] imageDB;
	double[][] univHistoDB;
	String[] paths;
	Vector retrieved;
	
	/** Weights learned with the ordered logit */
	public double posW = DEFAULT_WEIGHTS[0];
	public double volW = DEFAULT_WEIGHTS[1];
	public double oriW = DEFAULT_WEIGHTS[2];
	public double colW = DEFAULT_WEIGHTS[3];

//	public double posW = 0;
//	public double volW = 0.2;
//	public double oriW = 0.2;
//	public double colW = 0.6;
	
	
	/** Object to be queried */
	double[] query;
	
	/** Panel where to render the results */
	ScrollableJPanel sjpanel;
	
	/** To notify of changes in sjpanel */
	ModListener modListener;
	
	JTextArea log = null;
	
	JTextField textField = null;
		
	public ImageRetrieval(double[] query) {
		imageDB = null;
		retrieved = new Vector();
		this.query = query;
		
		filter = new ImageFileFilter();
		filter.addExtension("jpg");
		filter.addExtension("png");
		filter.addExtension("jpeg");
		filter.addExtension("bmp");
		filter.addExtension("tiff");
	}
	
	public ImageRetrieval() {
		this(null);
	}
	
	public int getLength() {
		if (paths==null) return 0;
		return paths.length;
	}
	public void setTextField(JTextField textField) {
		this.textField = textField;
	}
	
	public void setLog(JTextArea l) {
		log = l;
	}
	
	public void setModListener(ModListener modListener) {
		this.modListener = modListener;
	}
	
	public void setQuery(double[] query) {
		this.query = query;
	}
	
	public void setSJPanel(ScrollableJPanel sjpanel) {
		this.sjpanel = sjpanel;
	}
	
	
	public ScrollableJPanel getSJPanel() {
		return sjpanel;
	}
	
	public double[][] getImageDB() {
		return imageDB;
	}
	
	public void setImageDB(double[][] imageDB) {
		this.imageDB = imageDB;
	}
		
	public void addLocalDB(String path) throws java.io.FileNotFoundException,
		java.io.IOException {
		File indexFile = new File(path+File.separator+INDEX_NAME);

		print(path);
		File[] fileList = FileUtils.ls(path,filter);
		// we assume that the index is up to date...
		
		BufferedReader reader = new BufferedReader(new FileReader(indexFile));
		
		int initialSize = 0;
		if (imageDB != null) {
			initialSize = imageDB.length;
		}
		
		double[][] idb = new double[initialSize+fileList.length][];
		double[][] hdb = new double[initialSize+fileList.length][];
		String[] ps = new String[initialSize+fileList.length];
		for (int i = 0; i < fileList.length; i++) {
			File f = fileList[i];
			String descriptor = reader.readLine();
			//System.out.println(f+": "+descriptor);
			idb[initialSize+i]=AMath.vectorDouble(descriptor);
			hdb[initialSize+i]=universalHistogram(idb[initialSize+i]);
			ps[initialSize+i]=f.getAbsolutePath();
		}
		
		if (initialSize>0) {
			for (int i=0;i<initialSize;i++) {
				idb[i]=imageDB[i];
				hdb[i]=univHistoDB[i];
				ps[i]=paths[i];
			}
		}
		
		imageDB = idb;
		univHistoDB = hdb;
		paths = ps;
		
		reader.close();
		
		print("ImageRetrieval: "+imageDB.length+" images in the DB.");
	}
	
	
	double[] universalHistogram(double[] h) {
		double[] univHisto = new double[11];
		for (int i=0;i<ObjectImage.CHISTO_SIZE;i++) {
			int cat = equivalences[i];
			univHisto[cat]+=h[ObjectImage.RHISTO_SIZE+i];
		}
		return univHisto;
	}
	

	
	/** Clear all the retrieved elements in the retrieval window */
	public void clear() {
		sjpanel.removeAll();
		sjpanel.repaint();
	}
	
	/**
	 * This thread manages the interactive retrievals.
	 * We should start one thread for every Region to be retrieved. 
	 */
	public void run() {
		try {
			
			// wait for a query
			int timeout=0;
			while (((query == null) || (imageDB==null))&&timeout<TIMEOUT){
				Thread.sleep(500); //half second
				timeout++;
			}
			
			// build a treemap ordered by the weighed distance and the index j
			// paths[j] to get the location
			// For color, use the product of the distance to the normal histogram
			//            and the distance to the reduced histogram (11 bins).
			//            -- use equivalences[52] in {0,10} 
			//int pdist = AMath.findMin(query,imageDB,0,8);
			//int vdist = AMath.findMin(query,imageDB,9,18);
			//int sdist = AMath.findMin(query,imageDB,19,21);
			//int cdist = AMath.findMin(query,imageDB,22,query.length-1);
			//System.out.println("HDistances: "+pdist+", "+vdist+", "+sdist+", "+cdist);
			String keyword = (textField==null)?"":textField.getText();
			long time = System.currentTimeMillis();
			TreeMap<Double,Integer> treemap = new TreeMap<Double,Integer>();
			for (int i=0;i<imageDB.length;i++) {
				// check first keywords
				if (keyword=="" || paths[i].contains(keyword)) {
					double pdist = AMath.distance(query,imageDB[i],0,8);
					double vdist = AMath.distance(query,imageDB[i],9,18);
					double sdist = AMath.distance(query,imageDB[i],19,21);
					double cdist = AMath.distance(query,imageDB[i],22,query.length-1);
					double univdist = AMath.distance(universalHistogram(query),univHistoDB[i]);
					double distance = posW*pdist+volW*vdist+oriW*sdist+colW*cdist*univdist;
					
					if (distance < threshold) {
						treemap.put(new Double(distance), new Integer(i));					
					}
				} // else, keyword didn't match, so don't add
			}
			time = System.currentTimeMillis() - time;
			double t = (double) time / 1000.0;
			print("Image query time: "+t+" secs.");

			
			if (sjpanel != null) {
				clear();
				
				int i=0;
				String minmax = "";
				Double k = null;
				while (i<maxImages && treemap.size()>0) {
					k = treemap.firstKey();
					Integer v = treemap.remove(k);
					if (i==0) minmax+=k;
					String loc = paths[v.intValue()];
					String preview = ObjectImage.previewFromLocation(loc);
					File fp = new File(preview);
					BufferedImage icon = null;
					if (fp.exists()) {
						icon = Utilities.loadImage(preview);
					} else {
						icon = Utilities.resize(Utilities.loadImage(loc),120);
						// important that the images are INT_RGB! no alpha for JPEG!
						Utilities.saveImage(icon, Utilities.getExtension(loc), fp);
					}
					// ilabel = -1 to do a fullclip
					ValuePair vp = new ValuePair(0,0,-1,loc);
					CompositeButton b = new CompositeButton(new ImageIcon(icon), vp);
					b.addActionListener(sjpanel);
					sjpanel.add(b);
					i++;
				}
				minmax+=", "+k;
				print("Image distance interval: ("+minmax+")");
								
				if (modListener!=null) modListener.imageRetrieved();
				else sjpanel.validate();
			}
			
		} catch (Exception exc) {
			print("ImageRetrieval thread: "+exc);
		}
		// end of the thread
	}
	
	/**
		* Appends some text in the log output, and scrolls down the text
	 *
	 * @param  text  Description of the Parameter
	 */
	public void print(String text) {
		if (log == null) {
			System.out.println(text);			
		} else {
			log.append(text + "\n");
			log.setCaretPosition(log.getDocument().getLength());			
		}
	}
}
