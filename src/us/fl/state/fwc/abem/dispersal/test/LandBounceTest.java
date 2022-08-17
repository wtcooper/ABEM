package us.fl.state.fwc.abem.dispersal.test;

import java.io.IOException;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.impl.BOLTSParams;

public class LandBounceTest {
	private final  String landFileName = "c:\\work\\data\\BOLTs\\fl_40k_nowater_TBClip_simpleWGS_test.shp"; // Name of the land mask file
	private final  String landKey = "FID";
//	private final  String landLookup = "FID";
	private  boolean negLandCoord = true;			// Does the landmask use negative coordinates?

	BOLTSParams params = new BOLTSParams(null);

	public void step(){
		try {

			Particle p = new Particle(params);

			//for time step 0.02 with tolerance = 0 or <e-12
/*			p.setX(277.2676651340435);
			p.setY(27.650945672632822);
			p.setPX(277.26932932945834);
			p.setPY(27.65064745345356); 
*/
			//for time step 4.22
			p.setPX(277.26900213750434);
			p.setPY(27.681675738613556);
			p.setX(277.2692217050326);
			p.setY(27.681999934516483); 
			

			System.out.println(-(360d - p.getX()) + "\t" + p.getY());
			
			
			ShapefileBarrierTest landmask = new ShapefileBarrierTest();
			landmask.setDataSource(landFileName);
			landmask.setLookupField(landKey);
			landmask.setNegLon(negLandCoord);
			
			landmask.checkReflect(p);
			
			System.out.println(-(360d - p.getX()) + "\t" + p.getY());
			System.out.println("done");
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	
	public static void main(String[] args) {
		LandBounceTest bs = new LandBounceTest();
		bs.step();
	}
}
