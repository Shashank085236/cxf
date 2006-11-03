Overview 
========

Prerequisite:  This README assumes some familiarity with the Java
Business Integration specification.  See the following URL for more
information: http://java.sun.com/integration/

This demo illustrate how external cxf client can communicate with internal cxf server
which is deployed into cxf service engine through a
generic JBI binding component(as a router). 

The demo consists of a CXF Service Engine and a ServiceMix Soap
binding component. A cxf service unit (as provider) is deployed into
CXF Service Engine. A servicemix soap binding service unit(as
transport router) is deployed into ServiceMix Soap binding
component. CXF service unit and ServiceMix soap binding service
unit are wrapped in cxf demo service assembly.

A standalone cxf client(as consumer) invoke servicemix soap binding
service unit using soap/http, the servicemix soap binding service
route this request to cxf service unit using xml/NMR specified by
JBI. Here servicemix soap binding service unit play the role as a
router, connecting cxf service consumer and provider with different
transport and different binding.




Deploy CXF Service Engine into ServiceMix
============================================
Build Instructions
------------------
. Download & Install ServiceMix 
  http://incubator.apache.org/servicemix/main/download.html
  Note: Must isntall the lastest 3.0 SNAPSHOT version since any previous version still
	has bugs for CXF ServiceMix integration.

. export SERVICE_MIX_HOME for your shell envirnoment

. Edit build.properties to sepcify cxf.home and jbi.sdk.jar,
  jbi.sdk.jar=$SERVICE_MIX_HOME/lib/servicemix-jbi-3.0-SNAPSHOT.jar

. build everything using ant: 'ant build'

Installation & Deployment
-------------------------
Ensure that the $SERVICE_MIX_HOME/bin is on the path.

Start ServiceMix
 >servicemix
And then you can see logs from the shell which you start servicemix, including
ServiceEngine install log, Service Assembly deploy log, cxf service
consumer and provider communication log.

Install and start the CXF Service Engine:

 > ant -f $SERVICE_MIX_HOME/ant/servicemix-ant-task.xml install-component -Dsm.install.file=./service-engine/build/lib/cxf-service-engine.jar  -Dsm.username=smx -Dsm.password=smx
 > ant -f $SERVICE_MIX_HOME/ant/servicemix-ant-task.xml start-component -Dsm.component.name=CXFServiceEngine  -Dsm.username=smx -Dsm.password=smx

Install and start the ServiceMix soap binding component

copy servicemix-http-3.0-incubating-installer.zip and
servicemix-shared-3.0-incubating-installer.zip to the deploy directory
where servicemix monitor on, for a instance, start servicemix from
$SERVICE_MIX_HOME/bin, then copy the two zip files into deploy directory under
$SERVICE_MIX_HOME/bin 

Deploy the and start CXF demo service assembly

 > ant -f $SERVICE_MIX_HOME/ant/servicemix-ant-task.xml  deploy-service-assembly -Dsm.deploy.file=./service-assembly/build/lib/cxf-service-assembly.zip  -Dsm.username=smx -Dsm.password=smx
 > ant -f $SERVICE_MIX_HOME/ant/servicemix-ant-task.xml start-service-assembly -Dsm.service.assembly.name=cxf-demo-service-assembly  -Dsm.username=smx -Dsm.password=smx

Start cxf client
 > ant client



What happened
=============
A standalone cxf client(as consumer) invoke servicemix soap binding
service unit using soap/http, the servicemix soap binding service
route this request to cxf service unit using xml/NMR specified by
JBI. Here servicemix soap binding service unit play the role as a
router, connecting cxf service consumer and provider with different
transport and different binding.
