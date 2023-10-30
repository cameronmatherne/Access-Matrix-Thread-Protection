import java.util.*;
import java.util.concurrent.Semaphore;

public class objectList {
    static int numOfDomains;
    static int numOfThreads;
    static List<List<String>> objectAccessLists;
    static List<List<String>> domainAccessLists;
    static List<Semaphore> resourceSemaphores; // Semaphores for controlling resource access

    public static void run() {
        Random random = new Random();
        Scanner input = new Scanner(System.in);

        do {
            System.out.print("Domain Count: (enter a number between 3-7)");
            numOfDomains = input.nextInt();
        } while (numOfDomains < 3 || numOfDomains > 7);

        do {
            System.out.print("Object Count: (enter a number between 3-7)");
            numOfThreads = input.nextInt();
        } while (numOfThreads < 3 || numOfThreads > 7);

        input.close();

        generateAccessLists();
        printAccessLists(objectAccessLists, domainAccessLists);

        // Initialize semaphores for resources
        resourceSemaphores = new ArrayList<>();
        for (int i = 0; i < numOfThreads; i++) {
            resourceSemaphores.add(new Semaphore(1)); // Each resource has a semaphore
        }

        // Create and start a thread for each domain
        for (int i = 0; i < numOfDomains; i++) {
            MyThread t = new MyThread(i);
            t.start();
        }
    }

    public static class MyThread extends Thread {
        int threadNum;
        int domainNum;

        public MyThread(int domainNum) {
            this.domainNum = domainNum;
            this.threadNum = domainNum + 1;
        }

        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                int action = generateRandomNum(0, 2); // 0 - Read, 1 - Write, 2 - Domain Switch
                String actionText = (action == 0) ? "Read" : (action == 1) ? "Write" : "Domain Switch";

                if (action == 0 || action == 1) {
                    int targetNum = generateRandomNum(0, numOfThreads - 1);
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Attempting to " + actionText + " resource: F" + (targetNum + 1));

                    if (action == 0) {
                        arbitratorRead(threadNum, targetNum, domainNum);
                    } else if (action == 1) {
                        arbitratorWrite(threadNum, targetNum, domainNum);
                    }
                } else {
                    int newDomain;
                    do {
                        newDomain = generateRandomNum(1, numOfDomains - 1);
                    } while (newDomain == domainNum);
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Attempting to switch to D" + (newDomain));
                    arbitratorDomainSwitch(threadNum, newDomain, domainNum);
                }
            }
        }
    }

    public static boolean arbitratorRead(int threadNum, int targetNum, int domainNum) {
        String permission = objectAccessLists.get(targetNum).get(domainNum);
        if (permission.contains("R") || permission.contains("R/W")) {
            try {
                resourceSemaphores.get(targetNum).acquire(); // Acquire semaphore to access resource
                System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission allowed.");
                waitMethod(threadNum, domainNum);
                resourceSemaphores.get(targetNum).release(); // Release semaphore after accessing resource
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission denied.");
            waitMethod(threadNum, domainNum);
            return false;
        }
    }

    public static boolean arbitratorWrite(int threadNum, int targetNum, int domainNum) {
        String permission = objectAccessLists.get(targetNum).get(domainNum);
        if (permission.contains("W") || permission.contains("R/W")) {
            try {
                resourceSemaphores.get(targetNum).acquire(); // Acquire semaphore to access resource
                System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission allowed.");
                System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Writes: " + generateColorString());
                waitMethod(threadNum, domainNum);
                resourceSemaphores.get(targetNum).release(); // Release semaphore after accessing resource
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission denied.");
            waitMethod(threadNum, domainNum);
            return false;
        }
    }

    public static void arbitratorDomainSwitch(int threadNum, int domainNum, int domainNum2) {
        Random random = new Random();
        int newDomain;

        do {
            newDomain = random.nextInt(numOfDomains);
        } while (newDomain == domainNum); // Ensure that the newDomain is different from the current domain

        String permission = domainAccessLists.get(domainNum).get(newDomain);

        if (permission.equals("allow")) {
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Domain switch allowed to D" + (newDomain));
            domainNum = newDomain; // Update domainNum with newDomain
        } else {
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Domain switch denied.");
        }

        waitMethod(threadNum, domainNum);
    }

    public static void waitMethod(int threadNum, int domainNum) {
        Random random = new Random();
        int randomCycles = random.nextInt(4) + 3;
        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] " + "Yielding for " + randomCycles + " cycles");
        for (int j = 0; j < randomCycles; j++) {
            Thread.yield();
        }
    }

    public static int generateRandomNum(int lowerRange, int upperRange) {
        if (lowerRange > upperRange) {
            throw new IllegalArgumentException("Lower range cannot be greater than the upper range.");
        }
        Random random = new Random();
        return random.nextInt(upperRange - lowerRange + 1) + lowerRange;
    }

    public static void generateAccessLists() {
        Random random = new Random();
        objectAccessLists = new ArrayList<>();
        domainAccessLists = new ArrayList<>();

        for (int i = 0; i < numOfThreads; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < numOfDomains; j++) {
                String[] filePermissions = {"R", "W", "R/W", "N/A"};
                row.add(filePermissions[random.nextInt(filePermissions.length)]);
            }
            objectAccessLists.add(row);
        }

        for (int i = 0; i < numOfDomains; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < numOfDomains; j++) {
                if (j == i) {
                    row.add(""); // D1 can't allow itself
                } else {
                    row.add("");
                }
            }
            domainAccessLists.add(row);
        }

        // Randomly specify which domains each domain can allow
        for (int i = 0; i < numOfDomains; i++) {
            for (int j = 0; j < numOfDomains; j++) {
                if (i != j && random.nextBoolean()) {
                    domainAccessLists.get(i).set(j, "allow");
                }
            }
        }
    }

    public static void printAccessLists(List<List<String>> objects, List<List<String>> domains) {
        for (int row = 0; row < objects.size(); row++) {
            System.out.print("F" + (row + 1) + " --> ");
            for (int col = 0; col < objects.get(0).size(); col++) {
                String cell = objects.get(row).get(col);
                if (!cell.equals("")) {
                    System.out.print("D" + (col + 1) + ":" + cell);
                    if (col < objects.get(0).size() - 1) {
                        System.out.print(", ");
                    }
                }
            }
            System.out.println();
        }

        for (int row = 0; row < domains.size(); row++) {
            System.out.print("D" + (row + 1) + " --> ");
            for (int col = 0; col < domains.get(0).size(); col++) {
                String cell = domains.get(row).get(col);
                if (!cell.equals("")) {
                    System.out.print("D" + (col + 1) + ":" + cell);
                    if (col < domains.get(0).size() - 1) {
                        System.out.print(", ");
                    }
                }
            }
            System.out.println();
        }
    }

    public static String generateColorString() {
        Random random = new Random();
        String[] colors = {
                "Red",
                "Green",
                "Blue",
                "Yellow",
                "Orange",
                "Purple",
                "Brown",
                "Pink",
                "Grey",
                "Black"
        };

        // Pick a random color
        int colorToWrite = random.nextInt(colors.length);
        return colors[colorToWrite];
    }
}
