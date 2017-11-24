import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.vecmath.Matrix4d;

import eu.mihosoft.vrl.v3d.ext.org.poly2tri.DelaunayTriangle;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;
import com.neuronrobotics.bowlerstudio.threed.Line3D;


println "Loading slicer"
ISlice se = new ISlice (){
		
		BowlerStudioController bc = BowlerStudioController.getBowlerStudio() 
	     //BowlerStudioController.clearCSG()

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

		Vertex existing (Vertex desired, ArrayList<Vertex> uniquePoints){
			if(Math.abs(desired.getZ())>0.0001){
				//println "Bad point! "+desired
				throw new RuntimeException("Bad point!");
			}
			for(Vertex existing:uniquePoints)
						if(	touching(desired,existing)){
							return 	existing;		
						}
			return null;
		}
		Vertex getUnique(Vertex desired, ArrayList<Vertex> uniquePoints){
			Vertex exist = existing(desired,uniquePoints)
			if(exist!= null){
				return exist
			}
			uniquePoints.add(desired);
			return desired;
		}
		boolean triangleMatchList(Polygon tester,List<Polygon> other){
			for(Polygon p:other){
				if(triangleMatch(tester,p)){
					return true
				}
			}
			return false
		}
		boolean triangleMatch(Polygon tester,Polygon other){
			List<Edge> A = Edge.fromPolygon(tester)
			List<Edge> B = Edge.fromPolygon(other)
			for(int i=0;i<3;i++){
				int i0=i
				int i1=i+1
				int i2=i+2
				if(i1>2)
					i1=i1-3
				if(i2>2)
					i2=i2-3	
				//println "Testing trangle orentation "+i0+" "+i1+ " "+i2
				if(	edgeMatch(A.get(0), B.get(i0)) &&
					edgeMatch(A.get(1), B.get(i2)) &&
					edgeMatch(A.get(2), B.get(i3)) 
				){
					println "Matching tringle found"
					return true
				}
			}
			return false
		}
		ArrayList<Polygon> filterDuplicateTrangles(List<Polygon> incoming){
			ArrayList<Polygon> unique = []
			for(Polygon tester:incoming){
				if(!triangleMatchList(tester,unique)){
					unique.add(tester)
				}
			}
			return unique
		}
		boolean edgeMatch(Edge tester,Edge myEdge){
			if((tester!=null) && (myEdge!=null)){
				boolean p1Shared =  eq(myEdge.getP1().pos,tester.getP1().pos)&&
								eq(myEdge.getP2().pos,tester.getP2().pos)
				boolean p2Shared =  eq(myEdge.getP1().pos,tester.getP2().pos)&&
								eq(myEdge.getP2().pos,tester.getP1().pos	)					
				return p1Shared||p2Shared
				
			}
			return false
		}
		ArrayList<Edge> uniqueOnly(ArrayList<Edge> newList){
			ArrayList<Edge> edgesOnly = []
			for(int i=0;i<newList.size()-1;i++){
				Edge myEdge = newList.get(i);
				if(myEdge!=null){
					boolean internalEdge = false;
					
					for(int j=0;j<newList.size();j++){
						if(i!=j){
							Edge tester=newList.get(j);
							if(tester!=null){
								if(edgeMatch(tester,myEdge)){
									//println "Internal Line "+myEdge+" "+tester
									internalEdge=true;
								}
							}
						}
					}
					if(internalEdge==false){
						if(length(myEdge)>Plane.EPSILON)
							edgesOnly.add(myEdge)
						
					}
				}
			}
			return edgesOnly
		}

		void addEdges(Polygon p,ArrayList<Edge> newList,ArrayList<Vertex> uniquePoints){
			List<Vertex> vertices = p.vertices;
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
			
			
			//return rawPolygons
			//return Edge.boundaryPolygonsOfPlaneGroup(rawPolygons)		
			ArrayList<Vertex> uniquePoints = new ArrayList<>();
			ArrayList<ArrayList<Edge>> edges = new ArrayList<>();
			for(Polygon it: rawPolygons){
				ArrayList<Edge> newList = new ArrayList<>();
				edges.add(newList);
				addEdges(it,newList,uniquePoints)
			}
			//println "raw"
			//BowlerStudioController.clearCSG()
			//bc.getJfx3dmanager().clearUserNode()
			//bc.addObject((Object)rawPolygons,null)
			//ThreadUtil.wait(500)
			 //println "Begin checking edges"
			 
			//edges.forEach{// search the list of all edges
			for (int k = 0; k < edges.size(); k++) {
				//println "Checking list k "+k+" of "+edges.size()
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
						if(!fixEdgeIntersectingList(l,itList,testerList, uniquePoints)){
							i=edges.size()
							//l=l-1
							//println "Falling out of loop to re-search"
						}
					}//i for loop
					
				}

			}
			List<Polygon> fixed =  new ArrayList<>();
			
			for(ArrayList<Edge> it: edges){
				if(it.size()>2){
					fixed.add( Edge.toPolygon(
							Edge.toPoints(it)
							,Plane.XY_PLANE));
				}
			}		

			//println "Fixed"
			//BowlerStudioController.clearCSG()
			//bc.getJfx3dmanager().clearUserNode()
			//bc.addObject((Object)fixed,null)
			//ThreadUtil.wait(3000)
			//return rawPolygons
			//return fixed
			
			//List<Polygon> triangles  = new ArrayList<>();
			//for (int i = 0; i < fixed.size(); i++) {
			//	trianglesFromPolygon(fixed.get(i),triangles, uniquePoints )
			//}
			//List<Polygon> trianglesFiletered = filterDuplicateTrangles(triangles)
			//println "Started with "+triangles.size()+"triangles, filtered to "+trianglesFiletered.size()
			ArrayList<Edge> allEdges = []
			for(Polygon p:fixed){
				addEdges(p,allEdges,uniquePoints)
			}
			ArrayList<Edge> finalEdges=uniqueOnly(allEdges)
			//println "Final edges = "+finalEdges.size()+" from "+allEdges.size()
			//println "Edges Filtered"
			//BowlerStudioController.clearCSG()
			//bc.getJfx3dmanager().clearUserNode()
			//bc.addObject((Object)trianglesFiletered,null)
			//showEdges(finalEdges)
			//ThreadUtil.wait(1000)
			
			//return trianglesFiletered
			//println "Final outline"
			//List<Polygon> parts= Edge.boundaryPathsWithHoles(
	        //        	Edge.boundaryPaths(
	         //       		Edge.boundaryEdgesOfPlaneGroup(triangles)));
	         List<Polygon>boundaryPaths =  boundaryPaths(finalEdges)
	         //println "Boundary paths = "+boundaryPaths.size()
	         List<Polygon> parts= Edge.boundaryPathsWithHoles(boundaryPaths);       		
		    //println "Returning "  +parts.size()    		
		    return parts;  		
		}

		static boolean eq(eu.mihosoft.vrl.v3d.Vector3d v ,eu.mihosoft.vrl.v3d.Vector3d other){
		        if (Math.abs(v.x - other.x) > 0.001) {
		            return false;
		        }
		        if (Math.abs(v.y - other.y) > 0.001) {
		            return false;
		        }
		        if (Math.abs(v.z - other.z) > 0.001) {
		            return false;
		        }
		        return true;
		}
		/**
	     * Returns a list of all boundary paths.
	     *
	     * @param boundaryEdges boundary edges (all paths must be closed)
	     * @return the list
	     */
	    public static List<Polygon> boundaryPaths(List<Edge> boundaryEdges) {

			// the resulting boundary edge
			List<Polygon> result = new ArrayList<>();
			ArrayList<Edge> consumable = []
			for(Edge e:boundaryEdges){
				consumable.add(e)
			}
			List<eu.mihosoft.vrl.v3d.Vector3d> boundaryPath = new ArrayList<>();
			while(consumable.size()>0){
				Edge next=null;
				if(boundaryPath.size()==0){
					//println "Loading new path"
					next = consumable.remove(0)
					boundaryPath.add(next.getP1().pos)
					boundaryPath.add(next.getP2().pos)
				}else{
					eu.mihosoft.vrl.v3d.Vector3d v =boundaryPath.get(boundaryPath.size()-1)
					for(int i=0;i<consumable.size() && next==null;i++){
						Edge e = consumable.get(i)
						if(eq(v,e.getP1().pos)){
							consumable.remove(e)
							boundaryPath.add(e.getP2().pos)
							next = e
						}else if(eq(v,e.getP2().pos)){
							consumable.remove(e)
							boundaryPath.add(e.getP2().pos)
							next = e
						}
					}
					if(next == null){
						//println  " equals no point! "+v
						//boundaryPath.remove(v)
						if(boundaryPath.size()>2){
							boundaryPath.add(boundaryPath.get(boundaryPath.size()-1))
							result.add(Polygon.fromPoints(boundaryPath));
							println "Hanging point Polygon, adding "+boundaryPath.size()
							boundaryPath.clear()
						}else{
							println "Hanging point wih no ploygon, rejecting "+boundaryPath.size()
							boundaryPath.clear()
						}
						
					}
				}
				// check to see the path closed
				if(boundaryPath.size()>2){
					if(eq(boundaryPath.get(0),boundaryPath.get(boundaryPath.size()-1))){
						Polygon p = Polygon.fromPoints(boundaryPath)
						result.add(p);
						//println "Regular polygon detected and added "+boundaryPath.size()
						boundaryPath.clear()
						
					}
				}
				Thread.sleep(1)
			}
			if(boundaryPath.size()>2){
				//println "Last Polygon, correcting"
				boundaryPath.add(boundaryPath.get(boundaryPath.size()-1))
				result.add(Polygon.fromPoints(boundaryPath));
				println "Last Polygon, adding "+boundaryPath.size()
				boundaryPath.clear()
				boundaryPath.clear()
			}
			
			return result;
	    }
		
		ArrayList<Line3D> showEdges(ArrayList<Edge> edges){
			javafx.scene.paint.Color color = new javafx.scene.paint.Color(Math.random()*0.5+0.5,Math.random()*0.5+0.5,Math.random()*0.5+0.5,1);
			 ArrayList<Line3D> lines =[]
			for(Edge e: edges){
				Line3D line = new Line3D(e.getP1(),e.getP2());
				line.setStrokeWidth(0.5);
				line.setStroke(color);
				lines .add(line);
				bc.addNode(line)
			}
			return lines
		}
		void trianglesFromPolygon(Polygon tester, List<Polygon> triangles,ArrayList<Vertex>  uniquePoints ){
			
			List<Vector3d> vertices = Extrude.toCCW(tester.vertices.collect{it.pos});
			List<Polygon> workingPoly = Polygon.fromConcavePoints(vertices)
			if(vertices.size()==3){
				add(workingPoly.get(0),triangles,uniquePoints);
				return
			}
			for(Polygon newworking:workingPoly){
				eu.mihosoft.vrl.v3d.ext.org.poly2tri.Polygon p = PolygonUtil.fromCSGPolygon(newworking);
				eu.mihosoft.vrl.v3d.ext.org.poly2tri.Poly2Tri.triangulate(p);
				List<DelaunayTriangle> t = p.getTriangles();
				for (DelaunayTriangle d:t){
					Polygon testPoly =d.toPolygon()
					add(testPoly,triangles,uniquePoints)
				}
			}
		}
		private void add(Polygon tester, List<Polygon> triangles,ArrayList<Vertex>  uniquePoints){
			List<Vertex> vertices = tester.vertices;
			boolean badPoint = false
			for (Vertex v:vertices) {
				if( existing (v, uniquePoints) ==null ){
					badPoint=true;
					println "Dumping triangle with bad point "+v
				}
			}
			if(badPoint == false){
				triangles.add(tester);
			}
		}
		private boolean fixEdgeIntersectingList(int l,ArrayList<Edge> itList, ArrayList<Edge> testerList,ArrayList<Vertex> uniquePoints){
			Edge myEdge = itList.get(l);
			
			for(int j=0;j<testerList.size();j++){
				//Thread.sleep(0,10);// force a sleep so that interruptions can be allowed
				Edge tester=testerList.get(j);
				//println "Checking list j "+j+" of "+testerList.size()

				boolean p1Shared = eq(myEdge.getP1().pos,tester.getP1().pos)||
								eq(myEdge.getP1().pos,tester.getP2().pos)
				boolean p2Shared =eq( myEdge.getP2().pos,tester.getP1().pos)||
										eq(myEdge.getP2().pos,tester.getP2().pos	)					
				boolean sharedEndPoints = 	p1Shared||p2Shared
									
				boolean onP1 = tester.contains(myEdge.getP1().pos)&& !p1Shared
				boolean onP2 = tester.contains(myEdge.getP2().pos)&& !p2Shared
										
				int baseIndex = j	
				if(	onP1&&
						onP2){
					//println "Both on line \n" +myEdge.getP1()	+" "+myEdge.getP2()	
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
						//println "P1 on line " +myEdge.getP1()					
						testerList.remove(tester);
						testerList.add(baseIndex++,new Edge(tester.getP1(),myEdge.getP1()));
						testerList.add(baseIndex++,new Edge(myEdge.getP1(),tester.getP2()));
						//return fixEdgeIntersectingList(myEdge,testerList)// recourse when edge added
				
					}						
					if(onP2){	// point 2 is on the line not point one		
						//println "P2 on line " +myEdge.getP2()									
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
				 		//println "Edges are crossing at point "+newVertex
						testerList.remove(tester);
						testerList.add(baseIndex++,new Edge(tester.getP1(),newVertex));
						testerList.add(baseIndex++,new Edge(newVertex,tester.getP2()));

						itList.remove(myEdge);
						itList.add(otherBase++,new Edge(myEdge.getP1(),newVertex));
						itList.add(otherBase++,new Edge(newVertex,myEdge.getP2()));
						//myEdge=itList.get(l);
						//j=-1// restart loop
				 		return false
				 	}
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
	.toCSG()
	.movex(75)
	,
	pin.movex(60),
	pin.movex(-60),
	cubePin.movey(60),
	cubePin.movey(-60)
	]
	)
	//.roty(30)
	//.rotx(30)
	
Transform slicePlane = new Transform()
def headParts  = (ArrayList<CSG> )ScriptingEngine.gitScriptRun(
	"https://github.com/madhephaestus/ParametricAnimatronics.git", 
	"AnimatronicHead.groovy" ,  
	[false] )
List<Polygon> allParts = []

headParts.forEach{
		println it.getName()+" Adding parts "+allParts.size()
		myParts = Slice.slice(it.prepForManufacturing(),slicePlane, 0)
		BowlerStudioController
			.getBowlerStudio() 
			.addObject((Object)myParts,null)
		allParts.addAll(myParts)
		


	}

return allParts