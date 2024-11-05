//
//  CursorController.java
//  sketchRC
//
//  Created by David Gavilan on 19//07.
//  Copyright 2007 __MyCompanyName__. All rights reserved.
//
package titech.wt;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Timer;
import java.util.TimerTask;

/**
  * http://www.catalysoft.com/articles/busyCursor.html
  */
public class CursorController {
    public static final Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);
    public static final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
    public static final int delay = 500; // in milliseconds
	
    private CursorController() {}
    
    public static ActionListener createListener(final Component component, final ActionListener mainActionListener) {
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                
                TimerTask timerTask = new TimerTask() {
                    public void run() {
                        component.setCursor(busyCursor);
                    }
                };
                Timer timer = new Timer(); 
                
                try {   
                    timer.schedule(timerTask, delay);
                    mainActionListener.actionPerformed(ae);
                } finally {
                    timer.cancel();
                    component.setCursor(defaultCursor);
                }
            }
        };
        return actionListener;
    }
}
