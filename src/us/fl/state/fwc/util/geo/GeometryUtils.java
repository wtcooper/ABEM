package us.fl.state.fwc.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class GeometryUtils {

	
	public static Geometry getSearchBuffer(Coordinate midCoord, Double radius){
		// (1) Get a polygon geometry for a dodecagon within search radius
		Coordinate[] coords = new Coordinate[12]; 
		for (int i=0; i<coords.length; i++){
			coords[i] = new Coordinate(0,0,0); 
		}
		// set the first coordinate in the coords array to the point at 0 angle
		coords[0].x = midCoord.x+radius;
		coords[0].y = midCoord.y; 

		double angleIncrement = (Math.PI*2)/coords.length; // in radians, this is angle between each polygon point of circle, based on the total number of circle points defined in Sampler
		double angle = angleIncrement; // start at first increment

		for (int i=1; i<coords.length; i++){
			coords[i].x = midCoord.x + (Math.cos(angle)*radius); 
			coords[i].y = midCoord.y + (Math.sin(angle)*radius); 
			angle += angleIncrement; 
		}

		coords[11].x = coords[0].x;
		coords[11].y = coords[0].y;

		GeometryFactory gf =  new GeometryFactory();
		Geometry newGeometry = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 

		return newGeometry; 
	}
	
	public static Geometry getGeometry(Coordinate[] coords){
		GeometryFactory gf =  new GeometryFactory();
		return (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 
	}

}
