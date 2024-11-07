package medleySimulation;

import medleySimulation.Swimmer.SwimStroke;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;



import medleySimulation.Swimmer.SwimStroke;
import java.util.concurrent.Semaphore;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;



public class SwimTeam extends Thread {

    public static StadiumGrid stadium; // Shared resource
    private int teamNo; // Team number
    private ArrayList<Swimmer> swimmers; // List of swimmers

    private static final int maxLanes = 10; // Number of lanes
    private static final Semaphore entranceSemaphore = new Semaphore(maxLanes, true); // Semaphore for lane management

    private static final CyclicBarrier lineupBarrier = new CyclicBarrier(4); // Barrier for lineup, adjust based on team size

    public static final int sizeOfTeam = 4;
    private ArrayList<Swimmer> orderedSwimmers = new ArrayList<>();

    SwimTeam(int ID, FinishCounter finish, PeopleLocation[] locArr) {
        this.teamNo = ID;
        swimmers = new ArrayList<>();
        SwimStroke[] strokes = SwimStroke.values();

        for (int i = teamNo * sizeOfTeam, s = 0; i < ((teamNo + 1) * sizeOfTeam); i++, s++) {
            locArr[i] = new PeopleLocation(i, strokes[s].getColour());
            int speed = (int) (Math.random() * (3) + 30);
            Swimmer swimmer = new Swimmer(i, teamNo, locArr[i], finish, speed, strokes[s]);
            swimmers.add(swimmer);
        }

        orderedSwimmers.addAll(swimmers);
        orderedSwimmers.sort((s1, s2) -> Integer.compare(s1.getSwimStroke().getOrder(), s2.getSwimStroke().getOrder()));
    }

    public void run() {
        try {
            Map<Swimmer.SwimStroke, ArrayList<Swimmer>> strokeGroups = new HashMap<>();
            for (Swimmer swimmer : orderedSwimmers) {
                strokeGroups.computeIfAbsent(swimmer.getSwimStroke(), k -> new ArrayList<>()).add(swimmer);
            }

            ArrayList<Swimmer.SwimStroke> strokeOrder = new ArrayList<>();
            for (Swimmer swimmer : orderedSwimmers) {
                Swimmer.SwimStroke stroke = swimmer.getSwimStroke();
                if (!strokeOrder.contains(stroke)) {
                    strokeOrder.add(stroke);
                }
            }

            for (Swimmer.SwimStroke stroke : strokeOrder) {
                ArrayList<Swimmer> group = strokeGroups.get(stroke);
                if (group != null) {
                    for (Swimmer swimmer : group) {
                        enterEntrance(swimmer);
                        swimmer.start();
                        lineupBarrier.await(); // Wait for all swimmers to be lined up
                    }

                    for (Swimmer swimmer : group) {
                        swimmer.join();
                    }
                }
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    public static void enterEntrance(Swimmer swimmer) throws InterruptedException {
        entranceSemaphore.acquire(); // Acquire a permit
        try {
            swimmer.enterStadium(); // Swimmer enters the stadium
        } finally {
            entranceSemaphore.release(); // Release the permit
        }
    }

    public static CyclicBarrier getBarrier() {
        return lineupBarrier;
    }
}
