package us.fl.state.fwc.abem.dispersal.bolts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import us.fl.state.fwc.abem.ThreadService;
import us.fl.state.fwc.abem.dispersal.bolts.impl.BOLTSParams;
import us.fl.state.fwc.abem.dispersal.bolts.impl.SettleAvgPLDWriter;
import us.fl.state.fwc.abem.dispersal.bolts.impl.SettleSumWriter;
import us.fl.state.fwc.abem.dispersal.bolts.impl.SettlementWriter;
import us.fl.state.fwc.abem.dispersal.bolts.impl.TrajectoryWriter;
import us.fl.state.fwc.abem.dispersal.bolts.util.TimeConvert;

public class BOLTSScheduler {

	
	BOLTSParams params = new BOLTSParams(null);
	ThreadService tServe = new ThreadService(params.tPoolSize); 
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	SettleSumWriter sum; 
	SettleAvgPLDWriter pld;
	TrajectoryWriter traj ; 
	SettlementWriter settle; 

	
	
	
	
	
	public void disperse() {

		long outertimer = System.currentTimeMillis();
		System.out.println("\nTime started: " + new Date(outertimer));
		
		

		while (!params.releaseDates.isEmpty()){

			//get the next release date in the releaseDates queue
			GregorianCalendar releaseDate = (GregorianCalendar) params.releaseDates.poll();     


			long counter = 0l; 
			File file = new File(params.releaseFile); 
			BufferedReader reader; 

			try {
				reader = new BufferedReader(new FileReader(file));

				//loop over all release groups in the release.txt file
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					String tokens[] = line.split("\t"); 

					long reltimer = System.currentTimeMillis();
					
					
					String folder = params.outputFolder + df.format(releaseDate.getTime());
					//new File(folder).delete(); 
					File outputdir = new File(folder);
					if (!outputdir.isDirectory()) outputdir.mkdir();


					//set up writers
					if (params.writeSettleLocs) {
						settle = new SettlementWriter(folder +"\\" +  tokens[5] + ".set");
						params.setSettlementWrite(settle); 
					}
					if (params.writeSettleSums){
						sum = new SettleSumWriter(folder +"\\" +  tokens[5] + ".sum");
						params.setSettleSumWrite(sum);
					}
					if (params.writeSettleAvgPLD){
						pld = new SettleAvgPLDWriter(folder +"\\" +  tokens[5] + ".pld");
						params.setSettleAvgPLDWriter(pld);
					}
					if (params.writeTrajs){
						traj = new TrajectoryWriter(folder + "\\" + tokens[5] + ".trj");
						params.setTrajWrite(traj);
					}



					int nParts = Integer.parseInt(tokens[4]);
					
					//if uses effective migration, then kill off all of those that would die within the pre-comp dispersal time
					if (params.usesEffectiveMigration){
						double mrate = 1-Math.exp(- ( params.mortRt / ((double) (params.mortRtUnits*1000)/ (double) params.preCompTime))) ;	
						nParts -= (mrate*nParts);
					}
					
					System.out.println("Releasing " + nParts + " particle(s) at location " + tokens[5] + " on " + releaseDate.getTime().toString());

					List<Runnable> list = new ArrayList<Runnable>(); 

					//loop through the total number of particles per release group, and release each through the ThreadService
					for (int i=0; i<nParts; i++){
						Release release = new Release(tServe, params); 
						release.setRelID(Integer.parseInt(tokens[0]));
						release.setLat(Float.parseFloat(tokens[1]));
						release.setLon(Float.parseFloat(tokens[2]));
						release.setZ(Float.parseFloat(tokens[3]));
						release.setLocName(tokens[5]);
						release.setTime(releaseDate.getTimeInMillis());
						release.setId(counter++);

						list.add(release); 
						
					} // end of loop over the number of parts in a single release group

						tServe.setLatch(list.size());

						//add all the tasks to the ThreadService
						// NOTE: the addTask(Runnable r) uses a Semaphore (currently set to the #CPUS + 3)
						//		-- therefore, progression will wait until a Semaphore is released by a thread that finished
						Iterator<Runnable> it = list.iterator(); 

						int count2 = 0; 
						while (it.hasNext()){
							
							//System.out.println("Release " + (++count2) + " running");
							tServe.addTask(it.next()); //semaphore will set a latch here  
						}

						//await for the countdown latch to be released once all tasks are run
						tServe.await(); 
						
						System.out.println("Location " + tokens[5] + " on " + df.format(releaseDate.getTime())+ " complete. (" +TimeConvert.millisToString(System.currentTimeMillis()-reltimer)+ ")");

					//close the writers which writes and flushes them
					if (traj != null) traj.close();
					if (sum != null) sum.close();
					if (pld != null) pld.close();
					if (settle != null) settle.close(); 

				} // end of loop over release.txt reader
				reader.close();
				System.out.println("End of release.txt file.");
				



			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("catching error " + e);
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("catching error " + e);
				System.exit(1);
			}

			//releaseDate.add(params.releaseSpUnits, params.releaseSpacing); // add appropriate 
		} //end of while loop to go over all the release dates

		tServe.shutdown();
		params.closeConnections();

		System.out.println("\nTime finished: "
				+ new Date(System.currentTimeMillis())
				+ " ("
				+ TimeConvert.millisToString(System.currentTimeMillis()
						- outertimer) + ")");
	}


	public static void main(String[] args) {
		BOLTSScheduler bs = new BOLTSScheduler();
		bs.disperse();
	}

}
