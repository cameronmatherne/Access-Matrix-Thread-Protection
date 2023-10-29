import java.util.*;
import java.util.concurrent.Semaphore;

public class objectList {
    static int numOfDomains;
    static int numOfThreads;
    static List<List<String>> accessMatrix;
    static List<List<Semaphore>> accessMatrixLock;

    public static void main(String[] args) {
        Random random = new Random();
        Scanner input = new Scanner(System.in);

        do {
            System.out.print("Access control scheme: Access List\nDomain Count: (enter a number between 3-7)");
            numOfDomains = input.nextInt();
        } while (numOfDomains < 3 || numOfDomains > 7);

        do {
            System.out.print("Object Count: (enter a number between 3-7)");
            numOfThreads = input.nextInt();
        } while (numOfThreads < 3 || numOfThreads > 7);

        input.close();

        accessMatrixLock = new ArrayList<>();
        for (int i = 0; i < numOfDomains; i++) {
            List<Semaphore> row = new ArrayList<>();
            for (int j = 0; j < numOfThreads + numOfDomains; j++) {
                row.add(new Semaphore(1, true));
            }
            accessMatrixLock.add(row);
        }

        generateMatrix();
        printList(accessMatrix);

        // Create and start a thread for each file object
        for (int i = 0; i < numOfDomains; i++) {
            myThread t = new myThread(i);
            t.start();
        }
    }

    public static class myThread extends Thread {
        int threadNum;
        int domainNum;

        public myThread(int domainNum) {
            this.domainNum = domainNum;
            this.threadNum = domainNum + 1;
        }

        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                int columnNum = generateRandomNum(0, numOfThreads + numOfDomains - 1);

                if (columnNum < numOfThreads) {
                    int secondNum = generateRandomNum(0, 1);
                    if (secondNum == 1) {
                        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " attempting to read resource: F" + (columnNum + 1));
                        arbitratorRead(threadNum, columnNum, domainNum);
                    } else if (secondNum == 0) {
                        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " attempting to write resource: F" + (columnNum + 1));
                        arbitratorWrite(threadNum, columnNum, domainNum);
                    }
                } else {
                    int newDomain = columnNum - numOfThreads;
                    while (newDomain == domainNum) {
                        int newNum = generateRandomNum(0, numOfThreads + numOfDomains - 1);
                        newDomain = newNum - numOfThreads;
                    }

                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Attempting to switch from D" + (domainNum + 1) + " to D" + (newDomain + 1));
                    arbitratorDomainSwitch(threadNum, newDomain, domainNum, newDomain);
                }
            }
        }
    }

    public static boolean arbitratorRead(int threadNum, int columnNum, int domainNum) {
        accessMatrixLock.get(domainNum).get(columnNum).acquireUninterruptibly();
        if (columnNum < accessMatrix.size() && threadNum - 1 < accessMatrix.get(0).size()) {
            String permission = accessMatrix.get(columnNum).get(threadNum - 1);
            if (permission.contains("R") || (permission.contains("W") && permission.contains("R") && permission.contains("W"))) {
                System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission allowed.");
                waitMethod(threadNum, domainNum);
                accessMatrixLock.get(domainNum).get(columnNum).release();
                return true;
            }
        }
        waitMethod(threadNum, domainNum);
        accessMatrixLock.get(domainNum).get(columnNum).release();
        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission denied.");
        return false;
    }

    public static boolean arbitratorWrite(int threadNum, int columnNum, int domainNum) {
        accessMatrixLock.get(domainNum).get(columnNum).acquireUninterruptibly();
        if (columnNum < accessMatrix.size() && threadNum - 1 < accessMatrix.get(0).size()) {
            String permission = accessMatrix.get(columnNum).get(threadNum - 1);
            if (permission.contains("W") || (permission.contains("W") && permission.contains("R") && permission.contains("R"))) {
                System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission allowed.");
                System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Writes: " + generateColorString());
                waitMethod(threadNum, domainNum);
                accessMatrixLock.get(domainNum).get(columnNum).release();
                return true;
            }
        }
        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Permission denied.");
        waitMethod(threadNum, domainNum);
        accessMatrixLock.get(domainNum).get(columnNum).release();
        return false;
    }

    public static void arbitratorDomainSwitch(int threadNum, int newDomain, int oldDomain, int domainNum) {
        if (newDomain >= 0 && newDomain < accessMatrix.size() && oldDomain >= 0 && oldDomain < accessMatrix.size()) {
            accessMatrixLock.get(oldDomain).get(newDomain + numOfThreads).acquireUninterruptibly();
            if (accessMatrix.get(newDomain + numOfThreads).get(oldDomain).equals("allow")) {
                System.out.println("[Thread:" + threadNum + " (D" + (oldDomain + 1) + ")]" + " Attempting to switch from D" + (oldDomain + 1) + " to D" + (newDomain + 1));
                domainNum = newDomain;
                accessMatrixLock.get(oldDomain).get(newDomain + numOfThreads).release();
                System.out.println("[Thread:" + threadNum + " (D" + (newDomain + 1) + ")]" + " Switched to D" + (newDomain + 1));
            } else {
                System.out.println("[Thread:" + threadNum + " (D" + (oldDomain + 1) + ")]" + " Operation failed. Permission denied");
                accessMatrixLock.get(oldDomain).get(newDomain + numOfThreads).release();
            }
            waitMethod(threadNum, threadNum);
        } else {
            System.out.println("[Thread:" + threadNum + "] Invalid domain numbers.");
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

    public static void generateMatrix() {
        Random random = new Random();
        accessMatrix = new ArrayList<>();
        for (int i = 0; i < numOfDomains; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < numOfThreads; j++) {
                String[] filePermissions = {"R", "W", "R/W", "N/A"};
                row.add(filePermissions[random.nextInt(filePermissions.length)]);
            }
            accessMatrix.add(row);
        }

        for (int k = 0; k < numOfDomains; k++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < numOfDomains; j++) {
                if (j == k) {
                    row.add("allow");
                } else {
                    row.add("");
                }
            }
            accessMatrix.add(row);
        }
    }

    public static void printList(List<List<String>> table) {
        for (int row = 0; row < table.size(); row++) {
            if (row < numOfThreads) {
                System.out.print("F" + (row + 1) + " --> ");
            } else {
                System.out.print("D" + (row - numOfThreads + 1) + " --> ");
            }
            for (int col = 0; col < table.get(0).size(); col++) {
                String cell = table.get(row).get(col);
                if (!cell.equals("")) {
                    if (row < numOfThreads) {
                        System.out.print("D" + (col + 1) + ":" + cell);
                    } else {
                        if (!cell.equals("allow")) {
                            System.out.print("D" + (col - numOfThreads + 1) + ":" + cell);
                        } else {
                            System.out.print("allow");
                        }
                    }
                    if (col < table.get(0).size() - 1) {
                        System.out.print(", ");
                    }
                }
            }
            System.out.println();
        }
    }
}
