Hello World Demo using StreamHandler to transform message
=========================================================

This demo illustrates how to use a stream handler to transform a
message.  A StreamHandler is installed on both client and server
side. On an outgoing message, the handler replaces the OutputStream
used by the binding to unmarshal the message with an OutputStream
which compresses the message.  For an incoming message, a
decompressing InputStream is used.

The StreamHandler is a Celtix specific handler that is invoked just
before a message is written to or read from the underlying transport.
The handler receives a MessageContext which contains that
java.io.OutputStream or java.io.InputStream which is used to write or
read the message.  The handler can replace the stream so that some
transformation or other operation can be applied to the bytes of the
message.  In this case a GZIPOutputStream and GZIPInputStream are used
to apply compression to the message as it is being transmitted over
the wire.

Please review the README in the samples directory before
continuing.



Prerequisite
------------

If your environment already includes celtix.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to run the environment script described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment by running the script.



Building and running the demo using ant
---------------------------------------

From the samples/streams directory, the ant build script
can be used to build and run the demo.

Using either UNIX or Windows:

  ant build
  ant server
  ant client
    

To remove the code generated from the WSDL file and the .class
files, run:

  ant clean



Buildng the demo using wsdl2java and javac
------------------------------------------

From the samples/streams directory, first create the target
directory build/classes and then generate code from the WSDL file.

For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/hello_world.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\hello_world.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CELTIX_HOME/lib/celtix.jar:./build/classes
  javac -d build/classes src/demo/streams/common/*.java
  javac -d build/classes src/demo/streams/client/*.java
  javac -d build/classes src/demo/streams/server/*.java

For Windows:
  set classpath=%classpath%;%CELTIX_HOME%\lib\celtix.jar:.\build\classes
  javac -d build\classes src\demo\streams\common\*.java
  javac -d build\classes src\demo\streams\client\*.java
  javac -d build\classes src\demo\streams\server\*.java

Running the demo using java
---------------------------

From the samples/streams directory run the commands, entered on a
single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CELTIX_HOME/etc/logging.properties
         -Dceltix.config.file=file:///$CELTIX_HOME/samples/handlers/celtix-server.xml
	 demo.streams.server.Server &

    java -Djava.util.logging.config.file=$CELTIX_HOME/etc/logging.properties
         -Dceltix.config.file=file:///$CELTIX_HOME/samples/handlers/celtix-client.xml
         demo.streams.client.Client ./wsdl/hello_world.wsdl

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties
         -Dceltix.config.file=file:///%CELTIX_HOME%\samples\handlers\celtix-server.xml
         demo.streams.server.Server

    java -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties
         -Dceltix.config.file=file:///%CELTIX_HOME%\samples\handlers\celtix-client.xml
         demo.streams.client.Client .\wsdl\hello_world.wsdl

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean
