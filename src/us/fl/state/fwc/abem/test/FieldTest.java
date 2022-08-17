package us.fl.state.fwc.abem.test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;

public class FieldTest {

	public double val ;
	public int val2 ;

	public HashMap<String, Integer> map; 
	
	public void step() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{

		String fieldName = "val2"; 
		int value = 1; 

		Field f = this.getClass().getField(fieldName);
		Type t = f.getType();
	
		
		if (t.toString().equalsIgnoreCase("int")) 
			f.setInt(this, value);


		else if (t.toString().equalsIgnoreCase("double")) 
			f.setDouble(this, value);

		
		
		System.out.println(t.toString() + "\t" + f.get(this));
	}
	
	
	
	
	public static void main(String[] args) {
		FieldTest ft = new FieldTest();
		try {
			ft.step();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
