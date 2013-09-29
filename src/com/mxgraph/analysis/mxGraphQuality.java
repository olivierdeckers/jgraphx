package com.mxgraph.analysis;

import com.mxgraph.model.mxCell;

public class mxGraphQuality {
	
	public static final double EPSILON = 1e-8;

	public static int edgeCrossings(mxAnalysisGraph graph) {
		//TODO eerst bend promotion doen?
		Object[] edges = graph.getChildEdges(graph.getGraph().getDefaultParent());
		
		int crossings = 0;
		for(int i=0; i<edges.length; i++) {
			mxCell edge1 = (mxCell) edges[i];
			for(int j = i+1; j<edges.length; j++) {
				mxCell edge2 = (mxCell) edges[j];
				
				if(segmentsIntersect(edge1.getSource().getGeometry().getX(),
						edge1.getSource().getGeometry().getY(),
						edge1.getTarget().getGeometry().getX(),
						edge1.getTarget().getGeometry().getY(),
						edge2.getSource().getGeometry().getX(),
						edge2.getSource().getGeometry().getY(),
						edge2.getTarget().getGeometry().getX(),
						edge2.getTarget().getGeometry().getY())) {
					crossings ++;
				}
			}
		}
		
		return crossings;
	}
	
	public static int edgeBends(mxGraph graph) {
		Object[] edges = graph.getChildEdges(graph.getDefaultParent());
		
		int bends = 0;
		for(int i=0; i<edges.length; i++) {
			mxCell edge = (mxCell) edges[i];
			if(edge.getGeometry().getPoints() != null)
				bends += edge.getGeometry().getPoints().size();
		}
		
		return bends;
	}
	
	private static boolean segmentsIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
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
