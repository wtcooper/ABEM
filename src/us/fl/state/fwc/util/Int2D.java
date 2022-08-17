package us.fl.state.fwc.util;

/**	An integer point location in (x,y,z) space
 * 
 * @author Wade.Cooper
 *
 */
public class Int2D {

	public int x, y;

	public Int2D(){
	}

	public Int2D(int x, int y){
		this.x=x;
		this.y=y;
	}


	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}



	/*Overrides Object.hashCode for storing Points as keys in HashMap's
	 */
	public int hashCode() {
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash( result, x );
		result = HashCodeUtil.hash( result, y );
		return result;
	}


	/*Overrides Object.equals() for comparing Point's as keys in HashMap's
	 */
	public boolean equals(Object other){
		return ( !(other==null) && ((Int2D) other).x == this.x && ((Int2D) other).y == this.y ) ;
	}


}
