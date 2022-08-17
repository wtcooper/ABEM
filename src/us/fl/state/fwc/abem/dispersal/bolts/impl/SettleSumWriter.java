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

public class SettleSumWriter {

	BufferedWriter bw;
	TreeMap <String, Long> m = new TreeMap <String, Long>();
	String filename;
	boolean deleteEmpty = true;

	/**
	 * Constructor that uses a String to generate the output file.
	 * 
	 * @param outputFile -
	 *            The path and name of the output file
	 */

	public SettleSumWriter(String outputFile) {

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
			if (m.get(p.getDestination()) != null) {
				m.put(p.getDestination(), m.get(p.getDestination())+ 1);
			} else {
				m.put(p.getDestination(), 1l);
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
			Set<String> keys = m.keySet(); 
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String index = it.next();
				long settleLoc = m.get(index).longValue();
				bw.write(index + "\t" + settleLoc); 
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
			if (m.isEmpty()) {
				File f = new File(filename);
				f.delete();
			}
		}
	}
}

