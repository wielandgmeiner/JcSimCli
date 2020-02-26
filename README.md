# JcSimCli

A wrapper for the [jCardSim](https://jcardsim.org/) Javacard Simulator.

Repackages its dependencies in an uber-jar. Accepts input either as hex strings (from stdin or socket) or binary (from socket) and returns output in the same format and communicatioin channel.

### Usage

````
$ java -jar build/libs/JcSimCli-1.0-SNAPSHOT-all.jar --help
usage: java -jar path/to/JcSimCli-{$version}-all.jar <options>
 -a,--applet-aid <arg>     AID of the applet to use in the simulator.
 -c,--applet-class <arg>   Applet class to be loaded into the simulator.
 -h,--help                 Print this message.
 -p,--port <arg>           Port number where JcSimCli will listen.
 -u,--applet-url <arg>     Path to the applet in the file system.
 -x,--hex                  Interpret input/output as ascii hex instead of
                           binary (default).
````

There are scripts in the test directory that demonstrate the usage.

### Build

Run the gradle `shadowJar` task.

##### Requirements:
* Java 8
* JCardSim 3.0.4 or 3.0.5

##### Prerequisites

Obviously you will need a Javacard Applet class to run against.

The tests use the `jc101-hello-world` class of my experimental branch of the [javacard-tutorial](https://github.com/wielandgmeiner/javacard-tutorial).

