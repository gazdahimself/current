/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.james.cli.probe.ServerProbe;
import org.apache.james.cli.probe.impl.JmxServerProbe;
import org.apache.james.cli.type.CmdType;

/**
 * Command line utility for managing various aspect of the James server.
 */
public class ServerCmd {
    private static final String HOST_OPT_LONG = "host";
    private static final String HOST_OPT_SHORT = "h";
    private static final String PORT_OPT_LONG = "port";
    private static final String PORT_OPT_SHORT = "p";
    private static final int defaultPort = 9999;
    private static Options options = null;

    static {
        options = new Options();
        Option optHost = new Option(HOST_OPT_SHORT, HOST_OPT_LONG, true, "node hostname or ip address");
        optHost.setRequired(true);
        options.addOption(optHost);
        options.addOption(PORT_OPT_SHORT, PORT_OPT_LONG, true, "remote jmx agent port number");
    }

    /**
     * Main method to initialize the class.
     * 
     * @param args
     *            Command-line arguments.
     * @throws IOException
     * @throws InterruptedException
     * @throws ParseException
     */
    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        
        long start = Calendar.getInstance().getTimeInMillis();
        
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException parseExcep) {
            System.err.println(parseExcep);
            printUsage();
            System.exit(1);
        }

        // Verify arguments
        if (cmd.getArgs().length < 1) {
            System.err.println("Missing argument for command.");
            printUsage();
            System.exit(1);
        }
        
        String host = cmd.getOptionValue(HOST_OPT_LONG);
        int port = defaultPort;

        String portNum = cmd.getOptionValue(PORT_OPT_LONG);
        if (portNum != null) {
            try {
                port = Integer.parseInt(portNum);
            } catch (NumberFormatException e) {
                throw new ParseException("Port must be a number");
            }
        }

        ServerProbe probe = null;
        try {
            probe = new JmxServerProbe(host, port);
        } catch (IOException ioe) {
            System.err.println("Error connecting to remote JMX agent!");
            ioe.printStackTrace();
            System.exit(3);
        }

        ServerCmd sCmd = new ServerCmd();

        // Execute the requested command.
        String[] arguments = cmd.getArgs();
        String cmdName = arguments[0];
        CmdType cmdType = null;
        try {

            cmdType = CmdType.lookup(cmdName);

            if (CmdType.ADDUSER.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.addUser(arguments[1], arguments[2]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.REMOVEUSER.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.removeUser(arguments[1]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.LISTUSERS.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    sCmd.print(probe.listUsers(), System.out);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.ADDDOMAIN.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.addDomain(arguments[1]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.REMOVEDOMAIN.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.removeDomain(arguments[1]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.CONTAINSDOMAIN.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.containsDomain(arguments[1]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.LISTDOMAINS.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    sCmd.print(probe.listDomains(), System.out);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.LISTMAPPINGS.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    sCmd.print(probe.listMappings(), System.out);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.LISTUSERDOMAINMAPPINGS.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    sCmd.print(probe.listUserDomainMappings(arguments[1], arguments[2]).toArray(new String[0]), System.out);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.ADDADDRESSMAPPING.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.addAddressMapping(arguments[1], arguments[2], arguments[3]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.REMOVEADDRESSMAPPING.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.removeAddressMapping(arguments[1], arguments[2], arguments[3]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.ADDREGEXMAPPING.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.addRegexMapping(arguments[1], arguments[2], arguments[3]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.REMOVEREGEXMAPPING.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.removeRegexMapping(arguments[1], arguments[2], arguments[3]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if (CmdType.SETPASSWORD.equals(cmdType)) {
                if (cmdType.hasCorrectArguments(arguments.length)) {
                    probe.setPassword(arguments[1], arguments[2]);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else {
                System.err.println("Unrecognized command: " + cmdName + ".");
                printUsage();
                System.exit(1);
            }
        } catch (Exception e) {
            sCmd.onException(e, System.err);
            System.exit(1);
        }
        
        sCmd.print(new String[]{cmdType.getCommand() + " command executed sucessfully in " 
                + (Calendar.getInstance().getTimeInMillis() - start) + " ms."}, System.out);
        System.exit(0);
    }

    /**
     * Print data to an output stream.
     * 
     * @param data
     *            The data to print, each element representing a line.
     * @param out
     *            The output stream to which printing should occur.
     */
    public void print(String[] data, PrintStream out) {
        if (data == null)
            return;
        
        for (int i = 0; i < data.length; i++) {
            String u = data[i];
            out.println(u);
        }
        
        out.println();
    }

    public void print(Map<String,Collection<String>> map, PrintStream out) {
        if (map == null)
            return;
        
        Iterator<Entry<String, Collection<String>>> entries = map.entrySet().iterator();
        while(entries.hasNext()) {
            Entry<String, Collection<String>> entry = entries.next();
            out.print(entry.getKey());
            out.print("=");
            out.println(entry.getValue().toString());
        }
        out.println();
    }

    
    /*
     * Prints usage information to stdout.
     */
    private static void printUsage() {
        HelpFormatter hf = new HelpFormatter();
        String header = String.format("%nAvailable commands:%n" 
                + "adduser <username> <password>%n"
                + "setpassword <username> <password>%n"
                + "removeuser <username>%n" 
                + "listusers%n" 
                + "adddomain <domainname>%n"
                + "containsdomain <domainname>%n" 
                + "removedomain <domainname>%n" 
                + "listdomains%n"
                + "addAddressMapping <user> <domain> <fromaddress>%n"
                + "removeAddressMapping <user> <domain> <fromaddress>%n"
                + "addRegexMapping <user> <domain> <regex>%n"
                + "removeRegexMapping <user> <domain> <regex>%n"
                + "listuserdomainmappings <user> <domain>%n"
                + "listmappings%n"
                );
        String usage = String.format("java %s --host <arg> <command>%n", ServerCmd.class.getName());
        hf.printHelp(usage, "", options, header);
    }

    /*
     * Handle an exception.
     */
    private void onException(Exception e, PrintStream out) {
        out.println("Error while execute command:");
        out.println(e.getMessage());
    }
}
