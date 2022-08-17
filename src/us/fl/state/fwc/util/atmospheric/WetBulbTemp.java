package us.fl.state.fwc.util.atmospheric;

public class WetBulbTemp {

	public double increment = .1; //default
	public double startVal =0; //degrees celcius, default
	public double endVal = 50; //degrees celcius, default
	
	public double relHumidity; 
	public double dryBulbTemp;
	public double pressure;
	

	/**
	 * Set's the range of wet bulb temperatures (degrees C) over which to iterate for best fit
	 * 
	 * @param startValue
	 * @param endValue
	 * @param increment
	 */
	public void setWetBulbRange(double startValue, double endValue, double increment){
		this.startVal = startValue;
		this.endVal = endValue;
		this.increment = increment;
	}


	
	/**
	 * Returns the wet bulb temperature for a given dryBulbTemp, relative humidity, 
	 * and atmospheric pressure.
	 * 
	 * @param dryBulbTemp (in degrees C)
	 * @param relHumidity (as %)
	 * @param pressure (in kPa or atm/10.0)
	 */
	public double getWetBulbTemp(double dryBulbTemp, 
			double relHumidity, double pressure) {
		
		double minDiff = Double.MAX_VALUE;
		double relHumEst = 0;
		double bestWBValue = 0; 
		
		for (double i = startVal; i <= endVal; i=i+increment){
			relHumEst = getRelativeHumidity(i, dryBulbTemp, pressure);
			if ( Math.abs(relHumidity-relHumEst) < minDiff) {
				minDiff = Math.abs(relHumidity-relHumEst) ;
				bestWBValue = i; 
			}
		}
		
		return bestWBValue;
	}
	
	
	/**
	 * Calculates the relative humidity from wet bulb temperature, dry bulb temperature,
	 * and atmospheric pressure 
	 * 
	 * @param wetBulbTemp (degree C)
	 * @param dryBulbTemp (degree C)
	 * @param pressure (kPa or atm/10.0)
	 * @return relative humidity
	 */
	public double getRelativeHumidity(double wetBulbTemp, double dryBulbTemp, 
			double pressure) {
	// Calculates the relative humidity from
	//    Wet bulb temperature WetBulbTemp deg C
	//    Dry Bulb temperature DryBulbTemp deg C
	//    Atmospheric pressure AtmosPress  kPa
	// This equation is taken from BS 4485, part 2, 1988, Appendix D

	        return 100.0 * (getAirSatPress(wetBulbTemp) - 1000.0 * pressure * 
	        		(0.000666 * (dryBulbTemp - wetBulbTemp))) / getAirSatPress(dryBulbTemp);
	        
	}
	

	
	public double getAirSatPress(double AirTemp) {
	// Calculates the saturation vapour pressure of water (Pa) from temperature deg C
	// This equation is taken from BS 4485, part 2, 1988, Appendix D

	        return Math.pow(10, -2948.997118 / (AirTemp + 273.15) - 2.1836674 * 
	        		Math.log(AirTemp + 273.15) - 0.000150474 * 
	        		Math.pow(10, -0.0303738468 * (AirTemp - 0.01)) + 0.00042873 * 
	        		Math.pow(10, 4.76955 * (1 - 273.16 / (AirTemp + 273.15))) + 25.83220018);

	}

}
