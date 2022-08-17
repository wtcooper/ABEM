package us.fl.state.fwc.abem.environ;

import com.vividsolutions.jts.geom.Coordinate;

public interface HabitatShpFile {

	public double getValue(String attribName, Coordinate coord); 
	
}
