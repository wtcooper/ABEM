package us.fl.state.fwc.abem.dispersal.test;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.dispersal.DisperseMatrix;
import us.fl.state.fwc.abem.organism.OrgDatagram;
import us.fl.state.fwc.abem.organism.Seatrout;
import us.fl.state.fwc.abem.organism.builder.OrganismFactory;
import us.fl.state.fwc.abem.test.SeatroutInitializerTest;

import com.vividsolutions.jts.geom.Coordinate;

public class DisperseMatrixTest {

	Scheduler scheduler; 

	public void step(){

		SeatroutInitializerTest ini = new SeatroutInitializerTest();
		scheduler = ini.initialize();

		DisperseMatrix disperser = new DisperseMatrix(); 
		disperser.initialize(scheduler); //NEED TO CALL 
		
		OrganismFactory organFac = scheduler.getOrganismFactory();
		//Bunces
		double lat = 27.6540;
		double lon = -82.7135; 
		OrgDatagram data = scheduler.getOrganismFactory().getDatagram();
		Coordinate coord = new Coordinate(lon, lat, 0); 
		data.setCoord(coord);
		data.setClassName("Seatrout");
		Seatrout trout = (Seatrout) organFac.newAgent(data); 
		organFac.recycleDatagram(data); 

		disperser.disperse(trout, 4000000);
		
	}

	
	
	
	public static void main(String[] args) {
		DisperseMatrixTest test = new DisperseMatrixTest();
		test.step(); 
	}

}
