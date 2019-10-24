import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

public class Shell {
    private static ArrayList<String> command_history = new ArrayList<>();
    private static double ptime = 0;

    public static void main(String[] args) throws IOException {
        while(true) {
            String current_dir = System.getProperty("user.dir");
            Scanner input = new Scanner(System.in);
            System.out.print(current_dir + ": ");
            String command = input.nextLine();
            run_shell(command,current_dir);
        }
    }

    public static void run_shell(String command, String current_dir) throws IOException {
        String[] commands = splitCommand(command);
        command_history.add(command);
        switch (commands[0]){
            case "cd":
                System.setProperty("user.dir", changedir(commands,new File(current_dir)));
                break;
            case "list":
                list(commands);
                break;
            case "history":
                for (String c:command_history) {
                    System.out.println(c);
                }
                break;

            case "pwd":
                System.out.println(System.getProperty("user.dir"));
                break;

            case "^":
                if (commands.length == 1){
                    System.out.println("Invalid. Please supply a command number.");
                    break;
                }
                if (commands.length > 2){
                    System.out.println("Invalid.  Too many parameters supplied.");
                    break;
                }

                specific_hist(Integer.parseInt(commands[1]) - 1);
                break;

            case "exit":
                System.exit(1);
                break;

            case "ptime":
                System.out.printf("Total time in child processes: %6.3f \n", ptime);
                break;

            default:
                external_commands(command);
        }
    }

    public static String changedir(String[] commands, File curr) {
        if(commands.length == 1){
            return System.getProperty("user.home");
        }

        String args = commands[1];
        if(args.equals("..")){
            //go back a dir
            return curr.getParent();
        }

        File changefile = new File(curr.toString() + "/" + args); //test the file in question

        if(changefile.isDirectory()) //check if file is valid directory
            return changefile.getAbsolutePath();

        System.out.println(changefile.getName() + " is not a valid directory"); //print this message if not directory
        return System.getProperty("user.dir");
    }

    public static void list(String[] commands){
        if(commands.length > 1){
            System.out.println("list only takes one parameter");
            return;
        }
        File current = new File (System.getProperty("user.dir"));
        File[] children = current.listFiles();
        StringBuilder special = new StringBuilder();
        long size;

        for (File i: children) {
            if(i.isDirectory())
                special.append('d');
            else
                special.append('-');

            if(i.canRead())
                special.append('r');
            else
                special.append('-');

            if(i.canWrite())
                special.append('w');
            else
                special.append('-');

            if(i.canExecute())
                special.append('x');
            else
                special.append('-');

            size = i.length();
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date date = new Date();

            System.out.printf("%s %10d %s %s \n",special.toString(),size,formatter.format(date),i.getName());
            special = new StringBuilder();

        }

    }



    public static void specific_hist(int number) throws IOException {
        if(command_history.size() < number){ //check if command exists
            System.out.println("The desired command does not exist in the history");
            return;
        }
        run_shell(command_history.get(number),System.getProperty("user.dir")); //run the command back thru
    }

    public static void external_commands(String command) throws IOException {
        //process builder to use external command
        //first we check for piping
        double begin;
        try {
            if (command.contains("|")){
                String[] piping_commands = command.split("\\|");
                String[] command1 = splitCommand(piping_commands[0]);
                String[] command2 = splitCommand(piping_commands[1]);

                ProcessBuilder pb1 = new ProcessBuilder(command1);
                ProcessBuilder pb2 = new ProcessBuilder(command2);

                pb1.directory(new File(System.getProperty("user.dir")));
                pb1.redirectInput(ProcessBuilder.Redirect.INHERIT);

                pb2.directory(new File(System.getProperty("user.dir")));
                pb2.redirectOutput(ProcessBuilder.Redirect.INHERIT);

                begin = System.currentTimeMillis();
                Process left = pb1.start();
                Process right = pb2.start();

                java.io.InputStream in = left.getInputStream();
                java.io.OutputStream out = right.getOutputStream();

                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
                out.flush();
                out.close();
                left.waitFor();
                right.waitFor();
                ptime += (System.currentTimeMillis() - begin)/1000;
            }
            else{ //if not piping, we check for external commands.
                String[] commands = splitCommand(command);
                ProcessBuilder process = new ProcessBuilder(commands);
                process.directory(new File(System.getProperty("user.dir")));
                process.inheritIO();
                if (command.contains("&")){
                    begin = System.currentTimeMillis();
                    process.start(); //dont wait if &
                    ptime += (System.currentTimeMillis() - begin)/1000;
                }
                else{
                    begin = System.currentTimeMillis();
                    process.start().waitFor();
                    ptime += (System.currentTimeMillis() - begin)/1000;
                }
            }
        }
        catch (Exception e){
            System.out.println("Invalid command entered");
        }

    }

    public static String[] splitCommand(String command) {
        java.util.List<String> matchList = new java.util.ArrayList<>();

        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }

        return matchList.toArray(new String[matchList.size()]);
    }

}
