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

package demo.mtom.server;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.cxf.mime.TestMtom;

@WebService(serviceName = "TestMtomService",
                portName = "TestMtomPort",
                endpointInterface = "org.apache.cxf.mime.TestMtom",
                targetNamespace = "http://cxf.apache.org/mime")

public class TestMtomImpl implements TestMtom {


    public void testXop(Holder<String> name, Holder<byte[]> attachinfo) {
        System.out.println("Received image holder data from client");
        System.out.println("The image holder data length is " + attachinfo.value.length);        
        name.value = "return detail + " + name.value;        
    }

    public void testMtom(Holder<String> name, Holder<DataHandler> attachinfo) {
        try {
            System.out.println("Received image holder data with mtom enable from client");
            InputStream mtomIn = attachinfo.value.getInputStream();
            long fileSize = 0;
            System.out.println("The image holder data length is " + mtomIn.available());
            name.value = "return detail + " + name.value;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
