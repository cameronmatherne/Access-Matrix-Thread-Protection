
public class Main {
    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("-S")) {
            System.err.println("Invalid task number. Please use 1, 2, or 3.");
            System.exit(1);
        }

        int taskNumber = 0;
        try {
            taskNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid task number. Please provide a valid integer.");
            System.exit(1);
        }

        String taskDescription = null;

        switch (taskNumber) {
            case 1:
                taskDescription = "Task 1: Access Matrix";
                AccessMatrix.run();
                break;

        case 2:
                taskDescription = "Task 2: Access List for Objects";
                objectList.run();
                break;

            case 3:
                taskDescription = "Task 3: Capability List for Domains";
                CapabilityList.run();
                break;

            default:
                System.err.println("Invalid task number. Please use 1, 2, or 3.");
                System.exit(1);

        }

        System.out.println("Running: " + taskDescription);
    }
}
