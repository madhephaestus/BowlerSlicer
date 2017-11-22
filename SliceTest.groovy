import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.vecmath.Matrix4d;

import eu.mihosoft.vrl.v3d.ext.org.poly2tri.DelaunayTriangle;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;


println "Loading slicer"
ISlice se = new ISlice (){
		


		boolean touhing(Vertex point, Edge e){
			return e.contains(point.pos);
		}
		double length(Edge e){
			
			return Math.sqrt(Math.pow(e.getP1().getX()-e.getP2().getX(),2)+
			Math.pow(e.getP1().getY()-e.getP2().getY(),2)+
			Math.pow(e.getP1().getZ()-e.getP2().getZ(),2)
				);
		}
		
		boolean same(Edge point, Edge e){
			if(e.getP1()==point.getP1() && e.getP2()==point.getP2() )
				return true;
			if(e.getP1()==point.getP2() && e.getP2()==point.getP1() )
				return true	;
				
			return false;
		}
		boolean touching(Vertex p1, Vertex p2){
			double COINCIDENCE_TOLERANCE = 0.001;
			if(Math.abs( p1.getX()-p2.getX())>COINCIDENCE_TOLERANCE){
				return false;
			}
			if(Math.abs( p1.getY()-p2.getY())>COINCIDENCE_TOLERANCE){
				return false;
			}
			if(Math.abs( p1.getZ()-p2.getZ())>COINCIDENCE_TOLERANCE){
				return false;
			}
			return true;
		}
		Vertex getUnique(Vertex desired, ArrayList<Vertex> uniquePoints){
			if(Math.abs(desired.getZ())>0.0001){
				//println "Bad point! "+desired
				throw new RuntimeException("Bad point!");
			}
			for(Vertex existing:uniquePoints)
						if(	touching(desired,existing)){
							return 	existing;		
						}
			uniquePoints.add(desired);
			return desired;
		}

		
		/**
		 * An interface for slicking CSG objects into lists of points that can be extruded back out
		 * @param incoming			  Incoming CSG to be sliced
		 * @param slicePlane		  Z coordinate of incoming CSG to slice at
		 * @param normalInsetDistance Inset for sliced output
		 * @return					  A set of polygons defining the sliced shape
		 */
		public List<Polygon> slice(CSG incoming, Transform slicePlane, double normalInsetDistance){
			//println "Groovy Slicing engine"
			
			List<Polygon> rawPolygons = new ArrayList<>();

			// Actual slice plane
			CSG planeCSG = incoming.getBoundingBox()
					.toZMin();
			// Loop over each polygon in the slice of the incoming CSG
			// Add the polygon to the final slice if it lies entirely in the z plane
			for(Polygon p: incoming
					.transformed(slicePlane)
					.intersect(planeCSG)						
					.getPolygons()){
				if(Slice.isPolygonAtZero(p)){
					rawPolygons.add(p);
				}
			}
			BowlerStudioController bc = BowlerStudioController.getBowlerStudio() 
			BowlerStudioController.clearCSG()
			
			//return rawPolygons
			//return Edge.boundaryPolygonsOfPlaneGroup(rawPolygons)		
			ArrayList<Vertex> uniquePoints = new ArrayList<>();
			ArrayList<ArrayList<Edge>> edges = new ArrayList<>();
			for(Polygon it: rawPolygons){
				ArrayList<Edge> newList = new ArrayList<>();
				edges.add(newList);
				List<Vertex> vertices = it.vertices;
				for(int i=0;i<vertices.size()-1;i++){
					try{
						newList.add(new Edge(getUnique(vertices.get(i),uniquePoints), getUnique(vertices.get(i+1),uniquePoints)));
					}catch(Exception ex){
						//println "Point Pruned "
					}
				}
				try{
					newList.add(new Edge(getUnique(vertices.get(vertices.size()-1),uniquePoints), getUnique(vertices.get(0),uniquePoints)));
				}catch(Exception ex){
					//println "Point Pruned "
				}
			}
			BowlerStudioController.clearCSG()
			bc.getJfx3dmanager().clearUserNode()
			bc.addObject((Object)rawPolygons,null)
			ThreadUtil.wait(500)
			 println "Begin checking edges"
			//edges.forEach{// search the list of all edges
			for (int k = 0; k < edges.size(); k++) {
				println "Checking list k "+k+" of "+edges.size()
				ArrayList<Edge> itList = edges.get(k);
				for (int l = 0; l < itList.size(); l++) {
					//println "Checking list l "+l+" of "+itList.size()
					//Edge myEdge = itList.get(l);
					for(int i=0;i<edges.size();i++){// for each edge we cheack every other edge
						
						ArrayList<Edge> testerList = edges.get(i);
						if(itList==testerList){
							continue;// skip comparing to itself
						}
						//println "Checking list i "+i+" of "+edges.size()
						if(fixEdgeIntersectingList(l,itList,testerList, uniquePoints)){
							i=edges.size()
							l-=1
						}
					}//i for loop
					
				}
				
				
			}
			List<Polygon> fixed =  new ArrayList<>();
					

			for(ArrayList<Edge> it: edges){
				fixed.add( Edge.toPolygon(
						Edge.toPoints(it)
						,Plane.XY_PLANE));
			}
			println "Fixed edges"
			BowlerStudioController.clearCSG()
			bc.getJfx3dmanager().clearUserNode()
			bc.addObject((Object)fixed,null)
			ThreadUtil.wait(500)
			//return rawPolygons
			//return fixed
			List<Polygon> triangles  = new ArrayList<>();
			for (int i = 0; i < fixed.size(); i++) {
				eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon p = PolygonUtil.fromCSGPolygon(fixed.get(i));
				eu.mihosoft.vrl.v3d.ext.org.poly2tri.Poly2Tri.triangulate(p);
				List<DelaunayTriangle> t = p.getTriangles();
				for (int j = 0; j < t.size(); j++)
					triangles.add(t.get(j).toPolygon());
			}
			println "Triangles"
			BowlerStudioController.clearCSG()
			bc.getJfx3dmanager().clearUserNode()
			bc.addObject((Object)triangles,null)
			ThreadUtil.wait(500)
			println "Final outline"
			List<Polygon> parts= Edge.boundaryPathsWithHoles(
	                	Edge.boundaryPaths(
	                		Edge.boundaryEdgesOfPlaneGroup(triangles)));
	          println "Returning"      		
	          BowlerStudioController.clearCSG()
	          bc.getJfx3dmanager().clearUserNode()
	          bc.addObject((Object)parts,null)     		
	          return parts    ;  		
		}
		private boolean fixEdgeIntersectingList(int l,ArrayList<Edge> itList, ArrayList<Edge> testerList,ArrayList<Vertex> uniquePoints){
			Edge myEdge = itList.get(l);
			
			for(int j=0;j<testerList.size();j++){
				Thread.sleep(0,10);// force a sleep so that interruptions can be allowed
				Edge tester=testerList.get(j);
				//println "Checking list j "+j+" of "+testerList.size()
				boolean sameLine = (myEdge.getP1().pos.equals(tester.getP1().pos)&&
									myEdge.getP2().pos.equals(tester.getP2().pos))||
				(myEdge.getP1().pos.equals(tester.getP2().pos)&&
				myEdge.getP2().pos.equals(tester.getP1().pos) )
				;
				boolean p1Shared = myEdge.getP1().pos.equals(tester.getP1().pos)||
								myEdge.getP1().pos.equals(tester.getP2().pos)
				boolean p2Shared = myEdge.getP2().pos.equals(tester.getP1().pos)||
										myEdge.getP2().pos.equals(tester.getP2().pos	)					
				boolean sharedEndPoints = 	p1Shared||p2Shared
									
				boolean onP1 = tester.contains(myEdge.getP1().pos)&& !p1Shared
				boolean onP2 = tester.contains(myEdge.getP2().pos)&& !p2Shared
										
				int baseIndex = j	
				if(	onP1&&
						onP2){
					println "Both points on line \n" +tester+" \n"+myEdge
					//sub edge lies entirely on the line
					//make 3 new edges to deal with this
					testerList.remove(tester);
					// check the relative length of points
					// we know the path of tester is 1->2 so we interupt it in that order
					Edge fe=new Edge(tester.getP1(),myEdge.getP1())
					Edge se=new Edge(tester.getP1(),myEdge.getP2())
					double lenghtFirstToFirst = length(fe);
					double lenghtFirstToSecond = length(se);
					if(lenghtFirstToFirst<lenghtFirstToSecond){
						testerList.add(baseIndex++,fe);
						testerList.add(baseIndex++,new Edge(myEdge.getP1(),myEdge.getP2()));
						testerList.add(baseIndex++,new Edge(myEdge.getP1(),tester.getP2()));
						//return fixEdgeIntersectingList(myEdge,testerList)// recourse when edge added
					}else{
						testerList.add(baseIndex++,se);
						testerList.add(baseIndex++,new Edge(myEdge.getP2(),myEdge.getP1()));
						testerList.add(baseIndex++,new Edge(myEdge.getP1(),tester.getP2()));
						//return fixEdgeIntersectingList(myEdge,testerList)// recourse when edge added
					}
					
					
				 }// if both points are on the line
				 else{// maybe one is on the line if both arent
					if(onP1){	// point one is on the line segment but not p2	
						println "P1 on line \n" +tester+" \n"+myEdge						
						testerList.remove(tester);
						testerList.add(baseIndex++,new Edge(tester.getP1(),myEdge.getP1()));
						testerList.add(baseIndex++,new Edge(myEdge.getP1(),tester.getP2()));
						//return fixEdgeIntersectingList(myEdge,testerList)// recourse when edge added
				
					}						
					if(onP2){	// point 2 is on the line not point one		
						println "P2 on line \n" +tester+" \n"+myEdge											
						testerList.remove(tester);
						testerList.add(baseIndex++,new Edge(tester.getP1(),myEdge.getP2()));
						testerList.add(baseIndex++,new Edge(myEdge.getP2(),tester.getP2()));
						//return fixEdgeIntersectingList(myEdge,testerList)// recourse when edge added
					}
				 }
				 if(!sharedEndPoints)
				 if(!onP1 && !onP2){// if both points from the testing edge are not on the line
				 	def intersectionPoint = tester.getIntersection(myEdge)
				 	if(intersectionPoint.isPresent()){
				 		int otherBase = l
				 		Vertex newVertex = getUnique(new Vertex(intersectionPoint.get(),tester.getP1().normal),uniquePoints)
				 		println "Edges are crossing at point "+newVertex
						testerList.remove(tester);
						testerList.add(baseIndex++,new Edge(tester.getP1(),newVertex));
						testerList.add(baseIndex++,new Edge(newVertex,tester.getP2()));

						itList.remove(myEdge);
						itList.add(otherBase++,new Edge(myEdge.getP1(),newVertex));
						itList.add(otherBase++,new Edge(newVertex,myEdge.getP2()));
						
				 		return false;// new vertex added, search needs to start over
				 	}
				 }
				 
				 
				 if(baseIndex!=j){
				 	println "Edges added at index "+j
				 	//Thread.sleep(100)
				 	//j-=1;
				 }
			
			}// j for loop
			
			//println "Edge is not touching this polygon without a common point"
			return true;
		}

	};

Slice.setSliceEngine(se)
// Create a CSG to slice
CSG pin = new Cylinder(10, 100)
	.toCSG()
CSG cubePin = new Cube(20,20, 100)
	.toCSG()
CSG carrot = new Cylinder(100,  10)
.toCSG()
.difference(
	[new Cylinder(40, 100)
	.toCSG(),
	pin.movex(60),
	pin.movex(-60),
	cubePin.movey(60),
	cubePin.movey(-60)
	]
	)
	//.roty(30)
	//.rotx(30)
	
Transform slicePlane = new Transform()
//slicePlane.rotY(30)
//slicePlane.rotX(30)
// Get a slice
return [Slice.slice(carrot,slicePlane, 0),carrot]