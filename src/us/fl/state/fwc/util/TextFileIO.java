package us.fl.state.fwc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility file input/output class with synchronized methods to print or read lines.  
 *  
 * @author Wade.Cooper
 *
 */

public class TextFileIO {

	private String filename; 
	private PrintWriter out= null; 
	private BufferedReader reader = null; 
	private boolean appendFile = false;

	
	public TextFileIO(String filename){
		this.filename = new File(filename).getAbsolutePath();  
	}




	/**
	 * Prints a string via synchronized method. If the argument is null then the string "null" 
	 * is printed.  Otherwise, the string's characters are converted into bytes according to the 
	 * platform's default character encoding, and these bytes are written in exactly 
	 * the manner of the write(int) method.
	 * 
	 * @param s The String to be printed
	 */
	public synchronized void print(String s){
		if (out == null) out = getWriter();
		out.print(s);
	}

	/**
	 * Prints a String via syncrhonized method and then terminates the line. 
	 * This method behaves as though it invokes print(String) and then println().
	 * 
	 * @param s
	 */
	public synchronized void println(String s){
		if (out == null) out = getWriter();
		out.println(s);
	}

	/**
	 * Reads a line of text. A line is considered to be terminated by any one of a line feed ('\n'), 
	 * a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
	 * 
	 * @return A String containing the contents of the line, not including any line-termination 
	 * characters, or null if the end of the stream has been reached
	 */
	public synchronized String readLine(){
		if (reader == null) reader = getReader();
		try {
			return reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	

	/**
	 * Close up shop when done.
	 */
	public void close() {
		if (out != null) out.close();
		if (reader != null) {
			try { 
				reader.close();
			} catch (IOException e) {e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Sets whether the writer should append the file or replace the file.  Default is to replace.
	 * 
	 * @param append
	 */
	public void setAppend(boolean append) {
		this.appendFile = append;
	}
	
	/**
	 * Get a PrintWriter.  
	 * 
	 * @return
	 */
	private PrintWriter getWriter(){

		//erase any old files if writing to file and not appending
		if (!appendFile) new File(filename).delete();
		File fFile = new File(filename);

		try { 
			
			out= new PrintWriter(new FileWriter(fFile, true));
			return out;
		} catch (IOException e) {e.printStackTrace();
		}
		return null;
	}



	/**
	 * Get a BufferedReader.  Use as:
	 * BufferedReader reader = textFileIO.getReader();
	 * 
	 * 			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
	 *				String[] tokens = line.split("\t");
	 *				...
	 *			} 
	 * 
	 * @return
	 */
	private BufferedReader getReader() {

		File fFile = new File(filename);
		try { 
			reader = new BufferedReader(new FileReader(fFile));
			return reader; 
		} catch (IOException e) {e.printStackTrace();
		}
		return null;
	}


	


}
