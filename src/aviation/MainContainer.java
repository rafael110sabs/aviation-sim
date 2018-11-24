package aviation;


import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;


public class MainContainer {

	Runtime rt;
	ContainerController container;

	public ContainerController initContainerInPlatform(String host, String port, String containerName) {
		// Get the JADE runtime interface (singleton)
		this.rt = Runtime.instance();

		// Create a Profile, where the launch arguments are stored
		Profile profile = new ProfileImpl();
		profile.setParameter(Profile.CONTAINER_NAME, containerName);
		profile.setParameter(Profile.MAIN_HOST, host);
		profile.setParameter(Profile.MAIN_PORT, port);
		// create a non-main agent container
		ContainerController container = rt.createAgentContainer(profile);
		return container;
	}

	public void initMainContainerInPlatform(String host, String port, String containerName) {

		// Get the JADE runtime interface (singleton)
		this.rt = Runtime.instance();

		// Create a Profile, where the launch arguments are stored
		Profile prof = new ProfileImpl();
		prof.setParameter(Profile.CONTAINER_NAME, containerName);
		prof.setParameter(Profile.MAIN_HOST, host);
		prof.setParameter(Profile.MAIN_PORT, port);
		prof.setParameter(Profile.MAIN, "true");
		prof.setParameter(Profile.GUI, "true");

		// create a main agent container
		this.container = rt.createMainContainer(prof);
		rt.setCloseVM(true);

	}

	public void startAgentInPlatform(String name, String classpath, Object[] arguments) {
		try {
			AgentController ac = container.createNewAgent(name, classpath, arguments);
			ac.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		MainContainer a = new MainContainer();
		a.initMainContainerInPlatform("localhost", "9888", "MainContainer");
		int ap = 0;
	    
	    for(int i = 0; /*i <= 480*/ i <= 120; i+=120) {
	    	for(int j = 0; /*j <= 480*/ j<= 120; j+=120) {
	    	    a.startAgentInPlatform("Aeroporto-"+ap, "aviation.Aeroporto", new Object[] {i,j});
	    	    ap++;
	    	}
	    }
	    
	    for(int i = 0; i < 2; i++) {
	    	a.startAgentInPlatform("Aeronave"+i, "aviation.Aeronave", new Object[] {});	
	    }
	    //a.startAgentInPlatform("DI-Sensor", "ficha5.TemperatureSensorAgent");
		
		
		
		
		
		
		/*
		// Example of Container Creation (not the main container)
		ContainerController newcontainer = a.initContainerInPlatform("localhost", "9888", "OtherContainer");
		
		// Example of Agent Creation in new container
		try {
			AgentController ag = newcontainer.createNewAgent("agentnick", "ReceiverAgent", new Object[] {});// arguments
			ag.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		*/
	}
}