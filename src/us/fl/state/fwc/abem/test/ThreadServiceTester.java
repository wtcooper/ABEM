package us.fl.state.fwc.abem.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import us.fl.state.fwc.abem.ThreadService;


public class ThreadServiceTester {

	ThreadService threadService;

	
	public void step(){

		threadService = new ThreadService(); 
		
		long start = System.currentTimeMillis(); 
		
		//create a batch of Tasks to perform -- needs to implement ThreadServiccan be any class that implements Runnable 
		// here the Task class simple creates a large array of numbers, and then sorts them using ConcurrentCollections.sort() method
		int numTasks = 100; 
		List<Runnable> list = new ArrayList<Runnable>(); 
		for (int i=0; i<numTasks; i++){
			list.add(new Task(threadService)); //NEED to supply threadService instance to the Runnable Object so it can release the Semaphore
		}

		//set the countdown latch on the ThreadService so it knows how many tasks to perform concurrently
		int currentSize = list.size(); 
		threadService.setLatch(currentSize);

		//add all the tasks to the ThreadService
		// NOTE: the addTask(Runnable r) uses a Semaphore (currently set to the #CPUS + 3)
		//		-- therefore, progression will wait until a Semaphore is released by a thread that finished
		for (Runnable runner: list){
			threadService.addTask(runner);  
		}

		//await for the countdown latch to be released once all tasks are run
		threadService.await(); 

		System.out.println("base thread done"); 

		System.out.println("run time: " + (System.currentTimeMillis()-start)); 

		

		
		
		threadService.shutdown();
		System.out.println("threadService shut down"); 
		
	}

	
	public static void main(String[] args) {

		ThreadServiceTester m = new ThreadServiceTester(); 
		m.step(); 
	}

}
