package us.fl.state.fwc.abem.environ.impl;

import java.util.Calendar;

import us.fl.state.fwc.abem.environ.Temperature;
import us.fl.state.fwc.util.Int3D;

/**
 * Returns the temperature 
 * @author Wade.Cooper
 *
 */
public class TemperatureSpline implements Temperature {

	double x1,x2,x3,x4,x5,x6,x7,x8,x9,x10;
	double intercept =22.39000000; 
	double b1=0.0086320000;
	double b2 = 0.0031310000;
	double b3 = 0.0000206800;
	double b4 = -0.0000162800;
	double b5 = -0.0000070720;
	double b6 = 0.0000006139;
	double b7 =-0.0000045570;
	double b8 = 0.0000171800;
	double b9 = -0.0000075270;
	double b10 =-0.0000037650;

	double knot1 = -45;
	double knot2 = 13;
	double knot3 = 99;
	double knot4 = 189;
	double knot5 = 266;
	double knot6 = 332;
	double knot7 = 389;
	
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
		x8 = 0; 
		if (dayOfYear > knot5) x8 = Math.pow(dayOfYear-knot5, 3); 
		x9 = 0; 
		if (dayOfYear > knot6) x9 = Math.pow(dayOfYear-knot6, 3); 
		x10 = 0; 
		if (dayOfYear > knot7) x10 = Math.pow(dayOfYear-knot7, 3); 

		return intercept + b1*x1 + b2*x2 + b3*x3 + b4*x4 + b5*x5 
			+ b6*x6 + b7*x7 + b8*x8 + b9*x9 + b10*x10  ;
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

	public double getB8() {
		return b8;
	}

	public void setB8(double b8) {
		this.b8 = b8;
	}

	public double getB9() {
		return b9;
	}

	public void setB9(double b9) {
		this.b9 = b9;
	}

	public double getB10() {
		return b10;
	}

	public void setB10(double b10) {
		this.b10 = b10;
	}

	public double getKnot4() {
		return knot4;
	}

	public void setKnot4(double knot4) {
		this.knot4 = knot4;
	}

	public double getKnot5() {
		return knot5;
	}

	public void setKnot5(double knot5) {
		this.knot5 = knot5;
	}

	public double getKnot6() {
		return knot6;
	}

	public void setKnot6(double knot6) {
		this.knot6 = knot6;
	}

	public double getKnot7() {
		return knot7;
	}

	public void setKnot7(double knot7) {
		this.knot7 = knot7;
	}

}
