package titech.image.math;

import java.util.StringTokenizer;

/**
 *  Description of the Class
 *
 * @author     Owner
 * @created    2003/12/07
 */
public class AMath {

	/**
	 *  Near Zero (for our problem).
	 */
	public final static double NEZERO = 1e-10;
	/**
	 *  A big number for our problem.
	 */
	public final static double VERY_BIG = 1e+10;


	/**
 	* Euclidean distance between two vectors.
 	* @return the euclidean distance.
 	*/
	public static double distance(double[] a, double[] b) {
		double sum=0;
		for (int i=0;i<a.length;i++) {
			double f=a[i]-b[i];
			sum+=f*f;
		}
		return Math.sqrt(sum);
	}
	
	public static double distance(double[] a, double[] b, int dima, int dimb) {
		double sum=0;
		for (int i=dima;i<=dimb;i++) {
			double f=a[i]-b[i];
			sum+=f*f;
		}
		return Math.sqrt(sum);
	}

	/** @return the square of the euclidean distance */
	public static double distanceSqr(double[] a, double[] b) {
		double sum=0;
		for (int i=0;i<a.length;i++) {
			double f=a[i]-b[i];
			sum+=f*f;
		}
		return sum;
	}

	/** @return the square of the euclidean distance computed only between dima and dimb */
	public static double distanceSqr(double[] a, double[] b, int dima, int dimb) {
		double sum=0;
		for (int i=dima;i<=dimb;i++) {
			double f=a[i]-b[i];
			sum+=f*f;
		}
		return sum;
	}
	
	public static double distance(int[] a, int[] b) {
		double sum=0;
		for (int i=0;i<a.length;i++) {
			double f=a[i]-b[i];
			sum+=f*f;
		}
		return Math.sqrt(sum);
	}
	
	
	
	
	/**
	 *  Arc Tangent
	 *
	 * @param  x  Description of the Parameter
	 * @param  y  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double atangent(double x, double y) {
		return (Math.abs(y) > NEZERO) ? Math.atan2(x, y) :
				(x > 0) ? .5 * Math.PI : -.5 * Math.PI;
	}


	/**
	 * The square of a number
	 *
	 * @param  d  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double sqr(double d) {
		return d * d;
	}

	/**
	 * Third root
	 */
	public static double qbic(double d) {
		double res = (d<0)?-1.0:1.0;
		d = Math.abs(d);
		res *= Math.pow(d, 1.0/3.0);
		return res;
	}

	/**
	 *  Description of the Method
	 *
	 * @param  m  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static double sum(double[][] m) {
		double s = 0;
		for (int i = 0; i < m[0].length; i++) {
			for (int j = 0; j < m.length; j++) {
				s += m[i][j];
			}
		}
		return s;
	}

	public static double sum(double[] m) {
		double s = 0;
		for (int i = 0; i < m.length; i++) {
			s += m[i];
		}
		return s;
	}

	public static double sum(float[] m) {
		double s = 0;
		for (int i = 0; i < m.length; i++) {
			s += m[i];
		}
		return s;
	}
	
	
	/**
	 *  Multiplies a matrix by a value.
	 *
	 * @param  m    Description of the Parameter
	 * @param  val  Description of the Parameter
	 */
	public static double[][] mult(double[][] m, double val) {
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m[i].length; j++) {
				m[i][j] *= val;
			}
		}
		return m;
	}

	public static double[][] add(double[][] m, double val) {
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m[i].length; j++) {
				m[i][j] += val;
			}
		}
		return m;
	}

	/** Multiplies a Vector by a value */
	public static double[] multV(double[] m, double val) {
		for (int i = 0; i < m.length; i++) {
			m[i] *= val;
		}
		return m;
	}
	
	
	/** Multiplies a Vector by a Matrix */
	public static float[] fmultV(float[] v, float[][] M) {
		float r[] = new float[M[0].length]; // #columns
		for (int i=0;i<v.length;i++) {
			r[i]=0f;
			for (int j=0;j<M.length;j++) r[i]+=v[j]*M[i][j];
		}
		
		return r;
	}
	
	public static float[] fmultV(float[] v, float ct) {
		float r[] = new float[v.length];
		for (int i=0;i<v.length;i++) {
			r[i]=v[i]*ct;
		}
		
		return r;
	}		
	
	/** Converts the matrix from double to integer. */
	public static int[][] toInt(double[][] m) {
		int[][] res = new int[m.length][];
		for (int i=0;i<m.length;i++) {
			res[i] = new int[m[i].length];
			for (int j=0;j<m[i].length;j++) {
				res[i][j]=(int)m[i][j];
			}
		}
		return res;
	}
		

	/**
	 * Flattens a double matrix into a single dimension float matrix
	 *
	 * @param  m  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static float[] flattenFloat(double[][] m) {
		if (m == null) {
			return null;
		}
		int width = m[0].length;
		int height = m.length;
		float[] flat = new float[width * height];

		int k = 0;
		for (int i = 0; i < m[0].length; i++) {
			for (int j = 0; j < m.length; j++) {
				flat[k++] = (float) m[i][j];
			}
		}

		return flat;
	}


	/**
	 *  Flattens a 2D int matrix into a 1D int matrix.
	 *
	 * @param  m  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static int[] flattenInt(int[][] m) {
		if (m == null) {
			return null;
		}
		int width = m[0].length;
		int height = m.length;
		int[] flat = new int[width * height];

		int k = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				flat[k++] = m[i][j];
			}
		}

		return flat;
	}

	/** Flats columns */
	public static int[] flattenInt(byte[][] m) {
		if (m == null) {
			return null;
		}
		int width = m[0].length;
		int height = m.length;
		int[] flat = new int[width * height];

		int k = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				flat[k++] = (int)m[i][j];
			}
		}

		return flat;
	}

	/** Flats rows */
	public static int[] vflattenInt(byte[][] m) {
		if (m == null) {
			return null;
		}
		int width = m[0].length;
		int height = m.length;
		int[] flat = new int[width * height];

		int k = 0;
		for (int j = 0; j < width; j++) {
			for (int i = 0; i < height; i++) {
				flat[k++] = (int)m[i][j];
			}
		}

		return flat;
	}

	/**
	 *  Generates a String representation of the matrix.
	 *
	 * @param  m  Description of the Parameter
	 * @return    Description of the Return Value
	 */
	public static String showMatrix(double[][] m) {
		if (m == null) {
			return new String("{ (empty) }");
		}
		String s = new String("{ ");
		for (int i = 0; i < m.length; i++) {
			s += " " + m[i][0];
			for (int j = 1; j < m[i].length; j++) {
				s += ", " + m[i][j];
			}
			s += "\n";
		}
		s += "}";
		return s;
	}

	/**
	 *  Generates a String representation of the matrix.
	 *
	 * @parameter the matrix, usually a colormap.
	 * @return    The string.
	 */
	public static String showMatrix(byte[][] m) {
		if (m == null) {
			return new String("{ (empty) }");
		}
		String s = new String("{ ");
		for (int i = 0; i < m.length; i++) {
			s += " " + m[i][0];
			for (int j = 1; j < m[i].length; j++) {
				s += ", " + m[i][j];
			}
			s += "\n";
		}
		s += "}";
		return s;
	}


	public static String showMatrix(int[][] m) {
		if (m == null) {
			return new String("{ (empty) }");
		}
		String s = new String("{ ");
		for (int i = 0; i < m.length; i++) {
			s += " " + m[i][0];
			for (int j = 1; j < m[i].length; j++) {
				s += ", " + m[i][j];
			}
			s += "\n";
		}
		s += "}";
		return s;
	}
	
	public static String showVector(int[] m) {
		if (m == null) {
			return new String("{ (empty) }");
		}
		String s = new String("{ "+m[0]);
		for (int i = 1; i < m.length; i++) {
			s += ", " + m[i];
		}
		s += "}";
		return s;
	}

	public static String showVector(float[] m) {
		if (m == null) {
			return new String("{ (empty) }");
		}
		String s = new String("{ "+m[0]);
		for (int i = 1; i < m.length; i++) {
			s += ", " + m[i];
		}
		s += "}";
		return s;
	}
	
	public static String showVector(double[] m) {
		if (m == null) {
			return new String("(empty)");
		}
		String s = new String(""+m[0]);
		for (int i = 1; i < m.length; i++) {
			s += ", " + m[i];
		}
		//s += "}";
		return s;
	}
	
	public static String showVector(java.util.Vector v) {
		if (v == null) { return new String("{ (empty) }"); }
		String s = new String("{ "+v.firstElement());
		for (int i = 1; i < v.size(); i++) {
			s += ", " + v.elementAt(i);
		}
		s += "}";
		return s;
		
	}
	
	/** Checks whether: low[i] <= vector[i] <= top[i] for all i */ 
	public static boolean inRange(double[] low, double[] vector, double[] top) {
		for (int i=0; i<vector.length; i++) {
			if (low[i]>vector[i]) return false;
			if (vector[i]>top[i]) return false;
		}
		
		return true;
	}
	/** Checks whether: low[i] <= vector[i] <= top[i] for all i */ 
	public static boolean inRange(float[] low, float[] vector, float[] top) {
		for (int i=0; i<vector.length; i++) {
			if (low[i]>vector[i]) return false;
			if (vector[i]>top[i]) return false;
		}
		
		return true;
	}

	/** Checks if value is contained in vector */
	public static boolean inVector(int value, int[] vector) {
		for (int i=0; i<vector.length; i++) {
			if (vector[i]==value) return true;
		}
		
		return false;
	}
	
	
	/** Adjusts values not in range to [low, top] */
	public static void trim(float[] low, float[] vector, float[] top) {
		for (int i=0; i<vector.length; i++) {
			if (low[i]>vector[i]) vector[i]=low[i];
			if (vector[i]>top[i]) vector[i]=top[i];
		}
	}

	/** Trims just the top */
	public static void trimTop(float[] vector, float[] top) {
		for (int i=0; i<vector.length; i++) {
			if (vector[i]>top[i]) vector[i]=top[i];
		}
	}
	
	public static double[] toDouble(float[] v) {
		double[] dv = new double[v.length];
		for (int i=0;i<v.length;i++) dv[i]=(double)v[i];
		
		return dv;
	}
	
	public static float[] toFloat(double[] v) {
		float[] fv = new float[v.length];
		for (int i=0;i<v.length;i++) fv[i]=(float)v[i];
		
		return fv;
	}
	
	public static float[] toFloat(byte[] v) {
		float[] fv = new float[v.length];
		for (int i=0;i<v.length;i++) 
			fv[i]=(v[i]<0)?256f+(float)v[i]:(float)v[i];
		
		return fv;	
	}
	
	public static double max(double a, double b, double c) {
		return ((a>b)&&(a>c))?a:(b>c?b:c);
	}
	
	/** Encodes unsigned bytes in an integer */
	public static int byteToInt(byte[] v) {
		int r=0;
		for (int i=0,j=0;i<v.length;i++,j+=8) 
			r|=((v[i]<0)?256+(int)v[i]:(int)v[i])<<j;		
		return r;
	}
	
	public static int byteToInt(byte red, byte green, byte blue) {
		return byteToInt(new byte[] {blue, green, red});
	}
	
	public static int byteToInt(byte val) {
		return (val<0)?256+(int)val:(int)val;
	}
	
	/**
	 * Find the minimum Euclidean distance to a matrix and return its row index
	 */
	 public static int findMin(double[] vector, double[][] data) {
		 int minIndex = 0;
		 double minDistance = Double.MAX_VALUE;
		 for (int i=0; i<data.length; i++) {
			 double distance = 0;
			 for (int j=0; j<vector.length; j++)
				 distance+=(vector[j]-data[i][j])*(vector[j]-data[i][j]);
			 if (distance < minDistance) {
				 minDistance = distance;
				 minIndex = i;
			 }
		 }
		 
		 return minIndex;
	 }

	public static int findMin(float[] vector, float[][] data) {
		int minIndex = 0;
		double minDistance = Double.MAX_VALUE;
		for (int i=0; i<data.length; i++) {
			double distance = 0;
			for (int j=0; j<vector.length; j++)
				distance+=(vector[j]-data[i][j])*(vector[j]-data[i][j]);
			if (distance < minDistance) {
				minDistance = distance;
				minIndex = i;
			}
		}
		
		return minIndex;
	}
	
	/**
	  * Find the minimum Euclidean distance to a matrix and return its row index
	  * Just considers from dimension dima to dimb, both included.
	 */
	public static int findMin(double[] vector, double[][] data, int dima, int dimb) {
		int minIndex = 0;
		double minDistance = Double.MAX_VALUE;
		for (int i=0; i<data.length; i++) {
			double distance = 0;
			for (int j=dima; j<=dimb; j++)
				distance+=(vector[j]-data[i][j])*(vector[j]-data[i][j]);
			if (distance < minDistance) {
				minDistance = distance;
				minIndex = i;
			}
		}
		return minIndex;
	}	
	
	/**
	 * Find the minimum absolute distance to a vector and return its index
	 */
	 public static int findMin(double value, double[] data) {
		 int minIndex = 0;
		 double minDistance = Double.MAX_VALUE;
		 for (int i=0; i<data.length; i++) {
			 double distance = Math.abs(value-data[i]);
			 if (distance < minDistance) {
				 minDistance = distance;
				 minIndex = i;
			 }
		 }
		 return minIndex;
	 }
	
	
	/** Finds the index of the greatest value in a vector */
	public static int indexMax(int[] vec) {
		int index=0;
		int value = vec[0];
		for (int i=1;i<vec.length;i++){
			if (vec[i]>value) {
				index = i;
				value = vec[i];
			}
		}
		return index;
	}
	
	 
	/**
	 * Find the minimum angular distance to a vector and return its index.
	 * If elongation e is specified, then the distance is computed as follows,
	 *
	 *   d((a0,e0),(a1,e1)) = 1/2 (e0e1(1-|cos(alpha)|) + |e0-e1|)
	 *   where,
	 *   cos(alpha) = cos(a0)*cos(a1)+sin(a0)*sin(a1);
	 * 
	 */
	 public static int findMinAngular(double a, double[] adata, 
	 								  double e, double[] edata) {
		 int minIndex = 0;
		 double minDistance = Double.MAX_VALUE;
		 for (int i=0; i<adata.length; i++) {
			 double cosa = Math.cos(a)*Math.cos(adata[i])+Math.sin(a)*Math.sin(adata[i]);
			 double distance = 1.-Math.abs(cosa);
			 if (edata!=null) {
				 distance = (e*edata[i]*distance+Math.abs(e-edata[i]))/2.;
			 }
			 if (distance < minDistance) {
				 minDistance = distance;
				 minIndex = i;
			 }
		 }
		 
		 return minIndex;
	 }

	public static double[] vectorDouble(String s) {
		StringTokenizer stok = new StringTokenizer(s,",");
		int n = stok.countTokens();
		double[] v=new double[n];
		for (int i=0;i<n;i++) {
			v[i]=Double.parseDouble(stok.nextToken());
		}
		return v;
	}
	 
}

