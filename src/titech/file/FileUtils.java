package titech.file;

import java.io.*;
import java.util.*;

public class FileUtils extends java.lang.Object
{

	/**
	 * Returns a sorted file list.
	 */
	public static File[] ls(String path, FileFilter filter) {
		File sf = new File(path);
		File[] fileList = sf.listFiles(filter);
		// sort the list
		Vector<File> fileVector = new Vector<File>(fileList.length);
		for (int i = 0;i<fileList.length;i++) fileVector.add(fileList[i]);
		Collections.sort(fileVector);
		for (int i = 0;i<fileList.length;i++) fileList[i]=(File)fileVector.get(i);
		
		return fileList;
	}
	
	/**
	 * Returns a sorted file list, including all the subdirs (Recursive).
	 * Ignore hidden dirs and "thumbs".
	 */
	public static File[] lsR(String path, FileFilter filter) {
		File[] list = null;
		try {
			File sf = new File(path);
			if (!sf.exists()) return null;
			
			File[] fileList = sf.listFiles(filter);
			Vector<File> fileVector = new Vector<File>(fileList.length);
			for (int i = 0;i<fileList.length;i++) fileVector.add(fileList[i]);
			fileList = sf.listFiles(); // unfiltered, to get dirs
			for (int i = 0;i<fileList.length;i++) {
				File ff = fileList[i];
				String ss = ff.getCanonicalPath();
				if (ff.isDirectory() && !ss.startsWith(".") && !ss.contains("thumb")) {
					File[] ffList = lsR(ff.getAbsolutePath(), filter); //R
					for (int j = 0;j<ffList.length;j++) fileVector.add(ffList[j]);
				}
			}

			//sort
			Collections.sort(fileVector);
			list = new File[fileVector.size()];
			for (int i = 0;i<list.length;i++) list[i]=(File)fileVector.get(i);
		} catch (Exception e) {
			System.err.println("lsR: "+e);
		} 
		return list;	
	}

	/**
	 * Returns a sorted directory list, including all the subdirs (Recursive).
	 * Ignore hidden dirs and "thumbs".
	 */
	public static File[] lsDirsR(String path) {
		File[] list = null;
		try {
			File sf = new File(path);
			if (!sf.exists()) return null;
			
			File[] fileList = sf.listFiles();
			Vector<File> fileVector = new Vector<File>(fileList.length);
			for (int i = 0;i<fileList.length;i++) {
				File ff = fileList[i];
				String ss = ff.getCanonicalPath();
				if (ff.isDirectory() && !ss.startsWith(".") && !ss.contains("thumb")) {
					fileVector.add(ff);
					File[] ffList = lsDirsR(ff.getAbsolutePath()); //R
					for (int j = 0;j<ffList.length;j++) fileVector.add(ffList[j]);
				}
			}
			//sort
			Collections.sort(fileVector);
			list = new File[fileVector.size()];
			for (int i = 0;i<list.length;i++) list[i]=(File)fileVector.get(i);
		} catch (Exception e) {
			System.err.println("lsDirsR: "+e);
		} 
		return list;	
	}
	
	/** Given a file with paths written on it, returns the grand total of filtered files */ 
	public static int lsLength(File locationList, FileFilter filter) {
		int total = 0;
		try { 
			BufferedReader br = new BufferedReader(new FileReader(locationList));
			String path = br.readLine();
			while (path!=null && path.length() != 0) {
				File sf = new File(path);
				File[] fileList = sf.listFiles(filter);
				total += fileList.length;
				path = br.readLine();
			}
		} catch (Exception e) {
		
		}
		return total;
	}

	
	/**
	*  Gets the extension of a file, that is, just the substring after the last '.',
	 *  in lower case. It also returns the colon!
	 *
	 * @param  file  Description of the Parameter
	 * @return       The extension value
	 */
	public final static String getExtension(String file) {
		int pos = file.lastIndexOf('.');
		return file.substring(pos).toLowerCase();
	}
	
	/** Returns the file name without the extension part */
	public final static String nameWOExtension(String file) {
		int pos = file.lastIndexOf('.');
		return file.substring(0,pos);
	}
	
	public static void main(String[] args) {
		File[] list = ls(args[0],null);
		for (int i=0;i<list.length;i++) {
			System.out.println(list[i].getAbsolutePath());
		}
	}
	
} // -- end class FileUtils

