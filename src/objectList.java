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
            System.out.printf("Domain count: (enter a number between 3-7)");
            numOfDomains = input.nextInt();
        } while (numOfDomains < 3 || numOfDomains > 7);

        do {
            System.out.println("Object count: (enter a number between 3-7)");
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
            this.threadNum = domainNum;
        }

        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                int columnNum = generateRandomNum(0, numOfThreads + numOfDomains - 1);

                if (columnNum < numOfThreads) {
                    int secondNum = generateRandomNum(0, 1);
                    if (secondNum == 1) {
                        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " attempting to read resource: F" + columnNum);
                        arbitratorRead(threadNum, columnNum, domainNum);
                    } else if (secondNum == 0) {
                        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " attempting to write resource: F" + columnNum);
                        arbitratorWrite(threadNum, columnNum, domainNum);
                    }
                } else {
                    int newDomain = columnNum - numOfThreads;
                    while (newDomain == domainNum) {
                        int newNum = generateRandomNum(0, numOfThreads + numOfDomains - 1);
                        newDomain = newNum - numOfThreads;
                    }

                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Attempting to switch from D" + domainNum + " to D" + newDomain);
                    arbitratorDomainSwitch(threadNum, newDomain, domainNum);
                }
            }
        }
        public static boolean arbitratorRead(int threadNum, int columnNum, int domainNum) {
        accessMatrixLock.get(domainNum).get(columnNum).acquireUninterruptibly();
        if (threadNum < accessMatrix.size() && columnNum < accessMatrix.get(0).size()) {
            String permission = accessMatrix.get(threadNum).get(columnNum);
            if (permission.equals("R/W") || (permission.equals("R") && threadNum == domainNum)) {
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
        if (threadNum < accessMatrix.size() && columnNum < accessMatrix.get(0).size()) {
            String permission = accessMatrix.get(threadNum).get(columnNum);
            if (permission.equals("R/W") || (permission.equals("W") && threadNum == domainNum)) {
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

    public void arbitratorDomainSwitch(int threadNum, int newDomain, int oldDomain) {
        if (newDomain >= 0 && newDomain < accessMatrix.size() && oldDomain >= 0 && oldDomain < accessMatrix.size()) {
            accessMatrixLock.get(oldDomain).get(newDomain + numOfThreads).acquireUninterruptibly();
            if (accessMatrix.get(oldDomain).get(newDomain + numOfThreads).equals("allow")) {
                System.out.println("[Thread:" + threadNum + " (D" + oldDomain + ")]" + " Attempting to switch from D" + oldDomain + " to D" + newDomain);
                this.domainNum = newDomain;
                accessMatrixLock.get(oldDomain).get(newDomain + numOfThreads).release();
                System.out.println("[Thread:" + threadNum + " (D" + newDomain + ")]" + " Switched to D" + newDomain);
            } else {
                System.out.println("[Thread:" + threadNum + " (D" + oldDomain + ")]" + " Operation failed. Permission denied");
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

            // pick random color
            int colorToWrite = random.nextInt(colors.length);
            String randomColor = colors[colorToWrite];
            return randomColor;
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
    }

    public static void generateMatrix() {
        Random random = new Random();
        int count = 0;
        accessMatrix = new ArrayList<>();
        for (int i = 0; i < numOfDomains + 1; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < numOfThreads + numOfDomains + 1; j++) {
                if (j <= numOfThreads) {
                    row.add("F" + j);
                } else if (j > numOfThreads) {
                    count++;
                    row.add("D" + count);
                }
            }
            accessMatrix.add(row);
        }

        for (int k = 2; k < numOfDomains + 1; k++) {
            for (int j = 2; j < numOfThreads + numOfDomains + 1; j++) {
                if (j < numOfThreads) {
                    String[] filePermissions = {"R", "W", "R/W"};
                    accessMatrix.get(k).add(filePermissions[random.nextInt(filePermissions.length)]);
                } else {
                    String[] domainSwitching = {"allow", ""};
                    accessMatrix.get(k).add(domainSwitching[random.nextInt(domainSwitching.length)]);
                }
            }
            for (int i = 1; i < numOfDomains; i++) {
                accessMatrix.get(i).set(i + numOfThreads, "N/A");
            }
        }
    }

    public static void printList(List<List<String>> table) {
        for (int row = 1; row < table.size(); row++) {
            List<String> rowList = table.get(row);
            System.out.print(rowList.get(0) + " --> ");
            for (int col = 1; col < rowList.size(); col++) {
                String cell = rowList.get(col);
                if (!cell.equals("")) {
                    System.out.print(rowList.get(col));
                    if (col < rowList.size() - 1) {
                        System.out.print(", ");
                    }
                }
            }
            System.out.println();
        }
    }
}