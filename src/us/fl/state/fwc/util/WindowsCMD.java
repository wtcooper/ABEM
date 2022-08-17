package us.fl.state.fwc.util;

import java.util.*;
import java.io.*;

public class WindowsCMD {

	
	private boolean outputOn = true; 
	
	public void execute(String args[])
	{

		try
		{            
			String[] cmd = new String[3];
			cmd[0] = "cmd.exe" ;
			cmd[1] = "/C" ;
			cmd[2] = args[0];


			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(cmd);

			// any error message?
			StreamGobbler errorGobbler = new 
					StreamGobbler(proc.getErrorStream(), "ERROR", outputOn);            

			// any output?
			StreamGobbler outputGobbler = new 
					StreamGobbler(proc.getInputStream(), "OUTPUT", outputOn);

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			//wait for it to finish
			proc.waitFor();
			
		} catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	
	/**
	 * Toggle output on or off.
	 * 
	 * @param outputOn
	 */
	public void setOutputOn(boolean outputOn) {
		this.outputOn = outputOn;
	}

}

class StreamGobbler extends Thread
{
	InputStream is;
	String type;
	boolean printOut = true;

	StreamGobbler(InputStream is, String type, boolean printOutput)
	{
		this.is = is;
		this.type = type;
		this.printOut = printOutput;
	}

	public void run()
	{
		try
		{
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line=null;
			while ( (line = br.readLine()) != null)
				if (printOut) System.out.println(type + ">" + line);    
		} catch (IOException ioe)
		{
			ioe.printStackTrace();  
		}
	}
}
