package us.fl.state.fwc.util.dBase;

/**
 * Opens a connection to an Apache Derby database and provides simple routines to 
 * work with a database.  
 */
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;

public class DerbyConnection {

	private Connection con;
	private Statement stmt;
	private ResultSet rs;


	public DerbyConnection(String dbName, boolean createNew){
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			if (createNew)
				con = DriverManager.getConnection("jdbc:derby:" + dbName + ";create=true" );
			else
				con = DriverManager.getConnection("jdbc:derby:" + dbName );

			stmt = con.createStatement();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	public DerbyConnection(String dbName, String userName, String password, 
			boolean createNew){

		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

			if (userName == null) userName ="";
			if (password == null) password = "";

			if (createNew)
				con = DriverManager.getConnection(
						"jdbc:derby:" + dbName + ";user=" + userName + ";password=" + password 
						+ ";create=true");
			else 
				con = DriverManager.getConnection(
						"jdbc:derby:" + dbName + ";user=" + userName + ";password=" + password);

			stmt = con.createStatement();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Execute the given query string and return the ResultSet.
	 * 
	 * @param selectString
	 * @return
	 */
	public ResultSet executeQuery(String selectString){
		ResultSet rs = null; 
		try {
			rs = stmt.executeQuery(selectString);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}


	/**
	 * Create a new Schema in the database.
	 * 
	 * @param schemaName
	 */
	public void createSchema(String schemaName){
		try {
			stmt.executeUpdate("create SCHEMA " + schemaName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Creates a new table with the given table name and the column attributes.  Here, the 
	 * LinkedHashMap key is the column name, and the value is an Object array containing:
	 * 
	 * 	value[0] = (String) data type (int, double, boolean, date, string)
	 * 	value[1] = (Integer) for strings, the column width
	 * 	value[2] = (Boolean) whether the column can be empty/null or not  
	 * 
	 * @param tableName
	 * @param columnAttributes
	 */
	public void createTable(String schemaName, String tableName, 
			LinkedHashMap<String, Object[]> columnAttributes){
		try {

			String stmtBuilder;
			if (schemaName == null)
				stmtBuilder = "CREATE TABLE " + tableName + " (";
			else 
				stmtBuilder = "CREATE TABLE " + schemaName + "." + tableName + " (";

			int remainingColumns = columnAttributes.size();

			for (String columnName: columnAttributes.keySet()){

				Object[] values = columnAttributes.get(columnName);
				String datatype =(String) values[0];
				Integer clmnWidth = (Integer) values[1];
				Boolean beNull = (Boolean) values[2];

				remainingColumns--;
				stmtBuilder += columnName + " ";

				if (datatype.equals("int") || datatype.equals("INTEGER") || datatype.equals("INT")) 
					stmtBuilder += "INTEGER ";
				if (datatype.equals("double") || datatype.equals("DOUBLE")) 
					stmtBuilder += "DOUBLE ";
				if (datatype.equals("Date") || datatype.equals("DATE")) 
					stmtBuilder += "DATE ";
				if (datatype.equals("String") || datatype.equals("VARCHAR")) 
					stmtBuilder += "VARCHAR(" + clmnWidth.intValue() + ") ";

				if (!beNull) stmtBuilder += "NOT NULL";

				if (remainingColumns > 0) stmtBuilder += ","; //comma for additional column
				else stmtBuilder += ")"; //close up the string

			}

			stmt.executeUpdate(stmtBuilder);


		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Drop the given table from the database.
	 * 
	 * @param schemaName
	 * @param tableName
	 */
	public void dropTable(String schemaName, String tableName){

		try {
			String stmtBuilder;

			if (schemaName == null)
				stmtBuilder = "DROP TABLE " + tableName;
			else 
				stmtBuilder = "DROP TABLE " + schemaName + "." + tableName;

			stmt.executeUpdate(stmtBuilder);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}


	/**
	 * Insert data from a new table into an existing table.  If replace > 0, then the existing data will
	 * be replaced by the new table data.
	 * 
	 * @param schemaName
	 * @param tableName
	 * @param newTablePath
	 * @param replace
	 */
	public void insertTable(String schemaName, String tableName, String newTablePath, 
			String delimiter, boolean replace){

		try {

			int replaceInt = 0;
			if (replace) replaceInt = 1; 
			if (delimiter == null ) delimiter = ",";
			
			
			
	        CallableStatement ps = 
	            con.prepareCall(
	                "call SYSCS_UTIL.SYSCS_IMPORT_TABLE (?, ?, ?, ?, ?, ?, ?)");
	        ps.setString(1, schemaName);
	        ps.setString(2, tableName);
	        ps.setString(3, newTablePath);
	        ps.setString(4, delimiter);
	        ps.setString(5, null);
	        ps.setString(6, null);
	        ps.setInt(   7, replaceInt);
	        ps.executeUpdate();
	        ps.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Get the statement to an open connection. 
	 * 
	 * @return
	 */
	public Statement getStatement(){
		return stmt;
	}
	
	
	/**
	 * Close up the connections when finished.
	 */
	public void closeConnections(){
		try {
			stmt.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


}
