package us.fl.state.fwc.abem.dispersal.bolts.util;

import java.util.Iterator;
import java.util.Map;

import javolution.util.FastMap;

import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Calculates reflected position after bouncing off of a given Geometric object.
 */

public class Bouncer {

	private final double tolerance = 1E-10;
	private GeometryFactory gf = new GeometryFactory();
	private LineIntersector li = new RobustLineIntersector();

	// We assume that that there is at least one intersection.

	/**
	 * Implementation of a 2-D reflection off of a polygon object
	 */

	public synchronized LineString bounce2D(LineString ls, Geometry g) {

		MultiPolygon mp = (MultiPolygon) g;
		
		if (mp.getNumGeometries() > 1) {

			this.notifyAll();
			throw new IllegalArgumentException(
					"MultiPolygon encountered when intersecting land.  Coverage must be simple.");
		}

		Polygon pol = (Polygon) mp.getGeometryN(0);

		Point start = ls.getStartPoint();
		Point end = ls.getEndPoint();
		LineString lr = pol.getExteriorRing();

		// Get the intersection

		Geometry g2 = ls.intersection(lr);
		
		if (g2.isEmpty()) {
			this.notifyAll();
			return null;
		}

		// Ensure it's the nearest one

		Point intersect = null;
		double d1 = Double.MAX_VALUE;

		for (int i = 0; i < g2.getNumGeometries(); i++) {
			if (g2.getGeometryN(i).distance(start) < d1) {
				intersect = (Point) g2.getGeometryN(i);
				d1 = intersect.distance(start);
			}
		}

		System.out.println("Land bounce intersection:\n" + intersect.getX() + "\t" + intersect.getY() );
		// Determine the reflection distance

		double dist = intersect.distance(end);

		// Now you also need to find the correct edge. First determine which
		// edges intersect the line
		// We perform the previous step so that we can cut out part-way through.

		int intersections = g2.getNumPoints();
		int count = 0;
		Map<Point, LineString> edges = new FastMap<Point, LineString>();
		Coordinate i2;

		// Collect the intersections and corresponding edges

		while (intersections > 0) {
			li.computeIntersection(start.getCoordinate(), end.getCoordinate(),
					lr.getCoordinateN(count), lr.getCoordinateN(count + 1));

			if (li.hasIntersection()) {
				i2 = li.getIntersection(0);
				edges.put(gf.createPoint(i2), gf
						.createLineString(new Coordinate[] {
								lr.getCoordinateN(count),
								lr.getCoordinateN(count + 1) }));
				intersections--;
			}

			count++;
		}

		// Find the nearest intersection

		double proximity = Double.MAX_VALUE;

		Iterator<Point> it = edges.keySet().iterator();
		Point p;
		Point psel = null;

		while (it.hasNext()) {
			p = it.next();
			if (intersect.distance(p) < proximity) {
				psel = p;
				proximity = intersect.distance(p);
			}
		}

		i2 = psel.getCoordinate();

		// Determine the reference angle (to Cartesian plane)

		LineString edge = edges.get(psel);

		double ref_angle = angle(edge.getStartPoint().getCoordinate(),
				edge.getEndPoint().getCoordinate());

		// Determine the angle of incidence
		
		double i_angle = angle(i2,start.getCoordinate())-angle(i2,edge.getStartPoint().getCoordinate());

		// Subtract from the reference angle to get the angle of reflection
		
		double r_angle = ref_angle-i_angle;

		// Decompose the differential into x and y components.

		double xp1 = Math.cos(r_angle) * dist;
		double yp1 = Math.sin(r_angle) * dist;

		// Update the position of the particle

		double output1 = i2.x + xp1;
		double output2 = i2.y + yp1;

		double xput1 = i2.x+ xp1 * tolerance;
		double xput2 = i2.y+ yp1 * tolerance;

		// I was running into false intersection problems. This corrects
		// by removing a miniscule amount of line from the very beginning.

		this.notifyAll();
		return gf
				.createLineString(new Coordinate[] {
						new Coordinate(xput1, xput2),
						new Coordinate(output1, output2) });
	}
	
	  /**
	   * Returns the angle of the vector from p0 to p1,
	   * relative to the positive X-axis.
	   * The angle is normalized to be in the range [ -Pi, Pi ].
	   *
	   * @return the normalized angle (in radians) that p0-p1 makes with the positive x-axis.
	   */
	
	  private static double angle(Coordinate p0, Coordinate p1) {
	      double dx = p1.x - p0.x;
	      double dy = p1.y - p0.y;
	      return Math.atan2(dy, dx);
	  }
}

