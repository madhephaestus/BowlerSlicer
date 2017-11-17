import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.vecmath.Matrix4d;

import eu.mihosoft.vrl.v3d.ext.org.poly2tri.DelaunayTriangle;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;



ISlice se = new ISlice (){
	
	
	
	float betweenPointAndLine(Vertex point, Edge e){
	        float[] PointThing = new float[3];
	        float[] TotalThing = new float[3];
	        Vertex lineStart=e.p
	        Vertex lineEnd=e.q
	        PointThing[0] = lineStart.x - point.x;
	        PointThing[1] = lineStart.y - point.y;
	        PointThing[2] = lineStart.z - point.z;
	
	        TotalThing[0] = ((PointThing[1]*lineEnd.z) - (PointThing[2]*lineEnd.y));
	        TotalThing[1] = -((PointThing[0]*lineEnd.z) - (PointThing[2]*lineEnd.x));
	        TotalThing[2] = ((PointThing[0]*lineEnd.y) - (PointThing[1]*lineEnd.x));
	
	    float distance = (float) (Math.sqrt((TotalThing[0]*TotalThing[0]) + (TotalThing[1]*TotalThing[1]) + (TotalThing[2]*TotalThing[2])) /
	                    Math.sqrt((lineEnd.x * lineEnd.x) + (lineEnd.y * lineEnd.y) + (lineEnd.z * lineEnd.z) ));
	
	
	    return distance;
	}


	boolean touhing(Vertex point, Edge e){
		return e.contains(point.pos)
	}
	double length(Edge e){
		
		return Math.sqrt(Math.pow(e.p1.x-e.p2.x,2)+
		Math.pow(e.p1.y-e.p2.y,2)+
		Math.pow(e.p1.z-e.p2.z,2)
			)
	}
	
	boolean same(Edge point, Edge e){
		if(e.p1==point.p1 && e.p2==point.p2 )
			return true
		if(e.p1==point.p2 && e.p2==point.p1 )
			return true	
			
		return false
	}
	boolean touching(Vertex p1, Vertex p2){
		double COINCIDENCE_TOLERANCE = 0.001;
		if(Math.abs( p1.x-p2.x)>COINCIDENCE_TOLERANCE){
			return false;
		}
		if(Math.abs( p1.y-p2.y)>COINCIDENCE_TOLERANCE){
			return false;
		}
		if(Math.abs( p1.z-p2.z)>COINCIDENCE_TOLERANCE){
			return false;
		}
		return true
	}
	Vertex getUnique(Vertex desired, ArrayList<Vertex> uniquePoints){
		if(Math.abs(desired.z)>0.0001){
			println "Bad point! "+desired
			throw new RuntimeException("Bad point!")
		}
		for(Vertex existing:uniquePoints)
					if(	touching(desired,existing)){
						return 	existing;		
					}
		uniquePoints.add(desired)
		return desired
	}
	boolean falseBoundaryEdgeSharedWithOtherEdge(Edge fbe, Edge e) {

        // we don't consider edges with shared end-points since we are only
        // interested in "false-boundary-edge"-cases
        boolean sharedEndPoints = e.getP1().pos.equals(fbe.getP1().pos)|| 
					        e.getP1().pos.equals(fbe.getP2().pos)|| 
					        e.getP2().pos.equals(fbe.getP1().pos)||
					        e.getP2().pos.equals(fbe.getP2().pos);

        if (sharedEndPoints) {
            return false;
        }

        return fbe.contains(e.getP1().pos) || fbe.contains(e.getP2().pos);
    }
    
	/**
	 * Returns true if this polygon lies entirely in the z plane
	 *
	 * @param polygon
	 *            The polygon to check
	 * @return True if this polygon is entirely in the z plane
	 */
	private static boolean isPolygonAllAtZero(Polygon polygon) {
		// Return false if there is a vertex in this polygon which is not at
		// zero
		// Else, the polygon is at zero if every vertex in it is at zero
		for (Vertex v : polygon.vertices)
			if (Slice.isVertexAtZero(v))
				return true;

		return false;
	}
	
	/**
	 * An interface for slicking CSG objects into lists of points that can be extruded back out
	 * @param incoming			  Incoming CSG to be sliced
	 * @param slicePlane		  Z coordinate of incoming CSG to slice at
	 * @param normalInsetDistance Inset for sliced output
	 * @return					  A set of polygons defining the sliced shape
	 */
	List<Polygon> slice(CSG incoming, Transform slicePlane, double normalInsetDistance){
		println "Groovy Slicing engine"
		
		List<Polygon> rawPolygons = new ArrayList<>();

		// Actual slice plane
		CSG planeCSG = incoming.getBoundingBox()
				.toZMin()
		// Loop over each polygon in the slice of the incoming CSG
		// Add the polygon to the final slice if it lies entirely in the z plane
		rawPolygons
			.addAll(
					incoming
						.intersect(planeCSG)						
						.getPolygons()
						.findAll{Slice.isPolygonAtZero(it)}
						.collect{
							for(Vertex v:it.vertices){
								//v.pos=new Vector3d(v.pos.x,v.pos.y,0.0)
							}
							return it
						}
				);
		CSG flat = CSG.fromPolygons(rawPolygons)
		flat=flat.union(flat)
		
		//return Edge.boundaryPolygonsOfPlaneGroup(rawPolygons)		
		ArrayList<Vertex> uniquePoints = []
		ArrayList<ArrayList<Edge>> edges = []
		rawPolygons.forEach{
			ArrayList<Edge> newList = []
			edges.add(newList)
			List<Vertex> vertices = it.vertices;
			for(int i=0;i<vertices.size()-1;i++){
				try{
					newList.add(new Edge(getUnique(vertices.get(i),uniquePoints), getUnique(vertices.get(i+1),uniquePoints)))
				}catch(Exception ex){
					println "Point Pruned "
				}
			}
			try{
				newList.add(new Edge(getUnique(vertices.get(vertices.size()-1),uniquePoints), getUnique(vertices.get(0),uniquePoints)))
			}catch(Exception ex){
				println "Point Pruned "
			}
		}
		
		edges.forEach{// search the list of all edges
			for(Edge myEdge:it){// search through the edges in each list
				for(int i=0;i<edges.size();i++){// for each edge we cheack every other edge
					testerList = edges.get(i)
					for(int j=0;j<testerList.size();j++){
						Edge tester=testerList.get(j)
						if(tester==myEdge){
							continue;// skip comparing to itself
						}
						if(falseBoundaryEdgeSharedWithOtherEdge(tester,myEdge)
						
						){
							
							testerList.remove(tester)
							double lenghtFirstToFirst = length(new Edge(tester.p1,myEdge.p1))
							double lenghtFirstToSecond = length(new Edge(tester.p1,myEdge.p2))
							if(lenghtFirstToFirst<lenghtFirstToSecond){
								testerList.add(j,new Edge(tester.p1,myEdge.p1))
								testerList.add(j+1,new Edge(myEdge.p1,tester.p2))
							}else{
								testerList.add(j,new Edge(tester.p1,myEdge.p2))
								testerList.add(j+1,new Edge(myEdge.p2,tester.p2))
							}
							
							println "Line touching but not the same! "+length(myEdge)+" other "+length(tester)
							
						}
					}
				}
			}
		}
		List<Polygon> fixed =  edges.collect{
			return Edge.toPolygon(
					Edge.toPoints(it)
					,Plane.XY_PLANE)
		}

		//return fixed
		List<Polygon> triangles  = []
		for (int i = 0; i < fixed.size(); i++) {
			eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon p = PolygonUtil.fromCSGPolygon(fixed.get(i));
			eu.mihosoft.vrl.v3d.ext.org.poly2tri.Poly2Tri.triangulate(p);
			List<DelaunayTriangle> t = p.getTriangles();
			for (int j = 0; j < t.size(); j++)
				triangles.add(t.get(j).toPolygon());
		}
		//return triangles
		return Edge.boundaryPathsWithHoles(
                	Edge.boundaryPaths(
                		Edge.boundaryEdgesOfPlaneGroup(triangles)))
	}
	Polygon findboarder(List<Polygon> triangles,ArrayList<Vertex> uniquePoints){
		
	}
	boolean check(Vertex v,ArrayList<Vertex> uniquePoints){
		
	}
	void consume(Vertex v,ArrayList<Vertex> uniquePoints){
		
	}
}

Slice.setSliceEngine(se)
// Create a CSG to slice
CSG carrot = new Cube(10, 10, 10)
.toCSG()
.toXMin()
.difference(
	new Cube(4, 4, 100)
	.toCSG()
	.toXMin()
	)
	

// Get a slice
return [Slice.slice(carrot, new Transform(), 0),carrot]