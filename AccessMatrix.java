import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class AccessMatrix {
    static int numOfDomains;
    static int numOfThreads;
    static String accessMatrix[][];
    static Semaphore accessMatrixLock[][];

    public static void run() {
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

        accessMatrixLock = new Semaphore[numOfDomains][numOfThreads + numOfDomains];

        generateMatrix();
        printMatrix(accessMatrix);

        // Create and start a thread for each file object
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
            this.threadNum = domainNum;
        }

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
                    int newDomain;
                    do {
                        newDomain = generateRandomNum(1, numOfDomains);
                    } while (newDomain == domainNum);

                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Attempting to switch from D" + domainNum + " to D" + newDomain);
                    arbitratorDomainSwitch(threadNum, newDomain, domainNum);
                }
            }
        }

        public static boolean arbitratorRead(int threadNum, int columnNum, int domainNum) {
            accessMatrixLock[domainNum][columnNum].acquireUninterruptibly();
            if (threadNum < accessMatrix.length && columnNum < accessMatrix[0].length) {
                String permission = accessMatrix[threadNum][columnNum];
                if (permission.equals("R/W") || (permission.equals("R") && threadNum == domainNum)) {
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " Permission allowed.");
                    waitMethod(threadNum, domainNum);
                    accessMatrixLock[domainNum][columnNum].release();
                    return true;
                }
            }
            waitMethod(threadNum, domainNum);
            accessMatrixLock[domainNum][columnNum].release();
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] " + " Permission denied.");
            return false;
        }

        public static boolean arbitratorWrite(int threadNum, int columnNum, int domainNum) {
            accessMatrixLock[domainNum][columnNum].acquireUninterruptibly();
            if (threadNum < accessMatrix.length && columnNum < accessMatrix[0].length) {
                String permission = accessMatrix[threadNum][columnNum];
                if (permission.equals("R/W") || (permission.equals("W") && threadNum == domainNum)) {
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " Permission allowed.");
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " Writes: " + generateColorString());
                    waitMethod(threadNum, domainNum);
                    accessMatrixLock[domainNum][columnNum].release();
                    return true;
                }
            }
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " Permission denied.");
            waitMethod(threadNum, domainNum);
            accessMatrixLock[domainNum][columnNum].release();
            return false;
        }

        public void arbitratorDomainSwitch(int threadNum, int newDomain, int oldDomain) {
            accessMatrixLock[oldDomain][newDomain + numOfThreads].acquireUninterruptibly();
            if (accessMatrix[oldDomain][newDomain + numOfThreads].equals("allow")) {
                System.out.println("[Thread:" + threadNum + " (D" + oldDomain + ")]" + " Attempting to switch from D" + oldDomain + " to D" + newDomain);
                this.domainNum = newDomain;
                accessMatrixLock[oldDomain][newDomain + numOfThreads].release();
                System.out.println("[Thread:" + threadNum + " (D" + newDomain + ")] Switched to D" + newDomain);
            } else {
                System.out.println("[Thread:" + threadNum + " (D" + oldDomain + ")]" + " Operation failed. Permission denied.");
                accessMatrixLock[oldDomain][newDomain + numOfThreads].release();
            }
            waitMethod(threadNum, domainNum);
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
            int colorToWrite = random.nextInt(colors.length);
            return colors[colorToWrite];
        }
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
        accessMatrix = new String[numOfDomains + 1][numOfThreads + numOfDomains + 1];
        accessMatrix[0][0] = "Domain/Object";

        for (int i = 1; i <= numOfThreads + numOfDomains; i++) {
            if (i <= numOfThreads) {
                accessMatrix[0][i] = "F" + i;
            } else {
                accessMatrix[0][i] = "D" + (i - numOfThreads);
            }
        }

        for (int i = 1; i <= numOfDomains; i++) {
            accessMatrix[i][0] = "D" + i;
        }

        for (int k = 1; k <= numOfDomains; k++) {
            for (int j = 1; j <= numOfThreads + numOfDomains; j++) {
                if (j <= numOfThreads) {
                    String[] filePermissions = {"R", "W", "R/W"};
                    accessMatrix[k][j] = filePermissions[random.nextInt(filePermissions.length)];
                } else {
                    String[] domainSwitching = {"allow", ""};
                    accessMatrix[k][j] = domainSwitching[random.nextInt(domainSwitching.length)];
                }
            }
            accessMatrix[k][k + numOfThreads] = "N/A";
        }

        accessMatrixLock = new Semaphore[numOfDomains + 1][numOfThreads + numOfDomains + 1];
        for (int i = 0; i < accessMatrixLock.length; i++) {
            for (int j = 0; j < accessMatrixLock[i].length; j++) {
                accessMatrixLock[i][j] = new Semaphore(1, true);
            }
        }
    }

    public static void printMatrix(String[][] table) {
        int[] columnWidths = new int[table[0].length];
        for (int col = 0; col < table[0].length; col++) {
            int maxWidth = 0;
            for (int row = 0; row < table.length; row++) {
                int cellWidth = table[row][col] != null ? table[row][col].length() : 0;
                if (cellWidth > maxWidth) {
                    maxWidth = cellWidth;
                }
            }
            columnWidths[col] = maxWidth;
        }

        for (int row = 0; row < table.length; row++) {
            for (int col = 0; col < table[row].length; col++) {
                String cell = table[row][col] != null ? table[row][col] : "";
                System.out.print(cell);
                for (int padding = 0; padding < columnWidths[col] - cell.length() + 2; padding++) {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }
}
