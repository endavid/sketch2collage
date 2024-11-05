//
//  Retrieval.java
//  sketchRC
//
//  Created by David Gavilan on 7/9/06.
//  Copyright 2006 Tokyo Tech. All rights reserved.
//

import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.swing.*;
import titech.file.*;
import titech.util.*;
import titech.image.*;
import titech.wt.*;

/**
 * Retrieval.
 *
 * This class should contain methods to read and access DB (local, remote..)
 * and thread to update the result components.
 */
public class Retrieval implements Runnable {
	public static final String INDEX_NAME = ".blobs";
	public static final String THUMBSDIR = ".bthumbs";
	
	public static final int TIMEOUT = 10;
	
	ImageFileFilter filter;

	double regionThreshold = Double.MAX_VALUE;
	
	Vector objectDB;
	Vector retrieved;
	
	double posW = 0.25;
	double volW = 0.25;
	double colW = 0.25;
	double oriW = 0.25;
	
	/** Object to be queried */
	Region query;
	
	/** Panel where to render the results */
	ScrollableJPanel sjpanel;
	
	
	public Retrieval(Region query) {
		objectDB = new Vector();
		retrieved = new Vector();
		this.query = query;
		
		filter = new ImageFileFilter();
		filter.addExtension("jpg");
		filter.addExtension("png");
		filter.addExtension("jpeg");
		filter.addExtension("bmp");
		filter.addExtension("tiff");
	}
	
	public Retrieval() {
		this(null);
	}
	
	public void setQuery(Region query) {
		this.query = query;
	}
	
	public void setSJPanel(ScrollableJPanel sjpanel) {
		this.sjpanel = sjpanel;
	}
	
	
	public ScrollableJPanel getSJPanel() {
		return sjpanel;
	}
	
	public Vector getObjectDB() {
		return objectDB;
	}
	
	public void setObjectDB(Vector objectDB) {
		this.objectDB = objectDB;
	}
	
	public void addLocalDB(String path) throws java.io.FileNotFoundException,
		java.io.IOException {
		File indexFile = new File(path+File.separator+INDEX_NAME);

		System.out.println(path);
		File[] fileList = FileUtils.ls(path,filter);
		// we assume that the index is up to date...
		
		BufferedReader reader = new BufferedReader(new FileReader(indexFile));
				
		for (int i = 0; i < fileList.length; i++) {
			File f = fileList[i];
			String descriptor = reader.readLine();
			//System.out.println(f+": "+descriptor);
			ObjectImage obi = new ObjectImage(descriptor);
			obi.setLocation(f.getAbsolutePath());
			objectDB.add(obi);
		}
		
		reader.close();
		
		System.out.println("Retrieval: "+objectDB.size()+" images in the DB.");
	}
	
	/** Retrieves a list of Regions from the objectDB, sorted by distance.
	  * The Vector returned uses ValuePair, where
	  *   - index:  image number
	  *   - value:  distance to the queried object
	  *   - ilabel: index of the matching region (== palette index + 1)
	  *   - label:  location of the segmented image
	  */
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
	
	public void cloneFrom(Retrieval ret) {
		this.setObjectDB(ret.getObjectDB());
		this.setSJPanel(ret.getSJPanel());
		this.colW = ret.colW;
		this.posW = ret.posW;
		this.volW = ret.volW;
		this.oriW = ret.oriW;
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
			while (((query == null) || (objectDB.size()==0))&&timeout<TIMEOUT){
				Thread.sleep(500); //half second
				timeout++;
			}
			
			if (timeout < TIMEOUT) {
				
				retrieveRegions(query);
		
				for (int i=0;i<3;i++) {
					ValuePair vp = (ValuePair) retrieved.get(i);
					System.out.println("retrieved :"+vp);
					
					if (sjpanel != null) {
						BufferedImage bim = Utilities.loadImage(
											ObjectImage.thumbFromLocation(vp.label));
						BufferedImage original = Utilities.loadImage(vp.label);
						//bim = Utilities.maskIndexedImage(bim,vp.ilabel+1);
						bim = Utilities.maskIndexedImage(bim,vp.ilabel+1,original);
						CompositeButton b = new CompositeButton(new ImageIcon(bim),vp);
						b.addActionListener(sjpanel);
						sjpanel.add(b);
					}
				}
				if (sjpanel != null) sjpanel.validate();
			} else {
				String error = "";
				if (objectDB.size()==0) error+="No DB. ";
				if (query == null) error+="No query object. ";
				System.err.println("Retrieval thread: timeout! "+error);
			}
		
		} catch (Exception exc) {
			System.err.println("Retrieval thread: "+exc);
		}
		// end of the thread
	}
}
