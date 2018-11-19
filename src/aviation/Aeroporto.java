package aviation;

import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Aeroporto extends Agent{
	
	// CondMeteo: 0-> chuva, 1->sol, 2->nublado, 3->tempestadão
	
	private Position posicao;
	private int nPistasAterragem, nLimiteAero, nAeroEstacionadas, condMeteo;
	private ArrayList<AID> interessadasMeteo;
	private ArrayList<AID> pedidosDescolagem;
	
	
	@Override
	protected void setup() {
		
		Random rand = new Random();
		condMeteo = rand.nextInt(3); // Entre 0 e 2
		nPistasAterragem = rand.nextInt(3); // Entre 0 e 2
		nLimiteAero = rand.nextInt(11)+10; // Entre 10 e 20
		nAeroEstacionadas = 0;
		
		
		DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("estacao");
        sd.setName("Aeroporto");
        
        dfd.addServices(sd);
        
        try {
            
            DFService.register(this, dfd);
            
        } catch (FIPAException e) {
            e.printStackTrace();
        }
	}

}
