import java.util.*;
import java.io.IOException; 

/******************************************************************************
*  Generic text game table read from CSV file.
*
*  Row and column numbering is 1-indexed.
*  Table in CSV file must be rectangular (every row & column same size).
*  Null values can be represented by a single dash ("-").
*
*  @author   Daniel R. Collins (dcollins@superdan.net)
*  @since    2021-12-05
******************************************************************************/

public class Table {

	//--------------------------------------------------------------------------
	//  Constants
	//--------------------------------------------------------------------------

	/** Null entry value. */
	static final String NULL_ENTRY = "-";

	//--------------------------------------------------------------------------
	//  Fields
	//--------------------------------------------------------------------------

	/** The 2D table of string data. */
	String[][] table;

	//--------------------------------------------------------------------------
	//  Constructors
	//--------------------------------------------------------------------------

	/**
	*  Constructor.
	*/
	protected Table (String filename) throws IOException {
		table = CSVReader.readFile(filename);
	}

	//--------------------------------------------------------------------------
	//  Methods
	//--------------------------------------------------------------------------

	/**
	* Get the number of rows.
	*/
	public int getNumRows () {
		return table.length - 1;	
	}

	/**
	* Get the number of columns.
	*/
	public int getNumCols () {
		return table[0].length - 1;			
	}

	/**
	* Get title of a row.
	*/
	public String getRowName (int index) {
		return table[index][0];	
	}

	/**
	* Get title of a column.
	*/
	public String getColName (int index) {
		return table[0][index];
	}

	/**
	* Get index of row from name.
	*/
	public int getRowFromName (String name) {
		for (int i = 1; i <= getNumRows(); i++) {
			if (table[i][0].equals(name))
				return i;		
		}
		return -1;
	}

	/**
	* Get index of column from name.
	*/
	public int getColFromName (String name) {
		for (int i = 1; i <= getNumCols(); i++) {
			if (table[0][i].equals(name))
				return i;		
		}
		return -1;
	}

	/**
	* Get uniformly random entry in a given row.
	*/
	public String getRandomEntryOnRow (int index) {
		String entry;
		do {
			int roll = Dice.roll(getNumCols());
			entry = table[index][roll];
		} while (entry.equals(NULL_ENTRY));
		return entry;
	}

	/**
	* Get uniformly random entry in a given column.
	*/
	public String getRandomEntryOnCol (int index) {
		String entry;
		do {
			int roll = Dice.roll(getNumRows());
			entry = table[roll][index];
		} while (entry.equals(NULL_ENTRY));
		return entry;
	}

	/**
	* Get entry given row and column.
	*/
	public String getEntry (int row, int col) {
		return table[row][col];	
	}

	/**
	*  Main test method.
	*/
	public static void main (String[] args) throws IOException {
		Dice.initialize();		
		Table table = new Table("WildMainTable.csv");
		System.out.println("Number of rows: " + table.getNumRows());
		System.out.println("Number of columns: " + table.getNumCols());
		System.out.println();
		System.out.println("Column headers:");
		for (int i = 1; i <= table.getNumCols(); i++) {
			System.out.println(table.getColName(i));
		}
		System.out.println();
		System.out.println("Rolls on the last column:");
		for (int i = 0; i < 6; i++) {
			System.out.println(table.getRandomEntryOnCol(table.getNumCols()));
		}
		System.out.println();
		System.out.println("Index of the Mountain column:");
		System.out.println(table.getColFromName("Mountain"));
		System.out.println();
	}
}
