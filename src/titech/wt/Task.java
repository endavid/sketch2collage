//
//  Task.java
//  sketchRC
//
//  Created by David Gavilan on 16//07.
//  Copyright 2007 __MyCompanyName__. All rights reserved.
//
package titech.wt;

public interface Task {
	
	/** Returns a message with current task status */
	public String getMessage();
	
	/** Checks if the task has finished */
	public boolean isDone();
	
	/** Starts the task */
	public void go();
	
	/** Stops current task */
	public void halt();

	public int getLengthOfTask();
	
	/** Current progress of the task */
	public int getCurrent();
}
