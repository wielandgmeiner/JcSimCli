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
    // - alternatively read bytes directly from socket
    // - go through error handling -> robustness
    // - remove Debug stuff
    // - take on github and write a README.md for usage

    private static CardSimulator simulator;

    private static String APPLET_AID;
    private static String APPLET_CLASS;
    private static String APPLET_URL;
    private static String PORT;

    public static void main(String[] args) {

        try {

            processCliArgs(args);

            setupAPDU();

            processAPDU();

        } catch (ParseException pe) {
            System.err.println("Error parsing command line: " + pe.getMessage());
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

//        URL[] urls = urlClassLoader.getURLs();
//        for (URL url : urls) {
//            System.out.println("URL class loader paths:");
//            System.out.println(url.getFile());
//            System.out.println("URL class loader paths end.");
//        }

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

    private static String byteArrayToHexString(byte[] ba) {
        StringBuilder sb = new StringBuilder();
        for (byte b : ba) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void processCliArgs(String[] args) throws ParseException {
        Option help = Option.builder("h")
                .required(false)
                .longOpt("help")
                .desc("Print this message.")
                .hasArg(false)
                .build();
        Option port = Option.builder("p")
                .required(false)
                .longOpt("port")
                .desc("Port number where JcSimCli will listen.")
                .hasArg()
                .build();
        Option aid = Option.builder("a")
                .required(true)
                .longOpt("applet-aid")
                .desc("AID of the applet to use in the simulator.")
                .hasArg()
                .build();
        Option aClass = Option.builder("c")
                .required(true)
                .longOpt("applet-class")
                .desc("Applet class to be loaded into the simulator.")
                .hasArg()
                .build();
        Option url = Option.builder("u")
                .required(true)
                .longOpt("applet-url")
                .desc("Path to the applet in the file system.")
                .hasArg()
                .build();

        Options allOptions = new Options();
        allOptions.addOption(help);
        allOptions.addOption(port);
        allOptions.addOption(aid);
        allOptions.addOption(aClass);
        allOptions.addOption(url);

        // the --help option contradicts with the required options, so a call with only --help
        // throws an exception because the required args are missing. This is a workaround. The commandline
        // is parsed twice and checked for a --help option first.
        // this is pretty unbelievable that there is no sane solution for this extremely common problem in a
        // library that old and widely used. FIXME use other better lib
        try {
            Options helpOption = new Options();
            helpOption.addOption(help);
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(helpOption, args);
            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "java -jar path/to/JcSimCli-{$version}-all.jar <options>", allOptions );
                System.exit(0);
            }
        } catch (ParseException e) { /* other options present, proceed with another parse */ }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(allOptions, args);

        if (cmd.hasOption("port")) {
            String portNumber = cmd.getOptionValue("port");
            PORT = portNumber;
            // use  in socket
        } else {
            System.out.println("Stdin is it baby");
            // use stdin
        }
        if (cmd.hasOption("applet-aid")) {
            String appletAID = cmd.getOptionValue("applet-aid");
            APPLET_AID = appletAID;
        }
        if (cmd.hasOption("applet-class")) {
            String appletClass = cmd.getOptionValue("applet-class");
            APPLET_CLASS = appletClass;
        }
        if (cmd.hasOption("applet-url")) {
            String appletUrl = cmd.getOptionValue("applet-url");
            APPLET_URL = appletUrl;
        }
    }
}
