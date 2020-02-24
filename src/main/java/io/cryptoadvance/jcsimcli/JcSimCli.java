package io.cryptoadvance.jcsimcli;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import javacard.framework.Applet;
import org.apache.commons.cli.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Scanner;

public class JcSimCli {

    // TODO
    // - provide constants as cmd line parameters (see apache lib)
    // - alternatively read bytes directly from socket
    // - go through error handling -> robustness
    // - remove Debug stuff
    // - take on github and write a README.md for usage

    private static CardSimulator simulator;

    // FIXME those guys should be commandline parameters
    private static final String APPLET_AID = "01020304050607080901";
    private static final String APPLET_CLASS = "fr.bmartel.helloworld.HelloWorld";
    private static final String APPLET_URL
            = "file:///home/wieland/Work/tech/java/javacard/javacard-tutorial/jc101-hello-world/build/classes/main/";

    public static void main(String[] args) {

        try {

            processCliArgs(args);
            System.exit(0);

            setupAPDU();

            processAPDU();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processAPDU() {

        Scanner scanner = new Scanner(System.in);
        String line;
        while ( ! (line = scanner.nextLine()).isEmpty()) {
            if (line.length() == 1 && line.equals("\n")) {
                break;
            }
            if (line.length() % 2 != 0) {
                System.err.println("A byte has 2 nibbles, therefore an even number of nibbles required."
                        + " Please try again");
                continue;
            }
            System.out.println("read input string: " + line);
            byte[] buffer = hexStringToByteArray(line);
            System.out.println("input byte buffer: " + byteArrayToHexString(buffer));

            try {
                CommandAPDU commandAPDU = new CommandAPDU(buffer);
                ResponseAPDU response = simulator.transmitCommand(commandAPDU);

                System.out.println(response.toString());
                System.out.println("response.length: " + response.getBytes().length);
                System.out.println("response.data: " + byteArrayToHexString(response.getBytes()));
            } catch (Exception e) {
                System.err.println("Error in APDU: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void setupAPDU() throws ClassNotFoundException, MalformedURLException {

        simulator = new CardSimulator();
        final AID aid = AIDUtil.create(APPLET_AID);

        URL[] classLoaderUrls = new URL[] { new URL(APPLET_URL) };
        URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);

        URL[] urls = urlClassLoader.getURLs();
        for (URL url : urls) {
            System.out.println("URL class loader paths:");
            System.out.println(url.getFile());
            System.out.println("URL class loader paths end.");
        }

        Class<? extends Applet> appletClass = (Class<? extends Applet>) urlClassLoader.loadClass(APPLET_CLASS);
        simulator.installApplet(aid, appletClass);
        simulator.selectApplet(aid);
    }

    private static byte[] hexStringToByteArray(String s) {
        if (s.length() % 2 != 0) {
            // will throw an exception below, so let's pad with a leading zero
            s = "0" + s;
        }
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static void processCliArgs(String[] args) throws ParseException {
        Options options = new Options();

        Option help = new Option("h", "help", false, "print this message" );
        Option port = Option.builder("p")
                .required(false)
                .longOpt("port")
                .desc("port number where JcSimCli will listen")
                .hasArg()
                .build();
        Option aid = Option.builder("a")
                .required(false)
                .longOpt("applet-aid")
                .desc("AID of the applet to use in the simulator")
                .hasArg()
                .build();

        options.addOption(help);
        options.addOption(port);
        options.addOption(aid);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "java -jar path/to/JcSimCli-{$version}-all.jar <options>", options );
            System.exit(0);
        }
        if (cmd.hasOption("port")) {
            String portNumber = cmd.getOptionValue("port");
            System.out.println("Port: " + portNumber);
            // use  in socket
        } else {
            System.out.println("Stdin is it baby");
            // use stdin
        }
        if (cmd.hasOption("applet-aid")) {
            String appletAID = cmd.getOptionValue("applet-aid");
            System.out.println("Applet AID: " + appletAID);
        }
    }

    private static String byteArrayToHexString(byte[] ba) {
        StringBuilder sb = new StringBuilder();
        for (byte b : ba) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
