package aviation;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Aeroporto extends Agent{
	
	private Posicao posicao;
	private int nPistasAterragem, nLimiteAero, nAeroEstacionadas, condMeteo;
	private ArrayList<AID> interessadasMeteo;
	private ArrayList<AID> pedidosDescolagem;
	
	
	@Override
	protected void setup() {
		
		
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
