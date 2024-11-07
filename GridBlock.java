//M. M. Kuttel 2024 mkuttel@gmail.com
// GridBlock class to represent a block in the grid.
// only one thread at a time "owns" a GridBlock - this must be enforced

package medleySimulation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock; 


public class GridBlock {
	
	private AtomicInteger isOccupied; 
	
	private final AtomicBoolean isStart;  //is this a starting block?
	private int [] coords; // the coordinate of the block.

	private final ReentrantLock lock = new ReentrantLock(); // creating a lock to use in get(), to manipulate the block availability to threads. 
	
	GridBlock(boolean startBlock) throws InterruptedException {

		this.isStart = new AtomicBoolean(startBlock); //create an instance that starts the block		
		this.isOccupied = new AtomicInteger(-1); //creates a flag to determine if block is occupied

	}
	
	GridBlock(int x, int y, boolean startBlock) throws InterruptedException {
		this(startBlock);
		coords = new int [] {x,y};
	}
	// TODO : make mutually exclusive
	public synchronized int getX() {return coords[0];}  
	
	public  synchronized int getY() {return coords[1];}
		
	
	//Get a block
	//TODO: make it mutuallly exclusive by adding a synchronizer
	// what if we make it an AtomicBoolean??? 

	// we are going to implement a reentrant lock to ensure each swimmer per grd
    public boolean get(int threadID) throws InterruptedException {
        // Try to acquire the lock before checking or setting the block as occupied
        if (lock.tryLock()) {
            try {
                if (isOccupied.get() == threadID) {
                    return true; // Thread already in this block
                }
                if (isOccupied.get() >= 0) {
                    return false; // Space is occupied
                }
                return isOccupied.compareAndSet(-1, threadID); // Atomically set if unoccupied
            } finally {
                lock.unlock(); // Ensure the lock is released after the operation
            }
        } else {
            // Could not acquire the lock, another swimmer is in the block
            return false;
        }
    }

    // Additional method to release the GridBlock
    public void leave(int threadID) {
        if (isOccupied.compareAndSet(threadID, -1)) {
            lock.unlock(); // Release the lock when the swimmer leaves the block
        }
    }
	//release a block
	public synchronized void release() {

		lock.lock();

		try {

		isOccupied.set(-1);
		notifyAll();  // set a signal to all the threads that are using release 
		}finally{
			lock.unlock();
		}
	}
	
	//is a block already occupied?
	public  boolean occupied() {
		lock.lock();

		try{
		if(isOccupied.get()==-1) return false;
		}finally{
			lock.unlock();
		}
		return true;
	}
	
	
	//is a start block
	public  boolean isStart() {
		return isStart.get();	
	}

}
