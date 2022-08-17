package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.abem.dispersal.bolts.util.TimeConvert;
import us.fl.state.fwc.util.TestingUtils;


/**
 * Writes pertinent trajectory data to an output file in ASCII text format.
 * 
 * @author Johnathan Kool, modified by Wade Cooper
 * 
 */

public class SettlementWriter {

	BufferedWriter stl;
	String durationUnits = "Days";
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Constructor that uses a String to generate the output file.
	 * 
	 * @param outputFile -
	 *            The path and name of the output file
	 */

	public SettlementWriter(String outputFile) {

		try {

			// Create the file and use a BufferedWriter for efficiency.
			new File(outputFile).delete(); //delete existing
			
			FileWriter fw2 = new FileWriter(outputFile); 
			stl = new BufferedWriter(fw2);

			// Write column headers

			stl.write("ID\tTIME\tAGE(days)\tDEPTH\tLON\tLAT\tDIST\tLOCATION\n");

		} catch (IOException e) {
			System.out
					.println("Could not create/access trajectory output file: "
							+ outputFile + ".\n\n");
			e.printStackTrace();
		}

	}

	/**
	 * Actually writes the data parameters to the output file.
	 * 
	 * @param p -
	 *            The particle whose information will be persisted.
	 */

	public synchronized void apply(Particle p) {

		StringBuffer sb = new StringBuffer();

		/*
		 * Write ID, Time (as an actual Date/Time stamp), Duration (Days),
		 * Depth, Longitude, Latitude, Distance /* traveled and Status (S =
		 * settled, L = Lost, M = Dead, I = in transit)
		 */

		sb.append(p.getID() + "\t");
		sb.append(df.format(new Date(p.getT())) + "\t");
		sb.append(TimeConvert.convertFromMillis(durationUnits, p.getAge())
				+ "\t");
		sb.append(p.getZ() + "\t");
		if (p.getX() > 180) {
			sb.append(-(360d - p.getX()) + "\t");
		} else {
			sb.append(p.getX() + "\t");
		}
		sb.append(p.getY() + "\t");
		sb.append(p.getDistance() + "\t");
		if (p.isSettled() == true) {
			try {
				stl.write(sb.toString() + p.getDestination() + "\n");
			} catch (IOException e) {
				System.out.println("Could not access settlement output file: "
						+ stl.toString() + ".\n\n");
				e.printStackTrace();
			}
			sb.append("S" + p.getDestination() + "\t");

		}
		this.notifyAll();
	}

	public void flush() {
		try {
			stl.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
	}

	/**
	 * Closes and cleans up the output file
	 */

	public void close() {

		// Close and flush the settlement file

		try {
			stl.flush();
			stl.close();
		} catch (IOException e) {
			System.out.println("Could not close settlement output file: "
					+ stl.toString() + ".\n\n");
			e.printStackTrace();
		}
	}
}

