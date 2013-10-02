package com.mxgraph.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

public class mxGeometricUtilsTest {
	
	private mxCell edge;
	
	@Before
	public void setUp() throws Exception {
		mxCell start = new mxCell();
		start.setVertex(true);
		start.setGeometry(new mxGeometry(1, 1, 10, 10));
		mxCell end = new mxCell();
		end.setVertex(true);
		end.setGeometry(new mxGeometry(3, 2, 10, 10));
		edge = new mxCell();
		edge.setEdge(true);
		edge.setSource(start);
		edge.setTarget(end);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMirror() {
		mxPoint point = mxGeometricUtils.mirror(edge, new mxPoint(2, 1));
		assertEquals(1.6, point.getX(), 1e-8);
		assertEquals(1.8, point.getY(), 1e-8);
	}

}
