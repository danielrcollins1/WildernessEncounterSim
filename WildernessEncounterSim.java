import java.util.*;
import java.io.IOException; 

/******************************************************************************
*  Simulator for OD&D wilderness encounters.
*
*  @author   Daniel R. Collins (dcollins@superdan.net)
*  @since    2021-12-05
******************************************************************************/

public class WildernessEncounterSim {

	//--------------------------------------------------------------------------
	//  Constants
	//--------------------------------------------------------------------------

	final int NUM_ENCOUNTERS = 1000;
	final int MONSTER_NUM_COL = 1;
	final int MONSTER_HDN_COL = 12;
	final int MONSTER_EHD_COL = 13;

	//--------------------------------------------------------------------------
	//  Fields
	//--------------------------------------------------------------------------

	/** Encounters main table. */
	Table mainTable;
	
	/** Encounters sub tables. */
	Table subTable;

	/** Monster database table. */
	Table monsterTable;

	/** Terrain for encounters. */
	String terrain;

	/** Main table index for terrain. */
	int terrainIndex;

	/** Flag to escape after parsing arguments. */
	boolean exitAfterArgs;

	//--------------------------------------------------------------------------
	//  Constructors
	//--------------------------------------------------------------------------

	/**
	*  Constructor.
	*/
	public WildernessEncounterSim () throws IOException {
		Dice.initialize();
		mainTable = new Table("WildMainTable.csv");
		subTable = new Table("WildSubTable.csv");
		monsterTable = new Table("MonsterDatabase.csv");
	}

	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------

	/**
	*  Print program banner.
	*/
	void printBanner () {
		System.out.println("OED Wilderness Encounter Simulator");
		System.out.println("----------------------------------");
	}

	/**
	*  Print usage.
	*/
	public void printUsage () {
		System.out.println("Usage: WildernessEncounters terrain");
		System.out.println();
	}

	/**
	*  Parse arguments.
	*/
	public void parseArgs (String[] args) {
		if (args.length != 1) {
			exitAfterArgs = true;
		}
		else {
			terrain = args[0];
			terrainIndex = mainTable.getColFromName(terrain);
			if (terrainIndex < 1) {
				System.err.println("Unknown terrain: " + terrain);
				exitAfterArgs = true;
			}
		}
	}

	/**
	*  Roll encounter for the object terrain.
	*  @return Total EHD of encounter.
	*/
	int rollEncounterByTerrain () {

		// Roll subtable name, look up subtable
		String subTableName = mainTable.getRandomEntryOnCol(terrainIndex);
		subTableName = subTableFixup(terrain, subTableName);
		return rollEncounterBySubTable(subTableName);
	}

	/**
	*  Special fixups to determine exact subtable.
	*/
	String subTableFixup(String table, String sub) {

		// Find men subtable by terrain.
		if (sub.equals("Men")) {
			if (table.equals("Mountain")) return "Men Mountain";
			if (table.equals("Desert")) return "Men Desert";
			if (table.equals("River")) return "Men Water";
			return "Men Typical";
		}	
		return sub;	
	}

	/**
	*  Roll encounter for a named subtable.
	*  @return Total EHD of encounter.
	*/
	int rollEncounterBySubTable (String tableName) {
		//System.out.println(tableName);

		// Get subtable column
		int colIndex = subTable.getColFromName(tableName);
		if (colIndex < 1) {
			System.err.println("Unknown subtable: " + tableName);
			return 0;
		}

		// Roll monster name, look up monster
		String monsterName = subTable.getRandomEntryOnCol(colIndex);
		monsterName = monsterFixup(monsterName);
		return rollEncounterByMonster(monsterName);
	}

	/**
	*  Special fixups to determine exact monster type.
	*/
	String monsterFixup(String monName) {

		// Find giant subtype.
		if (monName.equals("Giant")) {
			switch(Dice.roll(10)) {
				default: return "Giant, Hill";
				case 7: return "Giant, Stone";
				case 8: return "Giant, Frost";
				case 9: return "Giant, Fire";
				case 10: return "Giant, Cloud";
			}		
		}
		
		// Find dragon subtype.
		if (monName.equals("Dragon")) {
			switch(Dice.roll(6)) {
				case 1: return "Dragon, White";
				case 2: return "Dragon, Black";
				case 3: return "Dragon, Green";
				case 4: return "Dragon, Blue";
				case 5: return "Dragon, Red";
				case 6: return "Dragon, Gold";
			}		
		}

		// Find specific giant animal.
		if (monName.equals("Giant Snake")) return "Giant Snake, Constrictor";
		if (monName.equals("Giant Beetle")) return "Giant Beetle, Bombardier";
		if (monName.equals("Giant Ant")) return "Giant Ant, Worker";
		if (monName.equals("Sea Monster")) return "Sea Monster, Small";
		if (monName.equals("Hydra")) return "Hydra, 10 Heads";
		if (monName.equals("Roc")) return "Roc, Small";		
		return monName;	
	}

	/**
	*  Roll up an encounter for a given monster type.
	*  @return Total EHD of encounter.
	*/
	int rollEncounterByMonster (String monsterName) {
		//System.out.println(monsterName);

		// Special handling for NPC types
		int NPCValue = handleNPCType(monsterName);
		if (NPCValue > 0) return NPCValue;

		// Find index in monster table
		int monsterIndex = monsterTable.getRowFromName(monsterName);
		if (monsterIndex < 1) {
			System.err.println("Unknown monster: " + monsterName);
			return 0;
		}

		// Roll number appearing
		Dice numDice = new Dice(monsterTable.getEntry(monsterIndex, MONSTER_NUM_COL));
		int number = numDice.roll();
		
		// Get the EHD value
		int EHD = strToInt(monsterTable.getEntry(monsterIndex, MONSTER_EHD_COL));
		EHD = EHDFixup(monsterName, EHD);
		if (EHD == 0) {
			System.err.println("Monster with null EHD: " + monsterName);		
		}

		// Compute the product
		int totalEHD = number * EHD;
		double hitDiceNum = strToDbl(monsterTable.getEntry(monsterIndex, MONSTER_HDN_COL));
		if (hitDiceNum <= 1.0) totalEHD /= 4; // sweep attack effect
		return totalEHD;
	}

	/**
	*  Special handling for NPC types.
	*  @return Total EHD of encounter (0 if not NPC)
	*/
	int handleNPCType (String type) {
		int NPClevel = 0;
		if (type.equals("Wizard")) NPClevel = 11;
		else if (type.equals("Necromancer")) NPClevel = 10;
		else if (type.equals("Lord")) NPClevel = 9;
		else if (type.equals("Superhero")) NPClevel = 8;
		else if (type.equals("Patriarch")) NPClevel = 8;
		else if (type.equals("Evil High Priest")) NPClevel = 8;
		return NPClevel == 0 ? 0 : NPClevel + getNPCEntourage();
	}

	/**
	*  Get value of NPC entourage.
	*  @return Total EHD of entourage.
	*/
	int getNPCEntourage () {
		int sum = 0;
		int number = new Dice(2, 6).roll();
		for (int i = 0; i < number; i++) {
			sum += Dice.roll(4);
		}		
		return sum;
	}

	/**
	*  Special fixups to determine estimated EHD.
	*/
	int EHDFixup(String monName, int EHD) {
	
		// Fill in null EHDs with estimated value.
		if (EHD == 0) {
			if (monName.equals("Dragon, Gold")) return 40;		
		}
		return EHD;
	}

	/**
	*  Convert a string to an integer.
	*  Invalid integer strings return 0.
	*/
	int strToInt (String s) {
		try {
			return Integer.parseInt(s);
		}
		catch (NumberFormatException ex) {
			return 0;
		}
	}

	/**
	*  Convert a string to a double.
	*  Invalid integer strings return 0.
	*/
	double strToDbl (String s) {
		try {
			return Double.parseDouble(s);
		}
		catch (NumberFormatException ex) {
			return 0;
		}
	}

	/**
	*  Main object method.
	*/
	public void runSim () {
 		for (int i = 0; i < NUM_ENCOUNTERS; i++) {
 			int totalEHD = rollEncounterByTerrain();
			System.out.println(totalEHD);
		}
	}

	/**
	*  Main application method.
	*/
	public static void main (String[] args) throws IOException {
		WildernessEncounterSim sim = new WildernessEncounterSim();
		sim.parseArgs(args);
		if (sim.exitAfterArgs) {
			sim.printUsage();
		}
		else {
			sim.runSim();
		}
	}
}
