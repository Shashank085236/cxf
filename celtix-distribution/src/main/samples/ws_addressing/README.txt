WS-Addressing Demo
==================

This demo shows how WS-Addressing support in Celtix may be enabled.  

The client and server both use handler configuration to install the
WS-Addressing handlers, comprising a LogicalHandler (MAPAggregator)
responsible for aggregating the WS-A MessageAddressingProperties for
the current message, and a ProtocolHandler (MAPCodec) responsible for
encoding/decoding these properties as SOAP Headers. 

An additional demo-specific ProtocolHandler (HeaderSnooper) is used to
snoop the SOAP Headers and display these to the console.

Normally the WS-Addressing MessageAddressProperties are generated and
propagated implicitly, without any intravention from the
application. In certain circumstances however, the application may wish
to participate in MAP assembly, for example to associate a sequence of
requests via the RelatesTo header. This demo illustrates both implicit
and explicit MAP propagation.

This demo also illustrates usage of the decoupled HTTP transport, whereby
a seperate server->client HTTP connection is used to deliver the responses.
Note the normal HTTP mode (where the response is delivered on the back-
channel of the original client->server HTTP connection) may of course also
be used  with WS-Addressing; in this case the <wsa:ReplyTo> header is set to
a well-known anonymous URI, "http://www.w3.org/2005/08/addressing/anonymous".

In all other respects this demo is based on the basic hello_world sample,
illustrating that WS-Addressing usage is independent of the application.
One notable addition to the familiar hello_world WSDL is the usage
of the <wsaw:UsingAddressing> extension element to indicate the
WS-Addressing support is enabled for the service endpoint.

Please review the README in the samples directory before continuing.


Prerequisite
------------

If your environment already includes celtix.jar on the CLASSPATH,
and the JDK and ant bin directories on the PATH, it is not necessary to
run the environment script described in the samples directory README.
If your environment is not properly configured, or if you are planning
on using wsdl2java, javac, and java to build and run the demos, you must
set the environment by running the script.


Building and running the demo using ant
---------------------------------------

From the samples/ws_addressing directory, the ant build script can be used to
build and run the demo.  The server and client targets automatically build
the demo.

Using either UNIX or Windows:

  ant server
  ant client

Both client and server will use the MAPAggregator and MAPCodec
handlers to aggregate and encode the WS-Addressing MAPs.

To remove the code generated from the WSDL file and the .class
files, run:

  ant clean


Buildng the demo using wsdl2java and javac
------------------------------------------

From the samples/ws_addressing directory, first create the target directory
build/classes and then generate code from the WSDL file.

For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/hello_world_addr.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\hello_world_addr.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CELTIX_HOME/lib/celtix.jar:./build/classes
  javac -d build/classes src/demo/ws_addressing/common/*.java
  javac -d build/classes src/demo/ws_addressing/client/*.java
  javac -d build/classes src/demo/ws_addressing/server/*.java

For Windows:
  set classpath=%classpath%;%CELTIX_HOME%\lib\celtix.jar;.\build\classes
  javac -d build\classes src\demo\ws_addressing\common\*.java
  javac -d build\classes src\demo\ws_addressing\client\*.java
  javac -d build\classes src\demo\ws_addressing\server\*.java

Running the demo using java
---------------------------

From the samples/ws_addressing directory run the commands (entered on a single command line):

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CELTIX_HOME/etc/logging.properties
         -Dceltix.config.file=file:///$CELTIX_HOME/samples/ws_addressing/celtix-server.xml
         demo.ws_addressing.server.Server &

    java -Djava.util.logging.config.file=$CELTIX_HOME/etc/logging.properties
         -Dceltix.config.file=file:///$CELTIX_HOME/samples/ws_addressing/celtix-client.xml
         demo.ws_addressing.client.Client ./wsdl/hello_world_addr.wsdl

The server process starts in the background.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties
         -Dceltix.config.file=file:///%CELTIX_HOME%\samples\ws_addressing\celtix-client.xml
         demo.ws_addressing.server.Server

    java -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties
         -Dceltix.config.file=file:///%CELTIX_HOME%\samples\ws_addressing\celtix-client.xml
         demo.ws_addressing.client.Client .\wsdl\hello_world_addr.wsdl

The server process starts in a new command window.

After running the client, terminate the server process.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean

