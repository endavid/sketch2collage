package titech.image;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *  A JPanel that implements the Scrollable interface. So, it may contain more
 *  components which is added size is bigger than the size of this component.
 *  <p>
 *  Example:<p>
 *  <pre>
 *  	ScrollableJPanel spanel = new ScrollableJPanel();
		for (int i = 0; i < 20; i++) {
			spanel.add(new JButton("" + i));
		}
		
		JScrollPane scroller = new JScrollPane(spanel,
			JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setPreferredSize(new Dimension(100,80));
 * </pre>
 * 
 * @author     David Gavilan
 * @created    2003/12/14
 */
public class ScrollableJPanel extends JPanel implements Scrollable, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int minXIncrement = 600;
	int minYIncrement = 600;

	public ScrollableJPanel() {
		super();
	}
	
	public ScrollableJPanel(LayoutManager layout) {
		super(layout);
	}

	/**
	 *  Description of the Method
	 *
	 * @param  comp  Description of the Parameter
	 * @return       Description of the Return Value
	 */
	public Component add(Component comp) {
		Component c = super.add(comp);
		Dimension d = comp.getPreferredSize();
		if (d.width/2 < minXIncrement) {
			minXIncrement = d.width/2;
		}
		if (d.height/2 < minYIncrement) {
			minYIncrement = d.height/2;
		}
		return c;
	}


	// Scrollable
	// ---------------------------------------------------------------------------------
	/**
	 *  Returns the preferred size of the viewport for a view component.
	 *
	 * @return    The preferredScrollableViewportSize value
	 */
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}


	/**
	 *Components that display logical rows or columns should compute the scroll increment that will completely expose one block of rows or columns, depending on the value of orientation.
	 *
	 * @param  visibleRect  Description of the Parameter
	 * @param  orientation  Description of the Parameter
	 * @param  direction    Description of the Parameter
	 * @return              The scrollableBlockIncrement value
	 */
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.VERTICAL) {
			return minYIncrement;
		}
		return minXIncrement;
	}


	/**
	 *Return true if a viewport should always force the height of this Scrollable to match the height of the viewport.
	 *
	 * @return    The scrollableTracksViewportHeight value
	 */
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}


	/**
	 *Return true if a viewport should always force the width of this Scrollable to match the width of the viewport.
	 *
	 * @return    The scrollableTracksViewportWidth value
	 */
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}


	/**
	 *Components that display logical rows or columns should compute the scroll increment that will completely expose one new row or column, depending on the value of orientation.
	 *
	 * @param  visibleRect  Description of the Parameter
	 * @param  orientation  Description of the Parameter
	 * @param  direction    Description of the Parameter
	 * @return              The scrollableUnitIncrement value
	 */
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.VERTICAL) {
			return minYIncrement;
		}
		return minXIncrement;
	}
	
	// ActionListener
	public void actionPerformed(ActionEvent e) {
		System.out.println(e.getSource().getClass().getSimpleName());
	}
}

