import java.util.Random;
import java.util.Scanner;

public class Main {
    static int numOfDomains;
    static int numOfThreads;
    static String accessMatrix[][];

    public static int generateRandomNum(int lowerRange, int upperRange) {
        if (lowerRange > upperRange) {
            throw new IllegalArgumentException("Lower range cannot be greater than the upper range.");
        }
        Random random = new Random();
        return random.nextInt(upperRange - lowerRange + 1) + lowerRange;
    }

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        System.out.printf("Domain count: (enter a number between 3-7)");
        numOfDomains = input.nextInt();

        System.out.println("Object count: (enter a number between 3-7)");
        numOfThreads = input.nextInt();

        // create access matrix
        accessMatrix = new String[numOfDomains][numOfThreads + numOfDomains];

        // attempt to randomly populate access matrix
        for (int i =0; i < numOfDomains; i++) {
            for (int j=0; j < numOfDomains + numOfThreads; j++) {
                // the file permissions of each domain: R, W, R/W
                if (j < numOfThreads) {
                    accessMatrix[i][j] = null;
                } else if (j > numOfThreads){
                    // the property of being to switch domains: allow, N/A
                    accessMatrix[i][j] = null;
                }
            }
        }

        // Create and start a thread for each file object
        for (int i = 0; i < numOfDomains; i++) {
            myThread t = new myThread();
            t.start();
        }


    }

    public static class myThread extends Thread {
        int id;

        @Override
        public void run() {
            // make 5 requests for each thread
            for (int i=0; i < 5; i++) {
                // generate a random number X to correspond to a column in the access matrix.
                int num = generateRandomNum(0, numOfThreads + numOfDomains);

                // if X < M
                if (num < numOfThreads) {
                    // generate another number [0,1]
                    int secondNum = generateRandomNum(0,1);
                    if (secondNum == 1) {

                    } else if (secondNum == 0) {

                    }

                }
                // if X >= M, attempt to switch to domain X-M
                else {

                }

            }
        }

        public void arbitrator() {

        }
    }
    public void generateMatrix(){
        for (int i=0; i < numOfThreads; i++) {
            System.out.println("F" + i + " -->");

        }
        for (int i=0; i < numOfDomains; i++) {
            System.out.println("D" + i + " -->");

        }

    }

}
