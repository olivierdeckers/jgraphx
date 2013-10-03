package com.mxgraph.util;

import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.mxgraph.model.mxCell;

public class mxGeometricUtils {
	
	public static final double EPSILON = 1e-8;
	
	public static mxPoint mirror(mxCell axis, mxPoint point) {
		double ax = axis.getSource().getGeometry().getX();
		double ay = axis.getSource().getGeometry().getY();
		
		double nx = axis.getTarget().getGeometry().getX() - ax;
		double ny = axis.getTarget().getGeometry().getY() - ay;
		double norm = Math.sqrt(nx * nx + ny * ny);
		nx /= norm; ny /= norm;
		
		double px = point.getX();
		double py = point.getY();
		
		double innerProduct = (ax-px) * nx + (ay-py) * ny;
		
		double resultX = px + 2*((ax-px) - innerProduct * nx);
		double resultY = py + 2*((ay-py) - innerProduct * ny);
		
		return new mxPoint(resultX, resultY);
	}
	
	public static double calculateEdgeAngle(mxCell vertex, mxCell a, mxCell b) {
		mxCell aTarget = (mxCell) ((a.getSource() == vertex) ? a.getTarget() : a.getSource());
		mxCell bTarget = (mxCell) ((b.getSource() == vertex) ? b.getTarget() : b.getSource());
		
		double ax = aTarget.getGeometry().getX() - vertex.getGeometry().getX();
		double ay = aTarget.getGeometry().getY() - vertex.getGeometry().getY();
		double bx = bTarget.getGeometry().getX() - vertex.getGeometry().getX();
		double by = bTarget.getGeometry().getY() - vertex.getGeometry().getY();
		
		double norm = Math.sqrt(ax*ax + ay*ay);
		ax /= norm;
		ay /= norm;
		
		norm = Math.sqrt(bx*bx + by*by);
		bx /= norm;
		by /= norm;
		
		double dotProduct = ax * bx + ay * by;
		return Math.acos(dotProduct);
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
	
	/**
	 * Souce: http://algs4.cs.princeton.edu/99hull/
	 * @param pts
	 */
	public static List<Point> calculateConvexHull(List<Point> pts) {
		Stack<Point> hull = new Stack<Point>();
		
        // defensive copy
        int N = pts.size();
        final Point[] points = new Point[N];
        for (int i = 0; i < N; i++)
        	points[i] = pts.get(i);

        // preprocess so that points[0] has lowest y-coordinate; break ties by x-coordinate
        // points[0] is an extreme point of the convex hull
        // (alternatively, could do easily in linear time)
        Arrays.sort(points, new Comparator<Point>() {
			@Override
			public int compare(Point p1, Point p2) {
				if (p1.y < p2.y) return -1;
		        if (p1.y > p2.y) return +1;
		        if (p1.x < p2.x) return -1;
		        if (p1.x > p2.x) return +1;
		        return 0;
			}
        });

        // sort by polar angle with respect to base point points[0],
        // breaking ties by distance to points[0]
        Arrays.sort(points, 1, N, new Comparator<Point>() {
        	Point p = points[0];
        	@Override
        	public int compare(Point q1, Point q2) {
        	 double dx1 = q1.x - p.x;
             double dy1 = q1.y - p.y;
             double dx2 = q2.x - p.x;
             double dy2 = q2.y - p.y;

             if      (dy1 >= 0 && dy2 < 0) return -1;    // q1 above; q2 below
             else if (dy2 >= 0 && dy1 < 0) return +1;    // q1 below; q2 above
             else if (dy1 == 0 && dy2 == 0) {            // 3-collinear and horizontal
                 if      (dx1 >= 0 && dx2 < 0) return -1;
                 else if (dx2 >= 0 && dx1 < 0) return +1;
                 else                          return  0;
             }
             else return -ccw(p, q1, q2);     // both above or below

             // Note: ccw() recomputes dx1, dy1, dx2, and dy2
        	}
        });

        hull.push(points[0]);       // p[0] is first extreme point

        // find index k1 of first point not equal to points[0]
        int k1;
        for (k1 = 1; k1 < N; k1++)
            if (!points[0].equals(points[k1])) break;
        if (k1 == N) return hull;        // all points equal

        // find index k2 of first point not collinear with points[0] and points[k1]
        int k2;
        for (k2 = k1 + 1; k2 < N; k2++)
            if (ccw(points[0], points[k1], points[k2]) != 0) break;
        hull.push(points[k2-1]);    // points[k2-1] is second extreme point

        // Graham scan; note that points[N-1] is extreme point different from points[0]
        for (int i = k2; i < N; i++) {
            Point top = hull.pop();
            while (ccw(hull.peek(), top, points[i]) <= 0) {
                top = hull.pop();
            }
            hull.push(top);
            hull.push(points[i]);
        }

        return hull;
    }
	
	/**
     * Is a->b->c a counterclockwise turn?
     * @param a first point
     * @param b second point
     * @param c third point
     * @return { -1, 0, +1 } if a->b->c is a { clockwise, collinear; counterclocwise } turn.
     */
    private static int ccw(Point a, Point b, Point c) {
        double area2 = (b.x-a.x)*(c.y-a.y) - (b.y-a.y)*(c.x-a.x);
        if      (area2 < 0) return -1;
        else if (area2 > 0) return +1;
        else                return  0;
    }
    
    public static double calculateConvexArea(List<Point> points) {
    	int sum = 0;
    	for(int i=0; i<points.size()-1; i++) {
    		sum += points.get(i).x * points.get(i+1).y - points.get(i).y * points.get(i+1).x;
    	}
    	int n = points.size()-1;
    	sum += points.get(n).x * points.get(0).y - points.get(n).y * points.get(0).x;
    	return sum / 2.0;
    }
}
