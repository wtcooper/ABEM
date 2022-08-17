package us.fl.state.fwc.abem.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import jsr166y.Phaser;

public class PhaserTest {

	final Phaser phaser = new Phaser();
	private ExecutorService exec; // = Executors.newFixedThreadPool(N_CPUS+1); // runs thread pool with #CPUs+1 threads
	private Semaphore semaphore; //= new Semaphore(N_CPUS+4); // blocks the task submission rate to work queue to #CPUs+4 tasks

	
	public static void main(String[] args) {
		PhaserTest t = new PhaserTest();
		t.step();
				
	}
	
	public void step(){
		exec = Executors.newFixedThreadPool(4); // add 1 as per Java Concurrency in Practice suggestion 
		//semaphore = new Semaphore(2); // add more than available threads so that exec list is always full when tasks await

		doWork(); 
		exec.shutdownNow();
	}

	public Runnable getRunnable(int i){
				final int count = i; 
		        return new Runnable(){
		                public void run(){

		                	phaser.register();

		                	Thread t = Thread.currentThread();

	                		System.out.println("thread: " + t.getName() + "\tstarting run for loop " + count); 


		                		boolean test = false;
		                		List<Double> numbers = new ArrayList<Double>();
		                		for (int i = 0; i < 1000000; i++){
		                			numbers.add(Math.random()*100); 
		                		}
		                		Collections.sort((List<Double>) numbers); 
		                		System.out.println("thread: " + t.getName() + "\tfinishing for loop " + count); 

		                		//semaphore.release(); 
		                        phaser.arriveAndDeregister(); //.arrive();
		                }
		        };
		}

	
	public void doWork(){
		        phaser.register();//register self
		        for(int i=0 ; i < 10; i++){
		    		/*try {
		    			semaphore.acquire();
		    		} catch (InterruptedException e1) {
		    			e1.printStackTrace();
		    		} */
		    		try { exec.execute(getRunnable(i));} // add TimeKeeper objects to work queue; ExecutorService will assign to threads when available  
		    		catch (RejectedExecutionException e){}  

		        }
		        phaser.arriveAndAwaitAdvance();
		        
		        System.out.println("all finished"); 
		}

}
