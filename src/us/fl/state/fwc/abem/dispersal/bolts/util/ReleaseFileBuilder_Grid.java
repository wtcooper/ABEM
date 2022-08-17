package us.fl.state.fwc.abem.dispersal.bolts.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import us.fl.state.fwc.abem.environ.impl.ABEMCell;
import us.fl.state.fwc.abem.environ.impl.ABEMGrid;
import us.fl.state.fwc.util.Int3D;

public class ReleaseFileBuilder_Grid {

	PrintWriter outFile= null; //need Time 	Speed	Direction, in meters per second



	public void step(){
		//2	27.7643	-82.8498	1.0	2	JohnsPass
		ABEMGrid grid = new ABEMGrid("data/ABEMGrid2_WGS_dispMatrixCells.shp", true);	
		new File("output/BOLTs/release.txt").delete(); 
		
		try { 
			outFile= new PrintWriter(new FileWriter("output/BOLTs/release.txt", true));
		} catch (IOException e) {e.printStackTrace();}

		int counter = 0 ; 
		HashMap<Int3D, ABEMCell> cells = grid.getGridCells();
		Set<Int3D> keys = cells.keySet();
		Iterator<Int3D> it = keys.iterator();
		while (it.hasNext()){
			Int3D index = it.next();
			ABEMCell cell = grid.getGridCell(index);
			String indexID = index.x + "_" + index.y;
			
			outFile.println(counter++ + 
					"\t" + cell.getCentroidCoord().x + 
					"\t" + cell.getCentroidCoord().y + 
					"\t" + 1.0 + "\t" + 10000 + "\t" + indexID);  
		}
	
		outFile.close(); 
	}

	
	
	public static void main(String[] args) {
		ReleaseFileBuilder_Grid r = new ReleaseFileBuilder_Grid();
		r.step(); 
	}


}
