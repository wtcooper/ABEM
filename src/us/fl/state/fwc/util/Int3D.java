package us.fl.state.fwc.util;

/**	An integer point location in (x,y,z) space
 * 
 * @author Wade.Cooper
 *
 */
public class Int3D {

	public int x, y, z;

	public Int3D(){
	}

	public Int3D(int x, int y){
		this.x=x;
		this.y=y;
		this.z = 0; 
	}

	public Int3D(int x, int y, int z){
		this.x=x;
		this.y=y;
		this.z=z;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}



	/*Overrides Object.hashCode for storing Points as keys in HashMap's
	 */
	public int hashCode() {
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash( result, x );
		result = HashCodeUtil.hash( result, y );
		result = HashCodeUtil.hash( result, z );
		return result;
	}


	/*Overrides Object.equals() for comparing Point's as keys in HashMap's
	 */
	public boolean equals(Object other){
		return ( !(other==null) && ((Int3D) other).x == this.x && ((Int3D) other).y == this.y && ((Int3D) other).z == this.z) ;
	}

	public Int3D clone() {
		return new Int3D(x, y, z);
	}

}
