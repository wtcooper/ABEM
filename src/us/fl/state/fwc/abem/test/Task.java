package us.fl.state.fwc.abem.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import us.fl.state.fwc.abem.ThreadService;

public class Task implements Runnable{

	ThreadService service; 
	
	public Task(ThreadService service){
		this.service = service;
	}

	@Override
	public void run() {

		
		Thread t = Thread.currentThread();
		//System.out.println("thread: " + t.getName() + "\tstarting run for Task."); 
		
		boolean test = false;
		List<Double> numbers = new ArrayList<Double>();
		
		for (int i = 0; i < 1000000; i++){
			numbers.add(Math.random()*100); 
		}

		Collections.sort((List<Double>) numbers); 

		System.out.println("thread: " + t.getName() + "\tfinishing run and releasing semaphore for Task."); 
		
		//service.releaseSemaphore(); 
	}

}
