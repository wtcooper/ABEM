package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.Settlement;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.SimpleShapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.index.SpatialIndex;

public class TBSimpleSettlement implements Settlement{

	ShapefileHabitat settlementPolys;
	//polyCheckStart must be pre-comp period
	double polyCheckStart;
	String destination;
	SimpleShapefile shp;
	SpatialIndex spatialIndex;
	private GeometryFactory gf = new GeometryFactory();



	public TBSimpleSettlement(){
		shp = new SimpleShapefile(BOLTSParams.releaseShpFileName);	
		shp.buildSpatialIndex();
		spatialIndex = shp.getSpatialIndex();
	}

	@SuppressWarnings("unchecked")
	public synchronized void apply(Particle p){

		//		long age = p.getAge(); 
		//		double checkStart = polyCheckStart;

		if (p.getAge() >= polyCheckStart) {

			//will return a long value which is a lookup field for the seagrass shapefile (or whatever habitat file used)
			long isect = settlementPolys.intersect(p.getX(), p.getY());

			//if encounters appropriate habitat and returns a value
			if (isect != settlementPolys.NO_INTERSECTION) {

				// We have encountered a suitable polygon.  What now?
				//convert to negative lon
				double x = p.getX(); 
				if (x > 180) x =  -(360d - x);

				// get a list of all release sites within a given radius (5 cells)
				//find the closest site
				//get it's name and set the desination
				Coordinate coord = new Coordinate(x, p.getY(), 0); 

				int numCellsRadius = 5; 
				Geometry searchGeometry = getSearchBuffer(coord, 0.004*numCellsRadius); 
				List<SimpleFeature> hits = spatialIndex.query(searchGeometry.getEnvelopeInternal());
				String name = null; 

				boolean foundPoint = false; 
				//if don't find any release sites within the specified radius, than increase radius and look again
				while (!foundPoint) {
					numCellsRadius += 5; 
					searchGeometry = getSearchBuffer(coord, 0.004*numCellsRadius); 
					hits = spatialIndex.query(searchGeometry.getEnvelopeInternal());

					SimpleFeature feature = null; 
					double closestDistance = Double.MAX_VALUE; 

					for (int i = 0; i < hits.size(); i++) {
						feature = hits.get(i);
						Point point = (Point) feature.getDefaultGeometry();

						//find the closest point
						if (searchGeometry.intersects(point)) { 
							foundPoint =true; 
							double tempDistance = CoordinateUtils.getDistance(coord, new Coordinate(point.getX(), point.getY(), 0));  
							if ( tempDistance < closestDistance){
								name = (String) feature.getAttribute("NAME"); 
								closestDistance = tempDistance; 
							}
						}
					}
				} //end of while(!foundPoint)
				p.setDestination(name);
				p.setSettled(true);

			} // end of if (isect != settlementPolys.NO_INTERSECTION); i.e., has found a settlement site 
		}
		this.notifyAll();
	}

	public void setSettlementPolys(ShapefileHabitat settlementPolys) {
		this.settlementPolys = settlementPolys;
	}

	public double getPolyCheckStart() {
		return polyCheckStart;
	}

	public void setPolyCheckStart(double polyCheckStart) {
		this.polyCheckStart = polyCheckStart;
	}




	public Geometry getSearchBuffer(Coordinate midCoord, Double radius){
		// (1) Get a polygon for a circle within search radius
		Coordinate[] coords = new Coordinate[13]; 
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

		coords[12].x = coords[0].x;
		coords[12].y = coords[0].y;


		Geometry newGeometry = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coords), gf), null); 

		return newGeometry; 
	}





}

