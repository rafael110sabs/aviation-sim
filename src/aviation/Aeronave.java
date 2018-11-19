package aviation;

import java.util.ArrayList;

import jade.core.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Aeronave extends Agent{
	
	private Mapa mapa;
	private AID destino;
	private AID aeroportoAtual;
	private int alertZone, protectedZone, nPassageiros, distPercorrida, velocidade, distPrevista;
	private ArrayList<Integer> condMeteo;
	private Posicao posicao;
	private ArrayList<AID> aeroportos, aeronaves;
	private Posicao[] rota;
	private long tempoPartida, tempoVoo;
	
	
	
	@Override
	protected void setup() {
		mapa = new Mapa();
		
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("voo");
        sd.setName("Aeronave");
        
        dfd.addServices(sd);
        
        try {
            
            DFService.register(this, dfd);
            
            //Busca todos os aeroportos e preenche a lista e o mapa
            //Aleatoriamente escolhe dois aeroportos para partida e destino.
            //...
            
        } catch (FIPAException e) {
            e.printStackTrace();
        }
	}
	
}
