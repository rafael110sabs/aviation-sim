package aviation;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.*;

import jade.core.Timer;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
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
	com os AAs como forma de observar o status e tomada de decis�o das aeronaves*/
	
	private ArrayList<AID> aeroportos, aeronaves;
	private HashMap<AID, Position> aeronavesGUI, aeroportosGUI;
	private Draw draw;
	private JFrame frame;
	private int birthed,takenoff,landed,colided;
	
	//cria��o do objeto painel.
    Panel panel=Panel.main(null);	
	@Override
	protected void setup() {
		
		aeroportos = new ArrayList<AID>();
		aeronaves = new ArrayList<AID>();
		aeronavesGUI = new HashMap<>();
		aeroportosGUI = new HashMap<>();

		frame = new JFrame();
		draw = new Draw(aeronavesGUI, aeroportosGUI);
		frame.add(draw);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setPreferredSize(new Dimension(480, 480));
		
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("interface");
        sd.setName("Interface");
        
        dfd.addServices(sd);

        try {
            
            DFService.register(this, dfd);
            
            //InfoXXX -> get information from other agents
            this.addBehaviour(new InfoBirth());
            this.addBehaviour(new InfoTakeOff());
            this.addBehaviour(new InfoLanding());
            this.addBehaviour(new InfoCollision());
            this.addBehaviour(new InfoState());
            this.addBehaviour(new InfoDecision());
            this.addBehaviour(new InfoAirportState());
            //get command from input
            this.addBehaviour(new GetCommand());
            //get state from command
            this.addBehaviour(new GetState());
            //create pie chart with stats
            this.addBehaviour(new StatLandings(this,10000));
            
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
			try {
					/*
					 * Falta converter o texto em um AID para procurar no DF e devolver a mensagem a mostrar.
					 */
				String pedido = command.split(" ")[0];
					if(pedido.equals("Aeronave"))
					{
						String nave="Aeronave"+command.split(" ")[1];
						AID agent_nave= new AID();
						agent_nave.setLocalName(nave);
						//tenta encontrar nave	
						ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
						request.setOntology("request-state");
						request.setPerformative(ACLMessage.REQUEST);
						request.setConversationId(""+ System.currentTimeMillis());
						request.addReceiver(agent_nave);
						request.setContent("Pedido Estado!");
						send(request);
						
					}
					else {
					block(5000);
					}
				}
			catch(Exception e)
			{
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
						"Posicao X:" + parts[0] + 
						" Y:" + parts[1] + 
						".Distanncia Percorrida:"+parts[2]+
						". Distancia Prevista:"+ parts[3]+
						".Velocidade:"+parts[4];
				System.out.println("INTERFACE ++-> A nave: " + info.getSender().getLocalName()+" "+ parts[0] + "esta ->"
						+ detalhes );
				//Enviar informacao para painel
				panel.GetState("INTERFACE ++-> A nave: " + info.getSender().getLocalName()+" "+ parts[0] + "esta ->"
						+ detalhes );
			} else
				block();

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
						"Posicao X:" + parts[1] + 
						" Y:" + parts[2] + 
						".Passageiros:"+parts[3]+
						". Origem:"+ parts[4];
				System.out.println("INTERFACE -> A nave nasceu: " + info.getSender().getLocalName()+ " ->"
						+ detalhes );
				panel.GetState("INTERFACE -> A nave nasceu: " + info.getSender().getLocalName()+ " ->"
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
				String destino = info.getContent();
				System.out.println("INTERFACE -> A nave "+ info.getSender().getLocalName() +" aterrou em -> "
						+ destino);
				panel.GetState("INTERFACE -> A nave "+ info.getSender().getLocalName() +" aterrou em -> "
						+ destino);
				landed++;
				
				aeronavesGUI.remove(info.getSender());
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
						"Origem:"+parts[0]+
						" Destino:"+parts[1];
				System.out.println("INTERFACE -> A nave "+ info.getSender().getLocalName() +" descolou -> " + detalhes);
				panel.GetState("INTERFACE -> A nave "+ info.getSender().getLocalName() +" descolou -> " + detalhes);
				takenoff++;
				
			} else 
				block();

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
						"Posicao X:" + parts[0] + 
						" Y:"+parts[1];
				System.out.println("INTERFACE -> A seguinte nave "+info.getSender().getLocalName()+" colidiu-> " + detalhes);
				panel.GetState("INTERFACE -> A seguinte nave "+info.getSender().getLocalName()+" colidiu-> " + detalhes);
				colided++;
			} else
				block();

		}
	}
	
	private class InfoState extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchOntology("inform-state");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt3 = MessageTemplate.and(mt1,mt2);
			ACLMessage info = receive(mt3);
			
			if (info!=null) {
				AID aeronave = info.getSender();
				String[] parts = info.getContent().split("::");
				String detalhes = 
						"Posicao X:"+parts[0] +
						" Y:" +parts[1] +
						". Distancia Percorrida:" +parts[2]+
						". Distancia Prevista:" +parts[3]+
						". Velocidade:"+parts[4];
						
				System.out.println("INTERFACE -> A nave "+info.getSender().getLocalName()+" esta -> "+detalhes);

				int x = Integer.parseInt(parts[0]);
				int y = Integer.parseInt(parts[1]);
				Position pos = new Position(x,y);
				aeronavesGUI.put(aeronave, pos);
			} else 
				block();

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
				String deci = "";
				int decision = Integer.parseInt(parts[0]);
				switch(decision) {
					case 1: deci = "recalcular a rota.";
							break;
					case 2: deci = "acelerar.";
							break;
				}
				
						
				System.out.println("INTERFACE -> A nave "+parts[0]+" decidiu que a nave " + parts[1] + " deve "+ deci);
			} else
				block();

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

	class InfoAirportState extends CyclicBehaviour { 
		@Override
		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt2 = MessageTemplate.MatchOntology("info-airport");
			MessageTemplate mt3 = MessageTemplate.and(mt1, mt2);

			ACLMessage msg = receive(mt3);

			if(msg != null) {
				System.out.println("received airport");
				AID aeroporto = msg.getSender();
				String[] content_split = msg.getContent().split("::");
				int x = Integer.parseInt(content_split[0]);
				int y = Integer.parseInt(content_split[1]);
				Position pos = new Position(x,y);
				aeroportosGUI.put(aeroporto, pos);
			}else 
				block();
		}
	}
	
	private  PieDataset createDataset() {
	   	
        DefaultPieDataset result = new DefaultPieDataset();  
            
        result.setValue("Descolaram "+takenoff, takenoff);
        result.setValue("Aterraram "+landed, landed);
        result.setValue("Colidiram "+colided, colided);
        return result;

        }
	
}

class Draw extends JPanel {
	private static final int D_W = 480;
	private static final int D_H = 480;
	private HashMap<AID, Position> aeronaves;
	private HashMap<AID, Position> aeroportos;

	public Draw(HashMap<AID, Position> aeronaves, HashMap<AID, Position> aeroportos) {
		this.aeronaves = aeronaves;
		this.aeroportos = aeroportos;

		/* JPanel Properties */
		//setBackground(Color.BLACK);
		ActionListener listener = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		};

		javax.swing.Timer timer = new javax.swing.Timer(1000, listener);
		timer.start();
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		BufferedImage background = null;

		try {
			background = ImageIO.read(new File("img/map.png"));
		} catch (IOException e) {
			System.out.println(e);
		}
		g.drawImage(background, 0, 0, 480, 480, null);


		for (HashMap.Entry<AID, Position> entry : aeronaves.entrySet()) {
			String id = entry.getKey().getLocalName();
			int x = entry.getValue().getX();
			int y = entry.getValue().getY();
			BufferedImage img = null;

			try {
				img = ImageIO.read(new File("img/airplane.png"));
			} catch (IOException e) {
				System.out.println(e);
			}
			g.drawImage(img, x, y, 12, 12, null);
			g.setColor(Color.CYAN);
			g.drawString(id, x - 24, y - 5);
			g.drawRect(x - 2, y - 2, 16, 16);
		}

		for (Position pos : aeroportos.values()) {
			int x = pos.getX();
			int y = pos.getY();
			BufferedImage img = null;

			try {
				img = ImageIO.read(new File("img/airport.png"));
			} catch (IOException e) {
				System.out.println(e);
			}
			g.drawImage(img, x, y, 12, 12, null);
		}

		g.setColor(Color.GREEN);
		g.drawOval(240 - 60 , 240 - 60 , 120, 120);
		g.drawOval(240 - 120, 240 - 120 , 240, 240);
		g.drawOval(240 - 180, 240 - 180, 360, 360);
		g.drawOval(0, 0, 480, 480);
	}

	public Dimension getPreferredSize() {
		return new Dimension(D_W, D_H);
	}

}