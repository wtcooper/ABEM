package us.fl.state.fwc.util;
import java.io.File;
import java.io.FilenameFilter;


public class DeleteFiles {


	/**Deletes the given file
	 * 
	 * @param filename
	 */
	void deleteFile(String filename){
		//	String tokens[] = filename.split("/"); 
		//	String file[] = tokens[1].split("\\."); 
		//	String dirname = tokens[0];
		//	String prefix = file[0];

		File file= new File(filename);
		file.delete();
	}


	/**Deletes the given array of filenames
	 * 
	 * @param filenames
	 */
	public void deleteFiles(String... filenames){
		//	String tokens[] = filename.split("/"); 
		//	String file[] = tokens[1].split("\\."); 
		//	String dirname = tokens[0];
		//	String prefix = file[0];

		for (int i=0; i<filenames.length; i++){
			File file= new File(filenames[i]);
			file.delete();
		}
	}



	/**Deletes any files in the given directory with the given extension (make sure to put the 'dot' in the string, e.g., ".shp").
	 * 
	 * @param dirname
	 * @param extension
	 */
	public void deleteByExt( String dirname, String extension ){

		File dir = new File(dirname); 
		FilenameFilter filter = new FileFilterByExtension(extension); 
		String list[] = dir.list(filter); 
		if (list.length == 0) return;
		File file;  
		for (int i=0; i < list.length; i++) { 
			file = new File(dirname +"/" + list[i]);
			file.delete();
		}
	}

	/**Deletes any files in the given directory with the given prefix before the 'dot' extension.
	 * e.g., for testDirectory/test.shp, would call via deleteFiles("testDirectory", "test")
	 * 
	 * @param dirname
	 * @param prefix 
	 */
	public void deleteByExt( String... filenames) { //dirname, String prefix ){

		for (int i=0; i<filenames.length; i++){
			File fileTemp = new File(filenames[i]);
			String name = fileTemp.getName();
			String dirname = fileTemp.getParentFile().getAbsolutePath();
			
			String filename[] = name.split("\\."); 
			String extension = filename[1];

			File dir = new File(dirname); 
			FilenameFilter filter = new FileFilterByExtension(extension); 
			String list[] = dir.list(filter); 
			if (list.length == 0) return;
			File file;  
			for (int j=0; j < list.length; j++) { 
				file = new File(dirname +"\\" + list[j]);
				file.delete();
			}
		}

	}


	/**Deletes any files in the given directory with the given prefix before the 'dot' extension.
	 * e.g., for testDirectory/test.shp, would call via deleteFiles("testDirectory", "test")
	 * 
	 * @param dirname
	 * @param prefix 
	 */
	public void deleteByPrefix( String dirname, String prefix ){

		File dir = new File(dirname); 
		FilenameFilter filter = new FileFilterByPrefix(prefix); 
		String list[] = dir.list(filter); 
		if (list.length == 0) return;
		File file;  
		for (int i=0; i < list.length; i++) { 
			file = new File(dirname +"/" + list[i]);
			file.delete();
		}
	}

	/**Deletes any files in the given directory with the given prefix before the 'dot' extension.
	 * e.g., for testDirectory/test.shp, would call via deleteFiles("testDirectory", "test")
	 * 
	 * @param dirname
	 * @param prefix 
	 */
	public void deleteByPrefix( String... filenames) { //dirname, String prefix ){

		for (int i=0; i<filenames.length; i++){
			File fileTemp = new File(filenames[i]);
			String name = fileTemp.getName();
			String dirName = fileTemp.getParentFile().getAbsolutePath();
			
			String filename[] = name.split("\\."); 
			String prefix = filename[0];

			File dir = new File(dirName); 
			FilenameFilter filter = new FileFilterByPrefix(prefix); 
			String list[] = dir.list(filter); 
			if (list.length == 0) return;
			File file;  
			for (int j=0; j < list.length; j++) { 
				file = new File(dirName +"\\" + list[j]);
				file.delete();
			}
		}

	}




	class FileFilterByExtension implements FilenameFilter {
		String extension; 
		public FileFilterByExtension(String extension) { 
			this.extension= extension; 
		}
		public boolean accept(File dir, String name) { 
			return name.endsWith(extension);
		}
	}


	class FileFilterByPrefix implements FilenameFilter {
		String prefix; 
		public FileFilterByPrefix(String prefix) { 
			this.prefix = prefix + "."; 
		}
		public boolean accept(File dir, String name) { 
			return name.startsWith(prefix);
		}
	}



}

