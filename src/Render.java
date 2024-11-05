//
//  Render.java
//  sketchRC
//
//  Created by David Gavilan on 1/12/07.
//  Copyright 2007 Nakajima Lab. All rights reserved.
//

import java.awt.image.*;


/**
 * This class is used to render the final result.
 * It accumulates the processes and then, given an image size, renders the final result.
 */
public class Render {
	

	/**
	 * This methods pulls the chain of transformations and renders the final result.
	 * The result is always a square image. Cut it later.
	 */
	public BufferedImage render(int size) {
		BufferedImage out = new BufferedImage(size, size, 
											  BufferedImage.TYPE_INT_ARGB);
		
		
		return out;
	}
}
