/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.ejbTimer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Schedule;
import javax.ejb.Singleton;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

/**
 * Demonstrates how to use the EJB's @Schedule.
 * 
 * @author <a href="mailto:ozizka@redhat.com">Ondrej Zizka</a>
 */
@Singleton
public class ScheduleExample {
	
	ModelControllerClient client = null;

	@Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
	public void txCleanup() {
		
		txProbe();
		
		try {

			ModelNode op = new ModelNode();
			op.get("operation").set("read-resource");
			
			ModelNode addr = op.get("address");
			addr.add("subsystem", "transactions");
			addr.add("log-store", "log-store");
			op.get("recursive").set(true);
			System.out.println(op);
			
			//subsystem=transactions/log-store=log-store:read-resource(recursive=true)

			
			client = ModelControllerClient.Factory.create(
					InetAddress.getByName("127.0.0.1"), 9999);
			
			System.out.println("client.execute start");
			ModelNode response = client.execute(new OperationBuilder(op)
					.build());
			System.out.println("client.execute finished");
			
			System.out.println("RESULT: " + response.get(ClientConstants.RESULT).toString());
			
			List<ModelNode> lst = response.get(ClientConstants.RESULT).asList();
			
			Iterator it = lst.iterator();
			
			while(it.hasNext()){

				op = (ModelNode) it.next();
				op.get("operation").set("read-attribute");
				op.get("name").set("age-in-seconds");
				
				//node.get("operation").set("read-resource");
				//node.add("age-in-seconds");
				
				System.out.println("client.execute start");
				response = client.execute(new OperationBuilder(op)
						.build());
				System.out.println("client.execute finished");
				
				System.out.println("RESULT in WHILE loop: " + response.get(ClientConstants.RESULT).toString());
				
				
			}
			
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
}
	
	
	//subsystem=transactions/log-store=log-store/:probe()
	public void txProbe() {
		System.out.println("txProbe");

		try {

			ModelNode op = new ModelNode();
			op.get("operation").set("probe");
			op.get("operations").set(true);
			
			ModelNode addr = op.get("address");
			addr.add("subsystem", "transactions");
			addr.add("log-store", "log-store");
			
			System.out.println("operation: " + op);
			
			client = ModelControllerClient.Factory.create(
					InetAddress.getByName("127.0.0.1"), 9999);
			
			System.out.println("client.execute start");
			ModelNode response = client.execute(op);
			System.out.println("client.execute finished");
			
			System.out.println("RESULT: " + response.get(ClientConstants.RESULT).toString());
			
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}