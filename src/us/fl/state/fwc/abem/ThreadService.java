package us.fl.state.fwc.abem;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for multithreading via an ExecutorService and a semaphore to limit concurrent
 * tasks, where the number of Runnable tasks to run is pre-known (in order to set a 
 * CountDownLatch).
 * 
 * @author Wade.Cooper
 *
 */
public class ThreadService {

	protected Object writeLock = new Object(); // lock for writing operations
	protected Object lock = new Object(); // lock for writing operations
	private ExecutorService exec; // = Executors.newFixedThreadPool(N_CPUS+1); // runs thread pool with #CPUs+1 threads
	private Semaphore semaphore; //= new Semaphore(N_CPUS+4); // blocks the task submission rate to work queue to #CPUs+4 tasks
	CountDownLatch latch; 

	int N_CPUS = Runtime.getRuntime().availableProcessors() + 1; 
	

	/**
	 * Construct with the default number of threads (number of CPUs + 1).
	 */
	public ThreadService(){
		exec = Executors.newFixedThreadPool(N_CPUS); // add 1 as per Java Concurrency in Practice suggestion 
		semaphore = new Semaphore(N_CPUS+2); // add more than available threads so that exec list is always full when tasks await
	}


	/**
	 * Construct with the given number of threads.  
	 * 
	 * @param numThreads
	 */
	public ThreadService(int numThreads){
		N_CPUS = numThreads; 
		exec = Executors.newFixedThreadPool(N_CPUS); // add 1 as per Java Concurrency in Practice suggestion 
		semaphore = new Semaphore(N_CPUS+3); // add more than available threads so that exec list is always full when tasks await
	}

	
	/**
	 * Reinitialize the Executor service for another threaded run.
	 */
	public void reinitialize() {
		exec = Executors.newFixedThreadPool(N_CPUS); // add 1 as per Java Concurrency in Practice suggestion 
	}

	
	/**
	 * Adds a Runnable task to the service, and awaits to acquire a semaphore if a given
	 * number of tasks (number of threads + 3) are already running.
	 * 
	 * @param r
	 */
	public void addTask(Runnable r){

		try {
			semaphore.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} 
		try { 
			exec.execute(new ThreadServiceRunner(r, this));
		}   
		catch (RejectedExecutionException e){
			System.out.println("rejected run!");
		}  

	}



	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed, 
	 * but no new tasks will be accepted. Invocation has no additional effect if already shut down.
	 */
	public void shutdown(){
		exec.shutdown(); //shutdown();  // call shutdown of threads once work queue is finished

		try { 
			if (exec.awaitTermination(60, TimeUnit.MINUTES)){ // blocks until all tasks complete, or timeout is reached
			}
		} catch (InterruptedException ex) {ex.printStackTrace(); }

	}


	
	/**
	 * Releases the semaphore (i.e., bottleneck) so another thread can start up
	 */
	public synchronized void releaseSemaphore(){
		if (latch != null) latch.countDown();
		semaphore.release();  // release semaphore "permit" so another task can be added to work queue

	}


	/**
	 * Sets a CountDownLatch of the number of thread to run to the given size. 
	 * 
	 * @param currentSize
	 */
	public void setLatch(int currentSize){
		latch = new CountDownLatch(currentSize); 
	}




	/**
	 * Await all the threads in the list to run.
	 * 
	 */
	public void await(){
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 

	}

}


/**
 * Accompanying class that wraps a Runnable so can call ThreadService.releaseSemaphore()
 * within.
 *   
 * @author Wade.Cooper
 *
 */
class ThreadServiceRunner implements Runnable {

	private Runnable r;
	private ThreadService service;
	
	public ThreadServiceRunner(Runnable r, ThreadService service){
		this.r = r;
		this.service = service;
	}

	@Override
	public void run() {
		r.run();
		service.releaseSemaphore();
	}
	
}
