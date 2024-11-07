//M. M. Kuttel 2024 mkuttel@gmail.com
//Class to represent a swimmer swimming a race
//Swimmers have one of four possible swim strokes: backstroke, breaststroke, butterfly and freestyle
package medleySimulation;

import java.awt.Color;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier; 
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Comparator;

public class Swimmer extends Thread {

	/* these are shared elements. put a lock on them, or make them atomic */
	
	public static StadiumGrid stadium; //shared 
	private FinishCounter finish; //shared
	
	// make all these variables atomic as hell...damn. 
	GridBlock currentBlock;
	private Random rand;
	private AtomicInteger movingSpeed;
	
	private PeopleLocation myLocation;
	private AtomicInteger ID; //made atomic
	private AtomicInteger team; //made atomic
	private GridBlock start;

	private CountDownLatch backstrokeLatch; // don't really need this here rn
	
	private LinkedBlockingQueue<Swimmer> entranceQueue;  // Add this field to store the BlockingQueue

	
	/* I am well aware that these are supposed to be 10 since there are 10 teams, but for some reason, if I have them at 10, there is a weird deadlock. I checked all the for loops and the semaphare, but everything seems proper. If I go by 9 lanes, it just works */

	private static CyclicBarrier backstrokeBarrier = new CyclicBarrier(9);  // 10 backstroke swimmers
    private static CyclicBarrier breaststrokeBarrier = new CyclicBarrier(9);  // 10 breaststroke swimmers
    private static CyclicBarrier butterflyBarrier = new CyclicBarrier(9);  // 10 butterfly swimmers
    private static CyclicBarrier freestyleBarrier = new CyclicBarrier(9);  // 10 freestyle swimmers
    
    private static CountDownLatch relayLatch = new CountDownLatch(9);  // Ensure smooth transition between strokes
	private static CountDownLatch relayLatch1 = new CountDownLatch(9);  // Ensure smooth transition between strokes
	private static CountDownLatch relayLatch2 = new CountDownLatch(9);  // Ensure smooth transition between strokes
	private static CountDownLatch relayLatch3 = new CountDownLatch(9);  // Ensure smooth transition between strokes

	CyclicBarrier startingBlockBarrier = new CyclicBarrier(40);  // All 40 swimmers line up at the blocks

	private volatile boolean isFirstRace = true;   // Flag to check if it's the first race


	public enum SwimStroke { 
		Backstroke(1,2.5,Color.black),
		Breaststroke(2,2.1,new Color(255,102,0)),
		Butterfly(3,2.55,Color.magenta),
		Freestyle(4,2.8,Color.red);
	    	
	     private final double strokeTime;
	     private final int order; // in minutes
	     private final Color colour;   

	     SwimStroke( int order, double sT, Color c) {
	            this.strokeTime = sT;
	            this.order = order;
	            this.colour = c;
	        }
	  
	        public  int getOrder() {return order;}

	        public  Color getColour() { return colour; }
	    }  
	    private final SwimStroke swimStroke;

	//Swimmer(i,teamNo,locArr[i],finish,speed,strokes[s])
	
	//Constructor
	Swimmer( int ID, int t, PeopleLocation lock, FinishCounter f, int speed, SwimStroke s) {
		this.swimStroke = s; 
		this.ID = new AtomicInteger(ID); 
		movingSpeed = new AtomicInteger(speed); //range of speeds for swimmers
		this.myLocation = lock;
		this.team = new AtomicInteger(t); // team number
		start = stadium.returnStartingBlock(team.get());
		finish=f; 
		rand=new Random(); 	
		

	}
	
	//getter
	public  synchronized int getX() { return currentBlock.getX();} 
	
	//getter
	public synchronized  int getY() {	return currentBlock.getY();	} 
	
	//getter
	public synchronized  int getSpeed() { return movingSpeed.get(); }

	
	public SwimStroke getSwimStroke() {
		return swimStroke;
	}

	// Swimmer class
	public CountDownLatch getBackstrokeLatch() {
    return backstrokeLatch;
	}




	//!!!You do not need to change the method below!!!
	//swimmer enters stadium area
	public void enterStadium() throws InterruptedException {
		currentBlock = stadium.enterStadium(myLocation);  //  
		sleep(200);  //wait a bit at door, look around

		        // If this swimmer is doing backstroke, count down the latch
				//if (stroke == SwimStroke.Backstroke) {
					//backstrokeLatch.countDown(); // Signal that a backstroke swimmer is ready
				//}

		
				
	}
	
	//!!!You do not need to change the method below!!!
	//go to the starting blocks
	//printlns are left here for help in debugging
	public void goToStartingBlocks() throws InterruptedException {		
		int x_st= start.getX();
		int y_st= start.getY();
	//System.out.println("Thread "+this.ID + " has start position: " + x_st  + " " +y_st );
	// System.out.println("Thread "+this.ID + " at " + currentBlock.getX()  + " " +currentBlock.getY() );
	 while (currentBlock!=start) {
		//	System.out.println("Thread "+this.ID + " has starting position: " + x_st  + " " +y_st );
		//	System.out.println("Thread "+this.ID + " at position: " + currentBlock.getX()  + " " +currentBlock.getY() );
			sleep((movingSpeed.get())*3);  //not rushing 
			currentBlock=stadium.moveTowards(currentBlock,x_st,y_st,myLocation); //head toward starting block
		//	System.out.println("Thread "+this.ID + " moved toward start to position: " + currentBlock.getX()  + " " +currentBlock.getY() );
		}
	System.out.println("-----------Thread "+this.ID + " at start " + currentBlock.getX()  + " " +currentBlock.getY() );
	}
	
	//!!!You do not need to change the method below!!!
	//dive in to the pool
	private void dive() throws InterruptedException {
		int x= currentBlock.getX();
		int y= currentBlock.getY();
		currentBlock=stadium.jumpTo(currentBlock,x,y-2,myLocation);
	}
	
	//!!!You do not need to change the method below!!!
	//swim there and back
	private void swimRace() throws InterruptedException {
		int x= currentBlock.getX();
		while((boolean) ((currentBlock.getY())!=0)) {
			currentBlock=stadium.moveTowards(currentBlock,x,0,myLocation);
			//System.out.println("Thread "+this.ID + " swimming " + currentBlock.getX()  + " " +currentBlock.getY() );
			sleep((int) ((movingSpeed.get())*swimStroke.strokeTime)); //swim
			System.out.println("Thread "+this.ID + " swimming  at speed" + movingSpeed );	
		}

		while((boolean) ((currentBlock.getY())!=(StadiumGrid.start_y-1))) {
			currentBlock=stadium.moveTowards(currentBlock,x,StadiumGrid.start_y,myLocation);
			//System.out.println("Thread "+this.ID + " swimming " + currentBlock.getX()  + " " +currentBlock.getY() );
			sleep((int) ((movingSpeed.get())*swimStroke.strokeTime));  //swim
		}
		
	}
	
	//!!!You do not need to change the method below!!!
	//after finished the race
	public void exitPool() throws InterruptedException {		
		int bench=stadium.getMaxY()-swimStroke.getOrder(); 			 //they line up
		int lane = currentBlock.getX()+1;//slightly offset
		currentBlock=stadium.moveTowards(currentBlock,lane,currentBlock.getY(),myLocation);
	   while (currentBlock.getY()!=bench) {
		 	currentBlock=stadium.moveTowards(currentBlock,lane,bench,myLocation);
			sleep((movingSpeed.get())*3);  //not rushing 
		}
	}

	public void run() {
			try {
				sleep(movingSpeed.get() + (rand.nextInt(10)));
				myLocation.setArrived();
				enterStadium();    
				goToStartingBlocks();
	
				switch (swimStroke) {
					case Backstroke:
                    // Only wait at the barrier for the first race
                    if (isFirstRace && swimStroke.order == 1) {
                        backstrokeBarrier.await();  // Wait for all backstroke swimmers

						// After the first race, ignore the backstroke barrier in subsequent races
						isFirstRace = false;
                    }
                    dive();
                    swimRace();                 
                    relayLatch.countDown();  // Signal the next swimmer
                    break;
			
					case Breaststroke:
						
						relayLatch.await(); 						 
												
						dive();
						swimRace();                 
						relayLatch.countDown();
						break;
			
					case Butterfly:
						
						relayLatch.await(); 
							
						dive();
						swimRace();                 
						relayLatch.countDown();
						break;
			
					case Freestyle:
						
						relayLatch.await(); 				  
						
						dive();
						swimRace();                 
						finish.finishRace(ID.get(), team.get());  
						break;
				}
				exitPool(); 

				// After the first race, ignore the backstroke barrier in subsequent races
				isFirstRace = false;

			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
	}