import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.vecmath.Matrix4d;

import eu.mihosoft.vrl.v3d.ext.org.poly2tri.DelaunayTriangle;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;

Slice.setSliceEngine({ incoming, slicePlane, normalInsetDistance -> 
		println "Groovy Slicing engine"
		double COINCIDENCE_TOLERANCE = 0.0001;
		List<Polygon> rawPolygons = new ArrayList<>();

		// Invert the incoming transform
		Matrix4d inverse = slicePlane.scale(1.0D / slicePlane.getScale()).getInternalMatrix();
		inverse.invert();

		// Actual slice plane
		CSG planeCSG = new Cube(incoming.getMaxX() - incoming.getMinX(), incoming.getMaxY() - incoming.getMinY(), 1)
				.noCenter().toCSG();
		planeCSG = planeCSG.movex((planeCSG.getMaxX() - planeCSG.getMinX()) / -2.0D)
				.movey((planeCSG.getMaxY() - planeCSG.getMinY()) / -2.0D);
		incoming.getPolygons();

		// Loop over each polygon in the slice of the incoming CSG
		// Add the polygon to the final slice if it lies entirely in the z plane
		rawPolygons
			.addAll(
					incoming
						.intersect(planeCSG)
						.getPolygons()
						.findAll{Slice.isPolygonAtZero(it)}
						.collect{it}
				);

		/* Convert the list of polygons to a list of triangles */
		List<Polygon> triangles = new ArrayList<>();
		for (int i = 0; i < rawPolygons.size(); i++) {
			eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon p = PolygonUtil.fromCSGPolygon(rawPolygons.get(i));
			eu.mihosoft.vrl.v3d.ext.org.poly2tri.Poly2Tri.triangulate(p);
			List<DelaunayTriangle> t = p.getTriangles();
			for (int j = 0; j < t.size(); j++)
				triangles.add(t.get(j).toPolygon());
		}

		/* List every edge */
		List<Edge> edges = new ArrayList<>();
		for (Polygon t : triangles) {
			if(t.vertices.size()!=3){
				throw new RuntimeException("Triangulation failed and an invalid poly is created!");
			}
			edges.add(new Edge(t.vertices.get(0), t.vertices.get(1)));
			edges.add(new Edge(t.vertices.get(1), t.vertices.get(2)));
			edges.add(new Edge(t.vertices.get(2), t.vertices.get(0)));
		}

		/* Remove internal edges */
//		for (int i = 0; i < edges.size(); i++) {
//			boolean match = false;
//			for (int j = 0; j < edges.size() && !match; j++) {
//				if (edges.get(i)
//						.getP1()
//						.pos
//						.minus(
//								edges
//								.get(j)
//								.getP2().pos)
//						.magnitude() <= COINCIDENCE_TOLERANCE
//						&& edges.get(i).getP2().pos.minus(edges.get(j).getP1().pos)
//								.magnitude() <= COINCIDENCE_TOLERANCE) {
//					edges.remove(i);
//					edges.remove(j);
//					i--;
//					match = false;
//				}
//			}
//		}
//
//		/* Generate polygons from edges */
//		List<Polygon> polygons = new ArrayList<>();
//		for (int edgeIndex = 0; edges.size() > 0;) {
//			List<Vertex> vertices = new ArrayList<>();
//			vertices.add(edges.get(0).getP1());
//			for (; edges.get(edgeIndex).getP2().pos.minus(vertices.get(0).pos).magnitude() <= COINCIDENCE_TOLERANCE;) {
//				vertices.add(edges.get(edgeIndex).getP2());
//				edges.remove(edgeIndex);
//				for (; edgeIndex < edges.size() && !(vertices.get(vertices.size() - 1).pos
//						.minus(edges.get(edgeIndex).getP1().pos).magnitude() <= COINCIDENCE_TOLERANCE); edgeIndex++)
//					;
//			}
//			edges.remove(edgeIndex);
//			polygons.add(new Polygon(vertices));
//		}

		//return polygons;
		return triangles;
	})
// Create a CSG to slice
CSG carrot = new Cube(10, 10, 10).toCSG().difference(new Cube(4, 4, 100).toCSG());

// Get a slice
return Slice.slice(carrot, new Transform(), 0);