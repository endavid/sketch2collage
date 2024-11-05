//
//  Retrieval.java
//  sketchRC
//
//  Created by David Gavilan on 7/9/06.
//  Copyright 2006 Tokyo Tech. All rights reserved.
//
package titech.db;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import titech.file.*;
import titech.util.*;
import titech.image.*;
import titech.wt.*;
import titech.image.dsp.*;

/**
 * Retrieval.
 *
 * This class should contain methods to read and access DB (local, remote..)
 * and thread to update the result components.
 */
public class Retrieval implements Runnable {
	public static final String INDEX_NAME = ".blobs";
	public static final String THUMBSDIR = ".bthumbs";
	public static final String PREVIEWDIR = "thumbs";
	
	public static final int TIMEOUT = 10;
	
	ImageFileFilter filter;

	double regionThreshold = 1.0;
	/** Maximum number of images to display */
	public int maxImages = 10;	
	
	Vector<ObjectImage> objectDB;
	Vector<ValuePair> retrieved;
	/** List of directories and locations */
	Vector<String> locations;
	
	public double posW = 0.25;
	public double volW = 0.25;
	public double colW = 0.25;
	public double oriW = 0.25;
	
	/** Object to be queried */
	Region query;
	
	/** Panel where to render the results */
	ScrollableJPanel sjpanel;
	JTextArea log = null;
	private ActionListener busyListener;
	JTextField textField = null;

		
	/** Size of the scrollable components */
	Dimension componentSize;
	
	public Retrieval(Region query) {
		objectDB = new Vector<ObjectImage>();
		retrieved = new Vector<ValuePair>();
		locations = new Vector<String>();
		this.query = query;
		componentSize = new Dimension(300,100);
		
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
	
	public void setWidth(int w) {
		componentSize = new Dimension(w, 110);
	}
	
	public void setTextField(JTextField textField) {
		this.textField = textField;
	}
	
	public void setLog(JTextArea l) {
		log = l;
	}

	public void setQuery(Region query) {
		this.query = query;
	}
	
	public void setSJPanel(ScrollableJPanel sjpanel) {
		this.sjpanel = sjpanel;
		busyListener = CursorController.createListener(sjpanel.getFocusCycleRootAncestor(), sjpanel);
	}
	
	
	public ScrollableJPanel getSJPanel() {
		return sjpanel;
	}
	
	public Vector<String> getLocations() {
		return locations;
	}
	
	public Vector<ObjectImage> getObjectDB() {
		return objectDB;
	}
	
	public void setObjectDB(Vector<ObjectImage> objectDB) {
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
		locations.add(path);
		
		print("Retrieval: "+objectDB.size()+" images in the DB.");
	}
	
	/** Retrieves a list of Regions from the objectDB, sorted by distance.
	  * The Vector returned uses ValuePair, where
	  *   - index:  image number
	  *   - value:  distance to the queried object
	  *   - ilabel: index of the matching region (== palette index + 1)
	  *   - label:  location of the segmented image
	  */
	@SuppressWarnings("unchecked")
	public Vector<ValuePair> retrieveRegions(Region query) {
		retrieved.clear();
		
		String keyword = (textField==null)?"":textField.getText();

		for (int i = 0; i < objectDB.size(); i++) {
			ObjectImage obi = objectDB.get(i);
			// check first keywords
			if (keyword=="" || obi.getLocation().contains(keyword)) {
				obi.setWeights(posW, volW, colW, oriW);
				int index = obi.minDistanceRegion(query);
				if (obi.getMinDist()<regionThreshold) {
					retrieved.add(new ValuePair(i, obi.getMinDist(), 
										index, obi.getLocation()));
				}
			} // else no matching keyword
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
		this.componentSize = ret.componentSize;
		this.log = ret.log;
		this.textField = ret.textField;
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
				
				long time = System.currentTimeMillis();
				retrieveRegions(query);
				time = System.currentTimeMillis() - time;
				double t = (double) time / 1000.0;
				print("Object query time: "+t+" secs.");
		
				ScrollableJPanel srpanel = new ScrollableJPanel();
				
				JScrollPane scroller = new JScrollPane(srpanel,
														JScrollPane.VERTICAL_SCROLLBAR_NEVER,
														JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scroller.setPreferredSize(componentSize);
				
				String rr="/resources/fileclose.png";
				JButton closeButton = new JButton(new ImageIcon(this.getClass().getResource(rr)));
				closeButton.addActionListener(busyListener);
				srpanel.add(closeButton);
				
				int i = 0;
				String minmax = "";
				double distance = 0;
				while (i<maxImages && i<retrieved.size()) {
					ValuePair vp = retrieved.get(i);
					distance = vp.value;
					if (i==0) minmax+=distance;
					i++;					
					if (sjpanel != null) {
						BufferedImage bim = Utilities.loadImage(
											ObjectImage.thumbFromLocation(vp.label));
						String preview = ObjectImage.previewFromLocation(vp.label);
						File fp = new File(preview);
						BufferedImage icon = null;
						if (fp.exists()) {
							icon = Utilities.loadImage(preview);
						} else {
							icon = Utilities.loadImage(vp.label);
							Utilities.saveImage(Utilities.resize(icon,120),
												Utilities.getExtension(vp.label), fp);
						}
						//bim = Utilities.maskIndexedImage(bim,vp.ilabel+1);
						bim = Utilities.maskIndexedImage(bim,vp.ilabel+1,icon);
						CompositeButton b = new CompositeButton(new ImageIcon(bim),vp);
						b.addActionListener(busyListener);
						srpanel.add(b);
					}
				}
				minmax+=", "+distance;
				print("Region distance interval: ("+minmax+")");
				if (sjpanel != null) {
					sjpanel.add(scroller);
					sjpanel.getTopLevelAncestor().validate();	
				}
			} else {
				String error = "";
				if (objectDB.size()==0) error+="No DB. ";
				if (query == null) error+="No query object. ";
				print("Retrieval thread: timeout! "+error);
			}
		
		} catch (Exception exc) {
			print("Retrieval thread: "+exc);
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
