package io.github.benjaminsoelberg.jft;

import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;
import java.net.URL;

//TODO: Test on windows (especially file separator)
public class Main {

    private static void showUsage() {
        System.out.println("usage: java -jar JavaForensicsToolkit.jar [-v] [-e] [-d destination.jar] [-s] [-p] [-f filter]... [-x] <pid>");
        System.out.println();
        System.out.println("options:");
        System.out.println("-v\tverbose agent logging");
        System.out.println("-e\tagent will log to stderr instead of stdout");
        System.out.println("-d\tjar file destination of dumped classes");
        System.out.println("\tRelative paths will be relative with respect to the target process.");
        System.out.println("\tA jar file in temp will be generated if no destination was provided.");
        System.out.println("-s\tignore system class loader (like java.lang.String)");
        System.out.println("-p\tignore platform class loader (like system extensions)");
        System.out.println("-f\tregular expression class name filter");
        System.out.println("\tCan be specified multiple times.");
        System.out.println("-x\texclude classes matching the filter");
        System.out.println("pid\tprocess id of the target java process");
        System.out.println();
        System.out.println("example:");
        System.out.println("java -jar JavaForensicsToolkit.jar -d dump.jar -f 'java\\\\..*' -f 'sun\\\\..*' -f 'jdk\\\\..*' -f 'com\\\\.sun\\\\..*' -x 1337");
    }

    /**
     * Get the absolut file location of the jar embedding this class
     *
     * @return absolut file location of jar
     */
    private static String getJarLocation() {
        URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
        String file = url.getFile();
        if (url.getProtocol().equals("file") && file.startsWith("/") && file.endsWith(".jar")) {
            return file;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        System.out.printf(Utils.getApplicationHeader() + "%n");
        String absolutJarLocation = getJarLocation();
        if (args.length < 1 || absolutJarLocation == null) {
            showUsage();
            System.exit(1);
        }

        // We pre-parse the command line to be sure that it is syntactically correct prior to sending it to agentmain
        Options options = new Options(args);

        String pid = options.getPid();
        System.out.println("Injecting agent into JVM with pid: " + pid);
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            System.out.println("Dumping classes to: " + options.getDestination());
            String[] cmdLine = options.getArgs();
            vm.loadAgent(absolutJarLocation, Utils.encodeArgs(cmdLine));
        } finally {
            try {
                vm.detach();
            } catch (IOException ioe) {
                System.out.println("Unable to detach from process");
            }
        }

        System.out.println("Done!");
    }

}