package aviation;

import java.awt.PageAttributes.OriginType;
import java.util.ArrayList;
import java.util.Random;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Aeronave extends Agent{
	
	private Map mapa;
	private int estado; // 0 -> para descolar, 1-> voo, 2-> para aterrar
	private AID destino;
	private AID aeroportoAtual;
	private int alertZone, protectedZone, nPassageiros, distPercorrida, velocidade, distPrevista;
	//private ArrayList<Integer> condMeteo;
	private Position posicao;
	private Position pos_destino;
	private ArrayList<AID> aeroportos, aeronaves;
	private ArrayList<Position> rota;
	private long tempoPartida, tempoVoo;
	
	@Override
	protected void setup() {
		mapa = new Map();
		rota = new ArrayList<Position>();
		aeroportos = new ArrayList<AID>();
		aeronaves = new ArrayList<AID>();
		Random rand = new Random();
		nPassageiros = rand.nextInt(50)+50;
		estado = 0;
		
		
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("voo");
        sd.setName("Aeronave");
        
        dfd.addServices(sd);
        
        try {
            
            DFService.register(this, dfd);
            
            this.addBehaviour(new ProcessAirportReplies());
            //Fetch all the airports
            this.addBehaviour(new FetchAirportsBehav());
            //Pick origin and destination and their coordinates
            this.addBehaviour(new PickOriginDestinyBehav());
            //Fill map with airports and territories (*)
            this.addBehaviour(new RequestAirportPositions());
            //Calculate route
            //Ask for permission to take off
           
            
        } catch (FIPAException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Fetch all the airports registered in the DF service and store their AID.
	 *
	 */
	class FetchAirportsBehav extends SimpleBehaviour{

		@Override
		public void action() {
			
			DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("estacao");
            sd.setName("Aeroporto");
            template.addServices(sd);
            
            DFAgentDescription[] result;
            
            try {
                
                result = DFService.search(myAgent, template);
                int nr_aeroportos = result.length;
                
                for(int i = 0; i < nr_aeroportos; i++){
                	
                	aeroportos.add(result[i].getName());
                	
                }
            } catch(Exception e){
                e.printStackTrace();
            }
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return true;
		}
		
	}
	
	/**
	 *  Find the origin and destination airports.
	 *	This behavior may be called n times if the origin airport can't receive the airplane.
	 *  Randomly picks 2 indexes and assigns them to the actual and the destination airports
	 *
	 */
	class PickOriginDestinyBehav extends SimpleBehaviour{
		
		boolean onlyOrigin, found;
		
		public PickOriginDestinyBehav() {
			onlyOrigin = false;
			found = false;
		}
		
		@Override
		public void action() {
			Random rand = new Random();
			
			do {
				aeroportoAtual = aeroportos.get(rand.nextInt(aeroportos.size()));
				if(!onlyOrigin) {
					destino = aeroportos.get(rand.nextInt(aeroportos.size()));
					onlyOrigin = true;
				}
			} while(aeroportoAtual == destino);
			
			System.out.println(aeroportoAtual.getLocalName() + " -> " + destino.getLocalName());
			
			//Send the request to the picked origin airport
			ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
			request.setOntology("propose-birth");
			request.setConversationId(""+ System.currentTimeMillis());
			request.addReceiver(aeroportoAtual);
			myAgent.send(request);
			
			//Wait for the request
			//MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("propose-birth");
			//MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);
			
			ACLMessage reply = blockingReceive(mt2);
			if(reply != null) {
				if(reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					// If it proposal is accepted
					AID airport = reply.getSender();
					String[] content_split = reply.getContent().split("::");
					int x = Integer.parseInt(content_split[0]);
					int y = Integer.parseInt(content_split[1]);
					Position pos = new Position(x,y);
					mapa.setAirport(pos, airport, false);
					posicao = pos;
					found = true;
					System.out.println(""+myAgent.getLocalName()+" has born in " + airport.getLocalName());
				}
				// else found still's false
			}
		}

		@Override
		public boolean done() {
			return found;
		}
	}
	
	/**
	 * Send a request to all airports except origin and destiny for their position
	 *
	 */
	class RequestAirportPositions extends OneShotBehaviour{
		
		@Override
		public void action() {
			for(AID aero: aeroportos) {
				if(aero != aeroportoAtual) {
					ACLMessage request_info = new ACLMessage(ACLMessage.REQUEST);
					request_info.setConversationId(""+System.currentTimeMillis());
					request_info.setOntology("request-pos");
					request_info.addReceiver(aero);
					myAgent.send(request_info);
				}
			}
		}
	}
	
	class AskForTakeOffBehav extends OneShotBehaviour{
		@Override
		public void action() {
			ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
			request.setOntology("propose-takeoff");
			request.setConversationId(""+ System.currentTimeMillis());
			request.addReceiver(aeroportoAtual);
			myAgent.send(request);
		}
	}
	
	class CalculateRouteBehav extends OneShotBehaviour{
		
		@Override
		public void action() {
			rota = mapa.calculateRoute(posicao, pos_destino);
		}
		
	}
	
	/**
	 *  Initiate the takeoff of the airplane
	 *
	 */
	class StartTakeOffBehav extends SimpleBehaviour{
		 private boolean takedoff = false;
		 
		public StartTakeOffBehav(Agent a) {
			super(a);
			takedoff = false;
		}

		@Override
		public void action() {
			if(rota.size() > 0) {
				tempoPartida = System.currentTimeMillis();
				estado = 1;
				posicao = rota.get(0);
				rota.remove(0);
				
				
				ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
				request.setOntology("info-takeoff");
				request.setConversationId(""+ System.currentTimeMillis());
				request.addReceiver(aeroportoAtual);
				myAgent.send(request);
				takedoff = true;
				
				//send message to interface too
				
				System.out.println(""+myAgent.getLocalName()+" as taken flight!");
			}
		}

		@Override
		public boolean done() {
			return takedoff;
		}
		
		
	}
	
	class StartLandingBehav extends OneShotBehaviour{

		@Override
		public void action() {
			
			
		}
		
	}
	
	class WaitLandingBehav extends CyclicBehaviour{
		@Override
		public void action() {
			
		}
	}
	
	/**
	 * Process the replies of the airports and take specific actions.
	 *
	 */
	class ProcessAirportReplies extends CyclicBehaviour{
		
		private int nr_airports_inserted = 0;

		@Override
		public void action() {
			
			ACLMessage reply = receive();
			
			if(reply != null) {
				String ontology = reply.getOntology();
				if(ontology == "request-pos") {
					//Process airport position request
					AID airport = reply.getSender();
					String[] content_split = reply.getContent().split("::");
					int x = Integer.parseInt(content_split[0]);
					int y = Integer.parseInt(content_split[1]);
					Position pos = new Position(x,y);
					if(airport.equals(destino)) {
						mapa.setAirport(pos, airport, false);
						pos_destino = pos;
					}else
						mapa.setAirport(pos, airport, true);
					
					nr_airports_inserted++;
					// If all the airports have reported then ask for take-off and start finding route
					if(nr_airports_inserted == aeroportos.size()-1) {
						myAgent.addBehaviour(new AskForTakeOffBehav());
						myAgent.addBehaviour(new CalculateRouteBehav());
					}
					
				} else if(ontology == "propose-takeoff") {
					
					int perf = reply.getPerformative();
					if(perf == ACLMessage.ACCEPT_PROPOSAL) {
						// If it has received order to take-off and route is calculated then start take-off
						if(rota.size() > 0)
							myAgent.addBehaviour(new StartTakeOffBehav(myAgent));
					}
					
				} else if(ontology == "propose-land"){
					
					int perf = reply.getPerformative();
					if(perf == ACLMessage.ACCEPT_PROPOSAL) {
						myAgent.addBehaviour(new StartLandingBehav());
					} else {
						myAgent.addBehaviour(new WaitLandingBehav());
					}
					
				} else if(ontology == "info-meteo"){
					//Process airport meteorology request
					AID airport = reply.getSender();
					String[] content = reply.getContent().split("::");
					
					int meteo = Integer.parseInt(content[2]);
					if(meteo == 3) {
						int x = Integer.parseInt(content[1]);
						int y = Integer.parseInt(content[1]);
						Position pos = new Position(x,y);
						mapa.setBadWeather(pos);
					}
				}
			} else {
				block();
			}
		}
	}
}
