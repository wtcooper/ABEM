package us.fl.state.fwc.abem.organism;

import java.util.AbstractQueue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.math.stat.regression.SimpleRegression;

import us.fl.state.fwc.util.TimeUtils;

/**
 * Keeps track of data specific to an individual organism (e.g., past locations, past environment
 * history).  
 * 
 * @author Wade.Cooper
 *
 */
public class OrgMemory {

	private int lengthOfTempTS = 31; //default
	private Organism me; 
	private ArrayBlockingQueue<double[]> tempTimeSeries = 
		new ArrayBlockingQueue<double[]>(lengthOfTempTS);
	
	private double[][] temps = new double[lengthOfTempTS][2];

	private long timeOfLastTemp=0;

	SimpleRegression tempReg = new SimpleRegression();

	int tempCounter = 0; 
	
	
	
	
	public OrgMemory(Organism org){
		me = org;
		
		for (int i=0; i<lengthOfTempTS; i++){
			tempTimeSeries.add(new double[] {1.0, 25.0}); //average temp of 25
		}
	}
	

	/**
	 * Adds the temperature measurement to the temp time series if a measurement hasn't 
	 * been added for over a day.
	 * 
	 * @param temp
	 */
	public void addTemp(double temp){

		long currentTime = me.getCurrentTime();

		if (currentTime-timeOfLastTemp >= 24*TimeUtils.MILLISECS_PER_DAY){
				tempTimeSeries.poll(); 
				tempTimeSeries.add(new double[] 
						{(double) me.getCurrentTime()/TimeUtils.MILLISECS_PER_DAY, temp});
				tempCounter++; 
		}
	}


	
	/**
	 * Returns the slope of the temperature time series
	 * 
	 * @return
	 */
	public double getTempSlope(){

		//return if haven't filled up array yet
		if (tempCounter < lengthOfTempTS) return 0; 
	
		tempTimeSeries.toArray(temps);
		
		tempReg.clear();
		tempReg.addData(temps);
		return tempReg.getSlope();
	}
	
	
	/**
	 * Sets the length of the temperature time series, with default of 31 days
	 * 
	 * @param length
	 */
	public void setTempTSLength(int length){
		lengthOfTempTS = length;
	}


}
