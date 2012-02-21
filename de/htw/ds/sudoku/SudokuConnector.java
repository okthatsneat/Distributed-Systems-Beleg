package de.htw.ds.sudoku;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * <p>Shop connector class abstracting the use of JDBC for a set of given operations.
 * Note that a connector always works on the same JDBC connection, therefore allowing
 * multiple method calls to operate within a single local transaction, if desired.</p>
 */
public final class SudokuConnector {

	private static final long serialVersionUID = 1L;

	private static final String SQL_INSERT_SUDOKU= "INSERT INTO Sudoku VALUES (/*insert params for Sudoku db*/)";
	private static final String SQL_SELECT_SUDOKU= "SELECT * FROM Sudoku WHERE /*insert query for exisiting Sudoku solutions*/";
	
	private final Connection connection;

	/**
	 * Public constructor. 
	 * @param connection a JDBC connection, usually created from a JDBC data source
	 */
	public SudokuConnector(final Connection connection) {
		this.connection = connection;

	}

	/**
	 * Returns the JDBC connection used.
	 * @return the connection
	 */
	public Connection getConnection() {
		return this.connection;
	}


	/**
	 * Stores solution in database.
	
	 * @throws NullPointerException if one of the given values is null
	 * @throws IllegalStateException if the insert is unsuccessful
	 * @throws SQLException if there is a problem with the underlying JDBC connection
	 */
	
	public void insertSolution(byte[] digitsToSolve, byte[] digitsSolved) throws SQLException {
		final PreparedStatement statement = this.connection.prepareStatement(SQL_INSERT_SUDOKU);
		// TODO some validations on input data, throw new IllegalArgumentException();
		
		// TODO not sure if we need this password crap
		//statement.setString(1, this.alias);
		//statement.setString(2, this.password);
		
		// TODO Hash incoming digitsToSolve byte array
		
		// TODO store digitsSolved byte Array in database with key: hashed digitsToSolver
		// ex: 		statement.setString(3, firstName);
		
		if(statement.executeUpdate() != 1) throw new IllegalStateException();
		}
	
	
	public byte[] querySolution(byte[] digitsToSolve) throws SQLException {
		
		// TODO invoke boolean querySolutions to make sure that a returnable Solutions byte Array exists. If so... 
		
		final PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_SUDOKU);
		final ResultSet resultSet = statement.executeQuery();
	
		while (resultSet.next()) {
			 byte [] digitsSolved = resultSet.getBytes("digitsSolved");
			
		}
		return digitsSolved;
		// TODO somehow initiazlize byte array before filling it with database result, legally return it
	}

	
	public boolean solutionExists(byte[] digitsToSolve) throws SQLException {
		
		final PreparedStatement statement = this.connection.prepareStatement(SQL_SELECT_SUDOKU);
		final ResultSet resultSet = statement.executeQuery();
	
		// TODO find out if an unsuccessful query produces a null result set, or something else
		return (resultSet.wasNull());
			
	}

	
}
