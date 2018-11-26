package aviation;


import java.util.ArrayList;
import java.util.List;

import jade.core.AID;

public class Map {
	
	private final int AVOID = 12;
	private final int TERRI_LENGTH = 80;
	private final int RESTRICTED_ZONE = 20;
	private final int MAP_SIZE = 481;
	private int[][] map;
	private AID[][] territory;
	
	public Map() {
		map = new int[MAP_SIZE][MAP_SIZE];
		territory = new AID[MAP_SIZE][MAP_SIZE];
		initMap();
	}
	
	public void setAirport(Position pos, AID id, boolean restricted) {
		setTerritory(pos,id);
		if(restricted) {
			setRestrictedZone(pos);
		}
	}
	
	public ArrayList<Position> calculateRoute(Position atual_pos, Position dest_pos) {
		Node initialNode = new Node(atual_pos.getX(), atual_pos.getY());
        Node finalNode = new Node(dest_pos.getX(), dest_pos.getY());
        int rows = MAP_SIZE, cols = MAP_SIZE;
        AStar aStar = new AStar(rows, cols, map, initialNode, finalNode, 1, 2);
        List<Node> path = aStar.findPath();
        ArrayList<Position> pos = new ArrayList<Position>();
        for (Node node : path) {
            Position p = new Position(node.getRow(), node.getCol());
            pos.add(pos.size(),p);
        }
        return pos;
	}
	
	public ArrayList<Position> avoidCrash(Position atual_pos, Position crash, Position dest_pos) {
		Node initialNode = new Node(atual_pos.getX(), atual_pos.getY());
        Node finalNode = new Node(dest_pos.getX(), dest_pos.getY());
        int rows = MAP_SIZE, cols = MAP_SIZE;
        setAvoid(crash);
        AStar aStar = new AStar(rows, cols, map, initialNode, finalNode, 1, 2);
        List<Node> path = aStar.findPath();
        ArrayList<Position> pos = new ArrayList<Position>();
        for (Node node : path) {
            Position p = new Position(node.getRow(), node.getCol());
            pos.add(pos.size(),p);
        }
        return pos;
	}
	
	private void setAvoid(Position pos) {
		int x = pos.getX()-(AVOID/2), y = pos.getY()-(AVOID/2);
		for(int i = 0; i < AVOID; i++) {
			for(int j = 0; j < AVOID; j++) {
				//System.out.println((x+i) + " " + (y+j));
				if(x+i >= 0 && y+j >= 0 && x+i < MAP_SIZE && y+j < MAP_SIZE)
					map[x+i][y+j]= 1;
			}
		}
	}
	
	private void initMap() {
		for(int i = 0; i < MAP_SIZE; i++)
			for(int j = 0; j < MAP_SIZE; j++)
				map[i][j] = 0;
	}
	/**
	 * Set the territory to an AID
	 * @param pos
	 * @param id
	 */
	private void setTerritory(Position pos, AID id) {
		int x = pos.getX()-(TERRI_LENGTH/2), y = pos.getY()-(TERRI_LENGTH/2);
		for(int i = 0; i < TERRI_LENGTH; i++) {
			for(int j = 0; j < TERRI_LENGTH; j++) {
				if(x+i >= 0 && y+j >= 0 && x+i < MAP_SIZE && y+j < MAP_SIZE)
					territory[x+i][y+j]= id;
			}
		}
	}
	
	public AID getTerritory(Position pos) {
		return  territory[pos.getX()][pos.getY()];
	}
	
	/**
	 * Change the corresponding territory to restricted.
	 * @param pos
	 */
	public void setWeather(Position pos, boolean bad) {
		int x = pos.getX()-(TERRI_LENGTH/2), y = pos.getY()-(TERRI_LENGTH/2);
		for(int i = 0; i < TERRI_LENGTH; i++) {
			for(int j = 0; j < TERRI_LENGTH; j++) {
				if(x+i >= 0 && y+j >= 0 && x+i < MAP_SIZE && y+j < MAP_SIZE)
					if(bad)
						map[x+i][y+j] = 1;
			}
		}
	}
	
	private void setRestrictedZone(Position pos) {
		int x = pos.getX()-(RESTRICTED_ZONE/2), y = pos.getY()-(RESTRICTED_ZONE/2);
		for(int i = 0; i < RESTRICTED_ZONE; i++) {
			for(int j = 0; j < RESTRICTED_ZONE; j++) {
				//System.out.println((x+i) + " " + (y+j));
				if(x+i >= 0 && y+j >= 0 && x+i < MAP_SIZE && y+j < MAP_SIZE)
					map[x+i][y+j]= 1;
			}
		}
	}

}
