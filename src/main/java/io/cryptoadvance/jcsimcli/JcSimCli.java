package io.cryptoadvance.jcsimcli;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import javacard.framework.Applet;
import org.apache.commons.cli.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.*;
import java.net.*;

public class JcSimCli {

    // we assume no longer input than 4096 bytes
    private static final int INPUT_BUFFER_SIZE = 4096;

    private static String   APPLET_AID;
    private static String   APPLET_CLASS;
    private static String   APPLET_URL;
    private static Integer  PORT;

    private static boolean use_stdin    = false;
    private static boolean use_hex      = false;

    private static CardSimulator simulator;


    public static void main(String[] args) {

        try {

            processCliArgs(args);

            setupSimulator();

            if (use_hex) {
                if (use_stdin) {
                    processStdin();
                } else {
                    processSocket();
                }
            } else { // use binary
                processSocketInBinary();
            }

        }
        catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
        }
        catch (IOException e) {
            System.err.println("Error opening connection: " + e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processStdin() throws IOException {

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(System.out, true);
        ) {
            processAPDU(in, out);
        }
    }

    private static void processSocket() throws IOException {

        try (
                ServerSocket serverSocket = new ServerSocket(PORT);
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            processAPDU(in, out);
        }
    }

    private static void processAPDU(BufferedReader in, PrintWriter out) throws IOException {

        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            if (line.length() % 2 != 0) {
                System.err.println("A byte has 2 nibbles, therefore an even number of nibbles required."
                        + " Please try again");
                continue;
            }
            byte[] buffer = hexStringToByteArray(line);
            System.out.println("input byte buffer: " + byteArrayToHexString(buffer));

            try {
                CommandAPDU commandAPDU = new CommandAPDU(buffer);
                ResponseAPDU response = simulator.transmitCommand(commandAPDU);
                out.println(byteArrayToHexString(response.getBytes()));
            } catch (Exception e) {
                System.err.println("Error in APDU: " + e.getMessage());
            }
        }
    }

    private static void processSocketInBinary() throws IOException {

        try (
                ServerSocket serverSocket = new ServerSocket(PORT);
                Socket clientSocket = serverSocket.accept();
                DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
        ) {
            byte[] input = new byte[INPUT_BUFFER_SIZE];
            int len = 0;
            while ((len = in.read(input)) != -1) { // EOF
                byte[] buffer = new byte[len];
                System.arraycopy(input, 0, buffer, 0, len);
                try {
                    CommandAPDU commandAPDU = new CommandAPDU(buffer);
                    ResponseAPDU response = simulator.transmitCommand(commandAPDU);
                    out.write(response.getBytes());
                    out.flush();
                } catch (Exception e) {
                    System.err.println("Error in APDU: " + e.getMessage());
                } finally {
                    // reset the input buffer
                    input = new byte[INPUT_BUFFER_SIZE];
                }
            }
        }
    }

    private static void setupSimulator() throws ClassNotFoundException, MalformedURLException {

        simulator = new CardSimulator();
        final AID aid = AIDUtil.create(APPLET_AID);
        URL[] classLoaderUrls = new URL[] { new URL(APPLET_URL) };
        URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);
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
        Option hex = Option.builder("x")
                .required(false)
                .longOpt("hex")
                .desc("Interpret input/output as ascii hex instead of binary (default).")
                .hasArg(false)
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
        allOptions.addOption(hex);

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
            PORT = Integer.parseInt(portNumber);
        } else {
            use_stdin = true;
        }
        if (cmd.hasOption("hex")) {
            use_hex = true;
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
