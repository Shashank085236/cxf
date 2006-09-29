@echo off 
rem 
rem  invoke the CXF java2wsdl tool
rem 
@setlocal

set CXF_HOME=%~dp0..

if not defined JAVA_HOME goto no_java_home

set SUN_TOOL_PATH=%JAVA_HOME%\lib\tools.jar;

if not exist "%CXF_HOME%\lib\cxf-incubator.jar" goto no_cxf_jar

set CXF_JAR=%CXF_HOME%\lib\cxf-incubator.jar

"%JAVA_HOME%\bin\java" -cp "%CXF_JAR%;%SUN_TOOL_PATH%;%CLASSPATH%" -Djava.util.logging.config.file="%CXF_HOME%\etc\logging.properties" org.apache.cxf.tools.java2wsdl.JavaToWSDL %*

@endlocal

goto end

:no_cxf_jar
echo ERROR: Unable to find cxf-incubator.jar in %cxf_home/lib
goto end

:no_java_home
echo ERROR: Set JAVA_HOME to the path where the J2SE 5.0 (JDK5.0) is installed
goto end 
:end



