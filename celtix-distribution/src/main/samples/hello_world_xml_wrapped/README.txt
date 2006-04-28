Hello World Demo using WRAPPED Style in XML Binding
=============================================

This demo illustrates how celtix works with xml binind instead of soap
binding. And this specific demo shows you how xml binding works
with the doc-lit wrapped style.

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

From the samples/hello_world_xml_wrapped directory, the ant build script
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

From the samples/hello_world_xml_wrapped directory, first create the target
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
  javac -d build/classes src/demo/hw/client/*.java
  javac -d build/classes src/demo/hw/server/*.java

For Windows:
  set classpath=%classpath%;%CELTIX_HOME%\lib\celtix.jar;.\build\classes
  javac -d build\classes src\demo\hw\client\*.java
  javac -d build\classes src\demo\hw\server\*.java



Running the demo using java
---------------------------

From the samples/hello_world_xml_wrapped directory run the commands, entered on a
single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CELTIX_HOME/etc/logging.properties
         demo.hw.server.Server &

    java -Djava.util.logging.config.file=$CELTIX_HOME/etc/logging.properties
         demo.hw.client.Client ./wsdl/hello_world.wsdl

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties
         demo.hw.server.Server

    java -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties
       demo.hw.client.Client .\wsdl\hello_world.wsdl

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean



Building and running the demo in a servlet container
----------------------------------------------------

From the samples/hello_world_xml_wrapped directory, the ant build script
can be used to create the war file that is deployed into the
servlet container.

Build the war file with the command:

  ant war
    

The war file will be included in the directory
samples/hello_world_xml_wrapped/build/war.  Simply copy the war file into
the servlet container's deployment directory.  For example,
with Tomcat copy the war file into the directory
<installationDirectory>/webapps.  The servlet container will
extract the war and deploy the application.

Using ant, run the client application with the command:

  ant client-servlet -Dbase.url=http://localhost:#

Where # is the TCP/IP port used by the servlet container,
e.g., 8080.

Using java, run the client application with the command:

  For UNIX:
    
    java -Djava.util.logging.config.file=$CELTIX_HOME/etc/logging.properties
         demo.hw.client.Client http://localhost:#/helloworld_xmlwrapped/celtix/hello_world_xml_wrapped

  For Windows:

    java -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties
       demo.hw.client.Client http://localhost:#/helloworld_xmlwrapped/celtix/hello_world_xml_wrapped

Where # is the TCP/IP port used by the servlet container,
e.g., 8080.
