//
//  Poisson.java
//  sketchRC
//
//  Created by David Gavilan on 11/13/06.
//  Copyright 2006 Nakajima Lab. All rights reserved.
//
package titech.image.matting;

import java.awt.*;
import java.awt.image.*;
import titech.image.dsp.*;
import titech.wt.*;

/**
  * Multithreaded Poisson optimization.
  *
  * 1 Thread per Color Channel.
  *
  * It extends Thread instead of implementing Runnable, to be able
  * to know "whoAmI".
  */
public class Poisson extends Thread {

	public final static int GRADIENT_IMPORT = 1;
	public final static int GRADIENT_MIX = 2;
	public final static int WEIGHTED_GRADIENT = 3;
	public final static int ALPHA_MATTE = 4;
	
	private Thread[] fils;
	
	BufferedImage source;
	BufferedImage target;
	BufferedImage result;
	double[] alpha = null;
	byte[][] domain;
	// don't clip here. Give it clipped to the constructor
	//Rectangle clip;
	int width;
	int height;
	
	BufferedImage omega;
	private int method;
	// no offset, align them before calling the constructor
	//private Point offset;
	private int area;
	
	public double sourceWeight;
	public double targetWeight;
	public double accuracy;
	
	/** To update visuals while processing */
	CompositeCanvas compositeCanvas = null;
	
	
	public void setComposite(CompositeCanvas canvas) {
		this.compositeCanvas = canvas;
	}
	
	
	/**
	  * The non-processed parts are directly the target
	  * and it's also used as the initial value for the Gauss-Siedel solver
	  * 
	  * Clip and align source, target and omega before constructing this object!
	 */
	public Poisson(BufferedImage source, BufferedImage target, BufferedImage omega) {
		this.target = target;
		sourceWeight = 0.5;
		targetWeight = 0.5;
		accuracy = 0.05;
		width = target.getWidth();
		height = target.getHeight();
		if (source.getWidth()!=width || source.getHeight()!=height || 
				omega.getWidth()!=width || omega.getHeight()!=height) {
			System.err.println("[WARNING] Poisson: image sizes differ!");
		}
		
		result = new BufferedImage(width, height,
								   BufferedImage.TYPE_INT_ARGB);
		
		Graphics gi = result.getGraphics();
		gi.drawImage(target,0,0,null);
		gi.dispose();
		
		this.source = source;
		//this.source = source.getSubimage(clip.x,clip.y,clip.width,clip.height);
		//omega = omega.getSubimage(clip.x,clip.y,clip.width,clip.height);
		//domain = MOps2D.getGradient(omega);
		//System.out.println(titech.image.math.AMath.showMatrix(domain));
		this.omega = omega;
		compositeCanvas = null;
		method = GRADIENT_IMPORT;
	}
	
	public void setMethod(int method) {
		this.method = method;
	}
	
	public void setMixture(double sGradient) {
		sourceWeight = sGradient;
		targetWeight = sGradient>=1.?0:1.-sGradient;
	}
	
	public BufferedImage optimize() {

		
		domain = MOps2D.getGradient(omega);
		
		//if (compositeCanvas != null) 
		//	compositeCanvas.updateObject(MOps2D.difference(omega, MOps2D.erode(omega)));
		//try { Thread.sleep(2000); } catch (Exception exc) {};
		
		//count area
		area = 0;
		for (int j=0;j<omega.getHeight();j++) {
			for (int i=0;i<omega.getWidth();i++) {
				if (domain[i][j]==1) { // p in Omega
					area++;
				}
			}			
		}

		fils = new Thread[3];
		try {
			for (int c=0;c<fils.length;c++) { // for every color channel
				fils[c]=new Thread(this,""+c);
				fils[c].start();
			} // end for all channels
			for (int c=0;c<fils.length;c++) { // for every thread
				fils[c].join(); // wait for the thread to finish
			}
		} catch (InterruptedException exc) {
			System.err.println("[POISSON] "+exc);
		}		
		
			
		return result;
	}
	
	public BufferedImage optimizeAlpha() {
		
		// initialize result with the source
		
		// initialize result.alpha with the mask
		
		

		// Gauss-Siedel optimization, updating alpha at each step
		
		
		return result;
	}
	
	/**
	 * Order:
	 *  |0|
	 * 1|p|2
	 *  |3|
	 */
	private void getNeighbors(int x, int y, byte[] nn) {
		nn[0]=(y<1?-1:domain[x][y-1]);
		nn[1]=(x<1?-1:domain[x-1][y]);
		nn[2]=(x+1>=width?-1:domain[x+1][y]);
		nn[3]=(y+1>=height?-1:domain[x][y+1]);
	}
	
	private void getDifference(int band, double[] d) {
		double[] g = new double[width*height];
		double[] t = null;
	
		source.getData().getSamples(0,0,width,height,band,d);
		source.getData().getSamples(0,0,width,height,band,g);
		switch(method) {
			case ALPHA_MATTE:
				if (alpha==null) { // it doesn't change. Take it only once
					alpha = new double[width*height];
					result.getAlphaRaster().getSamples(0,0,width,height,band,alpha);
				}
				// don't break!
			case WEIGHTED_GRADIENT:
			case GRADIENT_MIX:
				t = new double[width*height];
				target.getData().getSamples(0,0,width,height,band,t);
				break;
		} 
		


		byte[] np = new byte[4];
		for (int j=0;j<height;j++)
		for (int i=0;i<width;i++) {
			int p = i+j*width;
			getNeighbors(i,j,np);
			int[] nn = new int[] {
				i+(j-1)*width,
				i-1+j*width,
				i+1+j*width,
				i+(j+1)*width
			};
			d[p]=0;
			for (int k=0;k<4;k++){
				if (np[k]>=0) {
					int q=nn[k];
					switch(method) {
						case GRADIENT_IMPORT:
							d[p]+=g[p]-g[q];
							break;
						case GRADIENT_MIX:
							double tt = t[p]-t[q];
							double gg = g[p]-g[q];
							if (Math.abs(tt)>Math.abs(gg))
								d[p]+=tt;
							else
								d[p]+=gg;
							break;
						case WEIGHTED_GRADIENT:
							tt = t[p]-t[q];
							gg = g[p]-g[q];
							d[p]+=sourceWeight*gg+targetWeight*tt;
							break;
						case ALPHA_MATTE:
							double ap=alpha[p]/255.;
							double aq=alpha[q]/255.;
							if (ap<1. && aq<1.) { // similar to M(p)=1
								d[p]+=ap*g[q]-aq*g[q]+(1.-ap)*t[p]-(1.-aq)*t[q];
							} else { // gradient import
								d[p]+=g[p]-g[q];
							}
							break;
						default:
							d[p]+=g[p]-g[q];
					}
				}
			}				
		}
	} // end getDifference
	
	/** Optimizes a color channel */
	public void run() {
		int c = Integer.parseInt(Thread.currentThread().getName());
		
		double[] t = new double[width*height];
		double[] f0 = new double[width*height];
		double[] f1 = new double[width*height];		
		double[] d = new double[width*height];

		// set the initial value
		for (int p=0;p<f1.length;p++) f1[p]=0;
		result.getData().getSamples(0,0,width,height,c,f0);
		result.getData().getSamples(0,0,width,height,c,t);
		
		getDifference(c,d);		
		double change = Double.MAX_VALUE;
		
		byte[] np = new byte[4]; // neighbors
		// Optimize with Gauss-Siedel
		while (change > accuracy) { // 100*accuracy% of pixels didn't change
			change = 0;
			int[] nn = new int[4];
			for (int j=0;j<height;j++)
				for (int i=0;i<width;i++) {
					if (domain[i][j]==1) { // p in Omega
						int p = i+j*width;
						nn[0]=i+(j-1)*width;
						nn[1]=i-1+j*width;
						nn[2]=i+1+j*width;
						nn[3]=i+(j+1)*width;
						getNeighbors(i,j,np);
						f1[p]=d[p];
						int n = 0;
						for (int k=0;k<4;k++){
							if (np[k]==0) f1[p]+=t[nn[k]];
							if (np[k]>=0) n++;
						}
						if (np[0]==1) f1[p]+=f1[nn[0]];
						if (np[1]==1) f1[p]+=f1[nn[1]];
						if (np[2]==1) f1[p]+=f0[nn[2]];
						if (np[3]==1) f1[p]+=f0[nn[3]];
						
						f1[p]/=(double)n;
						
						if (f1[p]>255) f1[p]=255;
						if (f1[p]<0) f1[p]=0;
						
						change += (f1[p]-f0[p]);
						
					} // p in Omega
				} // for every point p
					for (int p=0;p<f1.length;p++) f0[p]=f1[p];
			change /= (double)area;
			change = Math.abs(change);
			
			//System.out.println("[POISSON] change "+c+": "+change);
			//if (compositeCanvas != null) {
			//	result.getRaster().setSamples(clip.x+offset.x,clip.y+offset.y,clip.width,clip.height,c,f1);
			//	compositeCanvas.updateObject(result);
			//	try { Thread.sleep(1000/30); } catch (Exception exc) {};
			//}
			
		} // end while
		result.getRaster().setSamples(0,0,width,height,c,f1);
		
	}
		
}

