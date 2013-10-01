package com.mxgraph.analysis;

import java.util.List;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

public class mxGraphQuality {
	
	public static final double EPSILON = 1e-8;
	
	public static final void assess(mxGraph graph) {
		System.out.println("crossings: " + mxGraphQuality.edgeCrossings(graph));
		System.out.println("bends: " + mxGraphQuality.edgeBends(graph));
		System.out.println("angular resolution: " + mxGraphQuality.angularResolution(graph));
	}
	
	private static mxGraph bendPromotion(mxGraph graph) {
		mxGraph copy = new mxGraph();
		copy.addCells(graph.cloneCells(graph.getChildCells(graph.getDefaultParent())));
		graph = copy;
		
		for(Object oEdge : graph.getChildEdges(graph.getDefaultParent())) {
			mxCell edge = (mxCell) oEdge;
			if(edge.getGeometry().getPoints() != null) {
				List<mxPoint> controlPoints = edge.getGeometry().getPoints();
				
				mxICell from = edge.getSource();
				for(int i=0; i<controlPoints.size(); i++) {
					mxPoint point = controlPoints.get(i);
					mxCell extraCell = new mxCell();
					extraCell.setVertex(true);
					extraCell.setGeometry(new mxGeometry(point.getX(), point.getY(), 0, 0));
					graph.addCell(extraCell);
					
					mxCell extraEdge = new mxCell();
					extraEdge.setEdge(true);
					graph.addEdge(extraEdge, graph.getDefaultParent(), from, extraCell, null);
					
					from = extraCell;
				}
				
				mxCell extraEdge = new mxCell();
				extraEdge.setEdge(true);
				graph.addEdge(extraEdge, graph.getDefaultParent(), from, edge.getTarget(), null);
				
				graph.removeCells(new Object[] {edge});
			}
		}
		
		return graph;
	}

	public static double edgeCrossings(mxGraph graph) {
		graph = bendPromotion(graph);
		Object[] edges = graph.getChildEdges(graph.getDefaultParent());
		
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
		
		int nbEdges = graph.getChildEdges(graph.getDefaultParent()).length;
		int maxCrossings = nbEdges * (nbEdges - 1) / 2;
		
		// subtract impossible crossings from maxCrossings
		int sum = 0;
		for(Object oVertex : graph.getChildVertices(graph.getDefaultParent())) {
			mxCell vertex = (mxCell) oVertex;
			sum += vertex.getEdgeCount() * (vertex.getEdgeCount()-1);
		}
		maxCrossings -= sum/2;
		
		return 1 - (2*crossings / (double) maxCrossings);
	}
	
	private static int calculateMaxDegree(mxGraph graph) {
		int max = 0;
		for(Object oNode : graph.getChildVertices(graph.getDefaultParent())) {
			mxCell node = (mxCell) oNode;
			
			if(node.getEdgeCount() > max)
				max = node.getEdgeCount();
		}
		return max;
	}
	
	public static double angularResolution(mxGraph graph) {
		Object[] vertices = graph.getChildCells(graph.getDefaultParent(), true, false);
		
		double angularResolution = Float.MAX_VALUE;
		for(Object oVertex : vertices) {
			mxCell vertex = (mxCell) oVertex;
			for(int i=0; i<vertex.getEdgeCount(); i++) {
				mxCell a = (mxCell) vertex.getEdgeAt(i);
				for(int j=i+1; j<vertex.getEdgeCount(); j++) {
					mxCell b = (mxCell) vertex.getEdgeAt(j);
					
					double angle = calculateEdgeAngle(vertex, a, b);
					
					if(angle < angularResolution) {
						angularResolution = angle;
					}
				}
			}
		}

		double maxAngularResolution = Math.PI * 2 / calculateMaxDegree(graph);
		
		return angularResolution / maxAngularResolution;
	}
	
	private static double calculateEdgeAngle(mxCell vertex, mxCell a, mxCell b) {
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
