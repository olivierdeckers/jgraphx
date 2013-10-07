package com.mxgraph.analysis;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
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
		System.out.println("symmetry: " + mxGraphQuality.symmetry(graph));
		System.out.println("edge length deviation: " + mxGraphQuality.edgeLengthDeviation(graph));
		System.out.println("edge orthogonality: " + mxGraphQuality.edgeOrthogonality(graph));
		System.out.println("uniform vertex distribution: " + mxGraphQuality.vertexUniformity(graph));
	}
	
	/**
	 * Assess how uniformly vertices are distributed. A grid is laid over the graph,
	 * and a Chi squared test is used to determine the uniformness of the vertex distribution.
	 * 
	 * @param graph
	 * @return
	 */
	private static double vertexUniformity(mxGraph graph) {
		Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
		
		int resolution = 3;
		int[][] sums = new int[resolution][resolution];
		
		double maxX = 0, minX = Integer.MAX_VALUE, maxY = 0, minY = Integer.MAX_VALUE;
		for(int i=0; i<vertices.length; i++) {
			mxCell v = (mxCell) vertices[i];
			Point2D p = v.getGeometry().getPoint();
			
			if(p.getX() < minX)
				minX = p.getX();
			if(p.getX() > maxX)
				maxX = p.getX()+1;
			if(p.getY() < minY)
				minY = p.getY();
			if(p.getY() > maxY)
				maxY = p.getY()+1;
		}
		
		double squareWidth = (maxX - minX) / resolution;
		double squareHeight = (maxY - minY) / resolution;
		
		for(int i=0; i<vertices.length; i++) {
			mxCell v = (mxCell) vertices[i];
			Point2D p = v.getGeometry().getPoint();
			
			int x = (int) ((p.getX() - minX) / squareWidth);
			int y = (int) ((p.getY() - minY) / squareHeight);
			
			sums[x][y]++;
		}
		
		double expectedVertsPerSquare = vertices.length / (double) (resolution * resolution);
		double chi = 0;
		for(int x = 0; x<resolution; x++) {
			for(int y=0; y<resolution; y++) {
				chi += (sums[x][y] - expectedVertsPerSquare) * (sums[x][y] - expectedVertsPerSquare) / expectedVertsPerSquare;
			}
		}
		
		// max chi value when all vertices except one are in the same square:
		double maxChi = (vertices.length - 1 - expectedVertsPerSquare) * (vertices.length - 1 - expectedVertsPerSquare) / expectedVertsPerSquare
				+ (resolution * resolution - 1) * expectedVertsPerSquare;
		
		return 1 - (chi / maxChi);
	}

	public static double edgeOrthogonality(mxGraph graph) {
		Object[] edges = graph.getChildEdges(graph.getDefaultParent());
		
		double sum = 0;
		for(int i=0; i<edges.length; i++) {
			mxCell edge = (mxCell) edges[i];
			mxCell source = (mxCell) edge.getSource();
			mxCell axis = new mxCell();
			axis.setSource(source);
			mxCell target = new mxCell();
			target.setGeometry(new mxGeometry(source.getGeometry().getX() + 10, source.getGeometry().getY(), 0, 0));
			axis.setTarget(target);
			
			double angle = mxGeometricUtils.calculateEdgeAngle(source, edge, axis);
			sum += Math.min(angle, Math.min(Math.PI-angle, Math.abs(Math.PI/2.0-angle))) / (Math.PI / 4.0);
		}
		
		return 1 - sum / (double) edges.length;
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
	
	public static double symmetry(mxGraph graph) {
		return symmetry(graph, 2, 10);
	}
	
	public static double symmetry(mxGraph graph, int minSymEdges, int tolerance) {
		double totalArea = 0;
		double totalSym = 0;
		graph = bendPromotion(graph);
		//TODO crosses promotion
		
		Object[] edges = graph.getChildEdges(graph.getDefaultParent());
		
		for(int i=0; i<edges.length; i++) {
			mxCell axis = calculateAxis((mxCell) edges[i]);
			
			double subSym = 0;
			List<Point> subgraphPoints = new ArrayList<Point>();
			int subgraphSize = 0;
			for(int j=0; j<edges.length; j++) {
				mxCell edge = (mxCell) edges[j];
				double sym = isMirrored(axis, edge, edges, tolerance);
				if(sym > 0) {
					subgraphSize += 1;
					subSym += sym;
					subgraphPoints.add(edge.getSource().getGeometry().getPoint());
					subgraphPoints.add(edge.getTarget().getGeometry().getPoint());
				}
			}
			
			if(subgraphSize >= minSymEdges) {
				subSym /= (double) subgraphSize;
				double subArea = mxGeometricUtils.calculateConvexArea(mxGeometricUtils.calculateConvexHull(subgraphPoints));
				totalArea += subArea;
				totalSym += subSym * subArea;
			}
		}
		
		Object[] vertices = graph.getChildVertices(graph.getDefaultParent());
		List<Point> points = new ArrayList<Point>(vertices.length);
		for(int i=0; i<vertices.length; i++) {
			points.add(((mxCell) vertices[i]).getGeometry().getPoint());
		}
		double wholeArea = mxGeometricUtils.calculateConvexArea(mxGeometricUtils.calculateConvexHull(points));
		
		return totalSym / Math.max(totalArea, wholeArea);
	}
	
	private static mxCell calculateAxis(mxCell edge) {
		double centerX = 0.5 * (edge.getSource().getGeometry().getX() + edge.getTarget().getGeometry().getX());
		double centerY = 0.5 * (edge.getSource().getGeometry().getY() + edge.getTarget().getGeometry().getY());
		double slope = (edge.getTarget().getGeometry().getY() - edge.getSource().getGeometry().getY()) /
				(edge.getTarget().getGeometry().getX() - edge.getSource().getGeometry().getX());
		double newSlope = -1.0 / slope;
		mxCell axis = new mxCell();
		mxCell source = new mxCell();
		source.setGeometry(new mxGeometry(centerX, centerY, 0, 0));
		axis.setSource(source);
		mxCell target = new mxCell();
		target.setGeometry(new mxGeometry(centerX + 10, centerY + newSlope * 10, 0, 0));
		axis.setTarget(target);
		return axis;
	}
	
	//TODO return FRACTION when edges are not of same type
	private static double isMirrored(mxCell axis, mxCell edge, Object[] edges, int tolerance) {
		mxPoint start = mxGeometricUtils.mirror(axis, edge.getSource().getGeometry());
		mxPoint end = mxGeometricUtils.mirror(axis, edge.getTarget().getGeometry());
		
		for(int i=0; i<edges.length; i++) {
			mxCell edge2 = (mxCell) edges[i];
			
			double diffStartX = edge2.getSource().getGeometry().getX() - start.getX();
			double diffStartY = edge2.getSource().getGeometry().getY() - start.getY();
			double diffEndX = edge2.getTarget().getGeometry().getX() - end.getX();
			double diffEndY = edge2.getTarget().getGeometry().getY() - end.getY();
			double distanceStart = Math.sqrt(diffStartX * diffStartX + diffStartY * diffStartY);
			double distanceEnd = Math.sqrt(diffEndX * diffEndX + diffEndY * diffEndY);
			
			if(distanceStart <= tolerance && distanceEnd <= tolerance) {
				return 1;
			}
				
			
			// check differences in case target and source should be swapped
			diffStartX = edge2.getTarget().getGeometry().getX() - start.getX();
			diffStartY = edge2.getTarget().getGeometry().getY() - start.getY();
			diffEndX = edge2.getSource().getGeometry().getX() - end.getX();
			diffEndY = edge2.getSource().getGeometry().getY() - end.getY();
			distanceStart = Math.sqrt(diffStartX * diffStartX + diffStartY * diffStartY);
			distanceEnd = Math.sqrt(diffEndX * diffEndX + diffEndY * diffEndY);
			
			if(distanceStart <= tolerance && distanceEnd <= tolerance) {
				return 1;
			}
		}
		
		return 0;
	}
	
	public static double edgeLengthDeviation(mxGraph graph) {
		Object[] edges = graph.getChildEdges(graph.getDefaultParent());
		double[] lengths = new double[edges.length];
		double mean = 0;
		double maxLength = 0;
		for(int i=0; i<edges.length; i++) {
			mxCell edge = (mxCell) edges[i];
			lengths[i] = edge.getSource().getGeometry().getPoint().distance(edge.getTarget().getGeometry().getPoint());
			mean += lengths[i];
			if(lengths[i] > maxLength)
				maxLength = lengths[i];
		}
		mean /= (double) lengths.length;
		
		double std = 0;
		for(double length : lengths) {
			std += (length - mean) * (length - mean);
		}
		std /= (double) lengths.length;
		std = Math.sqrt(std);
		
		// maxlength/2 is an upper bound for std
		return 1 - (std / (maxLength/2.0));
	}
	
	
	
	
}
