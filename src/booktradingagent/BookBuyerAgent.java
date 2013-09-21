/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package src.booktradingagent;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class BookBuyerAgent extends Agent {
	// The title of the book to buy
	private String targetBookTitle;
	// The list of known seller agents
	private AID[] sellerAgents;
	private int nResponders;

	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hallo! Buyer-agent "+getAID().getName()+" is ready.");

		// Get the title of the book to buy as a start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Target book is "+targetBookTitle);
			String[] args2Agents = ((String)args[1]).split(";");
			sellerAgents = new AID[args2Agents.length];
			
			nResponders = args2Agents.length;
	  		System.out.println("Trying to buy book out of "+nResponders+" responders.");
	  		
	  		// Fill the CFP message
	  		ACLMessage msg = new ACLMessage(ACLMessage.CFP);
	  		for (int i = 0; i < args2Agents.length; ++i) {
	  			msg.addReceiver(new AID((String) args2Agents[i], AID.ISLOCALNAME));
	  		}
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
				// We want to receive a reply in 10 secs
				msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
				msg.setContent(targetBookTitle);
				msg.setConversationId("book-trade");
				
				addBehaviour(new ContractNetInitiator(this, msg) {
					
					protected void handlePropose(ACLMessage propose, Vector v) {
						System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContent());
					}
					
					protected void handleRefuse(ACLMessage refuse) {
						System.out.println("Agent "+refuse.getSender().getName()+" refused");
					}
					
					protected void handleFailure(ACLMessage failure) {
						if (failure.getSender().equals(myAgent.getAMS())) {
							// FAILURE notification from the JADE runtime: the receiver
							// does not exist
							System.out.println("Responder does not exist");
						}
						else {
							System.out.println("Agent "+failure.getSender().getName()+" failed");
						}
						// Immediate failure --> we will not receive a response from this agent
						nResponders--;
					}
					
					protected void handleAllResponses(Vector responses, Vector acceptances) {
						if (responses.size() < nResponders) {
							// Some responder didn't reply within the specified timeout
							System.out.println("Timeout expired: missing "+(nResponders - responses.size())+" responses");
						}
						// Evaluate proposals.
						int bestProposal = -1;
						AID bestProposer = null;
						ACLMessage accept = null;
						Enumeration e = responses.elements();
						while (e.hasMoreElements()) {
							ACLMessage msg = (ACLMessage) e.nextElement();
							if (msg.getPerformative() == ACLMessage.PROPOSE) {
								ACLMessage reply = msg.createReply();
								reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
								acceptances.addElement(reply);
								int proposal = Integer.parseInt(msg.getContent());
								if(bestProposal == -1)
									bestProposal = proposal;
								else if (proposal < bestProposal) {
									bestProposal = proposal;
									bestProposer = msg.getSender();
									accept = reply;
								}
							}
						}
						// Accept the proposal of the best proposer
						if (accept != null) {
							System.out.println("Accepting proposal "+bestProposal+" from responder "+bestProposer.getName());
							accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
						}						
					}
					
					protected void handleInform(ACLMessage inform) {
						System.out.println("Agent "+inform.getSender().getName()+" successfully selled the book");
					}
				} );

		}
		else {
			// Make the agent terminate
			System.out.println("No target book title specified");
			doDelete();
		}
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
	}
}
