/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.systest.swa;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.Holder;

import org.apache.cxf.swa.SwAServiceInterface;
import org.apache.cxf.swa.types.DataStruct;

@WebService(endpointInterface = "org.apache.cxf.swa.SwAServiceInterface", 
            serviceName = "SwAService", 
            targetNamespace = "http://cxf.apache.org/swa", 
            portName = "SwAServiceHttpPort")
public class SwAServiceImpl implements SwAServiceInterface {

    public void echoData(Holder<DataStruct> text, Holder<DataHandler> data) {

        try {
            InputStream bis = null;
            bis = data.value.getDataSource().getInputStream();
            byte b[] = new byte[6];
            bis.read(b, 0, 6);
            String string = new String(b);
            
            ByteArrayDataSource source = 
                new ByteArrayDataSource(("test" + string).getBytes(), "text/xml");
            data.value = new DataHandler(source);
        } catch (IOException e) {
            // TODO Auto-generated catch block

        }

    }

}
