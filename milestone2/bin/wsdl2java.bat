@echo off 
rem 
rem  invoke the Celtix wsdl2java tool
rem 
@setlocal

if not defined CELTIX_HOME goto no_celtix_home

call %CELTIX_HOME%\bin\celtix_env.bat 
%JAVA_HOME%\bin\java -Djaxws.home=%JAXWS_HOME% -Djava.util.logging.config.file=%CELTIX_HOME%\etc\logging.properties org.objectweb.celtix.tools.Wsdl2Java "%*"

@endlocal

goto end 

:no_celtix_home
    echo The CELTIX_HOME environment variable is unset.  Please set CELTIX_HOME
    echo envionment variable to the location of the Celtix installation
goto end

:end