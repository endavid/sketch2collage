package titech.file;

import java.io.File;
import java.util.Hashtable;

public class ImageFileFilter implements java.io.FileFilter {
    private Hashtable<String, ImageFileFilter> filters = null;

    /**
     * Creates a file filter. If no filters are added, then all
     * files are accepted.
     *
     * @see #addExtension
     */
    public ImageFileFilter() {
	this.filters = new Hashtable<String, ImageFileFilter>();
    }
	
    
    /**
     * Adds a filetype "dot" extension to filter against.
     *
     * For example: the following code will create a filter that filters
     * out all files except those that end in ".jpg" and ".tif":
     *
     *   ExampleFileFilter filter = new ExampleFileFilter();
     *   filter.addExtension("jpg");
     *   filter.addExtension("tif");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     */
    public void addExtension(String extension) {
	if(filters == null) {
	    filters = new Hashtable<String, ImageFileFilter>(5);
	}
	filters.put(extension.toLowerCase(), this);
    }
    
    public boolean accept(File f) {
	if(f != null) {
	    if(f.isDirectory()) {
		return false;
	    }
	    String extension = getExtension(f);
	    if(extension != null && filters.get(getExtension(f)) != null) {
		return true;
	    };
	}
	return false;
    }

    /**
     * Return the extension portion of the file's name .
     *
     * @see #getExtension
     * @see FileFilter#accept
     */
     public String getExtension(File f) {
	if(f != null) {
	    String filename = f.getName();
	    int i = filename.lastIndexOf('.');
	    if(i>0 && i<filename.length()-1) {
		return filename.substring(i+1).toLowerCase();
	    };
	}
	return null;
    }

}
