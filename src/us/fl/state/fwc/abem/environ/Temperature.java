package us.fl.state.fwc.abem.environ;

import java.util.Calendar;
import us.fl.state.fwc.util.Int3D;

public interface Temperature {

	public abstract double getCurrentTemp(Calendar currentDate, Int3D location);
	
}
