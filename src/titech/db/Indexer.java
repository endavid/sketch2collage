//
//  Indexer.java
//  sketchRC
//
//  Created by David Gavilan on 16//07.
//  Copyright 2007 __MyCompanyName__. All rights reserved.
//
package titech.db;

import java.awt.image.*;
import java.io.*;

import titech.image.math.*;
import titech.image.dsp.*;
import titech.file.*;
import titech.wt.*;
import titech.util.*;

public class Indexer implements Runnable, Task {
	
	String path;
	boolean done;
	boolean halt;
	String statMessage;
	int current;
	
	ImageFileFilter filter;
	File[] fileList;
	File locationList;
	
	ImageRetrieval iretrieval;
	Retrieval retrieval;
	private boolean isDirectory;
	/** Use this if you wanna force rebuilding the indeces */
	private boolean rebuild;
	
	public Indexer(String path) {
		this(path, null, null);
	}
	
	public Indexer(String path, Retrieval retrieval, ImageRetrieval iretrieval) {
		this.path = path;
		this.retrieval = retrieval;
		this.iretrieval = iretrieval;
		isDirectory = false;
		rebuild = false;
		done = false;
		halt = false;
		current = 0;
		
		filter = new ImageFileFilter();
		filter.addExtension("jpg");
		filter.addExtension("png");
		filter.addExtension("jpeg");
		filter.addExtension("bmp");
		filter.addExtension("tiff");
		
		fileList = null;
		locationList = null;
		File f = new File(path);
		if (f.isDirectory()) {
			isDirectory = true;
			// get the list in the thread (run)
			//fileList = FileUtils.ls(path,filter);
		} else {
			locationList = f;
		}
	}
	
	/** Check if the task is finished */
	public boolean isDone() {
		return done;
	}
	
	/**
	  * Returns the most recent status message, or null
	  * if there is no current status message.
	  */
    public String getMessage() {
        return statMessage;
    }
	
	public void go() {
		Thread t = new Thread(this);
		t.start();
	}
	
	public void halt() {
		halt = true;
	}
	
	public int getLengthOfTask() {
		if (isDirectory) {
			File[] ll=FileUtils.lsR(path, filter);
			return ll.length;
		} else if (locationList != null) 
			return FileUtils.lsLength(locationList, filter);
		return 0;
	}
	public int getCurrent() {
		return current;
	}
	
	/**
	 * Returns true if the index has been created.
	 * False if the index files already existed.
	 */
	public boolean indexDir(String currentPath) throws IOException {
		
		// load the colormap for the color histogram
		//interpret("resource /resources/histogram.palette");
		
		File indexFile = new File(currentPath+File.separator+Retrieval.INDEX_NAME);
		File histogramFile = new File(currentPath+File.separator+ImageRetrieval.INDEX_NAME);
		boolean sameDate = (indexFile.lastModified()>=(new File(currentPath)).lastModified());
		if (indexFile.exists() && histogramFile.exists() && sameDate && !rebuild) {
			// give up, since it's already computed
			// The consistency is only checked by sameDate!
			return false;
		}
		
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
		
		Segmenter segmenter = new Segmenter();
		for (int i = 0; i < fileList.length; i++) {
			if (halt) break;
			current++;
			
			File f = fileList[i];
			
			BufferedImage img = Utilities.loadImage(f.getAbsolutePath());
			statMessage = currentPath+"\n";
			statMessage += "indexing... "+(i+1)+"/"+fileList.length;
			ObjectImage objectImage = segmenter.getKMedianRegions(img);
			
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
			
			Utilities.saveImage(segmenter.getSegmentedImage(), "png", 
					  currentPath+File.separator+Retrieval.THUMBSDIR+
					  File.separator+FileUtils.nameWOExtension(f.getName())+".png");
			
			// save also the color-clustered thumbnail for convenience
			Utilities.saveImage(segmenter.getQuantizedImage(), "png", 
					  currentPath+File.separator+Retrieval.THUMBSDIR+
					  File.separator+FileUtils.nameWOExtension(f.getName())+"-ccat.png");			
		}
		
		iWriter.close();
		
		
		return true;
	}

	
	public void run() {
		try {
			long totalTime = 0;
			long time = System.currentTimeMillis();

			if (isDirectory) { // recusively index everything
				File[] recList = FileUtils.lsDirsR(path);
				for (int i=0;i<recList.length;i++) {
					String dpath = recList[i].getAbsolutePath();
					fileList = FileUtils.ls(dpath,filter);
					indexDir(dpath);
					if (halt) break;
					if (retrieval!=null) retrieval.addLocalDB(dpath);
					if (iretrieval!=null) iretrieval.addLocalDB(dpath);
				}
			} else { // read the file that contains the locations
				BufferedReader br = new BufferedReader(new FileReader(locationList));
				path = br.readLine();
				while (path!=null) {
					fileList = FileUtils.ls(path,filter);
					indexDir(path);
					if (halt) break;
					if (retrieval!=null) retrieval.addLocalDB(path);
					if (iretrieval!=null) iretrieval.addLocalDB(path);
					path = br.readLine();
				}
			}
			totalTime += System.currentTimeMillis() - time;
			double t = (double) totalTime / 1000.0;
			if (!halt) {
				statMessage = "Computed index for "+fileList.length
								   +" images in "+t+" secs. in "+path;			
			} else {
				statMessage = "Indexing interrupted";
			}
			done = true;
		} catch (FileNotFoundException e) {
			statMessage = "Location not found!";
		} catch(Exception e) {
			statMessage = "Indexing interrupted";
		}
	} // end run
}
