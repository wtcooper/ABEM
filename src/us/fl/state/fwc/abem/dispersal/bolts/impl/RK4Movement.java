package us.fl.state.fwc.abem.dispersal.bolts.impl;

import us.fl.state.fwc.abem.dispersal.bolts.Movement;
import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.VelocityReader;
import us.fl.state.fwc.abem.dispersal.bolts.util.Utils;

/**
 *  @author Johnathan Kool based on FORTRAN code developed by Claire B. Paris,
 * 		    Ashwanth Srinivasan, and Robert K. Cowen.
 */


public class RK4Movement implements Movement, Cloneable {

	private float h;
	private VelocityReader vr = new TBNestedNetCDFVelocityReader();
	private double decay = 1 + (0);
	@SuppressWarnings("unused")
	private boolean nearNoData = false;

	public void apply(Particle p) {

		// Runge-Kutta components

		double aku1, aku2, aku3, aku4, aku5, aku6;
		double akv1, akv2, akv3, akv4, akv5, akv6;

		// Holding variables for displacement in x and y direction

		double dx, dy;

		// Holding variable for paired velocities (u and v)

		double[] ctmp;

		// Holding variable for new co-ordinates (location)

		double[] tmpcoord;

		// Strange constants - not sure what they are.
		// wade.cooper -- these are Cash-Karp Parameters for Embedded Runga-Kutta Method
		
		final double

		B21 = 0.2f, B31 = 3.0f / 40.0f, B32 = 9.0f / 40.0f, B41 = 0.3f, B42 = -0.9f, B43 = 1.2f, B51 = -11.0f / 54.0f, B52 = 2.5f, B53 = -70.0f / 27.0f, B54 = 35.0f / 27.0f, B61 = 1631.0f / 55296.0f, B62 = 175.0f / 512.0f, B63 = 575.0f / 13824.0f, B64 = 44275.0f / 110592.0f, B65 = 253.0f / 4096.0f, C1 = 37.0f / 378.0f, C3 = 250.0f / 621.0f, C4 = 125.0f / 594.0f, C6 = 512.0f / 1771.0f;

		// Get variables from the particle itself, t=time, (x,y,z)=location

		long t = p.getT();
		double z = p.getZ();
		double x = p.getX();
		double y = p.getY();

		
		//Set if incoming tide
		//p.setIncomingTide(vr.getIncomingTide(t));
		
		// Retrieve velocity values

		//|||||||||||||||||||||||||||| CALC 1 |||||||||||||||||||||||||||| 
		ctmp = vr.getUV(t, z, x, y);
		
		if(vr.isNearNoData()){
			nearNoData = true;
		}

		// If the velocity values are null, we are outside the boundary domain

		if (ctmp == null) {
			p.setLost(true);
			return;
		}

		// If we have velocity values, but their values are NODATA, check to see
		// if the particle is on land. If not, then handle accordingly (decay
		// function)

		if (ctmp == vr.getNODATA()) {

			// This is to prevent calculating stationary particles over and
			// over.

			if (Math.abs(p.getU()) < 0.0001 || Math.abs(p.getV()) < 0.0001) {
				return;
			}

			aku1 = p.getU() / decay;
			akv1 = p.getV() / decay;
			p.setNodata(true);

		} else {
			aku1 = ctmp[0];
			akv1 = ctmp[1];
			p.setNodata(false);
		}

		dx = B21 * h * aku1;
		dy = B21 * h * akv1;

		tmpcoord = Utils.latLon(new double[] { y, x }, dy, dx);

		//|||||||||||||||||||||||||||| CALC 2 |||||||||||||||||||||||||||| 
		ctmp = vr.getUV(t, z, tmpcoord[1], tmpcoord[0]);
		

		if(vr.isNearNoData()){
			nearNoData = true;
		}

		if (ctmp == null) {
			p.setLost(true);
			return;
		}

		if (ctmp == vr.getNODATA()) {

			aku2 = aku1 / decay;
			akv2 = akv1 / decay;
			p.setNodata(true);

		} else {

			aku2 = ctmp[0];
			akv2 = ctmp[1];
			p.setNodata(false);
		}

		dx = h * (B31 * aku1 + B32 * aku2);
		dy = h * (B31 * akv1 + B32 * akv2);

		tmpcoord = Utils.latLon(new double[] { y, x }, dy, dx);

		//|||||||||||||||||||||||||||| CALC 3 |||||||||||||||||||||||||||| 
		ctmp = vr.getUV(t, z, tmpcoord[1], tmpcoord[0]);
		
		if(vr.isNearNoData()){
			nearNoData = true;
		}

		if (ctmp == null) {
			p.setLost(true);
			return;
		}

		if (ctmp == vr.getNODATA()) {

			aku3 = aku2 / decay;
			akv3 = akv2 / decay;
			p.setNodata(true);

		} else {

			aku3 = ctmp[0];
			akv3 = ctmp[1];
			p.setNodata(false);
		}

		dx = h * (B41 * aku1 + B42 * aku2 + B43 * aku3);
		dy = h * (B41 * akv1 + B42 * akv2 + B43 * akv3);

		tmpcoord = Utils.latLon(new double[] { y, x }, dy, dx);

		//|||||||||||||||||||||||||||| CALC 4 |||||||||||||||||||||||||||| 
		ctmp = vr.getUV(t, z, tmpcoord[1], tmpcoord[0]);
		
		if(vr.isNearNoData()){
			nearNoData = true;
		}

		if (ctmp == null) {
			p.setLost(true);
			return;
		}

		if (ctmp == vr.getNODATA()) {

			aku4 = aku3 / decay;
			akv4 = akv3 / decay;
			p.setNodata(true);

		} else {

			aku4 = ctmp[0];
			akv4 = ctmp[1];
			p.setNodata(false);
		}
		dx = h * (B51 * aku1 + B52 * aku2 + B53 * aku3 + B54 * aku4);
		dy = h * (B51 * akv1 + B52 * akv2 + B53 * akv3 + B54 * akv4);

		tmpcoord = Utils.latLon(new double[] { y, x }, dy, dx);

		//|||||||||||||||||||||||||||| CALC 5 |||||||||||||||||||||||||||| 
		ctmp = vr.getUV(t, z, tmpcoord[1], tmpcoord[0]);
		
		if(vr.isNearNoData()){
			nearNoData = true;
		}

		if (ctmp == null) {
			p.setLost(true);
			return;
		}

		if (ctmp == vr.getNODATA()) {

			aku5 = aku4 / decay;
			akv5 = akv4 / decay;
			p.setNodata(true);

		} else {

			aku5 = ctmp[0];
			akv5 = ctmp[1];
			p.setNodata(false);
		}

		dx = h
				* (B61 * aku1 + B62 * aku2 + B63 * aku3 + B64 * aku4 + B65
						* aku5);
		dy = h
				* (B61 * akv1 + B62 * akv2 + B63 * akv3 + B64 * akv4 + B65
						* akv5);

		tmpcoord = Utils.latLon(new double[] { y, x }, dy, dx);

		//|||||||||||||||||||||||||||| CALC 6 |||||||||||||||||||||||||||| 
		ctmp = vr.getUV(t, z, tmpcoord[1], tmpcoord[0]);
		
		if(vr.isNearNoData()){
			nearNoData = true;
		}

		if (ctmp == null) {
			p.setLost(true);
			return;
		}

		if (ctmp == vr.getNODATA()) {

			aku6 = aku5 / decay;
			akv6 = akv5 / decay;
			p.setNodata(true);

		} else {

			aku6 = ctmp[0];
			akv6 = ctmp[1];
			p.setNodata(false);
		}

		dx = h * (C1 * aku1 + C3 * aku3 + C4 * aku4 + C6 * aku6);
		dy = h * (C1 * akv1 + C3 * akv3 + C4 * akv4 + C6 * akv6);

		tmpcoord = Utils.latLon(new double[] { y, x }, dy, dx);

		p.setPX(p.getX());
		p.setPY(p.getY());
		p.setX(tmpcoord[1]);
		p.setY(tmpcoord[0]);
		p.setU(aku6);
		p.setV(akv6);
		
		return;
	}

	public void setH(float h) {
		// Accepts h in milliseconds, uses h in seconds because that is how
		// velocity is delivered in the oceanographic data files.
		this.h = h / 1000;
	}

	public void setVr(VelocityReader vr) {
		this.vr = vr;
	}

	public synchronized boolean isNearNoData() {
		//return nearNoData;
		return vr.isNearNoData();
	}
	
	public RK4Movement clone(){
		RK4Movement rk4 = new RK4Movement();
		rk4.setH(h*1000);//We multiply by 1000 because h is stored as seconds
		rk4.setVr(vr.clone());
		return rk4;
	}

	@Override
	public void closeConnections() {
		vr.closeConnections();
	}
}

