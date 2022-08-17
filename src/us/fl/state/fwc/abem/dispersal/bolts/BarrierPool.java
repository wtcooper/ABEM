package us.fl.state.fwc.abem.dispersal.bolts;

import java.util.ArrayList;

public class BarrierPool { 
	
	private ArrayList<Barrier> pool = new ArrayList<Barrier>();
	private int index = 0;
	
	public BarrierPool(Barrier b, int poolSize){
		
		pool.add(b);
		
		for(int i = 0; i < poolSize-1;i++){
			pool.add(b.clone());
		}
	}
	
	public Barrier getBarrier(){
	
		Barrier b = pool.get(index);
		index = (index+1)%pool.size();
		return b;
	}
	
	public void closeConnections(){
		for (int i=0; i<pool.size(); i++){
			pool.get(i).closeConnections();
		}
	}
}

