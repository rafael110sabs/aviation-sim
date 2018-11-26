package aviation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import aviation.Aeronave.FetchAirportsBehav;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.*;

public class Interface extends Agent{
	/* com o qual o utilizador vai interagir
	com os AAs como forma de observar o status e tomada de decisão das aeronaves*/
	
	private ArrayList<AID> aeroportos, aeronaves;	
	private int birthed,takenoff,landed,colided;
	
	//criação do objeto painel.
    Panel panel=Panel.main(null);	
	@Override
	protected void setup() {
		
		aeroportos = new ArrayList<AID>();
		aeronaves = new ArrayList<AID>();
		
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("inteface");
        sd.setName("Interface");
        
        dfd.addServices(sd);

        try {
            
            DFService.register(this, dfd);
            //procurar aeroportos
            this.addBehaviour(new FetchAirportsBehav());
            
            //InfoXXX -> get information from other agents
            this.addBehaviour(new InfoBirth());
            this.addBehaviour(new InfoTakeOff());
            this.addBehaviour(new InfoLanding());
            this.addBehaviour(new InfoCollision());
            this.addBehaviour(new InfoState());
            this.addBehaviour(new InfoDecision());
            //get command from input
            this.addBehaviour(new GetCommand());
            //create pie chart with stats
            this.addBehaviour(new StatLandings(this,2000));
            
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
	
	
	private class GetCommand extends CyclicBehaviour {
		public void action() {
			String command=panel.SendCommand();
			try {
				/*
				 * Falta converter o texto em um AID para procurar no DF e devolver a mensagem a mostrar.
				 */
				
				String nave="Aeronave"+command.split(" ")[1];
				//tenta encontrar nave	
				
				
				
				/*ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.setOntology("request-state");
				request.setConversationId(""+ System.currentTimeMillis());
				request.addReceiver(reciever);
				send(request);*/
				block(5000);
				}
			catch(Exception e)
			{
				System.out.println("INTERFACE ->Esperando comando!Experimente: Aeronave X");
				panel.GetState(">Esperando comando!Experimente: Aeronave X");
				block(5000);
			}

				

				
		}
	}
	
	private class GetState extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-state");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) { 

				String[] parts = info.getContent().split("::");
				String detalhes=
						"Posição X:" + parts[1] + 
						" Y:" + parts[2] + 
						".Distância Percorrida:"+parts[3]+
						". Distância Prevista:"+ parts[4]+
						".Velocidade:"+parts[5];
				System.out.println("INTERFACE -> A nave: " + info.getSender().getLocalName()+" "+ parts[0] + "está ->"
						+ detalhes );
				//Enviar informaçao para painel
				panel.GetState("INTERFACE -> A nave: " + info.getSender().getLocalName()+" "+ parts[0] + "está ->"
						+ detalhes );
			}

		}
	}
	
	private class InfoBirth extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-birth");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) { 

				String[] parts = info.getContent().split("::");
				String detalhes=
						"Posição X:" + parts[1] + 
						" Y:" + parts[2] + 
						".Passageiros:"+parts[3]+
						". Origem:"+ parts[4];
				System.out.println("INTERFACE -> A nave originou: " + info.getSender().getLocalName()+" "+ parts[0] + " ->"
						+ detalhes );
				birthed++;
				/*PARA ENVENTUAIS ESTATISTICAS
				 * String[] parts = info.getContent().split("::");
				int n_passangers = Integer.parseInt(parts[3]);
				
				String agentname = info.getSender().getLocalName();
				agentname = agentname.substring(getLocalName().length() - 1);*/

			}

		}
	}
	
	private class InfoLanding extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-land");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
	
			if (info!=null) { 
				String[] parts = info.getContent().split("::");
				System.out.println("INTERFACE -> A nave "+ parts[0] +" aterrou em -> "
						+ parts[1]);
				landed++;
			}

		}
	}

	private class InfoTakeOff extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-takeoff");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) {  
				String[] parts = info.getContent().split("::");
				String detalhes=
						"Origem:"+parts[1]+
						" Destino:"+parts[2];
				System.out.println("INTERFACE -> A nave "+ parts[0] +" descolou -> " + detalhes);
				takenoff++;
				
			}

		}
	}

	private class InfoCollision extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-collision");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) {  
				String[] parts = info.getContent().split("::");
				String detalhes=
						"Posição X:" + parts[1] + 
						" Y:"+parts[2];
				System.out.println("INTERFACE -> A seguinte nave "+parts[0]+" colidiu-> " + detalhes);
				colided++;
			}

		}
	}
	
	private class InfoState extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-state");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) {  
				String[] parts = info.getContent().split("::");
				String detalhes = 
						"Posição X:"+parts[1] +
						" Y:" +parts[2] +
						". Distancia Percorrida:" +parts[3]+
						". Distancia Prevista:" +parts[4]+
						". Velocidade:"+parts[5];
						
				System.out.println("INTERFACE -> A nave "+parts[0]+" está -> "+detalhes);
			}

		}
	}
	
	private class InfoDecision extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-decision");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) {  
				String[] parts=info.getContent().split("::");
				String detalhes=
						"Posição X:"+parts[1] +
						" Y:" +parts[2] +
						". Decisão:"+parts[3];
						
				System.out.println("INTERFACE -> A nave "+parts[0]+" decidiu -> "+detalhes);
			}

		}
	}

	class  StatLandings extends TickerBehaviour{
		
		
		public StatLandings(Agent a, long period) {
			super(a, period);
		}

		PieDataset data=createDataset();	
		PieChart naves = new PieChart("Aeronaves", "Quantidades de aeronaves",data);
		public void onTick() {
		
		naves.setVisible(false);
		data=null;
		naves=null;
		block(2000);
		data=createDataset();	
		naves = new PieChart("Aeronaves", "Quantidades de aeronaves",data);
		naves.pack();
		naves.setVisible(true);
		
			}

	}
	
	private  PieDataset createDataset() {
	   	
        DefaultPieDataset result = new DefaultPieDataset();  
            
        result.setValue("Descolaram", takenoff);
        result.setValue("Aterraram", landed);
        result.setValue("Nasceram", birthed);
        result.setValue("Colidiram", colided);
        return result;

        }
	
}
