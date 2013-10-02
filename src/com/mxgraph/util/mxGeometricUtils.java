package com.mxgraph.util;

import com.mxgraph.model.mxCell;

public class mxGeometricUtils {
	
	public static final double EPSILON = 1e-8;
	
	public static mxPoint mirror(mxCell edge, mxPoint point) {
		double ax = edge.getSource().getGeometry().getX();
		double ay = edge.getSource().getGeometry().getY();
		
		double nx = edge.getTarget().getGeometry().getX() - ax;
		double ny = edge.getTarget().getGeometry().getY() - ay;
		double norm = Math.sqrt(nx * nx + ny * ny);
		nx /= norm; ny /= norm;
		
		double px = point.getX();
		double py = point.getY();
		
		double innerProduct = (ax-px) * nx + (ay-py) * ny;
		
		double resultX = px + 2*((ax-px) - innerProduct * nx);
		double resultY = py + 2*((ay-py) - innerProduct * ny);
		
		return new mxPoint(resultX, resultY);
	}
	
	public static boolean segmentsIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		double d = (y4-y3) * (x2-x1) - (x4-x3) * (y2-y1);
		
		if(d == 0) // parallel
			return false;
		
		double a = ((x4-x3) * (y1-y3) - (y4-y3) * (x1-x3)) / d;
		double b = ((x3-x1) * (y2-y1) - (y3-y1) * (x2-x1)) / d;
		
		if(a < EPSILON || a > 1-EPSILON)
			return false;
		if(b < EPSILON || b > 1-EPSILON)
			return false;
		
		return true;
	}
}
