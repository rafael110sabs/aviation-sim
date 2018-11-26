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
	private int nr_airplanes;
	private HashMap<AID, ACLMessage> airplanes;
	private boolean action;

	@Override
	protected void setup() {
		land_permission = 0;
		mapa = new Map();
		estado = 0;
		rota = new ArrayList<Position>();
		Random rand = new Random();
		nPassageiros = rand.nextInt(50)+50;
		alertZone = 16.0;
		protectedZone = 4.0;
		agent_interface = new AID();
		agent_interface.setLocalName("Interface");
		aeroportos = new ArrayList<AID>();
		condMeteoMap = new HashMap<AID,Integer>();
		changing_route = false;
		first_pass = true;
		nr_airplanes = 0;
		airplanes = new HashMap<AID, ACLMessage>();
		action = false;
		



		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("parking");
		sd.setName("Aeronave");

		dfd.addServices(sd);

		try {

			DFService.register(this, dfd);

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
			//Answer user command
			this.addBehaviour(new AnswerState());

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

			//System.out.println(aeroportoAtual.getLocalName() + " -> " + destino.getLocalName());

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
//					System.out.println(""+myAgent.getLocalName()+" has born in " + airport.getLocalName());
					
					ACLMessage info_interface = new ACLMessage(ACLMessage.INFORM);
					info_interface.setOntology("inform-birth");
					info_interface.setContent(myAgent +"::" + pos.getX() + "::" + pos.getY() + "::" + nPassageiros + "::"+aeroportoAtual.getLocalName());
					info_interface.addReceiver(agent_interface);
					myAgent.send(info_interface);
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


	// Last bug was of a route that never stopped.

	class CalculateRouteBehav extends OneShotBehaviour{

		@Override
		public void action() {
			changing_route = true;
//			System.out.println(myAgent.getLocalName() + ": Started calculting route.");
			rota = mapa.calculateRoute(posicao, pos_destino);
			distPrevista = rota.size();
//			System.out.println(myAgent.getLocalName() + ": Finished calculting route. Distance: "+distPrevista);

			if(first_pass) {
				//Get weather on route
				//Only after the first route calculation
				Position init;
				AID lastTerritory = aeroportoAtual;
				AID thisTerritory;
				ACLMessage meteo = new ACLMessage(ACLMessage.REQUEST);
				meteo.setOntology("request-meteo");
				meteo.setConversationId(""+System.currentTimeMillis());
				//System.out.println(myAgent.getLocalName()+": in "+aeroportoAtual.getLocalName() +" requested:");

				for(int i = 20; i < rota.size(); i+=20) {
					init = rota.get(i);
					thisTerritory = mapa.getTerritory(init);
					if(thisTerritory != null && !thisTerritory.equals(lastTerritory) && !thisTerritory.equals(destino)) {
						meteo.addReceiver(thisTerritory);
						lastTerritory = thisTerritory;
						condMeteoMap.put(thisTerritory, 0);
					}
				}
				//System.out.println(myAgent.getLocalName() + ": Requested weather of " + condMeteoMap.size());
				//in case there is no need to route recal
				if(condMeteoMap.size() == 0)
					changing_route = false;
				else
					myAgent.send(meteo);
				first_pass = false;

			} else {
				changing_route = false;

			}

			if(!changing_route) {
				//At this point, its safe to ask to take off
				ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
				request.setOntology("propose-takeoff");
				request.setConversationId(""+ System.currentTimeMillis());
				request.addReceiver(aeroportoAtual);
				myAgent.send(request);
//				System.out.println(myAgent.getLocalName() + ": Requested take off. Distance: "+distPrevista);
			}
		}

	}

	/**
	 *  Initiate the takeoff of the airplane
	 *
	 */
	class StartTakeOffBehav extends SimpleBehaviour{
		private boolean takedoff;

		public StartTakeOffBehav(Agent a) {
			super(a);
			takedoff = false;
		}

		@Override
		public void action() {
			if(!changing_route) {
				tempoPartida = System.currentTimeMillis();
				estado = 1;
				velocidade = 1;

				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(getAID());
				ServiceDescription sd = new ServiceDescription();
				sd.setType("voo");
				sd.setName("Aeronave");
				dfd.addServices(sd);

				try {
					DFService.deregister(myAgent);
					DFService.register(myAgent, dfd);
				} catch (FIPAException e) {
					e.printStackTrace();
				}

				ACLMessage request = new ACLMessage(ACLMessage.INFORM);
				request.setOntology("info-takeoff");
				request.setConversationId(""+ System.currentTimeMillis());
				request.addReceiver(aeroportoAtual);
				myAgent.send(request);

				//send message to interface too

				ACLMessage info = new ACLMessage(ACLMessage.INFORM);
				info.setOntology("inform-takeoff");
				info.setContent(""+aeroportoAtual.getLocalName() + "::"+destino.getLocalName());
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

				if((distPrevista - distPercorrida ) >= 35) {
					if(!action)
						myAgent.addBehaviour(new BeaconBehav());
					//					myAgent.addBehaviour(new MovePositionBehav());
				} else {
					if(land_permission == 2)
						myAgent.addBehaviour(new WaitLandingBehav());
					else if(land_permission == 1) {
						// start landing
						if(rota.size() == 0) {
							//landed = true;
							ACLMessage request = new ACLMessage(ACLMessage.INFORM);
							request.setOntology("info-land");
							request.setConversationId(""+ System.currentTimeMillis());
							request.addReceiver(aeroportoAtual);
							myAgent.send(request);

							System.out.println(myAgent.getLocalName()+ " has landed in " + aeroportoAtual.getLocalName());

							//send message to interface too

							ACLMessage info = new ACLMessage(ACLMessage.INFORM);
							info.setOntology("inform-land");
							info.setContent(""+destino.getLocalName());
							info.setConversationId(""+ System.currentTimeMillis());
							info.addReceiver(agent_interface);
							myAgent.send(info);

							myAgent.doDelete();

						} else {
							if(velocidade>1)
								velocidade--;
							myAgent.addBehaviour(new MovePositionBehav());
						}
					}
				}
			}
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
				ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.setOntology("request-info");

				for(int i = 0; i < result.length; i++){
					//Send the ping
					AID aeronave = result[i].getName();

					if(!aeronave.equals(myAgent.getAID())) {
						// dont send to myself
						request.addReceiver(aeronave);

					}
				}
				nr_airplanes = result.length - 1;
				if(nr_airplanes > 0) {
					request.setConversationId(""+System.currentTimeMillis());
					myAgent.send(request);

				} else {
					myAgent.addBehaviour(new MovePositionBehav());
				}
				
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	class RadarBehav extends OneShotBehaviour{

		@Override
		public void action() {

			for(ACLMessage reply: airplanes.values()) {
				String[] content = reply.getContent().split("::");
				int airplane_x = Integer.parseInt(content[0]);
				int airplane_y = Integer.parseInt(content[1]);
				double distance = calculateDistance(posicao.getX(), posicao.getY(), airplane_x, airplane_y);

				if(distance <= protectedZone) {
					// Protected zone
					System.out.println(myAgent.getLocalName()+": Mayday, we're going down. " + rota.size());
					
					ACLMessage info = new ACLMessage(ACLMessage.INFORM);
					info.setOntology("inform-collision");
					info.setContent(""+posicao.getX() + "::"+posicao.getY());
					info.setConversationId(""+ System.currentTimeMillis());
					info.addReceiver(agent_interface);
					myAgent.send(info);

				} else if (distance <= alertZone){
					//Alert Zone
//					System.out.println(myAgent.getLocalName()+": ALERT ZONE");
					int passengers = Integer.parseInt(content[2]);
					int dist_trav = Integer.parseInt(content[3]);
					int vel = Integer.parseInt(content[4]);
					int airplane_next_x = Integer.parseInt(content[5]);
					int airplane_next_y = Integer.parseInt(content[6]);
					boolean autority = calculateAutority(passengers, dist_trav, vel,airplane_x,airplane_y);
//					System.out.println(myAgent.getLocalName() + ": distance:" + distance + " " + rota.size());

					//Get the next position
					Position next_pos = rota.get(velocidade*8-1);
					double next_distance = calculateDistance(next_pos.getX(), next_pos.getY(), airplane_next_x, airplane_next_y);
//					System.out.println(myAgent.getLocalName() + ": next_distance:" + next_distance + "  " + rota.size());

					//If in the next position they get to close to each other
					if(next_distance <= protectedZone) {
						System.out.println(myAgent.getLocalName() + ": Crash at" + next_pos.getX() + " " + next_pos.getY());
						if(autority) {
							
							int decision = 0; // 0->do nothing, 1-> change route, 2-> slow down
							//Get my direction
							int h_dir = 0;  // -1 ->left, 0->steady, 1->right
							int v_dir = 0;  // -1 ->down, 0->steady, 1->up
							if(posicao.getX() -  next_pos.getX() < 0)
								h_dir = 1; 
							else if(posicao.getX() -  next_pos.getX() > 0)
								h_dir = -1;

							if(posicao.getY() -  next_pos.getY() < 0)
								v_dir = 1;
							else if( posicao.getY() -  next_pos.getY() > 0)
								v_dir = -1;

							// Get his direction
							int airplane_v_dir = 0;  // -1 ->left, 0->steady, 1->right 
							int airplane_h_dir = 0;  // -1 ->down, 0->steady, 1->up
							if(airplane_x -  airplane_next_x < 0)
								airplane_h_dir = 1;
							else if(airplane_x -  airplane_next_x > 0)
								airplane_h_dir = -1;

							if(airplane_y -  airplane_next_y < 0)
								airplane_v_dir = 1;
							else if( airplane_y -  airplane_next_y > 0)
								airplane_v_dir = -1;
							
							System.out.println(myAgent.getLocalName() + ": direction " + h_dir + " " + v_dir);
							System.out.println(reply.getSender().getLocalName() + ": direction " + airplane_h_dir + " " + airplane_v_dir);

							//if there is a imminent front collision
							if( (h_dir == 1 && airplane_h_dir == -1) || (h_dir == -1 && airplane_h_dir == 1) ||
									(v_dir == 1 && airplane_v_dir ==-1) || (v_dir == -1 && airplane_v_dir == 1)) {
								decision = 1;
							} else if((h_dir == 1 && airplane_h_dir == 1) || (h_dir == 1 && airplane_h_dir == 1) ||
									(v_dir == 1 && airplane_v_dir == 1) || (v_dir == -1 && airplane_v_dir == -1)) {
								decision = 2;

							}
							
							//Send decision
							ACLMessage contract = reply.createReply();
							contract.setPerformative(ACLMessage.INFORM);
							contract.setOntology("info-decision");
							contract.setContent(""+decision+"::"+airplane_next_x+"::"+airplane_next_y);
							contract.setConversationId(""+System.currentTimeMillis());
							myAgent.send(contract);
							System.out.println(myAgent.getLocalName() + ": sent action to -> " + reply.getSender().getLocalName());
							
							ACLMessage info = new ACLMessage(ACLMessage.INFORM);
							info.setOntology("inform-decision");
							info.setContent(""+decision+"::"+myAgent.getLocalName()+"::"+ reply.getSender().getLocalName());
							info.setConversationId(""+ System.currentTimeMillis());
							info.addReceiver(agent_interface);
							myAgent.send(info);

						}
					}
				}
				myAgent.addBehaviour(new MovePositionBehav());
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
	
	class MovePositionBehav extends OneShotBehaviour{

		@Override
		public void action() {
			posicao = rota.get((1 * velocidade)-1);
			for(int i = 0; i < velocidade; i++)
				rota.remove(i);
			distPercorrida += velocidade;
			aeroportoAtual = mapa.getTerritory(posicao);

			//			if(rota.size() % 10 == 0)
//			System.out.println(myAgent.getLocalName() + ": x=" + posicao.getX() + " y="+posicao.getY() + " distance left " + (distPrevista - distPercorrida ) + " at speed " + velocidade);

			if((distPrevista - distPercorrida ) <= 40) {
				if(land_permission == 0 && aeroportoAtual != null) {

					ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
					request.setOntology("propose-land");
					request.setConversationId(""+ System.currentTimeMillis());
					request.addReceiver(destino);
					myAgent.send(request);
					land_permission = 2;
					estado = 2;

					System.out.println(myAgent.getLocalName() + " asked to land in " + aeroportoAtual.getLocalName());
				}
			}

			ACLMessage info = new ACLMessage(ACLMessage.INFORM);
			info.setOntology("inform-state");
			info.setConversationId(""+ System.currentTimeMillis());
			info.setContent(posicao.getX() + "::" + posicao.getY() + "::" + distPercorrida + "::" + distPrevista + "::" + velocidade);
			info.addReceiver(agent_interface);
			myAgent.send(info);
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
		private int nr_meteo_received = 0;

		@Override
		public void action() {

			ACLMessage reply = receive();

			if(reply != null) {
				String ontology = reply.getOntology();
				int perf = reply.getPerformative();
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
						//System.out.println(myAgent.getLocalName() + "Received position from "+nr_airports_inserted+"/"+(aeroportos.size()-1));

						//Start route calculation
						changing_route = true;
						myAgent.addBehaviour(new CalculateRouteBehav());

					}

				} else if(ontology == "propose-takeoff") {

					if(perf == ACLMessage.ACCEPT_PROPOSAL) {
						// If it has received order to take-off and route is calculated then start take-off
//						System.out.println(myAgent.getLocalName() + ": Received take off clearance");
						if(rota.size() > 0) {
//							System.out.println(myAgent.getLocalName() + ": Initiated take off");
							myAgent.addBehaviour(new StartTakeOffBehav(myAgent));
						}
					}

				} else if(ontology == "propose-land"){

					if(perf == ACLMessage.ACCEPT_PROPOSAL) {
						land_permission = 1;
						System.out.println(myAgent.getLocalName()+" got land clearance.");
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
						mapa.setWeather(pos,true);
					} else {
						int x = Integer.parseInt(content[1]);
						int y = Integer.parseInt(content[1]);
						Position pos = new Position(x,y);
						mapa.setWeather(pos,false);
					}
					condMeteoMap.replace(airport, meteo);
					nr_meteo_received++;
					if(nr_meteo_received == condMeteoMap.size()) {
						//To make the route recalculation only after receiving meteo from every airport on path
						myAgent.addBehaviour(new CalculateRouteBehav());
						System.out.println(myAgent.getLocalName()+ " Meteorology changed: route recalculation.");

					}
				} else if(ontology == "request-info"){
					if(perf == ACLMessage.REQUEST) {
						
						ACLMessage info = reply.createReply();
						info.setPerformative(ACLMessage.CONFIRM);
						info.setOntology("request-info");
						Position next_pos = rota.get(velocidade*8-1);
						info.setContent(posicao.getX()+"::"+posicao.getY()+"::"+nPassageiros+"::"+distPercorrida+"::"+velocidade+"::"+next_pos.getX()+"::"+next_pos.getY());
						info.setConversationId(""+System.currentTimeMillis());
						myAgent.send(info);
						
					} else if(perf == ACLMessage.CONFIRM) {
						airplanes.put(reply.getSender(), reply);
//						System.out.println(myAgent.getLocalName() + " NrAirplanes " + nr_airplanes + " airplanes " + airplanes.size());
						if(nr_airplanes == airplanes.size()) {
							myAgent.addBehaviour(new RadarBehav());
						}
					}
					
				} else if(ontology == "info-decision") {
					// Receiving decisions from authoritarian
					System.out.println("Received decision" + reply);
					String[] content = reply.getContent().split("::");
					int aut_decision = Integer.parseInt(content[0]);
					int next_x = Integer.parseInt(content[1]);
					int next_y = Integer.parseInt(content[2]);
					Position next_airplane_pos = new Position(next_x, next_y);
					if(aut_decision == 1) {
						// recalculate
						System.out.println(myAgent.getLocalName()+": Recalculating"); 
						ArrayList<Position> detour = mapa.avoidCrash(posicao, next_airplane_pos, rota.get((velocidade*20)-1));
						System.out.println(""+detour.size());
						for(int j = 0; j < velocidade*20-1; j++) {
							rota.remove(j);
							rota.add(j,detour.get(j));
						}
						velocidade++;
					} else if(aut_decision == 2) {
						velocidade = 3;
					}
				}
			} else {
				block();
			}
		}
	}

	class AnswerState extends CyclicBehaviour {

		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("request-state");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) {
				ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
				info.setOntology("inform-state");
				info.setConversationId(""+ System.currentTimeMillis());
				info.setContent(posicao.getX() + "::" + posicao.getY() + "::" + distPercorrida + "::" + distPrevista + "::" + velocidade);
				info.addReceiver(agent_interface);
				myAgent.send(message);
			}
			else
			{
				block();
			}
			
		}

	
	}
	
	private double calculateDistance(double dc_x, double dc_y, double dt_x, double dt_y){
		return Math.sqrt(Math.pow((dt_x - dc_x), 2) + Math.pow((dt_y - dc_y), 2));
	}
}
