package aviation;

import java.awt.PageAttributes.OriginType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
	private int nPassageiros, distPercorrida, velocidade, distPrevista;
	private double alertZone, protectedZone ;
	private HashMap<AID,Integer> condMeteoMap;
	private Position posicao;
	private Position pos_destino;
	private ArrayList<AID> aeroportos;
	private ArrayList<Position> rota;
	private long tempoPartida, tempoVoo;
	private AID agent_interface;
	private int land_permission; // 0 -> no, 1->yes, 2->on hold
	private boolean changing_route;
	private boolean first_pass;

	@Override
	protected void setup() {
		land_permission = 0;
		mapa = new Map();
		estado = 0;
		rota = new ArrayList<Position>();
		Random rand = new Random();
		nPassageiros = rand.nextInt(50)+50;
		agent_interface = new AID();
		agent_interface.setLocalName("Interface");
		aeroportos = new ArrayList<AID>();
		condMeteoMap = new HashMap<AID,Integer>();
		changing_route = false;
		first_pass = true;



		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("parked");
		sd.setName("Aeronave");

		dfd.addServices(sd);

		try {

			DFService.register(this, dfd);

			//Prepare the agent to receive airplane requests
			this.addBehaviour(new SendStateToAirplaneBehav());
			//Prepare the agent to receive airport replies
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

	@Override
	protected void takeDown(){
		super.takeDown();

		try {
			DFService.deregister(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fetch all the airports registered in the DF service and store their AID.
	 *
	 */
	class FetchAirportsBehav extends OneShotBehaviour{

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

	class AskToLandBehav extends OneShotBehaviour{
		@Override
		public void action() {
			ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
			request.setOntology("propose-land");
			request.setConversationId(""+ System.currentTimeMillis());
			request.addReceiver(aeroportoAtual);
			myAgent.send(request);
			land_permission = 2;
		}
	}

	class CalculateRouteBehav extends OneShotBehaviour{
		
		@Override
		public void action() {
			changing_route = true;
			rota = mapa.calculateRoute(posicao, pos_destino);
			distPrevista = rota.size();
			changing_route = false;
			
			if(first_pass) {
				//Get weather on route
				//Only after the first route calculation
				Position init;
				AID lastTerritory = aeroportoAtual;
				AID thisTerritory;
				ACLMessage meteo = new ACLMessage(ACLMessage.REQUEST);
				meteo.setOntology("request-meteo");
				meteo.setConversationId(""+System.currentTimeMillis());
				
				for(int i = 20; i < rota.size(); i+=20) {
					init = rota.get(i);
					thisTerritory = mapa.getTerritory(init);
					if(thisTerritory != null && !thisTerritory.equals(lastTerritory)) {
						meteo.addReceiver(thisTerritory);
						lastTerritory = thisTerritory;
						condMeteoMap.put(thisTerritory, 0);
					}
				}
				System.out.println(condMeteoMap.size());
				myAgent.send(meteo);
				first_pass = false;
			}
			
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
				velocidade = 5;

				ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
				request.setOntology("info-takeoff");
				request.setConversationId(""+ System.currentTimeMillis());
				request.addReceiver(aeroportoAtual);
				myAgent.send(request);

				//send message to interface too

				ACLMessage info = new ACLMessage(ACLMessage.PROPOSE);
				info.setOntology("info-takeoff");
				info.setConversationId(""+ System.currentTimeMillis());
				info.addReceiver(agent_interface);
				myAgent.send(info);
				takedoff = true;

				myAgent.addBehaviour(new FlightBehav(myAgent, 500));

				System.out.println(""+myAgent.getLocalName()+" as taken flight!");
			}
		}

		@Override
		public boolean done() {
			return takedoff;
		}
	}

	class FlightBehav extends TickerBehaviour{

		public FlightBehav(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			
			if(!changing_route) {
				if((distPrevista - distPercorrida ) > 60) {
					myAgent.addBehaviour(new BeaconBehav());
					myAgent.addBehaviour(new MovePositionBehav());
				} else {
					if(land_permission == 2)
						myAgent.addBehaviour(new WaitLandingBehav());
					else if(land_permission == 1)
						myAgent.addBehaviour(new StartLandingBehav());
				}
			}
		}

	}

	class MovePositionBehav extends OneShotBehaviour{

		@Override
		public void action() {
			System.out.println(myAgent.getLocalName() + " distance left: " + rota.size() +"at speed " + velocidade);
			posicao = rota.get(rota.size()-(1 * velocidade));
			for(int i = 1; i <= velocidade; i++)
				rota.remove(rota.size()-(1*velocidade));
			distPercorrida += velocidade;
			aeroportoAtual = mapa.getTerritory(posicao);

			if((distPrevista - distPercorrida ) < 80) {
				if(land_permission == 0) {
					myAgent.addBehaviour(new AskToLandBehav());
					System.out.println(myAgent.getLocalName() + " asked to land in " + aeroportoAtual.getLocalName());
				}
			}
			//This marks the end of the flight Behav tick
			//System.out.println(myAgent.getLocalName() + " is moving to x=" + posicao.getX() + ", y="+posicao.getY());
		}

	}

	class BeaconBehav extends OneShotBehaviour{

		@Override
		public void action() {

			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("voo");
			sd.setName("Aeronave");
			template.addServices(sd);

			DFAgentDescription[] result;
			try {

				result = DFService.search(myAgent, template);
				for(int i = 0; i < result.length; i++){
					//Send the ping
					AID aeronave = result[i].getName();
					ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
					request.setOntology("request-info");
					request.setConversationId(""+ System.currentTimeMillis());
					request.addReceiver(aeronave);
					myAgent.send(request);

					//receive the state
					MessageTemplate mt1 = MessageTemplate.MatchOntology("request-info");
					MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
					MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
					ACLMessage reply = blockingReceive(mt3);

					if(request!=null) {
						String[] content = reply.getContent().split("::");
						int x = Integer.parseInt(content[0]);
						int y = Integer.parseInt(content[1]);
						double distance = calculateDistance(posicao.getX(), posicao.getY(), x, y);

						if(distance <= protectedZone) {
							//mayday mayday
						} else if (distance < alertZone){
							int passengers = Integer.parseInt(content[2]);
							int dist_trav = Integer.parseInt(content[3]);
							int vel = Integer.parseInt(content[4]);
							boolean autority = calculateAutority(passengers, dist_trav, vel,x,y);
							if(autority) {
								//take action
								System.out.println("Reached Alert zone and will take actions");
							} else {
								//wait for instructions
								System.out.println("Reached Alert zone and will wait for instructions");
							}
						}

					}

				}
			} catch(Exception e){
				e.printStackTrace();
			}
		}

		private boolean calculateAutority(int p, int d, int v, int x, int y) {
			boolean result = false;
			double wA = nPassageiros*0.7+distPercorrida*0.3+velocidade*0.1;
			double wN = p*0.7+d*0.3+v*0.1;
			if(wA == wN) {
				// In case of draw, the one has great y wins.
				wA = wA + (posicao.getY());
				wN = wN + (y);
			}
			if(wA > wN)
				result = true;

			return result;

		}
	}

	class StartLandingBehav extends SimpleBehaviour{
		private boolean landed;

		@Override
		public void action() {
			if(rota.size() == 0) {
				landed = true;
				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.setOntology("info-land");
				request.setConversationId(""+ System.currentTimeMillis());
				request.addReceiver(aeroportoAtual);
				myAgent.send(request);
				
				System.out.println(myAgent.getLocalName()+ " has landed in " + aeroportoAtual.getLocalName());

				//send message to interface too

				ACLMessage info = new ACLMessage(ACLMessage.INFORM);
				info.setOntology("info-land");
				info.setConversationId(""+ System.currentTimeMillis());
				info.addReceiver(agent_interface);
				myAgent.send(info);
			} else {
				if(velocidade>1)
					velocidade--;
				myAgent.addBehaviour(new MovePositionBehav());
			}

		}

		@Override
		public boolean done() {
			if(landed) {
				myAgent.doDelete();

			}
			return landed;

		}
	}

	class WaitLandingBehav extends CyclicBehaviour{
		@Override
		public void action() {

		}
	}
	
	class SendStateToAirplaneBehav extends CyclicBehaviour{

		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("request-info");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage request = receive(mt3);

			if(request != null) {
				if(estado == 1) {
					ACLMessage reply = request.createReply();
					reply.setPerformative(ACLMessage.CONFIRM);
					reply.setContent(""+posicao.getX()+"::"+posicao.getY()+"::"+nPassageiros+"::"+distPercorrida+"::"+velocidade);
					reply.setConversationId(""+System.currentTimeMillis());
					myAgent.send(reply);
				}
			} else
				block();

		}

	}
	/**
	 * Process the replies of the airports and take specific actions.
	 *
	 */
	class ProcessAirportReplies extends CyclicBehaviour{

		private int nr_airports_inserted = 0;
		private int nr_meteo_received = 0;

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
						land_permission = 1;
						System.out.println(myAgent.getLocalName()+" got land clearance.");
					}

				} else if(ontology == "info-meteo"){
					//Process airport meteorology request
					AID airport = reply.getSender();
					String[] content = reply.getContent().split("::");

					int meteo = Integer.parseInt(content[2]);
					if(!airport.equals(destino)) {
						if(meteo == 3) {
							int x = Integer.parseInt(content[1]);
							int y = Integer.parseInt(content[1]);
							Position pos = new Position(x,y);
							mapa.setWeather(pos,true);
						} else {
							int x = Integer.parseInt(content[1]);
							int y = Integer.parseInt(content[1]);
							Position pos = new Position(x,y);
							mapa.setWeather(pos,false);
						}
					}
					condMeteoMap.replace(airport, meteo);
					nr_meteo_received++;
					
					if(nr_meteo_received >= condMeteoMap.size()) {
						//To make the route recalculation only after receiving meteo from every airport on path
						myAgent.addBehaviour(new CalculateRouteBehav());
						System.out.println(myAgent.getLocalName()+ " Meteorology changed: route recalculation.");
					}
				}
			} else {
				block();
			}
		}
	}
	
	private double calculateDistance(float dc_x, float dc_y, float dt_x, float dt_y){
		return Math.sqrt(Math.pow((dt_x - dc_x), 2) + Math.pow((dt_y - dc_y), 2));
	}
}
