package us.fl.state.fwc.abem.dispersal.bolts.impl;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.TurbVar;
import us.fl.state.fwc.abem.dispersal.bolts.util.Utils;


/**
 * 
 * @author Johnathan Kool based on FORTRAN code developed by Robert K. Cowen,
 *         Claire Paris and Ashwanth Srinivasan.
 */

public class SimpleTurbVar implements TurbVar {

	
	private float var = 0.03f; // The magic number
	private float TL = 21600f; // 6 hours in seconds
	private float h; // Minimum integration time step (default=2hrs)

	public SimpleTurbVar(float h) {
		this.h = h / 1000;
	}


	public void apply(Particle p) {

		float usc = (float) Math.sqrt(2f * var * h / TL);

		double u = usc * (float) cern.jet.random.Normal.staticNextDouble(0, 1);
		double v = usc * (float) cern.jet.random.Normal.staticNextDouble(0, 1);

		// Why are we multiplying by h? What does this do?

		double dx = u * h;
		double dy = v * h;

		// Determine the new coordinates

		double[] coords = Utils.latLon(new double[] { p.getY(), p.getX() }, dy,
				dx);


		
		// Update the particle's coordinates.

		p.setX(coords[1]);
		p.setY(coords[0]);
	}
	
	/**
	 * Gets the minimum integration time step currently being used (in seconds)
	 * 
	 * @return
	 */

	public float getH() {
		return h * 1000;
	}

	/**
	 * Sets the minimum integration time step (in seconds)
	 * 
	 * @param h
	 */

	public void setH(float h) {
		this.h = h / 1000;
	}
	
	public SimpleTurbVar clone(){
		return new SimpleTurbVar(h*1000);
	}


}

