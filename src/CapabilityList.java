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
                    capabilityList.put("F" + (j + 1), permissions);
                }
            }
            for (int k = 0; k < numOfDomains; k++) {
                if (i != k) {
                    switchList.add("D" + (k + 1) + ":allow");
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
                System.out.println("[Domain " + (domainNum + 1) + "] Requesting access to " + object);

                accessMatrixLock.get(domainNum).get(objectNum).acquireUninterruptibly();
                if (capabilityLists.get(domainNum).containsKey(object)) {
                    String permissions = capabilityLists.get(domainNum).get(object);
                    int action = random.nextInt(3); // 0 - Read, 1 - Write, 2 - Read/Write
                    if ((action == 0 && permissions.contains("R")) ||
                            (action == 1 && permissions.contains("W")) ||
                            (action == 2 && permissions.contains("R/W"))) {
                        System.out.println("[Domain " + (domainNum + 1) + "] Access granted: " + object + " (" +
                                (action == 0 ? "Read" : (action == 1 ? "Write" : "Read/Write")) + ")");
                        // Display domains that can be switched to
                        List<String> switchList = switchMatrix.get(domainNum);
                        System.out.print("[Domain " + (domainNum + 1) + "] Can switch to: ");
                        for (String switchDomain : switchList) {
                            System.out.print(switchDomain + ", ");
                        }
                        System.out.println();
                    } else {
                        System.out.println("[Domain " + (domainNum + 1) + "] Access denied: " + object + " (" +
                                (action == 0 ? "Read" : (action == 1 ? "Write" : "Read/Write")) + ")");
                    }
                } else {
                    System.out.println("[Domain " + (domainNum + 1) + "] Access denied: " + object);
                }
                accessMatrixLock.get(domainNum).get(objectNum).release();
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
