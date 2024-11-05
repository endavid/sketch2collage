//
//  CompositeButton.java
//  sketchRC
//
//  Created by David Gavilan on 10/6/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//
package titech.wt;

import titech.util.*;
import javax.swing.*;

public class CompositeButton extends JButton {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ValuePair vp;
	
	
	public CompositeButton(Icon icon, ValuePair vp) {
		super(icon);
		this.vp = vp;
	}

	public ValuePair getValuePair() {
		return vp;
	}
}
