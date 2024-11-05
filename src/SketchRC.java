import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.net.URL;
import java.io.*;
import titech.image.*;
import titech.image.dsp.ObjectImage;
import titech.image.dsp.Segmenter;
import titech.image.matting.*;
import titech.util.Utilities;
import titech.util.ValuePair;
import titech.wt.*;
import titech.db.ImageRetrieval;
import titech.db.Retrieval;
import titech.db.Indexer;
import titech.file.FileUtils;


/**
 * S2C: Sketch-to-Collage,
 *   An image database based painting program. 
 *
 *
 * This is the main application window. It uses Swing for the interface.
 * Requires Java 1.4 or greater.
 *
 *
 * If you want the default Look&Feel:
 *
 *   java -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel -jar SketchIt.jar
 *
 * 0.92  Load/Save sketches.
 * 0.91  Directories can be added recursively now.
 * 0.90  NO_OPTIMIZATION works again. Changed default weights of image retrieval. 
 * 0.89  Change brush size for the clipping; select different optimization function for boundary.
 * 0.88  The clipping method has been substituted by a big brush to select the area of interest.
 * 0.87  Moved all the parameters to a common tabbed Parameter Window. 
 * 0.86  Added resize buttons for the clip window.
 * 0.85  Regions bordering (0,0) are properly segmented; full objects are extracted (not clipped) 
 * 0.84  Dragging is now (at last) centered around the point where you start dragging 
 * 0.83  Pressing SHIFT while selecting an image will start a NEW collage with that image. You can also start a new collage with the selected background color.
 * 0.82  Hard segmentation improved a bit by using the L*a*b* color distance instead of RGB.
 * 0.81  No more wrong offsets when moving the image in the clip canvas. 
 * 0.80  "New from image" option lets you directly load a background picture for the collage. 
 * 0.79  Can read a text file containing a list of directories to import, "Add list of locations".
 * 0.78  [daniel] Images are now being loaded in Windows too.
 * 0.77  [daniel] Solved the flickering problem on Win/Lin. Optimization progress not showed now.
 * 0.76  [daniel] Dragging works now on Windows and Linux too.
 * 0.75  For non-Mac systems, the menu's placed below the canvas now (it appeared hidden before!)
 * 0.74  Queries can be filtered by a single keyword. Must be encoded somehow in the file's path.
 * 0.73  Hard segmentation computation delayed to a thread. If failed, show the rough one.
 * 0.72  Option "show locations". Lists added directories (no remote locations available yet)
 * 0.71  Composite window size and object size (os) are now independent. Move respect (os/2,os/2).
 * 0.70  "New collage" option. Fixed list of resolutions (squared and panoramic)
 * 0.69  Save function added at last! All the collages are saved in PNG format.
 * 0.68  Busy cursor added when clicking over a retrieved segment. Non-blocking, though.
 * 0.67  Solved a bug in "discardRegions" that caused empty segment selections.
 * 0.66  Progress window to check the status of a directory being indexed.
 * 0.65  "Add folder" indexes a new image directory now. 
 * 0.64  Segmentation works fine with using "load".
 * 0.63  Roughly segments images loaded with the "load" command.
 * 0.62  Now folders can be added, and the program can be started without image folder.
 * 0.61  Corrected wrong ratio when resizing images in the clip canvas.
 * 0.60  Poisson went multithreading, one thread for color channel.
 * 0.59  Change accuracy of the Poisson optimization, from 0.1 to 0.001 - def. 0.05 (b4, 0.02)
 * 0.58  SOLVED all the boundary optimization BUGS?? I hope. Forced to create closed paths.
 * 0.57  Located boundary bug offset outside image! p.x+oy! everywhere instead of p.x+ox.
 * 0.56  Don't call the boundary update when it's not necessary (faster for alpha & hard)
 * 0.55  Added Intelligent Erode (Band). Applies boundary optimization around a specified band. 
 * 0.54  Parameter to change the size of the composite from the command line.
 * 0.53  Default segmentation updated after moving. It fails to capture the object sometimes.
 * 0.52  Segmentation works after resizing too
 * 0.51  Find better (higher res) segmentation with the first click
 * 0.50  Dilate lines of connectRegions, so the boundary optimization doesn't flood out.
 * 0.49  Log interval of distances for retrievals in log window.
 * 0.48  For faster retrieval, load thumbnails. Create them if they don't exist. C ImageRet.
 * 0.47  "Harder" right-click on color palette to avoid double retrievals. 
 * 0.46  Moved some files in the hierarchy; CVS updated
 * 0.45  Debug window. ConnectedRegions to avoid multiple paths in boundary optimization.
 * 0.44  BOUNDARY_OPTIONS and premature hires hard segmentation.
 * 0.43  Updated image retrieval (called twice??!) when clearing the canvas. thread.join(). 
 * 0.42  Lets you modify the images selected from the Image Retrieval window.
 * 0.41  Gradient mix slider. It goes from (0.01,0.99) to (2.0,0);
 * 0.40  Added image resize using MouseWheel to ClipCanvas. Boundary not updated to resize.
 * 0.39  Log moved to a separate Log Window (hidden by default). Parameter window moved.
 * 0.38  Option to disable boundary optimization. Interactively change it and poisson options.
 * 0.37  Object won't disappear after the first move.
 * 0.36  Press DEL in Composite to erase current object (can be recovered by editing the Clip)
 * 0.35  Added a parameter window for Poisson etc.
 * 0.34  Scrollable retrieval elements. Button to close retrieved elements.
 * 0.33  Added a simple alpha boundary
 * 0.32  Some optimizations for floodfill - boolean matrices.
 * 0.31  Limit the clipping boundaries to inside image boundaries and viewport.
 * 0.30  When making a clip selection, updates the object contour from the segmented image.
 * 0.29  Lets you select pix from Image Retrieval. Press 'w' to select the whole picture.
 * 0.28  Weighted gradient (multiples source gradient *2)
 * 0.27  Pressing 'g' in the clipcanvas will transform the image to gray.
 * 0.26  Sliders to adjust image retrieval parameters; default setting.
 * 0.25  Color histogram of 11 bins added (color equivalences). 
 * 0.24  Combined search as a simple weight of histograms. Returns 10 matches.
 * 0.23  Search by color histogram. Scroll bar in image retrieval window.
 * 0.22  Added an image retrieval window. It only uses the region histogram.
 * 0.21  Discrete/Continuous Palette menu. Added the discrete palette K to resources.
 * 0.20  Some boundary bugs solved (offset, 0 paths)
 * 0.19  Fixed the clipping outside bounds for Poisson
 * 0.18  Let the user modify the external boundary (lasso)
 * 0.17  Faster boundary optimization, Vector replaced by {TreeSet + matrices}
 * 0.16  Use SHIFT to drag Clipped and CTRL to transfer the whole image to Composite
 * 0.15  Load local files (to clipWindow)
 * 0.14  Added menu and SketchRC.properties (english locale)
 * 0.13  Offset of composite corrected. 
 * 0.12  Updates Poisson when moving the composite (new Boundary Thread). Wrong offsets!
 * 0.11  Poisson options ComboBox.
 * 0.10  Added GRADIENT_MIX to Poisson. Basic movement of clip area; still with bugs.
 * 0.09  Added Poisson blending
 * 0.08  Clipping Window showing some simple boundaries. Window layout.
 * 0.07  Composite Window
 * 0.06  Show in gray the part of the image not retrieved.
 * 0.05  Retrieve masked images; adjust retrieval parameters with sliders. 
 * 0.04  New window for retrieval (partial results show segmented images). Clean all code.
 * 0.03  Added retrieval threads (partial results shown in stdout)
 * 0.02  New color palette interface (color categories hues), redistributed components
 * 0.01  Branch from SketchIt 1.2 
 *
 * @author David Gavilan
 */
public class SketchRC extends JFrame implements ChangeListener, ActionListener, ModListener, ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static ResourceBundle resources;
	
    static {
        try {
            resources = ResourceBundle.getBundle("resources.SketchRC", 
                                                 Locale.getDefault());
        } catch (MissingResourceException mre) {
            System.err.println("resources/SketchRC.properties not found");
            System.exit(1);
        }
    }
	
	public static final String VERSION = "0.92";
	public static final int CANVAS_SIZE = 320;
	public static final int[] COMPOSITE_SIZES = new int[] {320, 480, 640, 800, 1024};
	public int compositeSize;
	public int defaultSize; 
	
	public static final String[] POISSON_OPTIONS = new String[] {
		"OFF",
		"Gradient import",
		"Gradient mix",
		"Weighted gradient",
		"Alpha"
	};
	
	public static final String[] BOUNDARY_OPTIONS = new String[] {
		"No optimization",
		"Hard segmentation",
		"Intelligent Scissors",
		"Intelligent Band"
	};
	
	public static final String[] GRADIENT_OPTIONS = new String[] {
		"Color difference",
		"Gradient of the difference"
	};
                               
	
	public static final String[] RESOLUTIONS = new String[] {
		"320x320",
		"600x800",
		"640x480",
		"800x320",
		"800x480",
		"800x600",
		"800x800",
		"1024x480",
		"1024x768",
		"1280x320",
		"1280x480",
		"1280x800"
	};
	
    JTextArea textArea;
	PaintCanvas paintCanvas;
	NaviBar naviBar;
	Retrieval retrieval;
	ImageRetrieval imageRetrieval;
	ToolBar toolBar;
	ScrollableJPanel sjpanel, isjpanel;
	JFrame retrievalWindow;
	JFrame iretrievalWindow;
	JFrame compositeWindow;
	JFrame clipWindow;
	JFrame parameterWindow;
	JFrame logWindow;
	JFrame debugWindow;
	CompositeCanvas compositeCanvas;
	ClipCanvas clipCanvas;
	JScrollPane scroller;
	SimpleCanvas debugCanvas;
	
	JTextField keywordField;
	JSlider colorSlider, icolorSlider;
	JSlider posSlider, iposSlider;
	JSlider volSlider, ivolSlider;
	JSlider oriSlider, ioriSlider;
	JComboBox poissonCombo;
	JComboBox boundaryCombo;
	JComboBox gradientCombo;
	JMenuBar menuBar;
	/** File dialog */
	JFileChooser fc;
	
	JButton clipBigger;
	JButton clipSmaller;	
	JSlider clipStrokeSlider;
	
	JCheckBox alphaCheck;
	JSlider gradientMixSlider;
	JSlider accuracySlider;
	
	Boundary boundary;
	Thread thread;
	Segmenter segmenter;
	
	javax.swing.Timer timer;
	Task task;
	ProgressMonitor progressMonitor;
	
	private boolean macOS;
	
	public SketchRC() {
		this(COMPOSITE_SIZES[0]);
	}
    /** Initialize the interface in the constructor. */
    public SketchRC(int compSize) {
		//Create a timer.
        timer = new javax.swing.Timer(1000, new TimerListener());
		
		macOS = System.getProperty("os.name").contains("Mac");

		defaultSize = 0;
		compositeSize = compSize;
		boundary = null;
		thread = null;
		segmenter = new Segmenter();
		
		fc = new JFileChooser(System.getProperty("user.home"));
		
		
		// Log window (hidden by default)
		// ------------------------------------------------
        textArea = new JTextArea(3,20);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
        JScrollPane pane = new JScrollPane(textArea);
		logWindow = new JFrame();
		logWindow.setTitle("Log");
		logWindow.add(pane);
		logWindow.pack();

				
		naviBar = new NaviBar();
		naviBar.setLog(textArea);
		paintCanvas=new PaintCanvas(CANVAS_SIZE);
		toolBar = new ToolBar();
		toolBar.setPaintCanvas(paintCanvas);
		toolBar.setParentWindow(this);
		toolBar.setLog(textArea);
		naviBar.setToolBar(toolBar);
		
		toolBar.setMode(ToolBar.PAINT);
		
		JSeparator jsep1 = new JSeparator(SwingConstants.VERTICAL);
		jsep1.setPreferredSize(new Dimension(10,10));
		JSeparator jsep2 = new JSeparator(SwingConstants.VERTICAL);
		jsep2.setPreferredSize(new Dimension(10,10));
		
		JPanel keywordPanel = new JPanel();
		keywordPanel.add(new JLabel("Keyword: "));
		keywordField = new JTextField(20);
		keywordPanel.add(keywordField);

        JPanel jpe = new JPanel(new BorderLayout());
		jpe.add(paintCanvas, BorderLayout.CENTER);
		JPanel tools = new JPanel(new BorderLayout());
		tools.add(toolBar, BorderLayout.CENTER);
		tools.add(keywordPanel, BorderLayout.SOUTH);
		jpe.add(tools, BorderLayout.SOUTH);
		
		
        JPanel jp = new JPanel(new BorderLayout()); // by default, FlowLayout
		//jp.add(naviBar, BorderLayout.NORTH);
		jp.add(jsep1, BorderLayout.WEST);
		jp.add(jpe, BorderLayout.CENTER);
		jpe.add(jsep2, BorderLayout.EAST);
 
        java.awt.Container contentPane = getContentPane();
        contentPane.add(jp);
		

		// menu bar
		// ---------------------------------------------
		menuItems = new Hashtable<String, JMenuItem>();
		menuBar = createMenuBar();
		// don't add menubar directly to panel; add to frame
		// to take advantage of screen menu bar if available
		// add("North", menubar);
		// take advantage of screen menu bar if available
		// Set the screen menu bar property before calling 
		// this constructor!!!
		if (macOS) {
			setJMenuBar(menuBar);			
		} else { // the menu bar is not visible because of the canvas in Windows/Linux
			tools.add(menuBar, BorderLayout.NORTH);
		}
	
		// composite Window
		// ----------------------------------------------
		compositeWindow = new JFrame();
		compositeWindow.setTitle("Composite");
		compositeCanvas = new CompositeCanvas(compositeSize);
		compositeCanvas.maxObjectWidth = compositeSize;
		compositeCanvas.maxObjectHeight = compositeSize;
		compositeCanvas.setModListener(this);
		compositeWindow.add(compositeCanvas);
		compositeWindow.pack();
		
		
		// clip Window
		// ----------------------------------------------
		clipCanvas = new ClipCanvas(compositeSize);
		clipCanvas.setModListener(this);
		JPanel clipPanel = new JPanel(new BorderLayout());
		//clipPanel.add(clipTools, BorderLayout.NORTH);
		clipPanel.add(clipCanvas, BorderLayout.CENTER);
		clipWindow = new JFrame();
		clipWindow.setTitle("Clipping");
		clipWindow.add(clipPanel);
		clipWindow.pack();
		
		
		// Retrieval window
		// -----------------------------------------------
		sjpanel = new ScrollableJPanel() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					String name = e.getSource().getClass().getSimpleName();
					if (name.equals("CompositeButton")) {
						CompositeButton b = (CompositeButton) e.getSource();
						ValuePair vp = b.getValuePair();
						
						if ((e.getModifiers() & ActionEvent.SHIFT_MASK)!=0) {
							newf(vp.label);
						} else {
							addToClipCanvas(vp);
						}
					} else if (name.equals("JButton")) { // there's only one: the close button
						JButton source = (JButton) e.getSource();
						sjpanel.remove(source.getParent().getParent().getParent());
						retrievalWindow.validate();
					}
				} catch (Exception exc) {
					System.out.println(exc);
				}
			}
		};
		
		scroller = new JScrollPane(sjpanel,
											JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
											JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
        //JPanel jpRetrieval = new JPanel(new BorderLayout());
		//jpRetrieval.add(sjpanel, BorderLayout.CENTER);
		retrievalWindow = new JFrame(); // SketchRC window is the owner
		retrievalWindow.setTitle("Retrieval");
		retrievalWindow.setLayout(new BorderLayout());
		retrievalWindow.add(scroller,BorderLayout.CENTER);

		// Parameter Window
		// ------------------------------------------
		setParameterWindow();

		
		//  Image Retrieval window
		// ------------------------------------------
		isjpanel = new ScrollableJPanel() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					if (e.getSource().getClass().getSimpleName().equals("CompositeButton")) {
						CompositeButton b = (CompositeButton) e.getSource();
						ValuePair vp = b.getValuePair();
						
						if ((e.getModifiers() & ActionEvent.SHIFT_MASK)!=0) {
							newf(vp.label);
						} else {
							addToClipCanvas(vp);
						}
					} 
				} catch (Exception exc) {
					System.out.println(exc);
				}
			}
		};
		
		JScrollPane iscroller = new JScrollPane(isjpanel,
											   JScrollPane.VERTICAL_SCROLLBAR_NEVER,
											   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		iscroller.setPreferredSize(new Dimension(CANVAS_SIZE+40,200));
		

		iretrievalWindow = new JFrame();
		iretrievalWindow.setTitle("Image match");
		iretrievalWindow.setLayout(new BorderLayout());
		iretrievalWindow.add(iscroller,BorderLayout.CENTER);
		iretrievalWindow.pack();
				
		
		// debug window to show partial results
		// -----------------------------------------------------
		debugCanvas = new SimpleCanvas(compositeSize);
		debugWindow = new JFrame();
		debugWindow.setTitle("Debug");
		debugWindow.add(debugCanvas);
		debugWindow.pack();
		
		
		// DO_NOTHING, or it will get blocked when CANCEL button is pressed
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {							
					exitAction();
				}
			});
    }
	
    void setParameterWindow() {
		parameterWindow = new JFrame();
		parameterWindow.setTitle("Parameters");
		JTabbedPane tabbedPane = new JTabbedPane();
		JLabel lb1 = new JLabel("Object substraction");
		Font font = lb1.getFont();
		Font smallerFont = new Font(font.getFamily(),Font.BOLD,10);
		tabbedPane.setFont(smallerFont);

		// boundary options
		// ---------------------------------------------------------
		boundaryCombo = new JComboBox(BOUNDARY_OPTIONS);
		boundaryCombo.setSelectedIndex(2); // intelligent scissors
		boundaryCombo.addActionListener(this);
		gradientCombo = new JComboBox(GRADIENT_OPTIONS);
		gradientCombo.addActionListener(this);
		JLabel lb2 = new JLabel("Optimization criterium");
		lb2.setFont(smallerFont);
		lb1.setFont(smallerFont);
		JSeparator sb1 = new JSeparator();
		JLabel lb3 = new JLabel("Clip Window size");
		lb3.setFont(smallerFont);
		clipBigger = new JButton("+");
		clipSmaller = new JButton("-");
		clipBigger.setFont(smallerFont);
		clipSmaller.setFont(smallerFont);
		clipBigger.addActionListener(this);
		clipSmaller.addActionListener(this);
		JLabel lb4 = new JLabel("Stroke size");
		lb4.setFont(smallerFont);
		clipStrokeSlider = new JSlider(5, 80, 50);
		clipStrokeSlider.addChangeListener(this);

		GridBagLayout gbBoundary = new GridBagLayout();
		GridBagConstraints cBoundary = new GridBagConstraints();
		JPanel boundaryPanel = new JPanel(gbBoundary);
		cBoundary.gridwidth = GridBagConstraints.REMAINDER;
		cBoundary.weightx = 1;
		gbBoundary.setConstraints(lb1, cBoundary);
		gbBoundary.setConstraints(boundaryCombo, cBoundary);
		gbBoundary.setConstraints(lb2, cBoundary);
		gbBoundary.setConstraints(gradientCombo, cBoundary);
		cBoundary.fill = GridBagConstraints.HORIZONTAL;
		gbBoundary.setConstraints(sb1, cBoundary);
		cBoundary.fill = GridBagConstraints.NONE;
		cBoundary.gridwidth = 1;
		gbBoundary.setConstraints(lb3, cBoundary);
		gbBoundary.setConstraints(clipBigger, cBoundary);
		cBoundary.gridwidth = GridBagConstraints.REMAINDER;
		gbBoundary.setConstraints(clipSmaller, cBoundary);
		cBoundary.gridwidth = 1;
		gbBoundary.setConstraints(lb4, cBoundary);
		cBoundary.gridwidth = GridBagConstraints.REMAINDER;
		cBoundary.fill = GridBagConstraints.HORIZONTAL;
		gbBoundary.setConstraints(clipStrokeSlider, cBoundary);

		boundaryPanel.add(lb1);
		boundaryPanel.add(boundaryCombo);
		boundaryPanel.add(lb2);
		boundaryPanel.add(gradientCombo);
		boundaryPanel.add(sb1);
		boundaryPanel.add(lb3);
		boundaryPanel.add(clipBigger);
		boundaryPanel.add(clipSmaller);
		boundaryPanel.add(lb4);
		boundaryPanel.add(clipStrokeSlider);

		// color blending options
		// ---------------------------------------------------------
		poissonCombo = new JComboBox(POISSON_OPTIONS);
		poissonCombo.addActionListener(this);
		gradientMixSlider = new JSlider(1,200,50);
		gradientMixSlider.addChangeListener(this);
		accuracySlider = new JSlider(1, 100, 50);
		accuracySlider.addChangeListener(this);
		alphaCheck = new JCheckBox("Alpha",false);
		alphaCheck.addItemListener(this);
		JLabel l1 = new JLabel("Poisson"); l1.setFont(smallerFont);
		JLabel l2 = new JLabel("Accuracy"); l2.setFont(smallerFont);
		JLabel l3 = new JLabel("Mixture amount"); l3.setFont(smallerFont);
		JSeparator s1 = new JSeparator();
		GridBagLayout gbBlend = new GridBagLayout();
		GridBagConstraints cBlend = new GridBagConstraints();
		JPanel blendPanel = new JPanel(gbBlend);
		cBlend.gridwidth = GridBagConstraints.REMAINDER;
		cBlend.weightx = 1;
		gbBlend.setConstraints(l1,cBlend);
		gbBlend.setConstraints(poissonCombo,cBlend);
		gbBlend.setConstraints(l2,cBlend);
		cBlend.fill = GridBagConstraints.HORIZONTAL;
		gbBlend.setConstraints(accuracySlider,cBlend);
		cBlend.fill = GridBagConstraints.NONE;
		gbBlend.setConstraints(l3,cBlend);
		cBlend.fill = GridBagConstraints.HORIZONTAL;
		gbBlend.setConstraints(gradientMixSlider,cBlend);
		gbBlend.setConstraints(s1, cBlend);
		cBlend.fill = GridBagConstraints.NONE;
		gbBlend.setConstraints(alphaCheck,cBlend);
		blendPanel.add(l1);
		blendPanel.add(poissonCombo);
		blendPanel.add(l2);
		blendPanel.add(accuracySlider);
		blendPanel.add(l3);
		blendPanel.add(gradientMixSlider);
		blendPanel.add(s1);
		blendPanel.add(alphaCheck);
		
		// retrieval options
		// ---------------------------------------------------------
		GridBagLayout gbRetrieval = new GridBagLayout();
		GridBagConstraints cRetrieval = new GridBagConstraints();
		JPanel retrievalPanel = new JPanel(gbRetrieval);
		iposSlider = new JSlider(0,100,
				(int)(100.*ImageRetrieval.DEFAULT_WEIGHTS[0]));
		ivolSlider = new JSlider(0,100,
				(int)(100.*ImageRetrieval.DEFAULT_WEIGHTS[1]));
		ioriSlider = new JSlider(0,100,
				(int)(100.*ImageRetrieval.DEFAULT_WEIGHTS[2]));
		icolorSlider = new JSlider(0,200,
				(int)(100.*ImageRetrieval.DEFAULT_WEIGHTS[3]));
		icolorSlider.addChangeListener(this);
		iposSlider.addChangeListener(this);
		ivolSlider.addChangeListener(this);
		ioriSlider.addChangeListener(this);
		colorSlider = new JSlider(0,100,25);
		posSlider = new JSlider(0,100,12);
		volSlider = new JSlider(0,100,25);
		oriSlider = new JSlider(0,100,25);
		colorSlider.addChangeListener(this);
		posSlider.addChangeListener(this);
		volSlider.addChangeListener(this);
		oriSlider.addChangeListener(this);
		Dimension minSize = new Dimension(90,30);
		colorSlider.setPreferredSize(minSize);
		posSlider.setPreferredSize(minSize);
		volSlider.setPreferredSize(minSize);
		oriSlider.setPreferredSize(minSize);
		icolorSlider.setPreferredSize(minSize);
		iposSlider.setPreferredSize(minSize);
		ivolSlider.setPreferredSize(minSize);
		ioriSlider.setPreferredSize(minSize);
		JLabel lr1 = new JLabel("Feature"); lr1.setFont(smallerFont);
		JLabel lr2 = new JLabel("Image"); lr2.setFont(smallerFont);
		JLabel lr3 = new JLabel("Object"); lr3.setFont(smallerFont);
		JSeparator sr1 = new JSeparator();

		JLabel icolorLabel = new JLabel("color",SwingConstants.CENTER);
		icolorLabel.setFont(smallerFont);
		JLabel iposLabel = new JLabel("x,y",SwingConstants.CENTER);
		iposLabel.setFont(smallerFont);
		JLabel ivolLabel = new JLabel("volume",SwingConstants.CENTER);
		ivolLabel.setFont(smallerFont);
		JLabel ioriLabel = new JLabel("shape",SwingConstants.CENTER);
		ioriLabel.setFont(smallerFont);
		cRetrieval.weightx = 1;
		cRetrieval.gridwidth = 1; cRetrieval.fill = GridBagConstraints.NONE;
		gbRetrieval.setConstraints(lr1,cRetrieval);
		gbRetrieval.setConstraints(lr2,cRetrieval);
		cRetrieval.gridwidth = GridBagConstraints.REMAINDER;
		gbRetrieval.setConstraints(lr3,cRetrieval);
		cRetrieval.fill = GridBagConstraints.HORIZONTAL;
		gbRetrieval.setConstraints(sr1,cRetrieval);
		cRetrieval.gridwidth = 1;
		gbRetrieval.setConstraints(icolorLabel,cRetrieval);
		cRetrieval.fill = GridBagConstraints.HORIZONTAL;
		gbRetrieval.setConstraints(icolorSlider,cRetrieval);
		cRetrieval.gridwidth = GridBagConstraints.REMAINDER;
		gbRetrieval.setConstraints(colorSlider,cRetrieval);		
		cRetrieval.gridwidth = 1; cRetrieval.fill = GridBagConstraints.NONE;
		gbRetrieval.setConstraints(iposLabel,cRetrieval);
		cRetrieval.fill = GridBagConstraints.HORIZONTAL;
		gbRetrieval.setConstraints(iposSlider,cRetrieval);
		cRetrieval.gridwidth = GridBagConstraints.REMAINDER;
		gbRetrieval.setConstraints(posSlider,cRetrieval);
		cRetrieval.gridwidth = 1; cRetrieval.fill = GridBagConstraints.NONE;
		gbRetrieval.setConstraints(ivolLabel,cRetrieval);
		cRetrieval.fill = GridBagConstraints.HORIZONTAL;
		gbRetrieval.setConstraints(ivolSlider,cRetrieval);
		cRetrieval.gridwidth = GridBagConstraints.REMAINDER;
		gbRetrieval.setConstraints(volSlider,cRetrieval);
		cRetrieval.gridwidth = 1; cRetrieval.fill = GridBagConstraints.NONE;
		gbRetrieval.setConstraints(ioriLabel,cRetrieval);
		cRetrieval.fill = GridBagConstraints.HORIZONTAL;
		gbRetrieval.setConstraints(ioriSlider,cRetrieval);
		cRetrieval.gridwidth = GridBagConstraints.REMAINDER;
		gbRetrieval.setConstraints(oriSlider,cRetrieval);
		retrievalPanel.add(lr1);
		retrievalPanel.add(lr2);
		retrievalPanel.add(lr3);
		retrievalPanel.add(sr1);
		retrievalPanel.add(icolorLabel);
		retrievalPanel.add(icolorSlider);
		retrievalPanel.add(colorSlider);
		retrievalPanel.add(iposLabel);
		retrievalPanel.add(iposSlider);
		retrievalPanel.add(posSlider);
		retrievalPanel.add(ivolLabel);
		retrievalPanel.add(ivolSlider);
		retrievalPanel.add(volSlider);
		retrievalPanel.add(ioriLabel);
		retrievalPanel.add(ioriSlider);
		retrievalPanel.add(oriSlider);

		
		tabbedPane.addTab("Boundary", boundaryPanel);
		tabbedPane.addTab("Blending", blendPanel);
		tabbedPane.addTab("Retrieval", retrievalPanel);
		JPanel container = new JPanel(new GridLayout(1,1));
		container.add(tabbedPane);
		parameterWindow.add(container, BorderLayout.CENTER);
		
		parameterWindow.pack();
    	
    }
    
	void addToClipCanvas(ValuePair vp) throws java.io.IOException, InterruptedException {
		// segmented thumbnail
		BufferedImage bim = Utilities.loadImage(
												ObjectImage.thumbFromLocation(vp.label));
		BufferedImage original = Utilities.loadImage(vp.label);
		
		BufferedImage ccat = Utilities.loadImage(
												 ObjectImage.ccatFromLocation(vp.label));

		addToClipCanvas(bim, original, ccat, vp.ilabel);
		System.out.println(vp);
	}
	
	void addToClipCanvas(BufferedImage bim,
						 BufferedImage original,
						 BufferedImage ccat,
						 int index) throws java.io.IOException, InterruptedException {
				
		//original = Utilities.resize(original, CANVAS_SIZE);
		// do the resize in the clipCanvas
		Dimension resized = Utilities.resizeDimension(original, compositeSize);

		
		clipCanvas.setMaxSize(resized);
		clipCanvas.setImage(original);
		if (index>=0) {
			boundary=new Boundary(bim, index+1, resized, 
								  clipCanvas.getClipArea());
			boundary.setPoissonType(poissonCombo.getSelectedIndex());
			boundary.setPoissonParameters(getMixtureValue(), 
										  getAccuracyValue());
			boundary.setComposite(compositeCanvas);
			boundary.setCanvas(clipCanvas);
			boundary.setDebugCanvas(debugCanvas);
			boundary.setSource(clipCanvas.getImage()); // don't clip it
			boundary.setTarget(compositeCanvas.getImage());
			boundary.setSegmented(ccat);
			boundary.setMethod(boundaryCombo.getSelectedIndex());
			boundary.alphaMatting = alphaCheck.isSelected();
			// add the object, whose contour will be updated in the Boundary thread
			// so add if BEFORE the thread starts
			compositeCanvas.add(boundary.maskImage());
			
			if (thread!=null) thread.join(); // wait for last operation to finish
			thread = new Thread(boundary);
			thread.start();
			// !!! It is never legal to start a thread more than once.
			// In particular, a thread may not be restarted once it 
			// has completed execution.			
		} else { // the whole image
			fullclip();
			boundary = new Boundary(bim, 0, resized, clipCanvas.getClipArea());
			boundary.setPoissonType(poissonCombo.getSelectedIndex());
			boundary.setPoissonParameters(getMixtureValue(), 
										  getAccuracyValue());
			boundary.setComposite(compositeCanvas);
			boundary.setCanvas(clipCanvas);
			boundary.setDebugCanvas(debugCanvas);
			boundary.setSource(clipCanvas.getClippedImage()); // grab clipped in the thread anyway
			boundary.setTarget(compositeCanvas.getImage());
			boundary.setSegmented(ccat);
			boundary.setMethod(boundaryCombo.getSelectedIndex());
			boundary.alphaMatting = alphaCheck.isSelected();
			// don't start the thread. Just prepare it just in case the user wanna cut something
		}
		
		//clipCanvas.addThumbnail(bim);
		//bim = Utilities.maskIndexedImage(bim,vp.ilabel+1,original,false,false);
		
	}
	

	/** Arrange them after creating the initial window (SketchRC), since
	  * its size is used as reference.
	  */
	public void arrangeWindows() {		
		Utilities.placeRight(compositeWindow);
		Utilities.placeBelow(clipWindow, compositeWindow);
		Utilities.fitBetween(retrievalWindow,this,compositeWindow, 320);
		Utilities.placeBelow(iretrievalWindow,this);
		//Utilities.placeBottomRight(parameterWindow);
		//Utilities.placeBelow(parameterWindow,iretrievalWindow);
		Utilities.placeBottomLeft(parameterWindow);
		sjpanel.setPreferredSize(new Dimension(retrievalWindow.getWidth()-18,2000));
		retrieval.setWidth(retrievalWindow.getWidth()-18);
		retrievalWindow.setVisible(true);
		clipWindow.setVisible(true);
		compositeWindow.setVisible(true);
		iretrievalWindow.setVisible(true);
		parameterWindow.setVisible(true);
	}
	
	// Implement ModListener 
	// -----------------------------------------------------
	public void fullclip() {
		if (thread!=null) {
			try {
				thread.join(); // wait for the thread to finish
			} catch (InterruptedException exc) {
				System.err.println("clip: "+exc);
			}
		}
		boundary = null;
		compositeCanvas.add(clipCanvas.getClippedImage());
	}
		
	public void clip() {
		if (boundary!=null) {
			if (thread!=null) {
				try {
					thread.join(); // wait for the thread to finish
				} catch (InterruptedException exc) {
					System.err.println("clip: "+exc);
				}
			}
			boundary.setExternalContour(clipCanvas.getContour());
			clipCanvas.moved = false;
			thread = new Thread(boundary);
			thread.start();
		}
	}
	
	/** The image in the clipCanvas has been displaced */
	public void shifted() {
		if (boundary!=null) {
			if (thread!=null) {
				try {
					thread.join(); // wait for the thread to finish
				} catch (InterruptedException exc) {
					System.err.println("clip: "+exc);
				}
			}
			clipCanvas.moved = false;
			boundary.update(clipCanvas.getClipArea());
			boundary.clearExternalContour();
			thread = new Thread(boundary);
			thread.start();
		}		
	}
	
	/** The object in the compositeCanvas has been moved */
	public void moved() {
		if (thread!=null) {
			try {
				thread.join(); // wait for the thread to finish
			} catch (InterruptedException exc) {
				System.err.println("clip: "+exc);
			}
		}
		clipCanvas.moved = false; // after the join() !
		// check if we need to update the boundary or the poisson optimization
		// CHANGED! update even if NO_OPTIMIZATION
		//if (poissonCombo.getSelectedIndex()>0 || boundaryCombo.getSelectedIndex()>1) {
			// if there is no contour, it will just set it to null
			boundary.setExternalContour(clipCanvas.getContour());
			thread = new Thread(boundary);
			thread.start();				
		//}
	}
	
	
	public void imageRetrieved() {
		//isjpanel.validate();
		iretrievalWindow.validate();
	}
	
	
	double getMixtureValue() {
		return (double)gradientMixSlider.getValue()/100.;
	}
	
	double getAccuracyValue() {
		return (double)accuracySlider.getValue()/1000.;
	}
	
	/**
	 *  Change Listener
	 */
	public void stateChanged(ChangeEvent e) {
		Object src = e.getSource();	
		if (src == gradientMixSlider || src == accuracySlider) {
			if (boundary != null) {
				boundary.setPoissonParameters(getMixtureValue(),
											  getAccuracyValue());
				if (poissonCombo.getSelectedIndex()==3) {
					moved();
				}
			}
		} else if (src == clipStrokeSlider) {
			clipCanvas.setStroke(clipStrokeSlider.getValue());
		} else if (retrieval != null) {
			if (src == colorSlider) {
				retrieval.colW = (double)colorSlider.getValue()/(double)colorSlider.getMaximum()/4.;
			} else if (src == posSlider) {
				retrieval.posW = (double)posSlider.getValue()/(double)posSlider.getMaximum()/4.;
			} else if (src == volSlider) {
				retrieval.volW = (double)volSlider.getValue()/(double)volSlider.getMaximum()/4.;
			} else if (src == oriSlider) {
				retrieval.oriW = (double)oriSlider.getValue()/(double)oriSlider.getMaximum()/4.;
			} else if (src == icolorSlider) {
				imageRetrieval.colW = 2.*(double)icolorSlider.getValue()/
						(double)icolorSlider.getMaximum();
			} else if (src == iposSlider) {
				imageRetrieval.posW = (double)iposSlider.getValue()/
						(double)iposSlider.getMaximum();		
			} else if (src == ivolSlider) {
				imageRetrieval.volW = (double)ivolSlider.getValue()/
						(double)ivolSlider.getMaximum();
			} else if (src == ioriSlider) {
				imageRetrieval.oriW = (double)ioriSlider.getValue()/
						(double)ioriSlider.getMaximum();
			}
		} else {
			System.err.println("SketchRC: No Retrieval object!");
		}
	}
	
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if (source == alphaCheck) {
			boundary.alphaMatting = alphaCheck.isSelected();
			moved();
		}
		
	}
	
	
	public void setRetrieval(Retrieval ret) {
		retrieval = ret;
		while (paintCanvas == null) {
			try {
				Thread.sleep(10);
			} catch(Exception exc) {
				System.out.println("SketchRC: "+exc);
			}
		}
		ret.setSJPanel(sjpanel);
		ret.setLog(textArea);
		ret.setTextField(keywordField);
		paintCanvas.setRetrieval(ret);
	}
	
	public void setImageRetrieval(ImageRetrieval iret) {
		imageRetrieval = iret;
		while (paintCanvas == null) {
			try {
				Thread.sleep(10);
			} catch(Exception exc) {
				System.out.println("SketchRC: "+exc);
			}
		}
		iret.setSJPanel(isjpanel);
		iret.setModListener(this);
		iret.setLog(textArea);
		iret.setTextField(keywordField);
		paintCanvas.setImageRetrieval(iret);
	}
	
	/** Ask for unsaved data, and exit. */
	public void exitAction() {
		int n = JOptionPane.showOptionDialog(this.getFocusCycleRootAncestor(),
			"Do you want to save your current collage?","Before you leave...",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null, null,	null);
			
		switch (n) {
			case JOptionPane.YES_OPTION:
				//naviBar.saveCurrent();
				saveAction();
				System.exit(0);
			
			case JOptionPane.NO_OPTION:
				System.exit(0);
			
			default:
				// just do nothing (CANCEL)
		}
		
	}
	
	public void renderAction() {
		
	}
	
	/** Clears the collage and creates a new one with the selected resolution */
	void newCollage(int width, int height) {
		compositeCanvas.setbgColor(paintCanvas.getbgColor());
		compositeCanvas.setSize(width,height);
		compositeCanvas.repaint();
		compositeWindow.pack();
	}
	
	void load(String file) throws java.io.IOException, InterruptedException {
		BufferedImage original = Utilities.loadImage(file);
		segmenter.getKMedianRegions(original);
		debugCanvas.add(segmenter.getSegmentedImage());
		
		addToClipCanvas(segmenter.getSegmentedImage(), 
						original,
						segmenter.getQuantizedImage(),-1); 
		// do the resize in the clipCanvas
		//Dimension resized = Utilities.resizeDimension(original, COMPOSITE_SIZE);
		//clipCanvas.setMaxSize(resized);
		//clipCanvas.setImage(original);
	}
	
	void loadSketch(String file) throws java.io.IOException, InterruptedException {
		BufferedImage original = Utilities.loadImage(file);
		paintCanvas.set(original);
	}
	
	/** Saves the collage to disk */
	void save(String file) throws java.io.IOException, InterruptedException {
		String format = "png";
		BufferedImage collage = compositeCanvas.getCollage();
		Utilities.saveImage(collage,format,file+"."+format);
	}
	
	/** Saves the sketch to disk */
	void saveSketch(String file) throws java.io.IOException, InterruptedException {
		String format = "png";
		BufferedImage sketch = paintCanvas.getImage();
		Utilities.saveImage(sketch,format,file+"."+format);
	}

	
	void newAction() {
		String s = (String)JOptionPane.showInputDialog(
													   this.getFocusCycleRootAncestor(),
													   "Resolution:",
													   "New collage",
													   JOptionPane.QUESTION_MESSAGE,
													   null,
													   RESOLUTIONS,
													   RESOLUTIONS[0]);
		//If a string was returned, clear the canvas
		if ((s != null) && (s.length() > 0)) {
			StringTokenizer stk = new StringTokenizer(s,"x");
			newCollage(Integer.parseInt(stk.nextToken()),
					   Integer.parseInt(stk.nextToken()));
		}
	}
	
	/**
		* Opens a file-load requester to load an image
	 */
	void loadAction() {
		try {
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			int returnVal = fc.showOpenDialog(this.getFocusCycleRootAncestor());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();
				
				load(path);
			} else {
				System.out.println("Open command cancelled by user.");
			}
		} catch (Exception e) {
			System.err.println("load: " + e );
		}
	}
	
	void loadSketchAction() {
		try {
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			int returnVal = fc.showOpenDialog(this.getFocusCycleRootAncestor());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();
				
				loadSketch(path);
			} else {
				System.out.println("Open command cancelled by user.");
			}
		} catch (Exception e) {
			System.err.println("load sketch: " + e );
		}
	}
	
	void newf(String path) throws java.io.IOException {
		BufferedImage original = Utilities.loadImage(path);
		newCollage(original.getWidth(),original.getHeight());
		compositeCanvas.setBackground(original);		
	}
	
	/**
	 * Opens a file-load requester to load an image
	 */
	void newfAction() {
		try {
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			int returnVal = fc.showOpenDialog(this.getFocusCycleRootAncestor());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();
				
				newf(path);
			} else {
				System.out.println("Open command cancelled by user.");
			}
		} catch (Exception e) {
			System.err.println("load: " + e );
		}
	}
	
	/**
	 * Opens a file-save requester to save current collage
	 */
	void saveAction() {
		try {
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
			int returnVal = fc.showSaveDialog(this.getFocusCycleRootAncestor());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();
				
				save(path);
			} else {
				System.out.println("Save command cancelled by user.");
			}
		} catch (Exception e) {
			System.err.println("save: " + e );
		}
	}
	
	void saveSketchAction() {
		try {
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
			int returnVal = fc.showSaveDialog(this.getFocusCycleRootAncestor());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();
				
				saveSketch(path);
			} else {
				System.out.println("Save command cancelled by user.");
			}
		} catch (Exception e) {
			System.err.println("save sketch: " + e );
		}
	}
	
	/** Shows the list of locations */
	void showAction() {
		Vector locations = retrieval.getLocations();
		String locs = "";
		if (imageRetrieval!=null) {
			locs+=imageRetrieval.getLength()+" images in the database.\n";
		}
		for (int i=0;i<locations.size();i++) {
			locs+=(String)locations.get(i)+"\n";
		}
		JOptionPane.showMessageDialog(this.getFocusCycleRootAncestor(),
									  locs, "List of locations", 
									  JOptionPane.INFORMATION_MESSAGE);
	}
	
	/** Adds a directory to the DB of images */
	void adddirAction() {
		try {
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			int returnVal = fc.showOpenDialog(this.getFocusCycleRootAncestor());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();
				
				task = (Task)(new Indexer(path, retrieval, imageRetrieval));
				
				progressMonitor = new ProgressMonitor(retrievalWindow,
													  "Indexing directory "+path,
													  "", 0, task.getLengthOfTask());
				progressMonitor.setProgress(0);
				progressMonitor.setMillisToDecideToPopup(2000);
				
				task.go();
				timer.start();				
			} else {
				System.out.println("Open command cancelled by user.");
			}
		} catch (Exception e) {
			System.err.println("add folder: " + e );
		}
	}
	
	void addlocationAction() {
		
	}

	void addlistAction() {
		try {
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			int returnVal = fc.showOpenDialog(this.getFocusCycleRootAncestor());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String path = (fc.getSelectedFile()).getAbsolutePath();
				
				task = (Task)(new Indexer(path, retrieval, imageRetrieval));
				
				progressMonitor = new ProgressMonitor(retrievalWindow,
													  "Indexing directories "+path,
													  "", 0, task.getLengthOfTask());
				progressMonitor.setProgress(0);
				progressMonitor.setMillisToDecideToPopup(2000);
				
				task.go();
				timer.start();				
			} else {
				System.out.println("Open command cancelled by user.");
			}
		} catch (Exception e) {
			System.err.println("add folder: " + e );
		}
	
	}
	/**
	 * As this class is implementing an <b>ActionListener</b>, we watch for events
	 * in this method. We want to know about the menu events.   
	 * The names of the item events are the same as those defined in the resource
	 * properties.
	 */
	public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
		// file menu
		if (menuItems.get("new")==source) {
			newAction();
		} else if (menuItems.get("newf")==source) {
			newfAction();
		} else if (menuItems.get("load")==source) {
			loadAction();
		} else if (menuItems.get("save")==source) {
			saveAction();
		} else if (menuItems.get("loads")==source) {
			loadSketchAction();
		} else if (menuItems.get("saves")==source) {
			saveSketchAction();
		} else if (menuItems.get("render")==source) {
			renderAction();
		} else if (menuItems.get("exit")==source) {
			exitAction();
			
		// db menu
		} else if (menuItems.get("show")==source) {
			showAction();
		} else if (menuItems.get("addlist")==source) {
			addlistAction();
		} else if (menuItems.get("adddir")==source) {
			adddirAction();
		} else if (menuItems.get("addlocation")==source) {
			addlocationAction();
			
		// palette menu
		} else if (menuItems.get("continuous")==source) {
			toolBar.setContinuousPalette();
		} else if (menuItems.get("discrete")==source) {
			toolBar.setDiscretePalette();
		
		// window menu
		} else if (menuItems.get("parameters")==source) {
			parameterWindow.setVisible(true);
		} else if (menuItems.get("iretrieval")==source) {
			iretrievalWindow.setVisible(true);
		} else if (menuItems.get("retrieval")==source) {
			retrievalWindow.setVisible(true);
		} else if (menuItems.get("composite")==source) {
			compositeWindow.setVisible(true);
		} else if (menuItems.get("clip")==source) {
			clipWindow.setVisible(true);
		} else if (menuItems.get("log")==source) {
			logWindow.setVisible(true);
		} else if (menuItems.get("debug")==source) {
			debugWindow.setVisible(true);
		} else if (source == poissonCombo) {
			if (boundary != null) {
				boundary.setPoissonType(poissonCombo.getSelectedIndex());
				moved();
			}
		} else if (source == boundaryCombo) {
			if (boundary != null) {
				boundary.setMethod(boundaryCombo.getSelectedIndex());
				moved();
			}
		} else if (source == gradientCombo) {
			if (boundary != null) {
				boundary.setGradientMethod(gradientCombo.getSelectedIndex());
				moved();
			}
		// clip buttons
		} else if (source == clipBigger) {
			if (defaultSize<COMPOSITE_SIZES.length-1) {
				defaultSize++;
				compositeSize = COMPOSITE_SIZES[defaultSize];
				compositeCanvas.maxObjectWidth = compositeSize;
				compositeCanvas.maxObjectHeight = compositeSize;
				clipCanvas.setSize(compositeSize, compositeSize);
				clipWindow.pack();
			}
		} else if (source == clipSmaller) {
			if (defaultSize>0) {
				defaultSize--;
				compositeSize = COMPOSITE_SIZES[defaultSize];
				compositeCanvas.maxObjectWidth = compositeSize;
				compositeCanvas.maxObjectHeight = compositeSize;
				clipCanvas.setSize(compositeSize, compositeSize);
				clipWindow.pack();
			}			
		}
				
		
	} // end actionPerformed
	
	// ------------------------------------------------------- MENU
    private Hashtable<String, JMenuItem> menuItems;

	

	protected String getResourceString(String nm) {
		String str;
		try {
			str = resources.getString(nm);
		} catch (MissingResourceException mre) {
			str = null;
		}
		return str;
    }

	protected URL getResource(String key) {
		String name = getResourceString(key);
		if (name != null) {
			URL url = this.getClass().getResource(name);
			return url;
		}
		return null;
    }
	
	
    /**
		* Take the given string and chop it up into a series
     * of strings on whitespace boundries.  This is useful
     * for trying to get an array of strings out of the
     * resource file.
     */
    protected String[] tokenize(String input) {
		Vector<String> v = new Vector<String>();
		StringTokenizer t = new StringTokenizer(input);
		String cmd[];
		
		while (t.hasMoreTokens())
			v.addElement(t.nextToken());
		cmd = new String[v.size()];
		for (int i = 0; i < cmd.length; i++)
			cmd[i] = (String) v.elementAt(i);
		
		return cmd;
    }	
	
    protected JMenuItem createMenuItem(String cmd) {
		JMenuItem mi;
		String lab=getResourceString(cmd+"Label");
		String img=getResourceString(cmd+"Image");
		if(img!=null)
			mi=addItem(lab,img);
		else
			mi=addItem(lab);
		menuItems.put(cmd,mi); // nos lo guardamos en la tabla hash
		return mi;
    }
	
	
	/**
		* Creates a menu element (menu option)
	 * @param s the string that represents that option
	 */ 
	JMenuItem addItem(String s) {
		JMenuItem menuItem=new JMenuItem(s);
		menuItem.addActionListener(this);
		return menuItem;
	}
	
	/**
		* Creates a menu element (menu option) with a image
	 * @param s the string that represents that option
	 * @param img the file name of the image
	 */
	JMenuItem addItem(String s,String img) {
		JMenuItem menuItem=new JMenuItem(s,new ImageIcon(img));
		menuItem.addActionListener(this);
		return menuItem;
	}	

    /**
		* Create the menubar for the app.  By default this pulls the
     * definition of the menu from the associated resource file. 
     */
    protected JMenuBar createMenuBar() {
		JMenuBar mb = new JMenuBar();
		
		String[] menuKeys = tokenize(getResourceString("menubar"));
		for (int i = 0; i < menuKeys.length; i++) {
			JMenu m = createMenu(menuKeys[i]);
			if (m != null) {
				mb.add(m);
			}
		}
		return mb;
    }

    /**
		* Create a menu for the app.  By default this pulls the
     * definition of the menu from the associated resource file.
     */
    protected JMenu createMenu(String key) {
		String[] itemKeys = tokenize(getResourceString(key));
		JMenu menu = new JMenu(getResourceString(key + "Label"));
		for (int i = 0; i < itemKeys.length; i++) {
			if (itemKeys[i].equals("-")) {
				menu.addSeparator();
			} else {
				JMenuItem mi = createMenuItem(itemKeys[i]);
				menu.add(mi);
			}
		}
		return menu;
    }
	

	/** Appends some text in the log output, and scrolls down the text.
		* If file logging is enabled, then output to the file too.
		*/
	public void print(String text) {
		textArea.append(text);
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
	
	
	/**
	 * The actionPerformed method in this class
     * is called each time the Timer "goes off".
     */
    class TimerListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            progressMonitor.setProgress(task.getCurrent());
            String s = task.getMessage();
            if (s != null) {
                progressMonitor.setNote(s);
            }
            if (progressMonitor.isCanceled() || task.isDone()) {
                progressMonitor.close();
                task.halt();
                Toolkit.getDefaultToolkit().beep();
                timer.stop();
                if (task.isDone()) {
                    print(task.getMessage()+"\n");
                } else {
                    print("Task canceled.\n");				
                }
            }
        }
    }
	
	
	
	
	// -----------------------------------------------------------  MAIN
	
	
    /** Create the application.
	  * The first parameter is the directory containing the images.
	  */
    public static void main(String s[]) {
		try {
            java.lang.System.setProperty("apple.laf.useScreenMenuBar", "true");
        } catch (Exception e) {
            // try the older menu bar property
            java.lang.System.setProperty("com.apple.macos.useScreenMenuBar", "true");
        }
		
		SketchRC window = null;
		if (s.length > 1) { 
			window = new SketchRC(Integer.parseInt(s[1]));
		} else {
			window = new SketchRC();
		}
		
		if (s.length > 0) { // try to read an index file, given an image dir path
			try {
				Retrieval ret = new Retrieval();				
				ImageRetrieval iret = new ImageRetrieval();
				File[] recList = FileUtils.lsDirsR(s[0]);
				for (int i=0;i<recList.length;i++) {
					ret.addLocalDB(recList[i].getAbsolutePath());
					iret.addLocalDB(recList[i].getAbsolutePath());
				}
				window.setRetrieval(ret);
				window.setImageRetrieval(iret);
				
				// check for the preview directory. If it doesn't exist, create it
				File fp = new File(s[0]+File.separator+Retrieval.PREVIEWDIR);
				if (!fp.exists()) {
					fp.mkdir();
					System.out.println("Created preview directory: "+fp.getAbsolutePath());
				}
				
			} catch (Exception exc) {
				System.err.println("Couldn't read index: "+exc);
			}
		} else {
			Retrieval ret = new Retrieval();
			ImageRetrieval iret = new ImageRetrieval();
			window.setRetrieval(ret);
			window.setImageRetrieval(iret);
		}
		
        String app = resources.getString("Title");
		String ver = System.getProperty("java.version");
		String os = System.getProperty("os.name");
		//String vmver = System.getProperty("java.vm.version");					
        window.setTitle(app+" v."+VERSION+" [JRE: "+ver+"/"+os+"]");

        window.pack();
        window.setVisible(true);
		window.arrangeWindows();
    }
}
