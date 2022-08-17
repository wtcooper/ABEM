package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import us.fl.state.fwc.abem.dispersal.bolts.Barrier;
import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.util.Bouncer;
import us.fl.state.fwc.util.TestingUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class ShapefileBarrier extends ShapefileHabitat implements Barrier{
	
	/**
	 * Checks to see if the path of the particle intersects the shapefile, and
	 * if so provides reflected coordinates.
	 * 
	 * @param p
	 */

	@SuppressWarnings("unchecked")
	public synchronized void checkReflect(Particle p) {

		Bouncer b = new Bouncer();
		int bounces = 0;
		
		// Get endpoints of the intersecting line.
		
		double px = p.getPX();
		double py = p.getPY();
		double x = p.getX();
		double y = p.getY();
		
		// Convert if using negative longitude values.

		if (neglon) {
			px = cvt(px);
			x = cvt(x);
		}
		
		// Turn the double values into Coordinate objects
		
		Coordinate p1 = new Coordinate(px, py);
		Coordinate p2 = new Coordinate(x, y);
		
		// Create the line
		
		LineString ls = gf.createLineString(new Coordinate[] { p1, p2 });
		
		// First pass query - does the line intersect an envelope?
		
		SimpleFeature f = null;
		Geometry g = null;
		double d;
		int select = -1;

		List<SimpleFeature > fl = index.query(ls.getEnvelopeInternal());

		// If there is no intersection, then break.
		
		if (fl.size() == 0) {
			this.notifyAll();
			return;	
		}

		// Otherwise, we will continuously loop until there are no more intersections.
		
		while (true) {
			
			// Reset the selection index and distance values
			
			select = -1;
			d = Double.MAX_VALUE;

			// Get the nearest feature to p1 by looping through each item in the list.
			// There must be at least one, since we've already checked

			for (int i = 0; i < fl.size(); i++) {
				
				// Retrieve the feature from the list of intersecting features
				
				f = fl.get(i);
					
					// Get the geometric object
					
					g = (Geometry) f.getAttribute(0);
					
					// Does the Line Segment cross into the Polygon?
					
					if (ls.intersects(g)) {
						
						// Determine the distance from the start of the line.
						// If it's the shortest distance yet encountered, update the fields.
						
						if (g.distance(ls.getStartPoint()) < d) {
							d = g.distance(ls.getStartPoint());
							select = i;
						}
					}
			}
			
			// If nothing suitable was encountered, then return.

			if (select == -1) {
				this.notifyAll();
				return;
			}		

			// Bounce it
			
			g = (Geometry) fl.get(select).getAttribute(0);

			ls = b.bounce2D(ls, g);

			//wade.cooper
			//added this in so will discard re-doing bounce if points are very close;
			//assumes that there isn't a spit of land on the scale of a few centimeters in size
			//because the LineString intersect will return a false positive when very close
			
/*			double distanceDiff = ls.getLength(); 
			if (distanceDiff < 0.0001){
				Point point = ls.getEndPoint(); 
				fl = index.query(point.getEnvelopeInternal());

				boolean intersect = false; 
				for (int i = 0; i < fl.size(); i++) {
					f = fl.get(i);
					g = (Geometry) f.getAttribute(0);
					if (point.intersects(g)) {
						intersect = true;
					}
				}
				if (!intersect){
					if (neglon) {
						p.setPX(p.getX());
						p.setX((360 + (ls.getEndPoint().getX()))%360);
					} else {
						p.setPX(p.getX());
						p.setX(ls.getEndPoint().getX());
					}
					p.setPY(p.getY());
					p.setY(ls.getEndPoint().getY());
					return;
				}
			}
*/			
			// This is a very rare exception but must be handled.  It occurs if the
			// particle enters a corner smaller than the degree of allowed tolerance
			// (1E-6).  In this case, the particle is lost.
			
			if(ls==null || bounces > 5){

				
				//wade.cooper -- this is getting set to null if it's moving very small distance too,
				//even against a relatively flat plane
				//as such, return without setting a new position and turn
				//off the p.setLost(true),  thus particle staying put

				//p.setLost(true);
				
				//this.notifyAll();
				return;				
			}
			
			// Update the particle's X and Y values
			
			if (neglon) {
				p.setPX(p.getX());
				p.setX((360 + (ls.getEndPoint().getX()))%360);
			} else {
				p.setPX(p.getX());
				p.setX(ls.getEndPoint().getX());
			}
			p.setPY(p.getY());
			p.setY(ls.getEndPoint().getY());

			// Intersect the bounce path - if intersections exist, loop again.

			fl = index.query(ls.getEnvelopeInternal());
			bounces++;
		}
	}
	
	public ShapefileBarrier clone(){
		ShapefileBarrier out = new ShapefileBarrier();
		try {
			out.setDataSource(super.fileName);
			out.neglon=neglon;
			out.luField=luField;
			
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 

		}
		return out;
	}

	@Override
	public void closeConnections() {
		// TODO Auto-generated method stub
		
	}

}

