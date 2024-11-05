//
//  ModListener.java
//  sketchRC
//
//  Created by David Gavilan on 12/5/06.
//  Copyright 2006 NakajimaLab. All rights reserved.
//
package titech.wt;

/**
* This interface should be implemented for those objects who can be notified
* of any kind of typical application events
*/
public interface ModListener {
	/** This is invoked whenever a clip area changes */
	public void clip();
	/** This is invoked whenever the clip area is directly the whole image */
	public void fullclip();
	/** This is invoked whenever the image is shifted in any direction */
	public void shifted();
	/** This is invoked whenever the object is moved */
	public void moved();
	/** Invoked when an image has been retrieved */
	public void imageRetrieved();
}
