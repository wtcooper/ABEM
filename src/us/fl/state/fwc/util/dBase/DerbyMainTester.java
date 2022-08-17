package us.fl.state.fwc.util.dBase;

import java.util.LinkedHashMap;

public class DerbyMainTester {


	String dbName = "C:/work/data/Database/ABEMData";
	String insertTablePath ="C:/work/data/SWFWMD/DoverET_4.21.96-3.16.11.csv";
	
	public void run(){

		//create a new database
		DerbyConnection dc = new DerbyConnection(dbName, false);

		//create a new schema
		//dc.createSchema("EFDC_TB");
		
		//dc.dropTable("EFDC_TB", "SWFWMD_WEATHERST");

		
		//here, Object[0] = datatype, Object[1] = column width if string, Object[2] = can be empty
		LinkedHashMap<String, Object[]> columnAtts = new LinkedHashMap<String, Object[]>();
		columnAtts.put("SID", new Object[] {"int", null, false} );
		columnAtts.put("Site", new Object[] {"String", 40, false} );
		columnAtts.put("Parameter", new Object[] {"String", 40, false} );
		columnAtts.put("Date", new Object[] {"Date", null, false} );
		columnAtts.put("Value", new Object[] {"double", null, true} );
		columnAtts.put("Units", new Object[] {"String", 40, false} );
		columnAtts.put("Records", new Object[] {"int", null, false} );
		columnAtts.put("Source", new Object[] {"String", 40, false} );
		columnAtts.put("Status", new Object[] {"String", 40, true} );
		columnAtts.put("Quality", new Object[] {"String", 128, false} );

		//dc.createTable("EFDC_TB", "SWFWMD_WEATHERST", columnAtts);

		//this insertTable function doesn't work, not sure why.  Could be that stored procedures
		//only work from ij> or .sql files
		dc.insertTable("EFDC_TB", "SWFWMD_WEATHERST", insertTablePath, ",", false);
		
		dc.closeConnections();
	}

	
	
	public static void main(String[] args) {
		DerbyMainTester dm = new DerbyMainTester();
		dm.run();
		
	}

}
