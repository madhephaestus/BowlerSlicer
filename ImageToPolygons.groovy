import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.vecmath.Matrix4d;

import eu.mihosoft.vrl.v3d.ext.org.poly2tri.DelaunayTriangle;
import eu.mihosoft.vrl.v3d.ext.org.poly2tri.PolygonUtil;
import com.neuronrobotics.bowlerstudio.threed.Line3D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import javax.imageio.ImageIO;
import com.neuronrobotics.bowlerstudio.utils.ImageTracer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.sdk.common.Log;
import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

println "Converting image to Polygons"
if (args == null)
	return []
double scaleX = (Double)args[1]
double scaleY = (Double)args[2]
double xOffset= (Double)args[3]
double yOffset= (Double)args[4]
double imageOffsetMotion = (Double)args[5]
BufferedImage bi = (BufferedImage)args[0]
CSG slicePart = (CSG)args[6]
double MMTOPX = 3.5409643774783404*100;
float outputScale = (float) (MMTOPX)
// Options
HashMap<String, Float> options = new HashMap<String, Float>();
// Tracing
options.put("ltres", 1f);// Error treshold for
                          // straight lines.
options.put("qtres", 1f);// Error treshold for
                          // quadratic splines.
options.put("pathomit", 0.02f);// Edge node paths
                          // shorter than this
                          // will be discarded for
                          // noise reduction.

// Color quantization
options.put("colorsampling", 1f); // 1f means true ;
                                  // 0f means
                                  // false:
                                  // starting with
                                  // generated
                                  // palette
options.put("numberofcolors", 16f);// Number of
                                  // colors to use
                                  // on palette if
                                  // pal object is
                                  // not defined.
options.put("mincolorratio", 0.02f);// Color
                                  // quantization
                                  // will
                                  // randomize a
                                  // color if
                                  // fewer pixels
                                  // than (total
                                  // pixels*mincolorratio)
                                  // has it.
options.put("colorquantcycles", 1f);// Color
                                  // quantization
                                  // will be
                                  // repeated this
                                  // many times.
//
// SVG rendering
options.put("scale", outputScale);// Every
                                  // coordinate
                                  // will be
                                  // multiplied
                                  // with this, to
                                  // scale the
                                  // SVG.
options.put("simplifytolerance", 1f);//
options.put("roundcoords", 2f); // 1f means rounded
                              // to 1 decimal
                              // places, like 7.3
                              // ; 3f means
                              // rounded to 3
                              // places, like
                              // 7.356 ; etc.
options.put("lcpr", 0f);// Straight line control
                      // point radius, if this is
                      // greater than zero, small
                      // circles will be drawn in
                      // the SVG. Do not use this
                      // for big/complex images.
options.put("qcpr",0f);// Quadratic spline control
                      // point radius, if this is
                      // greater than zero, small
                      // circles and lines will be
                      // drawn in the SVG. Do not
                      // use this for big/complex
                      // images.
options.put("desc", 0f); // 1f means true ; 0f means
                          // false: SVG
                          // descriptions
                          // deactivated
options.put("viewbox", 1f); // 1f means true ; 0f
                          // means false: fixed
                          // width and height

// Selective Gauss Blur
options.put("blurradius", 0f); // 0f means
                              // deactivated; 1f
                              // .. 5f : blur with
                              // this radius
options.put("blurdelta", 20f); // smaller than this
                          // RGB difference
                          // will be blurred


String svg = com.neuronrobotics.bowlerstudio.utils.ImageTracer.imageToSVG(bi,options,(byte[][])null)
int headerStart = svg.indexOf(">")+1
int headerEnd = svg.lastIndexOf("<")
//println "headerStart "+headerStart+ " headerEnd "+headerEnd
String header = svg.substring(0,headerStart)
String footer = svg.substring(headerEnd,svg.size())
String body = svg.substring(headerStart,headerEnd)
body = "<g id=\"g37\">\n"+body+"</g>\n"
svg=header+body+footer
//println header+"\n\n"
//println body+"\n\n"
//println footer+"\n\n"
File tmpsvg = new File( System.getProperty("java.io.tmpdir")+"/"+Math.random())
tmpsvg.createNewFile()
FileWriter fw = new FileWriter(tmpsvg.getAbsoluteFile());
BufferedWriter bw = new BufferedWriter(fw);
bw.write(svg);
bw.close();
double totalScale =scaleX/MMTOPX
Transform tr = new Transform()
			.translate(xOffset-imageOffsetMotion, yOffset-imageOffsetMotion,0)
			.scale(totalScale)
			//
SVGLoad l=new SVGLoad(tmpsvg.toURI())	
l.loadAllGroups(0.0004, 0.0, 0.0);
ArrayList<Polygon>  svgPolys = l.toPolygons().collect{
	it.transform(tr)
}
tmpsvg.delete()


def okParts = []
for(int x=0;x<svgPolys.size();x++){
	Polygon tester = svgPolys.get(x)
	Bounds b=tester.getBounds()
	CSG box =  b.toCSG() 
	boolean okToAdd=true
	if(	(slicePart.getTotalX()<(box.getTotalX()-imageOffsetMotion))&&
		(slicePart.getTotalY()<(box.getTotalY()-imageOffsetMotion))
	){
		okToAdd=false
		continue;
	}
	for(Polygon p:okParts){
		Bounds bp=p.getBounds()
		CSG bpBox =bp.toCSG()
		double xdiff = Math.abs(bpBox.getTotalX()-box.getTotalX())
		double ydiff = Math.abs(bpBox.getTotalY()-box.getTotalY())
		double xdiffCenter = Math.abs(box.getCenter().x-bpBox.getCenter().x)
		double ydiffCenter =Math.abs(box.getCenter().y-bpBox.getCenter().y)
		double delta =0.000001
		if(	(xdiff<delta)&&
			(ydiff<delta) &&
			(xdiffCenter<delta)&&
			(ydiffCenter<delta)
		){
			
			okToAdd=false
			//break;
		}
	}
	if(okToAdd){
		okParts.add(svgPolys.get(x))
	}
}
println "CSG Sliced to "+okParts.size()+" polygons "
//println svg
//BowlerStudioController.getBowlerStudio() .addObject((Object)okParts,(File)null)
return 	okParts