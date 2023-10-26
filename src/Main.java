import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Main {
    static int numOfDomains;
    static int numOfThreads;
    static String accessMatrix[][];
    static Semaphore accessMatrixLock[][];
    public static void main(String[] args) {

        Random random = new Random();
        Scanner input = new Scanner(System.in);

        System.out.printf("Domain count: (enter a number between 3-7)");
        numOfDomains = input.nextInt();

        System.out.println("Object count: (enter a number between 3-7)");
        numOfThreads = input.nextInt();

        accessMatrixLock = new Semaphore[numOfDomains+1][numOfThreads+numOfDomains+1];

        generateMatrix();
        print2DArrayAsTable(accessMatrix);

        // Create and start a thread for each file object
        for (int i = 0; i < numOfDomains; i++) {
            myThread t = new myThread(i);
            t.start();
        }

    }

    public static class myThread extends Thread {
        int tID;
        int domainNum;
        public myThread(int id) {
            this.tID = id;
            this.domainNum = id;
        }

        @Override
        public void run() {
            // make 5 requests for each thread
            for (int i = 0; i < 5; i++) {
                // generate a random number X to correspond to a column in the access matrix.
                int num = generateRandomNum(0, numOfThreads + numOfDomains);

                // if X (num) < M (numOfThreads)
                if (num < numOfThreads) {
                    // generate another number [0,1]
                    int secondNum = generateRandomNum(0, 1);
                    if (secondNum == 1) {
                        System.out.println("[Thread: " + tID + "(" + domainNum + ")]" + " attempting to read resource:");
                        arbitratorRead(tID, num, domainNum);

                    } else if (secondNum == 0) {
                        System.out.println("[Thread: " + tID + "(" + domainNum + ")]" + " attempting to write resource:");
                        arbitratorWrite(tID, num, domainNum);
                    }
                }
                // if X >= M, attempt to switch to domain X-M
                else if (num >= numOfThreads){
                    // THREAD ATTEMPTING TO SWITCH DO A DIFFERENT DOMAIN
                    System.out.println("[Thread: " + tID + "(D" + domainNum + ")] Attempting to switch from D" + domainNum + " to D" + (num-numOfThreads-1));
                    arbitratorDomainSwitch(num-numOfThreads-1);
                }

            }
        }

        // ARBITRATOR FUNCTION IN THE CASE OF A READ OR WRITE
    public static boolean arbitratorRead(int domain, int threadNum, int domainNum) {
        if (domain < accessMatrix.length && threadNum < accessMatrix[0].length) {
            // Check the access matrix for permissions 
            String permission = accessMatrix[domain][threadNum];
    
            // Check if the domain has permission to read
            if (permission.equals("R/W") || (permission.equals("R") && domain == domainNum)) {
                System.out.println("Permission allowed.");
                return true; // Read/Write permission is allowed
            }
        }
        System.out.println("Permission denied.");
        return false; // Permission denied
    }

    public static boolean arbitratorWrite(int domain, int threadNum, int domainNum) {
        
        if (domain < accessMatrix.length && threadNum < accessMatrix[0].length) {
            // Check the access matrix for permissions 
            String permission = accessMatrix[domain][threadNum];
    
            // Check if the domain has permission to write
            if (permission.equals("R/W") || (permission.equals("W") && domain == domainNum)) {
                System.out.println("Permission allowed.");
                return true; // Read/Write permission is allowed
            }
        }
        System.out.println("Permission denied.");
        return false; // Permission denied
    }

        // ARBITRATOR FUNCTION IN THE CASE OF A DOMAIN SWITCH
        public void arbitratorDomainSwitch(int domain) {

            // acquire semaphore for access matrix
            accessMatrixLock[domainNum][domain-numOfThreads].acquireUninterruptibly();

            // check permissions
            if (accessMatrix[domainNum][domain-numOfThreads] == "allow") {
                System.out.println("[Thread: " + tID + "(" + domainNum + ")]" + " Attempting to switch from D" + domainNum + " to D" + domain);

                // release access matrix semaphore
                accessMatrixLock[domainNum][domain-numOfThreads].release();

                domainNum = domain;
                System.out.println("Switched to D" + domain);


            } else  {
                System.out.println("[Thread: " + tID + "(" + domainNum + ")]" + " Operation failed. Permission denied.");

                // release access matrix semaphore
                accessMatrixLock[domainNum][domain-numOfThreads].release();

            }

            // yield for 3-7 clock cycles
            waitMethod();
        }

        // method might not need to be used
        public String generateColorString() {
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

    public static void waitMethod() {
        Random random = new Random();
        int randomCycles = random.nextInt(4) + 3;
        System.out.println("Yielding for " + randomCycles + " cycles");
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
        // with this matrix design, values start at the first column and first row. (not zero)
        // the [0] index in both dimensions is reserved for labels used when printing matrix.
        // so accessMatrix[0][n] and accessMatrix[n][0] will not be actual values !!

        Random random = new Random();
        // count for labeling domains
        int count = 0;
        // create access matrix, used for permission checking
        accessMatrix = new String[numOfDomains+1][numOfThreads+numOfDomains+1];

        // label corner cell
        accessMatrix[0][0] = "Domain/Object";
        // label the first row
        for (int i = 1; i < numOfDomains + numOfThreads+1; i++) {
            if (i <= numOfThreads) {
                accessMatrix[0][i] = "F" + i;
            } else if (i > numOfThreads) {
                count++;
                accessMatrix[0][i] = "D" + count;
            }
        }

        // label the first column
        for (int i = 1; i < numOfDomains+1; i++) {
            accessMatrix[i][0] = "           D" + i;
        }

        // Populate the accessMatrix with random values
        for (int k = 1; k < numOfDomains+1; k++) {
            for (int j = 1; j < numOfThreads + numOfDomains+1; j++) {
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
            // overwrite the domains that are eachother and replace anything there with N/A
            for (int i=1; i < numOfDomains+1; i++) {
                accessMatrix[i][i+numOfThreads] = "N/A";
            }
        }

        // initialize all the mutex semaphores for matrix access
        for (int i=1;i<numOfDomains+1;i++) {
            for (int j=1;j<numOfThreads+numOfDomains+1; j++) {
                accessMatrixLock[i][j] = new Semaphore(1, true);
            }
        }
    }

    public static void print2DArrayAsTable(String[][] table) {
        // Determine the width of each column
        int[] columnWidths = new int[table[0].length];
        for (int col = 0; col < table[0].length; col++) {
            int maxWidth = 0;
            for (int row = 0; row < table.length; row++) {
                int cellWidth = table[row][col].length();
                if (cellWidth > maxWidth) {
                    maxWidth = cellWidth;
                }
            }
            columnWidths[col] = maxWidth;
        }

        // Print the table
        for (int row = 0; row < table.length; row++) {
            for (int col = 0; col < table[row].length; col++) {
                String cell = table[row][col];
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


