package us.fl.state.fwc.abem.dispersal.bolts;

import java.text.DecimalFormat;

import us.fl.state.fwc.abem.ThreadService;
import us.fl.state.fwc.abem.dispersal.bolts.impl.BOLTSParams;
import us.fl.state.fwc.abem.dispersal.bolts.impl.SettleAvgPLDWriter;
import us.fl.state.fwc.abem.dispersal.bolts.impl.SettleSumWriter;
import us.fl.state.fwc.abem.dispersal.bolts.impl.SettlementWriter;
import us.fl.state.fwc.abem.dispersal.bolts.impl.TrajectoryWriter;
import us.fl.state.fwc.abem.dispersal.bolts.util.Utils;

/**
 * 
 * Performs actions associated with releasing a single Particle object from a
 * single Habitat source.
 * 
 * @author Johnathan Kool, modified by Wade Cooper
 * 
 */

public class Release implements Runnable {
	DecimalFormat df = new DecimalFormat("#.##"); 

	boolean consoleOutput = false; 
	boolean drawParticle = false; 
	private ThreadService tServe; 
	BOLTSParams params; 

	@SuppressWarnings("unused")
	private int relID; // Release ID
	private float lon, lat, depth; // Longitude, Latitude and Depth
	private String locName; // Location name
	private long time;
	long id;

	private Mortality mort;
	private VerticalMigration vm;
	private HorizontalMigration hm;
	private Settlement sm;
	private Movement mv;
	private TurbVar tbv;
	private Barrier landmask;

	private SettleSumWriter sum; 
	private SettleAvgPLDWriter pld; 
	private TrajectoryWriter traj; 
	private SettlementWriter settle; 




	public Release(ThreadService tServe, BOLTSParams params){
		this.tServe = tServe; 
		this.params = params;

		mort = params.getMort();
		vm = params.getVertMigrClone();
		if (params.usesHorizontalMigration) hm = params.getHorMigrClone();
		sm = params.getSettle();
		mv = params.getMoveClone();
		landmask = params.getLandMask(); 
		tbv = params.getTurbVarClone();
		
		sum =params.getSettleSumWrite();
		pld = params.getSettleAvgPLDWriter();
		traj = params.getTrajWrite();
		settle = params.getSettlementWrite(); 
	}


	/**
	 * Release event of a single particle from a single polygon
	 */

	@SuppressWarnings("static-access")
	public void run() {


		// Create the particle object.

		Particle p = new Particle(params );


		p.setID(id);

		// Then set the coordinates.
		if (params.negPolyCoord) {
			p.setX((lon + 360) % 360);
		} else {
			p.setX(lon);
		}

		p.setY(lat);
		p.setZ(depth);

		p.setPX(p.getX());
		p.setPY(p.getY());
		p.setBirthday(time);
		p.setSource(locName);


		long writect = 0;
		int step = 0;

		// For each time step...
		for (long j = 0; j < params.compTime; j += params.releaseTimeStep) {
			
			long startTime = System.currentTimeMillis(); 

			// Update the time reference

			p.setT(time + j);

			//if the time is greater than the max computation date (i.e., date that no more hydro data exists for) then break
			if (p.getT()>params.maxTime) {
				closeConnections();
				if (params.drawParticles) params.removeFromMap(p);
				break;
			}

			
			
			// Perform mortality in advance for computational
			// efficiency.

			if (params.usesEffectiveMigration) {
				if (p.getAge() > params.preCompTime) {
					mort.apply(p);
					if (p.isDead()) {
						closeConnections();
						if (params.drawParticles) params.removeFromMap(p);
						break;
					}
				}

				// Or, go about it the traditional way.

			} else {
				mort.apply(p);
				if (p.isDead()) {
					closeConnections();
					if (params.drawParticles) params.removeFromMap(p);
					break;
				}
			}

			// Save the original latitude and longitude so we can
			// calculate distance traveled.

			// Use the Runge-Kutta function to move the particle

			mv.apply(p);


			// Apply stochastic turbulent velocity
			// do this before biological movements (i.e., vert and horz migration) since this is
			//function of physics
			if (params.usesTurbVar){
				tbv.apply(p);
			}

			
			
			// Apply vertical migration
			if (params.usesVerticalMigration) {
				vm.apply(p);
			}

			
			// Apply horizontal migration after vertMigration
			if (params.usesHorizontalMigration){
				if (params.usesHorizontalMigration) hm.apply(p);
			}
			




			
			
			// Calculate the change in distance first, otherwise
			// reflection would truncate the distance traveled.

			p.setDistance(p.getDistance() + Utils.distance_Sphere(p.getPX(), p.getPY(), p.getX(), p.getY()));

			// Check and see if the particle has bounced off land. 
			// if we're using a grid barrier versus a shapefile barrier, then run
			// checkReflect(p) each time, since will check within GridBarrier

			if (params.useGridBarrier == true) {
				landmask.checkReflect(p);
			}
			else if (mv.isNearNoData()) {
				landmask.checkReflect(p);
			}

			if (params.drawParticles) params.updateMap(p); 
			
			//check the location of the particle to see if it is out of the model bounds, and if so, set it = lost
			double lon = p.getX(); 
			if (lon> 180) lon = -(360d - lon);
			double lat = p.getY(); 
			if ( lon<params.minLon || lon>params.maxLon || lat<params.minLat || lat>params.maxLat) p.setLost(true); 
			
			
			if (p.isLost()) {
				//System.out.println("\tparticle " + p.getID() + " @ age (days) " + df.format((double)p.getAge()/(1000*60*60*24))+ " IS LOST!!!");
				closeConnections();
				if (params.drawParticles) params.removeFromMap(p);
				break;
			}
			
			// Can the particle settle?
			sm.apply(p);

			// If we have exceeded the output frequency threshold,
			// or if settling has occurred, then write.

			if (writect >= params.outputFreq || p.isSettled()) {

				if (settle != null) settle.apply(p); 
				if (pld != null) pld.apply(p);
				if (sum != null) sum.apply(p);
				if (traj != null) traj.apply(p); 

				writect = 0;
			}
			writect += params.releaseTimeStep;

			// If we're lost or dead, then there's no point in going
			// on...
			String age = df.format((double)p.getAge()/(1000*60*60*24));

			
//			if (age.equals("0.35")) {
//				System.out.println("test stop"); 
//			}
			
			
			if (p.isSettled()) {
				//System.out.println("\tparticle " + p.getID() + " @ age (days) " +age + " has settled @ " + p.getDestination()); 
				closeConnections();
				if (params.drawParticles) params.removeFromMap(p);
				break;
			}

			step++;
			
			if (params.outputParticleSteps) System.out.println("\tparticle " + p.getID() + " @ age (days) " +age +  ":\tu=" + df.format(p.getU()) + ":\tv=" + df.format(p.getV()) + "\tStep run time: " + ((System.currentTimeMillis()-startTime)) + " millisecs");


			//add in a display lag
			if (params.drawParticles && params.displayLag > 0) try { Thread.currentThread().sleep((long) (params.displayLag*1000)); } catch (InterruptedException e) { e.printStackTrace(); }

		}

		//tServe.releaseSemaphore(); 
	}

	
	/**Closes netCDF connections where they exist in clones
	 * 
	 */
	public void closeConnections(){
		vm.closeConnections(); 
		mv.closeConnections(); 
		if (params.usesHorizontalMigration) hm.closeConnections();
	}


	public void setRelID(int relID) {
		this.relID = relID;
	}

	public void setLon(float lon) {
		this.lon = lon;
	}

	public void setLat(float lat) {
		this.lat = lat;
	}

	public void setZ(float z) {
		this.depth = z;
	}

	public void setLocName(String locName) {
		this.locName = locName;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId(){
		return id; 
	}


}

