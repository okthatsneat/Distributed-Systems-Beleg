package de.htw.ds.sudoku;

import javax.jws.WebParam;
import javax.jws.WebService;


/**
 * <p>Sudoku Soap service interface.</p>
 */
@WebService
public interface SoapSudokuService {

	/**
	 * Stores a byte array of Sudoku Solution on the server
	 
	 * @throws NullPointerException if one of the given values is null
	 * @throws JdbcException if there is a problem with the underlying JDBC connection
	 * @throws IllegalStateException if there is a problem with the java state at method invocation time
	 */
	
	void storeSolution(
			@WebParam (name="digitsToSolve") byte[] digitsToSolve,
			@WebParam (name="digitsSolved") byte[] digitsSolved
			) throws NullPointerException, IllegalStateException, JdbcException;
	
	
	/**
	 * Returns byte array of Sudoku Solution if passed Sudoku byte array is found in db
	 * @throws NullPointerException if one of the given values is null
	 * @throws JdbcException if there is a problem with the underlying JDBC connection
	 * @throws IllegalStateException if there is a problem with the java state at method invocation time
	 */
	
	byte[] getSolution(
			@WebParam (name="digitsToSolve") byte[] digitsToSolve
			) throws NullPointerException, IllegalStateException, JdbcException;
	
	
	/**
	 * Returns bool expressing that solution on server db has been found
	 * @throws NullPointerException if one of the given values is null
	 * @throws JdbcException if there is a problem with the underlying JDBC connection
	 * @throws IllegalStateException if there is a problem with the java state at method invocation time
	 */
	
	boolean solutionExists (
			@WebParam (name="digitsToSolve") byte[] digitsToSolve
			) throws NullPointerException, IllegalStateException,JdbcException;
	
}
