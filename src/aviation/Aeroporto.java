package aviation;

import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Aeroporto extends Agent {

	// CondMeteo: 0->chuva, 1->sol, 2->nublado, 3->tempestad�o

	private Position posicao;
	private int nPistasAterragem, nPistasDisponiveis, nLimiteAero, nAeroEstacionadas, condMeteo;
	private ArrayList<AID> interessadasMeteo;
	private ArrayList<AID> pedidosDescolagem;
	private ArrayList<AID> pedidosAterragem;


	@Override
	protected void setup() {
		Object[] args = getArguments();
		posicao = new Position((int) args[0],(int) args[1]);

		Random rand = new Random();
		condMeteo = rand.nextInt(4); // Entre 0 e 3
		nPistasAterragem = rand.nextInt(3) + 1; // Entre 1 e 2
		nLimiteAero = rand.nextInt(11)+10; // Entre 10 e 20
		nAeroEstacionadas = 0;
		nPistasDisponiveis = nPistasAterragem;
		interessadasMeteo = new ArrayList<AID>();
		pedidosDescolagem = new ArrayList<AID>();
		pedidosAterragem = new ArrayList<AID>();


		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("estacao");
		sd.setName("Aeroporto");

		dfd.addServices(sd);

		try {

			DFService.register(this, dfd);

			this.addBehaviour(new PositionRequest());
			this.addBehaviour(new ParkingRequest());
			this.addBehaviour(new TakeoffRequest());
			this.addBehaviour(new TakeoffClearance());
			this.addBehaviour(new TakeoffInform());
			this.addBehaviour(new LandingRequest());
			this.addBehaviour(new LandingClearance());
			this.addBehaviour(new LandingInform());
			this.addBehaviour(new InformMeteo());
			this.addBehaviour(new InformMeteoChange(this));

		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	class PositionRequest extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("request-pos");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);

			if(msg != null) {
				AID aeronave = msg.getSender();
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.CONFIRM);
				reply.setOntology("request-pos");
				reply.setContent(posicao.getX() + "::" + posicao.getY());
				myAgent.send(reply);
			} else
				block();
		}
	}

	class ParkingRequest extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("propose-birth");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);
			
			if(msg != null) {
				AID aeronave = msg.getSender();
				ACLMessage reply = msg.createReply();

				if(nAeroEstacionadas < nLimiteAero) {
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					reply.setOntology("propose-birth");
					reply.setContent(posicao.getX() + "::" + posicao.getY());
					reply.addReceiver(aeronave);
					myAgent.send(reply);
					nAeroEstacionadas++;
				}
				else {
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					reply.setOntology("propose-birth");
					reply.addReceiver(aeronave);
					myAgent.send(reply);
				}
			}else
				block();
		}
	}

	class TakeoffRequest extends CyclicBehaviour {
		@Override
		public void action() {

			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("propose-takeoff");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);

			if(msg != null) {
				System.out.println(myAgent.getLocalName() + ": got a takeoff request");
				AID aeronave = msg.getSender();
				pedidosDescolagem.add(aeronave);
			} else
				block();
		}
	}

	class TakeoffClearance extends CyclicBehaviour {
		@Override
		public void action() {

			if(!pedidosDescolagem.isEmpty()) {
				if(nPistasDisponiveis > 0) {
					nPistasDisponiveis--;
					AID aeronave = pedidosDescolagem.get(0);
					ACLMessage accept_takeoff = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					accept_takeoff.setOntology("propose-takeoff");
					accept_takeoff.addReceiver(aeronave);
					myAgent.send(accept_takeoff);
					pedidosDescolagem.remove(aeronave);
				}
				else {
					System.out.println(myAgent.getLocalName() + ": No airstrips available.");
					AID aeronave = pedidosDescolagem.get(0);
					ACLMessage reject_takeoff = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
					reject_takeoff.setOntology("propose-takeoff");
					reject_takeoff.addReceiver(aeronave);
					myAgent.send(reject_takeoff);
					pedidosDescolagem.remove(aeronave);
				}
			} else
				block(5000);

		}
	}

	class TakeoffInform extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("info-takeoff");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);

			if(msg != null)
				nPistasDisponiveis++;
			else
				block();
		}
	}

	class LandingRequest extends CyclicBehaviour {
		@Override
		public void action() {

			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("propose-land");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);

			if(msg != null) {
				System.out.println(myAgent.getLocalName() + ": got a landing request");
				AID aeronave = msg.getSender();
				pedidosAterragem.add(aeronave);
			} else
				block();
		}
	}

	class LandingClearance extends CyclicBehaviour {
		@Override
		public void action() {

			if(!pedidosAterragem.isEmpty()) {
				if(nPistasDisponiveis > 0) {
					nPistasDisponiveis--;
					AID aeronave = pedidosAterragem.get(0);
					ACLMessage accept_landing = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					accept_landing.setOntology("propose-land");
					accept_landing.addReceiver(aeronave);
					myAgent.send(accept_landing);
					pedidosAterragem.remove(aeronave);
				} else
					System.out.println(myAgent.getLocalName() + ": No airstrips available.");
			}
			block(5000);
		}
	}

	class LandingInform extends CyclicBehaviour {
		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("info-landing");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);

			if(msg != null)
				nPistasDisponiveis++;
			else
				block();
		}
	}

	class InformMeteo extends CyclicBehaviour {
		@Override
		public void action() {

			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("request-meteo");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);

			if(msg != null) {
				AID aeronave = msg.getSender();
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				reply.setOntology("info-meteo");
				reply.setContent(posicao.getX()+"::" + posicao.getY()+"::"+condMeteo);
				myAgent.send(reply);
				interessadasMeteo.add(aeronave);
			}else
				block();
		}
	}

	class InformMeteoChange extends TickerBehaviour {
		public InformMeteoChange(Agent a){
			super(a,10000);
		}

		@Override
		public void onTick() {
			Random rand = new Random();
			int newMeteo = rand.nextInt(4);
			
			//Only notify notory changes.
			if(((condMeteo == 0 || condMeteo == 1 || condMeteo == 2) && newMeteo == 3) ||
					(condMeteo == 3 && (newMeteo == 0 || newMeteo == 1 || newMeteo == 2))) {
				for(int i = 0; i < interessadasMeteo.size(); i++) {
					AID aeronave = interessadasMeteo.get(i);
					ACLMessage msg  = new ACLMessage(ACLMessage.INFORM);
					msg.setOntology("info-meteo");
					msg.setContent(posicao.getX()+"::" + posicao.getY()+"::"+condMeteo);
					msg.addReceiver(aeronave);
					myAgent.send(msg);
				}
			}
		}
	}
}