package aviation;

import java.util.ArrayList;

import javax.swing.JTextField;

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import aviation.Aeronave.FetchAirportsBehav;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.*;

public class Interface extends Agent{
	/* com o qual o utilizador vai interagir
	com os AAs como forma de observar o status e tomada de decisão das aeronaves*/
	
	private ArrayList<AID> aeroportos, aeronaves;	
	private int birthed,takenoff,landed,collision;
	
	//criação do objeto painel.
    Panel panel=new Panel();	
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
        //invocaçao do painel
        panel.main(null);
        try {
            
            DFService.register(this, dfd);
            //this.addBehaviour(new makeChart());
            this.addBehaviour(new InfoBirth());
            this.addBehaviour(new InfoTakeOff());
            this.addBehaviour(new InfoLanding());
            this.addBehaviour(new InfoCollision());
            this.addBehaviour(new InfoState());
            this.addBehaviour(new InfoDecision());
            this.addBehaviour(new GetCommand());
            
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
	

	
	private class GetCommand extends CyclicBehaviour {
		public void action() {
			String command=panel.SendCommand();
			String nave="Aeronave"+command.split(" ")[1];
			
			/*
			 * Falta converter o texto em um AID para procurar no DF e devolver a mensagem a mostrar.
			 */
			
			
		try { //tenta encontrar nave	
			
			
			
			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.setOntology("request-state");
			request.setConversationId(""+ System.currentTimeMillis());
			request.addReceiver(reciever);
			send(request);
			
			
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
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
				collision++;
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

	class  StatLandings extends SimpleBehaviour{
		@Override
		public void action() {
		PieDataset data=createDataset();
		PieChart naves = new PieChart(data,"Aeronaves", "Descolagens/Aterragens de aeronaves");
		naves.pack();
		naves.setVisible(true);
		// TODO Auto-generated method stub
	}

				@Override
				public boolean done() {
					// TODO Auto-generated method stub
					return false;
				}

	}
	
	private  PieDataset createDataset() {
	   	
        DefaultPieDataset result = new DefaultPieDataset();  
            
        result.setValue("Aeronaves que descolaram", takenoff);
        result.setValue("Aeronaves que aterraram", landed);
        result.setValue("Aeronaves que nasceram", birthed);
        return result;

        }
	}

}
