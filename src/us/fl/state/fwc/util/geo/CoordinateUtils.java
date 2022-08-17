package us.fl.state.fwc.util.geo;

import com.vividsolutions.jts.geom.Coordinate;

/**	Coordinate utility class for basic operations (e.g., distance between two coordinates).
 * 
 * @author wade.cooper
 *
 */
public final class CoordinateUtils {

	static double pi = 3.14159265358979;

	/* Ellipsoid model constants (actual values here are for WGS84) */
	static double sm_a = 6378137.0;
	static double sm_b = 6356752.314;
	static double sm_EccSquared = 6.69437999013e-03;

	static double UTMScaleFactor = 0.9996;


	//constants for executing a change in position
	final static double R_EARTH = 6372795.477598f; // Quadratic mean radius of  the earth (meters)
	final static double REINV = 1d / R_EARTH;// Inverse Radius of the Earth

	
	/**	Get's the distance between two coordinates in Euclidian Space
	 * 
	 * @param thisCoord
	 * @param thatCoord
	 * @return distance traveled in units of coordinates
	 */
	public static double getDistance(Coordinate thisCoord, Coordinate thatCoord){
		double distance =0;
		Coordinate difference = new Coordinate(); 
		difference.x = thisCoord.x-thatCoord.x; 
		difference.y = thisCoord.y-thatCoord.y; 
		difference.z = thisCoord.z-thatCoord.z; 
		distance = Math.sqrt(difference.x*difference.x+ difference.y*difference.y + difference.z*difference.z);
		return distance; 
	}

	
	/**	Get's the distance between two coordinates in Euclidian space
	 * 
	 * @param thisCoord
	 * @param thatCoord
	 * @return distance
	 */
	public static double getDistance(double[] thisCoord, double[] thatCoord){
		double distance =0;

		for (int i=0; i<thisCoord.length; i++){
			distance += (thisCoord[i]-thatCoord[i])*(thisCoord[i]-thatCoord[i]); 
		}
		distance = Math.sqrt(distance); 
		return distance; 
	}

	
	/**
	 * Calculates the distance traveled along a sphere (great circle distance)
	 * 
	 * @param lon1 -
	 *            The longitude of origin
	 * @param lat1 -
	 *            The latitude of origin
	 * @param lon2 -
	 *            The destination longitude
	 * @param lat2 -
	 *            The destination latitude.
	 * @return - Distance traveled in meters.
	 */

	public static double getDistanceAlongSphere(double lon1, double lat1, double lon2, double lat2) {

		lon1 = Math.toRadians(lon1);
		lat1 = Math.toRadians(lat1);
		lon2 = Math.toRadians(lon2);
		lat2 = Math.toRadians(lat2);

		double a = Math.sin(Math.abs(lat1-lat2)/2) * 
			Math.sin(Math.abs(lat1-lat2)/2) + 
			Math.cos(lat1) * 
			Math.cos(lat2) * 
			Math.sin(Math.abs(lon1 - lon2)/2) * 
			Math.sin(Math.abs(lon1 - lon2)/2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 

		return R_EARTH * c; 
	}
	
	/**
	 * Executes a change in position within a spherical coordinate system.
	 * 
	 * @param coord -
	 *            Coordinates, latitude first, then longitude
	 * @param dy -
	 *            Change in the y direction (latitude) in meters
	 * @param dx -
	 *            Change in the x direction (longitude) in meters
	 * @return - The new position, latitude then longitude.
	 */

	public static void moveCoordAlongSphere(Coordinate coord, double dy, double dx) {

		double rlat2, rlon2;
		double dlon, rln1, rlt1;

		rln1 = Math.toRadians(coord.x); // Convert longitude to radians
		rlt1 = Math.toRadians(coord.y); // Convert latitude to radians
		rlat2 = rlt1 + dy * REINV; // Convert distance to radians
		rlat2 = Math.asin(Math.sin(rlat2) * Math.cos(dx * REINV)); // Trigonometry
		// magic!
		dlon = Math.atan2(Math.sin(dx * REINV) * Math.cos(rlt1), (Math.cos(dx
				* REINV) - Math.sin(rlt1) * Math.sin(rlat2)));
		rlon2 = Math.toDegrees(rln1 + dlon); // Convert back
		rlat2 = Math.toDegrees(rlat2); // same

		coord.x = rlon2; 
		coord.y = rlat2; 
	}

	
	/**
	 * Executes a change in position within a spherical coordinate system.
	 * 
	 * @param coord -
	 *            Coordinates, latitude first, then longitude
	 * @param dy -
	 *            Change in the y direction (latitude) in meters
	 * @param dx -
	 *            Change in the x direction (longitude) in meters
	 * @return - The new position, latitude then longitude.
	 */

	public static Coordinate getNewCoordAlongSphere(Coordinate coord, double dy, double dx) {

		double rlat2, rlon2;
		double dlon, rln1, rlt1;

		rln1 = Math.toRadians(coord.x); // Convert longitude to radians
		rlt1 = Math.toRadians(coord.y); // Convert latitude to radians
		rlat2 = rlt1 + dy * REINV; // Convert distance to radians
		rlat2 = Math.asin(Math.sin(rlat2) * Math.cos(dx * REINV)); // Trigonometry
		// magic!
		dlon = Math.atan2(Math.sin(dx * REINV) * Math.cos(rlt1), (Math.cos(dx
				* REINV) - Math.sin(rlt1) * Math.sin(rlat2)));
		rlon2 = Math.toDegrees(rln1 + dlon); // Convert back
		rlat2 = Math.toDegrees(rlat2); // same

		return new Coordinate(rlon2, rlat2, 0); 
	}
	
	
	
	/**
	 * Executes a change in position within a spherical coordinate system.
	 * 
	 * @param coords -
	 *            Coordinates, latitude first, then longitude
	 * @param dy -
	 *            Change in the y direction (latitude) in meters
	 * @param dx -
	 *            Change in the x direction (longitude) in meters
	 * @return - The new position, latitude then longitude.
	 */

	public static float[] getNewLatLon(float[] coords, float dy, float dx) {

		double rlat2, rlon2;
		double dlon, rln1, rlt1;

		rln1 = Math.toRadians(coords[1]); // Convert longitude to radians
		rlt1 = Math.toRadians(coords[0]); // Convert latitude to radians
		rlat2 = rlt1 + dy * REINV; // Convert distance to radians
		rlat2 = Math.asin(Math.sin(rlat2) * Math.cos(dx * REINV)); // Trigonometry
		// magic!
		dlon = Math.atan2(Math.sin(dx * REINV) * Math.cos(rlt1), (Math.cos(dx
				* REINV) - Math.sin(rlt1) * Math.sin(rlat2)));
		rlon2 = Math.toDegrees(rln1 + dlon); // Convert back
		rlat2 = Math.toDegrees(rlat2); // same

		return new float[] { (float) rlat2, (float) rlon2 };

	}

	/**
	 * Executes a change in position within a spherical coordinate system.
	 * 
	 * @param coords -
	 *            Coordinates, latitude first, then longitude
	 * @param dy -
	 *            Change in the y direction (latitude) in meters
	 * @param dx -
	 *            Change in the x direction (longitude) in meters
	 * @return - The new position, latitude then longitude.
	 */

	public static double[] getNewLatLon(double[] coords, double dy, double dx) {

		double rlat2, rlon2;
		double dlon, rln1, rlt1;

		rln1 = Math.toRadians(coords[1]); // Convert longitude to radians
		rlt1 = Math.toRadians(coords[0]); // Convert latitude to radians
		rlat2 = rlt1 + dy * REINV; // Convert distance to radians
		rlat2 = Math.asin(Math.sin(rlat2) * Math.cos(dx * REINV)); // Trigonometry
		// magic!
		dlon = Math.atan2(Math.sin(dx * REINV) * Math.cos(rlt1), (Math.cos(dx
				* REINV) - Math.sin(rlt1) * Math.sin(rlat2)));
		rlon2 = Math.toDegrees(rln1 + dlon); // Convert back
		rlat2 = Math.toDegrees(rlat2); // same

		return new double[] { rlat2, rlon2 };

	}



	
	/**Converts a latitude/longitude pair to x and y coordinates in the
	 * Universal Transverse Mercator projection.
	 *
	 * Inputs:
	 *   lat - Latitude of the point, in degrees.
	 *   lon - Longitude of the point, in degrees.
	 *   zone - UTM zone to be used for calculating values for x and y.
	 *          If zone is less than 1 or greater than 60, the routine
	 *          will determine the appropriate zone from the value of lon.
	 * 
	 * @param lat
	 * @param lon
	 * @param zone
	 * @return double[y][x] in meters
	 */
	public static double[] convertLatLonToUTM (double lat, double lon, double zone)
	{

		double[] xy = new double[2];
		MapLatLonToXY (degToRad(lat), degToRad(lon), UTMCentralMeridian (zone), xy);

		/* Adjust easting and northing for UTM system. */
		xy[0] = xy[0] * UTMScaleFactor + 500000.0;
		xy[1] = xy[1] * UTMScaleFactor;
		if (xy[1] < 0.0)
			xy[1] = xy[1] + 10000000.0;

		//flip them so in [y][x] format
		return new double[] {xy[1], xy[0]};
	}


	/**Converts a latitude/longitude pair to x and y coordinates in the
	 * Universal Transverse Mercator projection.
	 *
	 * Inputs:
	 *   lat - Latitude of the point, in degrees.
	 *   lon - Longitude of the point, in degrees.
	 *   zone - UTM zone to be used for calculating values for x and y.
	 *          If zone is less than 1 or greater than 60, the routine
	 *          will determine the appropriate zone from the value of lon.
	 * 
	 * @param coord
	 * @param zone
	 */
	public static void convertLatLonToUTM (Coordinate coord, double zone)
	{

		double[] xy = new double[2];
		MapLatLonToXY (degToRad(coord.y), degToRad(coord.x), UTMCentralMeridian (zone), xy);

		/* Adjust easting and northing for UTM system. */
		xy[0] = xy[0] * UTMScaleFactor + 500000.0;
		xy[1] = xy[1] * UTMScaleFactor;
		if (xy[1] < 0.0)
			xy[1] = xy[1] + 10000000.0;

		coord.x = xy[0];
		coord.y = xy[1];
	}

	

	/**Converts x and y coordinates in the Universal Transverse Mercator
	 * projection to a latitude/longitude pair.
	 *
	 * Inputs:
	 *	x - The easting of the point, in meters.
	 *	y - The northing of the point, in meters.
	 *	zone - The UTM zone in which the point lies.
	 *	southhemi - True if the point is in the southern hemisphere;
	 *               false otherwise.
	 *
	 * Returns:
	 *	The function does not return a value.
	 *
	 * 
	 * @param x
	 * @param y
	 * @param zone
	 * @param southhemi
	 * @return double[lat][lon] in degrees
	 */
	public static double[] convertUTMToLatLon (double x,double  y, double zone, boolean southhemi)
	{
		double[] latlon = new double[2];
		double  cmeridian;
		x -= 500000.0;
		x /= UTMScaleFactor;

		/* If in southern hemisphere, adjust y accordingly. */
		if (southhemi)
			y -= 10000000.0;
		y /= UTMScaleFactor;

		cmeridian = UTMCentralMeridian (zone);
		MapXYToLatLon (x, y, cmeridian, latlon);

		return new double[] {radToDeg(latlon[0]),radToDeg(latlon[1])} ;
	}

	
	
	/**Converts x and y coordinates in the Universal Transverse Mercator
	 * projection to a latitude/longitude pair.
	 *
	 * Inputs:
	 *	x - The easting of the point, in meters.
	 *	y - The northing of the point, in meters.
	 *	zone - The UTM zone in which the point lies.
	 *	southhemi - True if the point is in the southern hemisphere;
	 *               false otherwise.
	 *
	 * Returns:
	 *	The function does not return a value.
	 *
	 * 
	 * @param coord
	 * @param zone
	 * @param southhemi
	 */
	public static void convertUTMToLatLon (Coordinate coord, double zone, boolean southhemi)
	{
		double[] latlon = new double[2];
		double  cmeridian;
		coord.x -= 500000.0;
		coord.x /= UTMScaleFactor;

		/* If in southern hemisphere, adjust y accordingly. */
		if (southhemi) coord.y -= 10000000.0;
		coord.y /= UTMScaleFactor;

		cmeridian = UTMCentralMeridian (zone);
		MapXYToLatLon (coord.x, coord.y, cmeridian, latlon);

		coord.x = radToDeg(latlon[1]);
		coord.y = radToDeg(latlon[0]);
	}
	
	
	
	
	
	
	
	
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
	//Methods below are for lat/lon - UTM conversions
	//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

	
	/**Converts degrees to radians
	 * 
	 * @param deg
	 * @return radians
	 */
	public static double degToRad (double deg)
	{
		return (deg / 180.0 * pi);
	}


	/**Converts radians to degrees
	 * 
	 * @param rad
	 * @return
	 */
	public static double radToDeg (double rad)
	{
		return (rad / pi * 180.0);
	}



	/*
	 * ArcLengthOfMeridian
	 *
	 * Computes the ellipsoidal distance from the equator to a point at a
	 * given latitude.
	 *
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
	 *
	 * Inputs:
	 *     phi - Latitude of the point, in radians.
	 *
	 * Globals:
	 *     sm_a - Ellipsoid model major axis.
	 *     sm_b - Ellipsoid model minor axis.
	 *
	 * Returns:
	 *     The ellipsoidal distance of the point from the equator, in meters.
	 *
	 */
	private static double ArcLengthOfMeridian (double phi)
	{
		double alpha, beta, gamma, delta, epsilon, n;
		double result;

		/* Precalculate n */
		n = (sm_a - sm_b) / (sm_a + sm_b);

		/* Precalculate alpha */
		alpha = ((sm_a + sm_b) / 2.0)
		* (1.0 + (Math.pow (n, 2.0) / 4.0) + (Math.pow (n, 4.0) / 64.0));

		/* Precalculate beta */
		beta = (-3.0 * n / 2.0) + (9.0 * Math.pow (n, 3.0) / 16.0)
		+ (-3.0 * Math.pow (n, 5.0) / 32.0);

		/* Precalculate gamma */
		gamma = (15.0 * Math.pow (n, 2.0) / 16.0)
		+ (-15.0 * Math.pow (n, 4.0) / 32.0);

		/* Precalculate delta */
		delta = (-35.0 * Math.pow (n, 3.0) / 48.0)
		+ (105.0 * Math.pow (n, 5.0) / 256.0);

		/* Precalculate epsilon */
		epsilon = (315.0 * Math.pow (n, 4.0) / 512.0);

		/* Now calculate the sum of the series and return */
		result = alpha
		* (phi + (beta * Math.sin (2.0 * phi))
				+ (gamma * Math.sin (4.0 * phi))
				+ (delta * Math.sin (6.0 * phi))
				+ (epsilon * Math.sin (8.0 * phi)));

		return result;
	}



	/*
	 * UTMCentralMeridian
	 *
	 * Determines the central meridian for the given UTM zone.
	 *
	 * Inputs:
	 *     zone - An integer value designating the UTM zone, range [1,60].
	 *
	 * Returns:
	 *   The central meridian for the given UTM zone, in radians, or zero
	 *   if the UTM zone parameter is outside the range [1,60].
	 *   Range of the central meridian is the radian equivalent of [-177,+177].
	 *
	 */
	private static double UTMCentralMeridian (double zone)
	{
		double cmeridian;

		cmeridian = degToRad (-183.0 + (zone * 6.0));

		return cmeridian;
	}



	/*
	 * FootpointLatitude
	 *
	 * Computes the footpoint latitude for use in converting transverse
	 * Mercator coordinates to ellipsoidal coordinates.
	 *
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 *   GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
	 *
	 * Inputs:
	 *   y - The UTM northing coordinate, in meters.
	 *
	 * Returns:
	 *   The footpoint latitude, in radians.
	 *
	 */
	private static double FootpointLatitude (double y)
	{
		double y_, alpha_, beta_, gamma_, delta_, epsilon_, n;
		double result;

		/* Precalculate n (Eq. 10.18) */
		n = (sm_a - sm_b) / (sm_a + sm_b);

		/* Precalculate alpha_ (Eq. 10.22) */
		/* (Same as alpha in Eq. 10.17) */
		alpha_ = ((sm_a + sm_b) / 2.0)
		* (1 + (Math.pow (n, 2.0) / 4) + (Math.pow (n, 4.0) / 64));

		/* Precalculate y_ (Eq. 10.23) */
		y_ = y / alpha_;

		/* Precalculate beta_ (Eq. 10.22) */
		beta_ = (3.0 * n / 2.0) + (-27.0 * Math.pow (n, 3.0) / 32.0)
		+ (269.0 * Math.pow (n, 5.0) / 512.0);

		/* Precalculate gamma_ (Eq. 10.22) */
		gamma_ = (21.0 * Math.pow (n, 2.0) / 16.0)
		+ (-55.0 * Math.pow (n, 4.0) / 32.0);

		/* Precalculate delta_ (Eq. 10.22) */
		delta_ = (151.0 * Math.pow (n, 3.0) / 96.0)
		+ (-417.0 * Math.pow (n, 5.0) / 128.0);

		/* Precalculate epsilon_ (Eq. 10.22) */
		epsilon_ = (1097.0 * Math.pow (n, 4.0) / 512.0);

		/* Now calculate the sum of the series (Eq. 10.21) */
		result = y_ + (beta_ * Math.sin (2.0 * y_))
		+ (gamma_ * Math.sin (4.0 * y_))
		+ (delta_ * Math.sin (6.0 * y_))
		+ (epsilon_ * Math.sin (8.0 * y_));

		return result;
	}



	/*
	 * MapLatLonToXY
	 *
	 * Converts a latitude/longitude pair to x and y coordinates in the
	 * Transverse Mercator projection.  Note that Transverse Mercator is not
	 * the same as UTM; a scale factor is required to convert between them.
	 *
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
	 *
	 * Inputs:
	 *    phi - Latitude of the point, in radians.
	 *    lambda - Longitude of the point, in radians.
	 *    lambda0 - Longitude of the central meridian to be used, in radians.
	 *
	 * Outputs:
	 *    xy - A 2-element array containing the x and y coordinates
	 *         of the computed point.
	 *
	 * Returns:
	 *    The function does not return a value.
	 *
	 */
	private static void MapLatLonToXY (double phi, double lambda, double lambda0, double[] xy)
	{
		double N, nu2, ep2, t, t2, l;
		double l3coef, l4coef, l5coef, l6coef, l7coef, l8coef;
		//double  tmp;

		/* Precalculate ep2 */
		ep2 = (Math.pow (sm_a, 2.0) - Math.pow (sm_b, 2.0)) / Math.pow (sm_b, 2.0);

		/* Precalculate nu2 */
		nu2 = ep2 * Math.pow (Math.cos (phi), 2.0);

		/* Precalculate N */
		N = Math.pow (sm_a, 2.0) / (sm_b * Math.sqrt (1 + nu2));

		/* Precalculate t */
		t = Math.tan (phi);
		t2 = t * t;
		//tmp = (t2 * t2 * t2) - Math.pow (t, 6.0);

		/* Precalculate l */
		l = lambda - lambda0;

		/* Precalculate coefficients for l**n in the equations below
           so a normal human being can read the expressions for easting
           and northing
           -- l**1 and l**2 have coefficients of 1.0 */
		l3coef = 1.0 - t2 + nu2;

		l4coef = 5.0 - t2 + 9 * nu2 + 4.0 * (nu2 * nu2);

		l5coef = 5.0 - 18.0 * t2 + (t2 * t2) + 14.0 * nu2
		- 58.0 * t2 * nu2;

		l6coef = 61.0 - 58.0 * t2 + (t2 * t2) + 270.0 * nu2
		- 330.0 * t2 * nu2;

		l7coef = 61.0 - 479.0 * t2 + 179.0 * (t2 * t2) - (t2 * t2 * t2);

		l8coef = 1385.0 - 3111.0 * t2 + 543.0 * (t2 * t2) - (t2 * t2 * t2);

		/* Calculate easting (x) */
		xy[0] = N * Math.cos (phi) * l
		+ (N / 6.0 * Math.pow (Math.cos (phi), 3.0) * l3coef * Math.pow (l, 3.0))
		+ (N / 120.0 * Math.pow (Math.cos (phi), 5.0) * l5coef * Math.pow (l, 5.0))
		+ (N / 5040.0 * Math.pow (Math.cos (phi), 7.0) * l7coef * Math.pow (l, 7.0));

		/* Calculate northing (y) */
		xy[1] = ArcLengthOfMeridian (phi)
		+ (t / 2.0 * N * Math.pow (Math.cos (phi), 2.0) * Math.pow (l, 2.0))
		+ (t / 24.0 * N * Math.pow (Math.cos (phi), 4.0) * l4coef * Math.pow (l, 4.0))
		+ (t / 720.0 * N * Math.pow (Math.cos (phi), 6.0) * l6coef * Math.pow (l, 6.0))
		+ (t / 40320.0 * N * Math.pow (Math.cos (phi), 8.0) * l8coef * Math.pow (l, 8.0));


	}



	/*
	 * MapXYToLatLon
	 *
	 * Converts x and y coordinates in the Transverse Mercator projection to
	 * a latitude/longitude pair.  Note that Transverse Mercator is not
	 * the same as UTM; a scale factor is required to convert between them.
	 *
	 * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
	 *   GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
	 *
	 * Inputs:
	 *   x - The easting of the point, in meters.
	 *   y - The northing of the point, in meters.
	 *   lambda0 - Longitude of the central meridian to be used, in radians.
	 *
	 * Outputs:
	 *   philambda - A 2-element containing the latitude and longitude
	 *               in radians.
	 *
	 * Returns:
	 *   The function does not return a value.
	 *
	 * Remarks:
	 *   The local variables Nf, nuf2, tf, and tf2 serve the same purpose as
	 *   N, nu2, t, and t2 in MapLatLonToXY, but they are computed with respect
	 *   to the footpoint latitude phif.
	 *
	 *   x1frac, x2frac, x2poly, x3poly, etc. are to enhance readability and
	 *   to optimize computations.
	 *
	 */
	private static void MapXYToLatLon (double x, double y, double lambda0, double[] philambda)
	{
		double  phif, Nf, Nfpow, nuf2, ep2, tf, tf2, tf4, cf;
		double  x1frac, x2frac, x3frac, x4frac, x5frac, x6frac, x7frac, x8frac;
		double  x2poly, x3poly, x4poly, x5poly, x6poly, x7poly, x8poly;

		/* Get the value of phif, the footpoint latitude. */
		phif = FootpointLatitude (y);

		/* Precalculate ep2 */
		ep2 = (Math.pow (sm_a, 2.0) - Math.pow (sm_b, 2.0))
		/ Math.pow (sm_b, 2.0);

		/* Precalculate cos (phif) */
		cf = Math.cos (phif);

		/* Precalculate nuf2 */
		nuf2 = ep2 * Math.pow (cf, 2.0);

		/* Precalculate Nf and initialize Nfpow */
		Nf = Math.pow (sm_a, 2.0) / (sm_b * Math.sqrt (1 + nuf2));
		Nfpow = Nf;

		/* Precalculate tf */
		tf = Math.tan (phif);
		tf2 = tf * tf;
		tf4 = tf2 * tf2;

		/* Precalculate fractional coefficients for x**n in the equations
           below to simplify the expressions for latitude and longitude. */
		x1frac = 1.0 / (Nfpow * cf);

		Nfpow *= Nf;   /* now equals Nf**2) */
		x2frac = tf / (2.0 * Nfpow);

		Nfpow *= Nf;   /* now equals Nf**3) */
		x3frac = 1.0 / (6.0 * Nfpow * cf);

		Nfpow *= Nf;   /* now equals Nf**4) */
		x4frac = tf / (24.0 * Nfpow);

		Nfpow *= Nf;   /* now equals Nf**5) */
		x5frac = 1.0 / (120.0 * Nfpow * cf);

		Nfpow *= Nf;   /* now equals Nf**6) */
		x6frac = tf / (720.0 * Nfpow);

		Nfpow *= Nf;   /* now equals Nf**7) */
		x7frac = 1.0 / (5040.0 * Nfpow * cf);

		Nfpow *= Nf;   /* now equals Nf**8) */
		x8frac = tf / (40320.0 * Nfpow);

		/* Precalculate polynomial coefficients for x**n.
           -- x**1 does not have a polynomial coefficient. */
		x2poly = -1.0 - nuf2;

		x3poly = -1.0 - 2 * tf2 - nuf2;

		x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2
		- 3.0 * (nuf2 *nuf2) - 9.0 * tf2 * (nuf2 * nuf2);

		x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2;

		x6poly = -61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2
		+ 162.0 * tf2 * nuf2;

		x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2);

		x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575 * (tf4 * tf2);

		/* Calculate latitude */
		philambda[0] = phif + x2frac * x2poly * (x * x)
		+ x4frac * x4poly * Math.pow (x, 4.0)
		+ x6frac * x6poly * Math.pow (x, 6.0)
		+ x8frac * x8poly * Math.pow (x, 8.0);

		/* Calculate longitude */
		philambda[1] = lambda0 + x1frac * x
		+ x3frac * x3poly * Math.pow (x, 3.0)
		+ x5frac * x5poly * Math.pow (x, 5.0)
		+ x7frac * x7poly * Math.pow (x, 7.0);

	}
	
}
