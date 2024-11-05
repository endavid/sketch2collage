//
//  Retrieval.java
//  sketchRC
//
//  Created by David Gavilan on 14/12/06.
//  Copyright 2006 Tokyo Tech. All rights reserved.
//

import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.swing.*;
import titech.file.*;
import titech.util.*;
import titech.image.*;
import titech.image.math.*;
import titech.wt.*;

/**
 * ImageRetrieval.
 *
 * This class should contain methods to read and access DB (local, remote..)
 * and thread to update the result components.
 */
public class ImageRetrieval implements Runnable {
	public static final String INDEX_NAME = ".histogram";
	public static final String THUMBSDIR = ".bthumbs";
	
	private int[] equivalences = new int[] {
		0, 1, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6,
		7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10
	};
	
	public static final int TIMEOUT = 10;
	
	ImageFileFilter filter;

	double regionThreshold = Double.MAX_VALUE;
	
	double[][] imageDB;
	double[][] univHistoDB;
	String[] paths;
	Vector retrieved;
	
	double posW = 0;
	double volW = 0.2;
	double oriW = 0.2;
	double colW = 0.6;
	
	/** Object to be queried */
	double[] query;
	
	/** Panel where to render the results */
	ScrollableJPanel sjpanel;
	
	/** To notify of changes in sjpanel */
	ModListener modListener;
	
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

		System.out.println(path);
		File[] fileList = FileUtils.ls(path,filter);
		// we assume that the index is up to date...
		
		BufferedReader reader = new BufferedReader(new FileReader(indexFile));
				
		imageDB = new double[fileList.length][];
		univHistoDB = new double[fileList.length][];
		paths = new String[fileList.length];
		for (int i = 0; i < fileList.length; i++) {
			File f = fileList[i];
			String descriptor = reader.readLine();
			//System.out.println(f+": "+descriptor);
			imageDB[i]=AMath.vectorDouble(descriptor);
			univHistoDB[i]=universalHistogram(imageDB[i]);
			paths[i]=f.getAbsolutePath();
		}
		
		reader.close();
		
		System.out.println("ImageRetrieval: "+imageDB.length+" images in the DB.");
	}
	
	
	double[] universalHistogram(double[] h) {
		double[] univHisto = new double[11];
		for (int i=0;i<ObjectImage.CHISTO_SIZE;i++) {
			int cat = equivalences[i];
			univHisto[cat]+=h[ObjectImage.RHISTO_SIZE+i];
		}
		return univHisto;
	}
	
	/** Retrieves a list of Regions from the objectDB, sorted by distance.
	  * The Vector returned uses ValuePair, where
	  *   - index:  image number
	  *   - value:  distance to the queried object
	  *   - ilabel: index of the matching region (== palette index + 1)
	  *   - label:  location of the segmented image
	  *
	public Vector retrieveRegions(Region query) {
		retrieved.clear();
		
		for (int i = 0; i < objectDB.size(); i++) {
			ObjectImage obi = (ObjectImage)objectDB.get(i);
			obi.setWeights(posW, volW, colW, oriW);
			int index = obi.minDistanceRegion(query);
			if (obi.getMinDist()<regionThreshold) {
				retrieved.add(new ValuePair(i, obi.getMinDist(), 
									index, obi.getLocation()));
			}
		}
		Collections.sort(retrieved);
		
		return retrieved;
	}
	
	public void cloneFrom(ImageRetrieval ret) {
		this.setObjectDB(ret.getObjectDB());
		this.setSJPanel(ret.getSJPanel());
		this.colW = ret.colW;
		this.posW = ret.posW;
		this.volW = ret.volW;
		this.oriW = ret.oriW;
	}
	*/
	
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
			
			TreeMap<Double,Integer> treemap = new TreeMap<Double,Integer>();
			for (int i=0;i<imageDB.length;i++) {
				double pdist = AMath.distance(query,imageDB[i],0,8);
				double vdist = AMath.distance(query,imageDB[i],9,18);
				double sdist = AMath.distance(query,imageDB[i],19,21);
				double cdist = AMath.distance(query,imageDB[i],22,query.length-1);
				double univdist = AMath.distance(universalHistogram(query),univHistoDB[i]);
				double distance = posW*pdist+volW*vdist+oriW*sdist+colW*cdist*univdist;
				
				treemap.put(new Double(distance), new Integer(i));
			}
			
			if (sjpanel != null) {
				clear();
				
				for (int i=0;i<10;i++) {
					Double k = treemap.firstKey();
					Integer v = treemap.remove(k);
					System.out.println("Image distance: "+k);
					sjpanel.add(new JButton(
								new ImageIcon(
								Utilities.resize(
								Utilities.loadImage(paths[v.intValue()]),120))));					
				}
				
				/*
				sjpanel.add(new JButton(new ImageIcon(Utilities.resize(Utilities.loadImage(paths[pdist]),120))));
				sjpanel.add(new JButton(new ImageIcon(Utilities.resize(Utilities.loadImage(paths[vdist]),120))));
				sjpanel.add(new JButton(new ImageIcon(Utilities.resize(Utilities.loadImage(paths[sdist]),120))));
				sjpanel.add(new JButton(new ImageIcon(Utilities.resize(Utilities.loadImage(paths[cdist]),120))));
				 */
				
				if (modListener!=null) modListener.imageRetrieved();
				else sjpanel.validate();
			}
		
		} catch (Exception exc) {
			System.err.println("ImageRetrieval thread: "+exc);
		}
		// end of the thread
	}
}
