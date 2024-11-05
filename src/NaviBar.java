import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import titech.util.*;

/**
 * @author     David Gavilan
 * @created    2005/10/14
 */
public class NaviBar
		 extends JPanel
		 implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final static int ICON_WIDTH = 28;
	public final static int ICON_HEIGHT = 28;
	
	
	Window parentWindow = null;
	JTextArea log = null;
	ToolBar toolBar = null;

	String resultDir = "drawings";
	File[] fileList = null;
	int currentImage = -1;

	/**
	 *Constructor for the NaviBar object
	 */
	public NaviBar() {

		// interface
		setLayout(new FlowLayout());

		JButton startB = new JButton("START");
		//startB.setPreferredSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
		startB.setActionCommand("start");
		startB.addActionListener(this);

		JButton nextB = new JButton(new ImageIcon(this.getClass().getResource(
				"/resources/next.png")));
		nextB.setPreferredSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
		nextB.setActionCommand("next");
		nextB.addActionListener(this);
				
		
		add(startB);
		add(nextB);
		
	}


	public void setToolBar(ToolBar tb) {
		toolBar = tb;
	}

		
	/**
	 *  Sets the parentWindow attribute of the NaviBar object
	 *
	 * @param  pw  The new parentWindow value
	 */
	public void setParentWindow(Window pw) {
		parentWindow = pw;
	}


	/**
	 *  Sets the log attribute of the NaviBar object
	 *
	 * @param  l  The new log value
	 */
	public void setLog(JTextArea l) {
		log = l;
	}

	
	/** This method should be called before advancing to the next image,
	  * and before suddenly exiting, if the user wants to!
	  */
	public void saveCurrent() {
		int ji = currentImage+1;
		//String fname = fileList[currentImage].getName();
		// save the sketch in the current dir
		//fname = Utilities.clearExtension(fname)+".sketch.png";
		String fname = resultDir+File.separator+"drawing"+
		  ((ji < 10) ? "0" + ji : "" + ji)+".png";
		
		
		try {
			// create the directory
			File tt = new File(resultDir);
			if (!tt.exists()) tt.mkdir();
			
			tt=new File(fname);
			if (!tt.exists()) {			
				Utilities.saveImage(toolBar.getDrawing(),"png",new File(fname));
			} else {
				print("You already drew this before.");
			}
		} catch (Exception exc) {
			print("Saving: "+exc);
		}
	}		
	
	/**
	 * ActionListener implementation
	 *
	 * @param  e  Description of the Parameter
	 */
	public void actionPerformed(java.awt.event.ActionEvent e) {


	}


	/**
	 * Appends some text in the log output, and scrolls down the text
	 *
	 * @param  text  Description of the Parameter
	 */
	public void print(String text) {
		log.append(text + "\n");
		log.setCaretPosition(log.getDocument().getLength());
		System.out.println(text);
	}
}

