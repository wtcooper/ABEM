package us.fl.state.fwc.abem.environ.impl;

import java.util.Calendar;

import us.fl.state.fwc.abem.environ.Temperature;
import us.fl.state.fwc.util.Int3D;

/**
 * Returns the temperature 
 * @author Wade.Cooper
 *
 */
public class MEPSTemperatureSpline implements Temperature {

	
	
	
	

	
	
	

	
	double x1,x2,x3,x4,x5,x6,x7;
	double intercept =18.0700000000; 
	double b1=-0.0411100000;
	double b2 = 0.0013350000;
	double b3 = 0.0000069190;
	double b4 = -0.0000123700;
	double b5 =0.0000042380; 
	double b6 =0.0000059390; 
	double b7 =-0.0000015860;

	double knot1 = 7.2;
	double knot2 = 132.6;
	double knot3 = 230.2;
	double knot4 =  339.0;
	
	@Override
	public double getCurrentTemp(Calendar currentDate, Int3D location) {
		int dayOfYear = currentDate.get(Calendar.DAY_OF_YEAR);
		x1 = dayOfYear;
		x2 = Math.pow(dayOfYear, 2);
		x3 = Math.pow(dayOfYear, 3);
		x4 = 0; 
		if (dayOfYear > knot1) x4 = Math.pow(dayOfYear-knot1, 3); 
		x5 = 0; 
		if (dayOfYear > knot2) x5 = Math.pow(dayOfYear-knot2, 3); 
		x6 = 0; 
		if (dayOfYear > knot3) x6 = Math.pow(dayOfYear-knot3, 3); 
		x7 = 0; 
		if (dayOfYear > knot4) x7 = Math.pow(dayOfYear-knot4, 3); 

		return intercept + b1*x1 + b2*x2 + b3*x3 + b4*x4 + b5*x5 
			+ b6*x6 + b7*x7 ;
	}

	public double getIntercept() {
		return intercept;
	}

	public void setIntercept(double intercept) {
		this.intercept = intercept;
	}

	public double getB1() {
		return b1;
	}

	public void setB1(double b1) {
		this.b1 = b1;
	}

	public double getB2() {
		return b2;
	}

	public void setB2(double b2) {
		this.b2 = b2;
	}

	public double getB3() {
		return b3;
	}

	public void setB3(double b3) {
		this.b3 = b3;
	}

	public double getB4() {
		return b4;
	}

	public void setB4(double b4) {
		this.b4 = b4;
	}

	public double getB5() {
		return b5;
	}

	public void setB5(double b5) {
		this.b5 = b5;
	}

	public double getB6() {
		return b6;
	}

	public void setB6(double b6) {
		this.b6 = b6;
	}

	public double getKnot1() {
		return knot1;
	}

	public void setKnot1(double knot1) {
		this.knot1 = knot1;
	}

	public double getKnot2() {
		return knot2;
	}

	public void setKnot2(double knot2) {
		this.knot2 = knot2;
	}

	public double getKnot3() {
		return knot3;
	}

	public void setKnot3(double knot3) {
		this.knot3 = knot3;
	}

	public double getB7() {
		return b7;
	}

	public void setB7(double b7) {
		this.b7 = b7;
	}


	public double getKnot4() {
		return knot4;
	}

	public void setKnot4(double knot4) {
		this.knot4 = knot4;
	}


}
