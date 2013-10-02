package com.mxgraph.analysis;

import java.util.List;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxGeometricUtils;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

public class mxGraphQuality {
	
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
				
				if(mxGeometricUtils.segmentsIntersect(edge1.getSource().getGeometry().getX(),
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
					
					double angle = mxGeometricUtils.calculateEdgeAngle(vertex, a, b);
					
					if(angle < angularResolution) {
						angularResolution = angle;
					}
				}
			}
		}

		double maxAngularResolution = Math.PI * 2 / calculateMaxDegree(graph);
		
		return angularResolution / maxAngularResolution;
	}
	
	public static double edgeBends(mxGraph graph) {
		int m = graph.getChildEdges(graph.getDefaultParent()).length;
		mxGraph bendPromotedGraph = bendPromotion(graph);
		int mprime = bendPromotedGraph.getChildEdges(bendPromotedGraph.getDefaultParent()).length;
		
		return 1 - (mprime - m) / (double) mprime;
	}
	
	
	
	
}
