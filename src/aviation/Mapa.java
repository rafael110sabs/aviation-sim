package aviation;


import jade.core.AID;

public class Mapa {
	
	private int[][] mapa;
	private AID[][] territorio;
	private final int terri_length = 200;
	
	public Mapa() {
		mapa = new int[1000][1000];
		territorio = new AID[1000][1000];
		initMap();
	}
	
	public void setAeroporto(Posicao pos, AID id) {
		mapa[pos.getX()][pos.getY()] = 1;
		setTerritorio(pos,id);
	}
	
	private void initMap() {
		for(int i = 0; i < 1000; i++)
			for(int j = 0; j < 1000; j++)
				mapa[i][j] = 0;
	}
	
	private void setTerritorio(Posicao pos, AID id) {
		int x = pos.getX()-(terri_length/2), y = pos.getY()-(terri_length/2);
		for(int i = 0; i < terri_length; i++) {
			for(int j = 0; j < terri_length; j++) {
				if(x+i >= 0 && y+i >= 0 && x+1 < 1000 && y+1 < 1000)
					territorio[x+i][y+1]= id;
			}
		}
	}

}
