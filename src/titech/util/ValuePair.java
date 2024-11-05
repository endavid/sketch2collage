package titech.util;

import java.util.Vector;

public class ValuePair implements Comparable {
	public int index;
	public double value;
	public String label;
	public int ilabel;
	public Object object;
	public ValuePair pointer;
	
	public ValuePair(int index, double value) {
		this(index, value, 0, null, null);
	}

	public ValuePair(int index, double value, String label) {
		this(index, value, 0, label, null);
	}
	
	public ValuePair(Object o) {
		this(0,0,0,null,o);
	}

	public ValuePair(double value, Object o) {
		this(0,value,0,null,o);
	}
	public ValuePair(int index, double value, int ilabel, String label) {
		this(index,value,ilabel,label,null);
	}

	public ValuePair(int index, double value, int ilabel, String label, Object obj) {
		this.index = index;
		this.value = value;
		this.ilabel = ilabel;
		this.label = label;
		object = obj;
		pointer = null;
	}
	
	public void setObject(Object o) {
		this.object = o;
	}
	public Object getObject() {
		return object;
	}
	public Vector getObjectList() {
		Vector<Object> v = new Vector<Object>();
		if (object==null) return v;
		v.add(object);
		ValuePair vp = pointer;
		while (vp != null) {
			v.add(vp.object);
			vp = vp.pointer;
		}
		return v;
	}
	
	public Object getFirstObject() {
		Object obj = object;
		ValuePair vp = pointer;
		while (vp != null) {
			obj = vp.object;
			vp = vp.pointer;
		}
		return obj;
	}
	
	public int compareTo(Object o) {
		ValuePair vp = (ValuePair) o;
		if (value<vp.value) return -1;
		if (value>vp.value) return 1;
		return 0;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (object!=null) 
			if (object.getClass().isInstance(obj)) 
				return object.equals(obj);
		if (this.getClass().isInstance(obj)) {
			ValuePair vp = (ValuePair) obj;
			if (vp.object!=null) return object.equals(vp.object); 
			else if (object==null && this.index == vp.index) return true;
		}
		return false;
	}
	
	public String toString() {
		return "ValuePair("+index+", "+value+", "+ilabel+", "+label+")";
	}
	
    public static void main(String s[]) {
		Vector<ValuePair> v=new Vector<ValuePair>();
		for (int i=0;i<5;i++) {
			ValuePair vp=new ValuePair(i,(double)i);
			java.awt.Point p = new java.awt.Point(i+1,i+2);
			vp.setObject(p);
			//v.add(p);
			// the result should be all true!
			v.add(vp);
			System.out.println("vp == p? "+vp.equals(new java.awt.Point(i+1,i+2)));
		}
		for (int i=0;i<6;i++) {
			System.out.println("contains "+i+"? "+v.contains(
					new ValuePair(new java.awt.Point(i+1,i+2))));
		}
		System.out.println("contains null? "+v.contains(null));
	}
}

