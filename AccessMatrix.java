//package com.company;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class AccessMatrix {
    static int numOfDomains;
    static int numOfThreads;
    static String accessMatrix[][];
    static Semaphore accessMatrixLock[][];

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


        accessMatrixLock = new Semaphore[numOfDomains][numOfThreads + numOfDomains];

        generateMatrix();
        printMatrix(accessMatrix);

        // Create and start a thread for each file object
        for (int i = 0; i < numOfDomains; i++) {
            myThread t = new myThread(i);
            t.start();
        }

    }

    public static class myThread extends Thread {
        // ID of current thread (does not change)
        int threadNum;

        // domain that the thread is currently in
        int domainNum;

        public myThread(int domainNum) {
            this.domainNum = domainNum;
            this.threadNum = domainNum;
        }

        @Override
        public void run() {
            // make 5 requests for each thread
            for (int i = 0; i < 5; i++) {
                // generate a random number X to correspond to a column in the access matrix.
                int columnNum = generateRandomNum(1, numOfThreads + numOfDomains - 1);

                if (columnNum < numOfThreads) {
                    // generate another number [0,1]
                    int secondNum = generateRandomNum(0, 1);
                    if (secondNum == 1) {
                        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " attempting to read resource: F"+columnNum);
                        arbitratorRead(threadNum, columnNum, domainNum);

                    } else if (secondNum == 0) {
                        System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " attempting to write resource: F"+columnNum);
                        arbitratorWrite(threadNum, columnNum, domainNum);

                    }
                }
                // if X > M, attempt to switch to domain X-M
                else {
                    int newDomain;
                    do {
                        newDomain = generateRandomNum(1, numOfDomains);
                    } while (newDomain == domainNum);
                    

                    // THREAD ATTEMPTING TO SWITCH DO A DIFFERENT DOMAIN
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] Attempting to switch from D" + domainNum + " to D" + newDomain);
                    arbitratorDomainSwitch(threadNum, newDomain, domainNum);
                }

            }
        }

        // ARBITRATOR FUNCTION IN THE CASE OF A READ OR WRITE
        public static boolean arbitratorRead(int threadNum, int columnNum, int domainNum) {

            accessMatrixLock[domainNum][columnNum].acquireUninterruptibly();

            if (threadNum < accessMatrix.length && columnNum < accessMatrix[0].length) {
                // Check the access matrix for permissions
                String permission = accessMatrix[threadNum][columnNum];

                // Check if the domain has permission to read
                if (permission.equals("R/W") || (permission.equals("R") && threadNum == domainNum)) {
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " Permission allowed.");

                    waitMethod(threadNum, domainNum);
                    accessMatrixLock[domainNum][columnNum].release();
                    return true; // Read/Write permission is allowed
                }
            }
            waitMethod(threadNum, domainNum);
            accessMatrixLock[domainNum][columnNum].release();
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")] " + " Permission denied.");
            return false; // Permission denied
        }

        public static boolean arbitratorWrite(int threadNum, int columnNum, int domainNum) {
            accessMatrixLock[domainNum][columnNum].acquireUninterruptibly();
            if (threadNum < accessMatrix.length && columnNum < accessMatrix[0].length) {
                // Check the access matrix for permissions
                String permission = accessMatrix[threadNum][columnNum];

                // Check if the domain has permission to write
                if (permission.equals("R/W") || (permission.equals("W") && threadNum == domainNum)) {
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " Permission allowed.");
                    System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" +  " Writes: " + generateColorString());
                    waitMethod(threadNum, domainNum);
                    accessMatrixLock[domainNum][columnNum].release();
                    return true; // Read/Write permission is allowed
                }
            }
            System.out.println("[Thread:" + threadNum + " (D" + domainNum + ")]" + " Permission denied.");
            waitMethod(threadNum, domainNum);
            accessMatrixLock[domainNum][columnNum].release();
            return false; // Permission denied
        }

        // ARBITRATOR FUNCTION IN THE CASE OF A DOMAIN SWITCH
        public void arbitratorDomainSwitch(int threadNum, int newDomain, int oldDomain) {
            // acquire semaphore for access matrix
            accessMatrixLock[oldDomain][newDomain+numOfThreads].acquireUninterruptibly();

            // check permissions
            if (accessMatrix[oldDomain][newDomain+numOfThreads] == "allow") {
                System.out.println("[Thread:" + threadNum + " (D" + oldDomain + ")]" + " Attempting to switch from D" + oldDomain + " to D" + newDomain);
                this.domainNum = newDomain;
                // release access matrix semaphore
                accessMatrixLock[oldDomain][newDomain+numOfThreads].release();
                System.out.println("[Thread:" + threadNum + " (D" + newDomain + ")] Switched to D" + newDomain);

            } else {
                System.out.println("[Thread:" + threadNum + " (D" + oldDomain + ")]" + " Operation failed. Permission denied.");

                // release access matrix semaphore
                accessMatrixLock[oldDomain][newDomain + numOfThreads].release();

            }
            // yield for 3-7 clock cycles
            waitMethod(threadNum, domainNum);
        }

        // method might not need to be used
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
        // Create access matrix, used for permission checking
        accessMatrix = new String[numOfDomains+1][numOfThreads + numOfDomains+1];

        // Label corner cell
        accessMatrix[0][0] = "Domain/Object";

        // Label the first row
        for (int i = 1; i <= numOfThreads+numOfDomains; i++) {
            if (i <= numOfThreads) {
                accessMatrix[0][i] = "F" + i;
            } else {
                accessMatrix[0][i] = "D" + (i - numOfThreads);
            }
        }

        // Label the first column
        for (int i = 1; i <= numOfDomains; i++) {
            accessMatrix[i][0] = "D" + i;
        }

        // Populate the accessMatrix with random values
        for (int k = 1; k <= numOfDomains; k++) {
            for (int j = 1; j <= numOfThreads+numOfDomains; j++) {
                if (j <= numOfThreads) {
                    // Randomly select R, W, or R/W for file permissions
                    String[] filePermissions = {"R", "W", "R/W"};
                    accessMatrix[k][j] = filePermissions[random.nextInt(filePermissions.length)];
                } else {
                    // Randomly select allow, N/A, or nothing for domain switching
                    String[] domainSwitching = {"allow", ""};
                    accessMatrix[k][j] = domainSwitching[random.nextInt(domainSwitching.length)];
                }
            }
            // Overwrite the domains that are each other and replace anything there with N/A
            accessMatrix[k][k+numOfThreads] = "N/A";
        }

        // Initialize all the mutex semaphores for matrix access
        accessMatrixLock = new Semaphore[numOfDomains+1][numOfThreads + numOfDomains+1];

        for (int i = 0; i < accessMatrixLock.length; i++) {
            for (int j = 0; j < accessMatrixLock[i].length; j++) {
                accessMatrixLock[i][j] = new Semaphore(1, true);
            }
        }
    }

    public static void printMatrix(String[][] table) {
        // Determine the width of each column
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

        // Print the table
        for (int row = 0; row < table.length; row++) {
            for (int col = 0; col < table[row].length; col++) {
                String cell = table[row][col] != null ? table[row][col] : "";
                System.out.print(cell);
                // Add padding to align columns
                for (int padding = 0; padding < columnWidths[col] - cell.length() + 2; padding++) {
                    System.out.print(" ");
                }
            }
            System.out.println(); // Move to the next row
        }
    }
}


