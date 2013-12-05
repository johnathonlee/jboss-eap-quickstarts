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
package com.redhat.gss.sample.txRemovalTimer;

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
 * Sample using the EJB's @Schedule and Native Management API for Transaction removal.
 * 
 * Note that arbitrarily removing entries in the local transaction store can result in recoverable transactions being lost
 * 
 * @author <a href="mailto:jolee@redhat.com">Johnathon Lee</a>
 */
@Singleton
public class TxRemovalSample {

	ModelControllerClient client = null;

	// For time-saving purposes of the sample this is set to run every 5 seconds
	@Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
	public void txCleanup(){

		try {
			
			client = ModelControllerClient.Factory.create(
					InetAddress.getByName("127.0.0.1"), 9999);
			
			txProbe();

			//subsystem=transactions/log-store=log-store:read-resource(recursive=true)
			ModelNode op = new ModelNode();
			op.get("operation").set("read-children-resources");
			op.get("child-type").set("transactions");
			op.get("operations").set(true);

			ModelNode addr = op.get("address");
			addr.add("subsystem", "transactions");
			addr.add("log-store", "log-store");
			op.get("recursive").set(true);
			//show the operation
			System.out.println("OPERATION: " + op);

			

			ModelNode response = client.execute(new OperationBuilder(op)
					.build());
			
			//show the result
			System.out.println("RESULT: "
					+ response.get(ClientConstants.RESULT).toString());

			List<ModelNode> lst = response.get(ClientConstants.RESULT).asList();

			Iterator<ModelNode> it = lst.iterator();

			while (it.hasNext()) {

				op = (ModelNode) it.next();
				System.out.println("age-in-seconds: "
						+ op.get(0).get("age-in-seconds"));

				// if it's older than an hour then delete it
				if (op.get(0).get("age-in-seconds").asInt() > 3600) {
					///subsystem=transactions/log-store=log-store/transactions=0\:ffff7f000001\:611e\:529f4dcc\:11:delete
					ModelNode deleteOp = new ModelNode();
					deleteOp.get("operation").set("delete");
					deleteOp.get("operations").set(true);

					ModelNode deleteAddr = deleteOp.get("address");
					deleteAddr.add("subsystem", "transactions");
					deleteAddr.add("log-store", "log-store");
					deleteAddr.add("transactions", op.get(0).get("id"));
					deleteOp.get("recursive").set(true);
					System.out.println("OPERATION:" + deleteOp);
					response = client.execute(new OperationBuilder(deleteOp)
							.build());
				}

				client.close();
			}

		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	// subsystem=transactions/log-store=log-store/:probe()
	public void txProbe() throws IOException {
		System.out.println("txProbe");

			ModelNode op = new ModelNode();
			op.get("operation").set("probe");
			op.get("operations").set(true);

			ModelNode addr = op.get("address");
			addr.add("subsystem", "transactions");
			addr.add("log-store", "log-store");

			System.out.println("OPERATION: " + op);

			ModelNode response = client.execute(op);
			
			System.out.println("RESULT: "
					+ response.get(ClientConstants.RESULT).toString());

	}
}