package aviation;


import jade.core.AID;

public class Map {
	
	private int[][] map;
	private AID[][] territory;
	private final int TERRI_LENGTH = 200;
	private final int RESTRICTED_ZONE = 60;
	private final int DESTINY = 3;
	private final int ORIGIN = 2;
	
	public Map() {
		map = new int[1000][1000];
		territory = new AID[1000][1000];
		initMap();
	}
	
	public void setAirport(Position pos, AID id, boolean restricted) {
		map[pos.getX()][pos.getY()] = 1;
		setTerritory(pos,id);
		if(restricted) {
			setRestrictedZone(pos);
		}
	}
	
	public void setDestiny(Position pos) {
		map[pos.getX()][pos.getY()] = DESTINY;
	}
	public void setOrigin(Position pos) {
		map[pos.getX()][pos.getY()] = ORIGIN;
	}
	
	private void initMap() {
		for(int i = 0; i < 1000; i++)
			for(int j = 0; j < 1000; j++)
				map[i][j] = 0;
	}
	
	private void setTerritory(Position pos, AID id) {
		int x = pos.getX()-(TERRI_LENGTH/2), y = pos.getY()-(TERRI_LENGTH/2);
		for(int i = 0; i < TERRI_LENGTH; i++) {
			for(int j = 0; j < TERRI_LENGTH; j++) {
				if(x+i >= 0 && y+j >= 0 && x+i < 1000 && y+j < 1000)
					territory[x+i][y+j]= id;
			}
		}
	}
	
	private void setRestrictedZone(Position pos) {
		int x = pos.getX()-(RESTRICTED_ZONE/2), y = pos.getY()-(RESTRICTED_ZONE/2);
		for(int i = 0; i < RESTRICTED_ZONE; i++) {
			for(int j = 0; j < RESTRICTED_ZONE; j++) {
				if(x+i >= 0 && y+i >= 0 && x+1 < 1000 && y+1 < 1000)
					map[x+i][y+i]= 1;
			}
		}
	}

}
