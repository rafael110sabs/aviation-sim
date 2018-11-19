package aviation;

import java.util.ArrayList;
import java.util.Random;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Aeronave extends Agent{
	
	private Map mapa;
	private AID destino;
	private AID aeroportoAtual;
	private int alertZone, protectedZone, nPassageiros, distPercorrida, velocidade, distPrevista;
	private ArrayList<Integer> condMeteo;
	private Position posicao;
	private Position pos_destino;
	private ArrayList<AID> aeroportos, aeronaves;
	private Position[] rota;
	private long tempoPartida, tempoVoo;
	
	
	
	@Override
	protected void setup() {
		mapa = new Map();
		aeroportos = new ArrayList<AID>();
		aeronaves = new ArrayList<AID>();
		Random rand = new Random();
		nPassageiros = rand.nextInt(50)+50;
		
		
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("voo");
        sd.setName("Aeronave");
        
        dfd.addServices(sd);
        
        try {
            
            DFService.register(this, dfd);
            
            //Fetch all the airports
            this.addBehaviour(new FetchAirportsBehav());
            //Pick origin and destination and their coordinates
            
            //Fill map with airports and territories (*)
            
            //Calculate route
            
            //Ask for permission
            
            
            
        } catch (FIPAException e) {
            e.printStackTrace();
        }
	}
	
	class FetchAirportsBehav extends SimpleBehaviour{
		// Fetch all the airports registered in the DF service and store their AID.

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
	
	class PickADBehav extends OneShotBehaviour{
		// Find the origin and destination airports.

		@Override
		public void action() {
			Random rand = new Random();
			// Randomly picks 2 indexes and assigns them to the actual and the destination airports
			while(true) {
				int o = rand.nextInt(aeroportos.size());
				int d = rand.nextInt(aeroportos.size());
				if(o != d) {
					aeroportoAtual = aeroportos.get(o);
					destino = aeroportos.get(d);
					break;
				}
			}
		}
	}
	
	class AirportRequestBehav extends OneShotBehaviour{
		// Send a request to all airports for their position
		// Can be a request for info or for meteo
		String ontology;
		public AirportRequestBehav(String ontology) {
			this.ontology = ontology;
		}

		@Override
		public void action() {
			for(AID aero: aeroportos) {
				// Request the position of the airport
				ACLMessage request_info = new ACLMessage(ACLMessage.REQUEST);
				request_info.setConversationId(""+System.currentTimeMillis());
				request_info.setOntology(ontology);
				request_info.addReceiver(aero);
				myAgent.send(request_info);
			}
		}
	}
	
	class ProcessAirportReplies extends CyclicBehaviour{
		// Process the replies of the airports to info and meteo updates and to proposes!

		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("request-info");
			MessageTemplate mt3 = MessageTemplate.MatchOntology("request-meteo");
			MessageTemplate mt4 = MessageTemplate.or(mt2,mt3);
			MessageTemplate mt5 = MessageTemplate.and(mt1,mt4);
			
			ACLMessage reply = receive(mt5);
			
			/*Stopped here because i didn't knew where i was going.*/
			
			
		}
		
		
	}
}
