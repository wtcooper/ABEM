package us.fl.state.fwc.util;

public class ClassUtils {


	/**	Gets the class name without the package.  Takes in class argument, e.g., object.getClass(), 
	 * and returns a string
	 * 
	 * @param object
	 * @return String -- the class name without package
	 */
	public static String getClassName(Object ob) {
		String FQClassName = ob.getClass().getName();
		int firstChar;
		firstChar = FQClassName.lastIndexOf ('.') + 1;
		if ( firstChar > 0 ) {
			FQClassName = FQClassName.substring ( firstChar );
		}
		return FQClassName;
	}
	
	
}
