import java.util.*;
import java.util.concurrent.Semaphore;

public class CapabilityList {
    static int numOfDomains;
    static int numOfObjects;
    static List<Map<String, String>> capabilityLists;
    static List<List<Semaphore>> accessMatrixLock;
    static List<List<String>> switchMatrix;

    public static void main(String[] args) {
        Random random = new Random();
        Scanner input = new Scanner(System.in);

        do {
            System.out.print("Domain Count: ");
            numOfDomains = input.nextInt();
        } while (numOfDomains < 1);  // Ensure there's at least one domain

        do {
            System.out.print("Object Count: ");
            numOfObjects = input.nextInt();
        } while (numOfObjects < 1);  // Ensure there's at least one object

        input.close();

        capabilityLists = new ArrayList<>();
        switchMatrix = new ArrayList<>();
        for (int i = 0; i < numOfDomains; i++) {
            Map<String, String> capabilityList = new HashMap<>();
            List<String> switchList = new ArrayList<>();
            for (int j = 0; j < numOfObjects; j++) {
                String permissions = generateRandomPermissions(random);
                if (!permissions.isEmpty()) {
                    capabilityList.put("F" + (j + 1), permissions); //adds permissions to each File
                }
            }
            for (int k = 0; k < numOfDomains; k++) {
                if (i != k) {
                    // Generate a random boolean (true or false)
                    boolean includeAllow = new Random().nextBoolean();
                    
                    // Add "allow" to the switchList with a 50/50 chance
                    if (includeAllow) {
                        switchList.add("D" + (k + 1) + ":allow");
                    }
                }
            }
            
            capabilityLists.add(capabilityList);
            switchMatrix.add(switchList);
        }

        printCapabilityLists();

        accessMatrixLock = new ArrayList<>();
        for (int i = 0; i < numOfDomains; i++) {
            List<Semaphore> row = new ArrayList<>();
            for (int j = 0; j < numOfDomains; j++) {
                row.add(new Semaphore(1, true));
            }
            accessMatrixLock.add(row);
        }
        System.out.println("");

        // Create and start a thread for each domain
        for (int i = 0; i < numOfDomains; i++) {
            myThread t = new myThread(i);
            t.start();
        }
    }

    public static class myThread extends Thread {
        int domainNum;

        public myThread(int domainNum) {
            this.domainNum = domainNum;
        }

        @Override
        public void run() {
            Random random = new Random();
            for (int i = 0; i < 5; i++) {
                int objectNum = generateRandomNum(0, numOfObjects - 1);
                String object = "F" + (objectNum + 1);
                int action = random.nextInt(2); // 0 - Read, 1 - Write
                String actionText = (action == 0) ? "Read" : "Write";
                //prints to user which file a domain is trying to access
                System.out.println("[Domain " + (domainNum + 1) + "] Requesting " + actionText + " access to " + object);
        
                accessMatrixLock.get(domainNum).get(objectNum).acquireUninterruptibly();
                //checks if the domain has the correct permissions to access read or write to a file
                if (capabilityLists.get(domainNum).containsKey(object)) {
                    String permissions = capabilityLists.get(domainNum).get(object);
                    //default case is denying access
                    boolean accessGranted = false;
                    if (action == 0 && permissions.contains("R")) {
                        accessGranted = true;
                    } else if (action == 1 && permissions.contains("W")) {
                        accessGranted = true;
                    } else if (action == 0 && permissions.contains("R/W")) {
                        accessGranted = true;
                    }
        
                    if (accessGranted) {
                        System.out.println("[Domain " + (domainNum + 1) + "] Access granted");
        
                        if (action == 1) {
                            // prints a random color after permission is allowed to write to a file
                            String color = generateColorString();
                            System.out.println("[Domain " + (domainNum + 1) + "] Writing '" + color + "' to resource " + object);
                        }

                        waitMethod(domainNum+1); // Wait between permissions
        
                        List<String> switchList = switchMatrix.get(domainNum);
        
                       
                        if (!switchList.isEmpty()) {
                            // randomize which domain it is trying to switch to
                            int randomSwitchIndex = random.nextInt(switchList.size());
                            String selectedSwitch = switchList.get(randomSwitchIndex);
        
                            //outputs to user which domain it wants to switch to
                            System.out.println("[Domain " + (domainNum + 1) + "] Attempting to switch to: " + selectedSwitch);
                        } else {
                            System.out.println("[Domain " + (domainNum + 1) + "] Access denied.");
                        }
                    } else {
                        System.out.println("[Domain " + (domainNum + 1) + "] Access denied");
                    }
                } else {
                    System.out.println("[Domain " + (domainNum + 1) + "] Access denied");
                }
                //release semaphore after resource has been used
                accessMatrixLock.get(domainNum).get(objectNum).release();
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

            // pick random color
            int colorToWrite = random.nextInt(colors.length);
            String randomColor = colors[colorToWrite];
            return randomColor;
        }

        public static void waitMethod(int threadNum) {
            Random random = new Random();
            int randomCycles = random.nextInt(4) + 3;
            System.out.println("[Domain " + threadNum + "] Yielding for " + randomCycles + " cycles");
            for (int j = 0; j < randomCycles; j++) {
                Thread.yield();
            }
        }
        


    }

    public static String generateRandomPermissions(Random random) {
        int choice = random.nextInt(4); // 0 - Empty, 1 - R, 2 - W, 3 - R/W
        switch (choice) {
            case 0:
                return "";
            case 1:
                return "R";
            case 2:
                return "W";
            case 3:
                return "R/W";
                //default case is no permission
            default:
                return "";
        }
    }

    public static int generateRandomNum(int lowerRange, int upperRange) {
        if (lowerRange > upperRange) {
            throw new IllegalArgumentException("Lower range cannot be greater than the upper range.");
        }
        Random random = new Random();
        return random.nextInt(upperRange - lowerRange + 1) + lowerRange;
    }

    public static void printCapabilityLists() {
        for (int i = 0; i < numOfDomains; i++) {
            System.out.print("D" + (i + 1) + " --> ");
            Map<String, String> capabilityList = capabilityLists.get(i);
            StringBuilder output = new StringBuilder();
            for (Map.Entry<String, String> entry : capabilityList.entrySet()) {
                output.append(entry.getKey()).append(":").append(entry.getValue()).append(", ");
            }
            List<String> switchList = switchMatrix.get(i);
            if (output.length() > 0) {
                output.setLength(output.length() - 2); // Remove the trailing comma and space
            }
            if (!switchList.isEmpty()) {
                output.append(" ");
                for (String switchDomain : switchList) {
                    output.append(switchDomain).append(", ");
                }
                output.setLength(output.length() - 2); // Remove the trailing comma and space
            }
            System.out.println(output.toString());
        }
    }
}
