//
//  KMeans.java
//  blobby
//
//  Created by David Gavilan on 10/30/06.
//
package titech.cluster;

public class KMeans {
	/** Maximum number of iterations */
	public static final int MAXITERS = 100;
	
	/** the number of clusters */
	int K; 

	/** the assignments of each element to its class */
	int[] cAssignment;
	/** the cluster centers */
	double[][] mu;
	
	public int[] getAssignments() {
		return cAssignment;
	}
	public double[][] getMeans() {
		return mu;
	}
	
	public KMeans(int k, double[][] data) {
		this(k,data,null);
	}
	/**
	 * @param k number of clusters
	 * @param data the data in an array of the form n x d, where d is the dimension
	 *    of each vector, and n is the total number of samples
	 */
	public KMeans(int k, double[][] data, int[] initialAssignment) {
		this.K = k;
		int n = data.length;
		int d = data[0].length;
		mu = new double[k][d];
		if (initialAssignment == null) {
			cAssignment = new int[n];
			randomInit();
		} else {
			cAssignment = initialAssignment;
		}
		cluster(k, data);
	} 
	
	void randomInit() {
		for (int i=0;i<cAssignment.length;i++) {
			cAssignment[i] = (int)Math.floor(Math.random()*K)+1;
		}
	}
	
	void cluster(int k, double[][] data) {
		int n=data.length;
		int d=data[0].length;
		
		int e=1;
		int iter=0;
		int[] count = new int[k];
		double[] dist = new double[k];
		
		while (e>0 && iter<MAXITERS) {
			// compute the means and the distances
			for (int i=0;i<k;i++) {
				count[i]=0;
				for (int v=0;v<d;v++) mu[i][v]=0.;
			}
			for (int j=0;j<n;j++) {
				int member = cAssignment[j];
				if (member>0) {
					count[member-1]++;
					for (int v=0;v<d;v++) mu[member-1][v] += data[j][v];
				}
			}
			for (int i=0;i<k;i++) 
				if (count[i]>0)
					for (int v=0;v<d;v++) mu[i][v]/=(double)count[i];
				// else =0 
			
			//System.out.println(titech.image.math.AMath.showMatrix(mu));
			// dist(1:n,i)=sum((X-ones(n, 1)*mu(i,:)).^2,2);
			// argmin
			// [Y,I]=min(dist,[],2);
			// e=n-sum(C==I); C=I;
			e=0;
			for (int j=0;j<n;j++) {
				for (int i=0;i<k;i++) {
					dist[i]=0.;
					for (int v=0;v<d;v++)
						dist[i]+=(data[j][v]-mu[i][v])*(data[j][v]-mu[i][v]);
				}
				// min
				int clase = 1;
				double min=dist[clase-1];
				for (int i=1;i<k;i++) {
					if (dist[i]<min) {
						clase = i+1;
						min = dist[i];
					}
				}
				if (cAssignment[j]!=clase) e++;
				cAssignment[j]=clase;
			}
			
			iter++;
		} // end while
		
		//System.out.println("Iterated "+iter+" times.");
	} // end cluster
	
	public static void main(String s[]) {
		// small test
		double[][] data=new double[7][2];
		for (int i=0;i<7;i++) {
			data[i][0]=(double)i;
			data[i][1]=Math.sin(i);
		}
		KMeans cluster = new KMeans(2, data);
		System.out.println(titech.image.math.AMath.showMatrix(cluster.getMeans()));
		System.out.println(titech.image.math.AMath.showVector(cluster.getAssignments()));		
	}

}
