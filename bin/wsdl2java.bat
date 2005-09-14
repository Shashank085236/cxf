@echo off 
rem 
rem  invoke the Celtix wsdl2java tool
rem 
@setlocal

if not defined CELTIX_HOME goto no_celtix_home

call %CELTIX_HOME%\bin\celtix_env.bat 
java -Djaxws.home=%JAXWS_HOME% org.objectweb.celtix.tools.Wsdl2Java "%*"

@endlocal

goto end 

:no_celtix_home
    echo The CELTIX_HOME environment variable is unset.  Please set CELTIX_HOME
    echo envionment variable to the location of the Celtix installation
goto end

:end