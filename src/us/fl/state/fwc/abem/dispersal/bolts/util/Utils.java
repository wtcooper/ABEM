package us.fl.state.fwc.abem.dispersal.bolts.util;

/**
 * Utilities for determinign change in position and distance along a sphere.
 * 
 * @author Johnathan Kool - modified from code developed by Ashwanth Srinivasan.
 * 
 */

public class Utils {

	final static double R_EARTH = 6372795.477598f; // Quadratic mean radius of
												   // the earth (meters)
	final static double REINV = 1d / R_EARTH;// Inverse Radius of the Earth

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

	public static float[] latLon(float[] coords, float dy, float dx) {

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

	public static double[] latLon(double[] coords, double dy, double dx) {

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

	/**
	 * Calculates the distance traveled along a sphere (great circle distance)
	 * 
	 * @param rlon1 -
	 *            The longitude of origin
	 * @param rlat1 -
	 *            The latitude of origin
	 * @param rlon2 -
	 *            The destination longitude
	 * @param rlat2 -
	 *            The destination latitude.
	 * @return - Distance traveled in meters.
	 */

	public static double distance_Sphere(float rlon1, float rlat1, float rlon2,
			float rlat2) {

		double rln1, rlt1, rln2, rlt2;
		double dist;

		rln1 = Math.toRadians(rlon1);
		rlt1 = Math.toRadians(rlat1);
		rln2 = Math.toRadians(rlon2);
		rlt2 = Math.toRadians(rlat2);

		double d_lambda = Math.abs(rln1 - rln2);

		// Simple great circle distance

		// dist = Math.acos(Math.cos(rlt1) * Math.cos(rlt2) * Math.cos(rln2 -
		// rln1)
		// + Math.sin(rlt1) * Math.sin(rlt2));

		// More complex great circle distance formula to reduce error due to
		// rounding.

		double n1 = Math.pow(Math.cos(rlt2) * Math.sin(d_lambda), 2);
		double n2 = Math.pow(Math.cos(rlt1) * Math.sin(rlt2) - Math.sin(rlt1)
				* Math.cos(rlt2) * Math.cos(d_lambda), 2);
		double numerator = Math.sqrt(n1 + n2);
		double denominator = Math.sin(rlt1) * Math.sin(rlt2) + Math.cos(rlt1)
				* Math.cos(rlt2) * Math.cos(d_lambda);
		dist = Math.atan2(numerator, denominator);

		return R_EARTH * Math.toDegrees(dist) / 360;
	}

	/**
	 * Calculates the distance traveled along a sphere (great circle distance)
	 * 
	 * @param rlon1 -
	 *            The longitude of origin
	 * @param rlat1 -
	 *            The latitude of origin
	 * @param rlon2 -
	 *            The destination longitude
	 * @param rlat2 -
	 *            The destination latitude.
	 * @return - Distance traveled in meters.
	 */

	public static double distance_Sphere(double rlon1, double rlat1,
			double rlon2, double rlat2) {

		double rln1, rlt1, rln2, rlt2;
		double dist;

		rln1 = Math.toRadians(rlon1);
		rlt1 = Math.toRadians(rlat1);
		rln2 = Math.toRadians(rlon2);
		rlt2 = Math.toRadians(rlat2);

		double d_lambda = Math.abs(rln1 - rln2);

		// Simple great circle distance

		// dist = Math.acos(Math.cos(rlt1) * Math.cos(rlt2) * Math.cos(rln2 -
		// rln1)
		// + Math.sin(rlt1) * Math.sin(rlt2));

		// More complex great circle distance formula to reduce error due to
		// rounding.

		double n1 = Math.pow(Math.cos(rlt2) * Math.sin(d_lambda), 2);
		double n2 = Math.pow(Math.cos(rlt1) * Math.sin(rlt2) - Math.sin(rlt1)
				* Math.cos(rlt2) * Math.cos(d_lambda), 2);
		double numerator = Math.sqrt(n1 + n2);
		double denominator = Math.sin(rlt1) * Math.sin(rlt2) + Math.cos(rlt1)
				* Math.cos(rlt2) * Math.cos(d_lambda);
		dist = Math.atan2(numerator, denominator);

		return R_EARTH * Math.toDegrees(dist) / 360;
	}
}

