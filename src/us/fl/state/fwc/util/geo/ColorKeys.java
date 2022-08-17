package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class ColorKeys {
	Random seed = new Random();
	HashMap<Integer, Color> categories = new HashMap<Integer, Color>();
	ArrayList<Color> colors = new ArrayList<Color>(); 
	int inc = 51;  // 216 unique colors

	public Color getKeyColor(int i) {
		Integer index = new Integer(i); 
		if(!categories.containsKey(index)) {

			while(true) {
				Color color = getColor();
				if(!colors.contains(color)) {
					colors.add(color);
					categories.put(index, color); 
					return color;
				}
			}
		}
		else return categories.get(index); 
	}

	public Color getKeyColor(float i) {
		Integer index = new Integer((int) i); 
		if(!categories.containsKey(index)) {

			while(true) {
				Color color = getColor();
				if(!colors.contains(color)) {
					colors.add(color);
					categories.put(index, color); 
					return color;
				}
			}
		}
		else return categories.get(index); 
	}

	
	private Color getColor() {
		int[] n = new int[3];
		for(int j = 0; j < 3; j++) {
			n[j] = seed.nextInt(6);
		}
		return new Color(n[0]*inc, n[1]*inc, n[2]*inc);
	}
	
	public void setColors(Color[] colorArray){
		for (int i=0; i<colorArray.length; i++){
			categories.put(new Integer(i), colorArray[i]); 
		}
	}
}
