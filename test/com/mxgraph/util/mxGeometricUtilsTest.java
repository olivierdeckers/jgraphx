package com.mxgraph.util;

import static org.junit.Assert.assertEquals;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

public class mxGeometricUtilsTest {
	
	@Before
	public void setUp() throws Exception {
	}
	
	public mxCell createEdge(double x1, double y1, double x2, double y2) {
		mxCell start = new mxCell();
		start.setGeometry(new mxGeometry(x1, y1, 0, 0));
		mxCell end = new mxCell();
		end.setGeometry(new mxGeometry(x2, y2, 0, 0));
		mxCell edge = new mxCell();
		edge.setSource(start);
		edge.setTarget(end);
		return edge;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMirror() {
		mxCell edge = createEdge(1, 1, 3, 2);
		mxPoint point = mxGeometricUtils.mirror(edge, new mxPoint(2, 1));
		assertEquals(1.6, point.getX(), 1e-8);
		assertEquals(1.8, point.getY(), 1e-8);
	}
	
	@Test
	public void testMirrorAxisOriginBeyond() {
		mxCell edge = createEdge(3, 2, 5, 3);
		mxPoint point = mxGeometricUtils.mirror(edge, new mxPoint(2, 1));
		assertEquals(1.6, point.getX(), 1e-8);
		assertEquals(1.8, point.getY(), 1e-8);
	}
	
	private List<Point> getPoints() {
		ArrayList<Point> points = new ArrayList<Point>();
		points.add(new Point(0, 0));
		points.add(new Point(10,0));
		points.add(new Point(10,10));
		points.add(new Point(0,10));
		points.add(new Point(5,5));
		return points;
	}
	
	@Test
	public void testConvexHull() {
		assertEquals(4, mxGeometricUtils.calculateConvexHull(getPoints()).size());
	}
	
	@Test
	public void testConvexArea() {
		List<Point> hull = mxGeometricUtils.calculateConvexHull(getPoints());
		assertEquals(100, mxGeometricUtils.calculateConvexArea(hull), 1e-5);
	}

}
