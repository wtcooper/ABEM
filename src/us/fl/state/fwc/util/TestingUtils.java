package us.fl.state.fwc.util;

/**	Class with simple testing utilities for outputting current thread location in source code.
 * 
 * @author Wade.Cooper
 *
 */
public final class TestingUtils {

	
	/**	Outputs to console the class name, method, and line number at the point of calling.
	 */
	public static void dropBreadCrumb() {
		String FQClassName = Thread.currentThread().getStackTrace()[2].getClassName();
		int firstChar;
		firstChar = FQClassName.lastIndexOf ('.') + 1;
		if ( firstChar > 0 ) {
			FQClassName = FQClassName.substring ( firstChar );
		}
		
		String method = Thread.currentThread().getStackTrace()[2].getMethodName();
		int lineNum = Thread.currentThread().getStackTrace()[2].getLineNumber();
		
		System.out.println(".........bread crumb at " + FQClassName+"."+method+"(), line number: " + lineNum);
	}
	
	
	public static int getLineNumber() {
	    return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}
	
	
	public static String getMethodName() {
	    return Thread.currentThread().getStackTrace()[2].getMethodName();
	}
	
	
	public static String getClassName() {
		String FQClassName = Thread.currentThread().getStackTrace()[2].getClassName();
		int firstChar;
		firstChar = FQClassName.lastIndexOf ('.') + 1;
		if ( firstChar > 0 ) {
			FQClassName = FQClassName.substring ( firstChar );
		}
		return FQClassName;
	}
}
