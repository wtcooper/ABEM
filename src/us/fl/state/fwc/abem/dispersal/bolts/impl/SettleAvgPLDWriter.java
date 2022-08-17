package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.util.TestingUtils;


/**
 * Persists particle information to a text-based output file.
 * 
 * @author Johnathan Kool
 *
 */

public class SettleAvgPLDWriter {

	BufferedWriter bw;
	//map of sum of settlement per site (settlement locations are recorded as 
	//closest release location to create a matrix
	TreeMap <String, Long> sumPerSite = new TreeMap <String, Long>();
	//map of the sum of ages per site
	TreeMap <String, Double> sumAgesPerSite = new TreeMap <String, Double>();

	
	String filename;
	boolean deleteEmpty = true;

	/**
	 * Constructor that uses a String to generate the output file.
	 * 
	 * @param outputFile -
	 *            The path and name of the output file
	 */

	public SettleAvgPLDWriter(String outputFile) {

		filename = outputFile;

		try {

			// Create the file and use a BufferedWriter for efficiency.
			new File(outputFile).delete(); //delete existing

			FileWriter fw = new FileWriter(outputFile);
			bw = new BufferedWriter(fw);

		} catch (IOException e) {
			System.out.println("Could not create/access matrix output file: "
					+ outputFile + ".\n\n");
			e.printStackTrace();
		}
	}

	/**
	 * Performs the action of persisting the Particle's relevant information.
	 * 
	 * @param p
	 */
	
	public synchronized void apply(Particle p) {
		if (p.isSettled()) {
			if (sumPerSite.get(p.getDestination()) != null) {
				sumPerSite.put(p.getDestination(), sumPerSite.get(p.getDestination())+ 1);
				sumAgesPerSite.put(p.getDestination(), sumAgesPerSite.get(p.getDestination())+ p.getAgeInDays());
			} else {
				sumPerSite.put(p.getDestination(), 1l);
				sumAgesPerSite.put(p.getDestination(), p.getAgeInDays());
			}
		}
	}
	
	/**
	 * Performs close and cleanup operations for the output file.
	 */

	public void flush(){
		try {
			bw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 

		}
	}
	
	public void close() {

		try {
			Set<String> keys = sumPerSite.keySet(); 
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String index = it.next();
				double avgPLD = sumAgesPerSite.get(index).doubleValue() / (double) sumPerSite.get(index).longValue();
				bw.write(index + "\t" + avgPLD + "\t" + sumPerSite.get(index).longValue()); 
				bw.newLine(); 
			}
			
			//bw.write(m.toString());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println("Could not close trajectory output file: "
					+ bw.toString() + ".\n\n");
			e.printStackTrace();
		}

		if (deleteEmpty) {
			if (sumPerSite.isEmpty()) {
				File f = new File(filename);
				f.delete();
			}
		}
	}
}

