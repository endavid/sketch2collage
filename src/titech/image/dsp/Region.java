//
//  Region.java
//  sketchRC
//
//  Created by David Gavilan on 11/8/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//
package titech.image.dsp;

import java.awt.*;
import java.util.Vector;
import titech.image.math.*;
import titech.util.*;

public class Region {
	/** Center, x coordinate */
	double cx;
	/** Center, y coordinate */
	double cy;
	/** Volume */
	double vol;
	/** Relative volume */
	double rvol;
	/** Average color (in L*a*b* /100) */
	double[] color;
	double[] deviation; // not used
	double[] skewness; // not used
	
	/** Color Category */
	int ccat;
	double orientation;
	double elongation;
	
	void setCenter(double cx, double cy) {
		this.cx = cx; this.cy = cy;
	}
	void setVolume(double vol, double rvol) {
		this.vol = vol; this.rvol = rvol;
	}
	void setOrientation(double orientation) {
		this.orientation = orientation;
	}
	void setElongation(double elongation) {
		this.elongation = elongation;
	}
	
	/** Converts the color to L*a*b* and stores it as the color of this region
		* The Luminance is normalized between 0 and 1 (not 100!)
		*/
	void setColor(Color rgb) {
		float[] rgbvector = new float[] {rgb.getRed()/255.f, rgb.getGreen()/255f,
			rgb.getBlue()/255f};
		color = AMath.multV(AMath.toDouble(Utilities.sRGBtoLab(rgbvector)),0.01);
	}
	void setColor(int rgb) {
		float[] v=new float[3];
		v[0] = (float)(rgb>>16);
		v[1] = (float)((rgb>>8)&0x000000FF);
		v[2] = (float)(rgb&0x000000FF);
		v[0]/=255f;v[1]/=255f;v[2]/=255f;
		color = AMath.multV(AMath.toDouble(Utilities.sRGBtoLab(v)),0.01);		
	}
	void setColor(double[] mean, double[] dev, double[] skw) {
		float[] v=new float[3];
		for (int i=0;i<3;i++) v[i]=(float)mean[i];
		color = AMath.multV(AMath.toDouble(Utilities.sRGBtoLab(v)),0.01);
		for (int i=0;i<3;i++) v[i]=(float)dev[i];
		deviation = AMath.multV(AMath.toDouble(Utilities.sRGBtoLab(v)),0.01);
		for (int i=0;i<3;i++) v[i]=(float)skw[i];
		skewness = AMath.multV(AMath.toDouble(Utilities.sRGBtoLab(v)),0.01);
	}

	void setColorCat(int cat) {
		ccat = cat;
	}
	void setFeatures(double[] features) {
		cx = features[0];
		cy = features[1];
		vol = features[2];
		rvol = features[3];
		color = new double[]{features[4],features[5],features[6]};
		deviation = new double[] {features[7],features[8],features[9]};
		skewness = new double[] {features[10],features[11],features[12]};
		orientation = features[13];
		elongation = features[14];			
	}
	
	double[] getFeatures() {
		return getFeatures(null);
	}
	
	double[] getFeatures(double[] ff) {
		if (ff==null) 
			ff = new double[15];
		ff[0] = cx;
		ff[1] = cy;
		ff[2] = vol;
		ff[3] = rvol;
		for (int i=0;i<3;i++) {
			ff[4+i]=color[i];
			ff[7+i]=deviation[i];
			ff[10+i]=skewness[i];
		}
		ff[13] = orientation;
		ff[14] = elongation;
		return ff;
	}
	
	double distance(Region pair) {
		return distance(pair, new double[] {.25,.25,.25,.25}, null);
	}
	
	double distance(Region pair, double[] distances) {
		return distance(pair, new double[] {.25,.25,.25,.25}, distances);
	}
	
	double distance(Region pair, double[] weights, double[] distances) {
		double pd = Math.sqrt((cx-pair.cx)*(cx-pair.cx)+
							  (cy-pair.cy)*(cy-pair.cy));
		double vd = Math.sqrt((vol-pair.vol)*(vol-pair.vol)+
							  (rvol-pair.rvol)*(rvol-pair.rvol));
		double cd=0;
		if (weights[2] > 0){
			for (int c=0;c<3;c++) {
				cd+=(color[c]-pair.color[c])*(color[c]-pair.color[c]);
				//+(deviation[c]-pair.deviation[c])*(deviation[c]-pair.deviation[c])
				//+(skewness[c]-pair.skewness[c])*(skewness[c]-pair.skewness[c]);
			}
			cd=Math.sqrt(cd);
		}
		double cosa=Math.cos(orientation)*Math.cos(pair.orientation)+
			Math.sin(orientation)*Math.sin(pair.orientation);
		double ed=Math.abs(elongation-pair.elongation);
		double od=(elongation*pair.elongation*(1.-Math.abs(cosa))+ed)/2.;
		
		if (distances!=null) {
			if (distances.length>3) {
				distances[0]=pd; distances[1]=vd; distances[2]=cd; distances[3]=od;
			}
		}
		
		double dist = pd*weights[0]+vd*weights[1]+cd*weights[2]+od*weights[3];
		return dist;
	}
	
	
	/** Returns the closest region by comparing their centers of gravities */
	public Region closest(Vector regions) {
		Region min = null;
		double dist = Double.MAX_VALUE;
		for (int i=0;i<regions.size();i++) {
			Region r = (Region)regions.get(i);
			double x = cx - r.cx;
			double y = cy - r.cy;
			double d = x*x+y*y;
			if (d<dist) {
				dist = d;
				min = r;
			}
		}
		
		return min;
	}
	
	public String toString() {
		return "{("+cx+","+cy+"), "+"("+vol+","+rvol+"), "+orientation+", "+
		elongation+"}";
	}
}

