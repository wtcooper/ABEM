package us.fl.state.fwc.abem.dispersal;

/**
 * Class to build a simple spatial prey field to simulate variability in recruitment
 * 
 *  Can use a fractal approach where create high spots and low spots in space, say at 1 month 
 *  intervals, and simple have each cell shift from current value to next value linearly.  This would 
 *  simulate a changing prey field, but this wouldn't include spatial movement of the prey field.
 *  
 *  Other option would be to have high spots and low spots move via random walk over
 *  time at 1 month interval, and change value randomly within bounds, and then have each 
 *  cell shift from current value to next value linearly on short time scales.
 *  
 * @author Wade.Cooper
 *
 */
public class SimpleSpatialPreyBuilder {

}
