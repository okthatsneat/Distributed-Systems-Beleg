package de.htw.ds.sudoku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;


import javax.jws.WebService;
import javax.sql.DataSource;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import de.htw.ds.SocketAddress;


/**
 * <p>Sudoku server class implementing a JAX-WS based server.
 */

@WebService (endpointInterface="de.htw.ds.sudoku.SoapSudokuService", serviceName="SoapSudokuService", portName="SoapSudokuPort")


public class SoapSudokuServer implements SoapSudokuService{

	private final Endpoint endpoint;
	private final DataSource dataSource;
	
	public SoapSudokuServer(final String binding, final URI serviceURI, final DataSource dataSource) {
		super();
		if (dataSource == null) throw new NullPointerException();
		this.dataSource = dataSource;
		this.endpoint = Endpoint.create(binding, this);
		this.endpoint.publish(serviceURI.toASCIIString());
 	}
	
	
	
	public void storeSolution(byte[] digitsToSolve, byte[] digitsSolved)
			throws NullPointerException, IllegalStateException, JdbcException {
		try {
			final Connection connection = this.dataSource.getConnection();
			try {
				connection.setAutoCommit(false);
				final SudokuConnector connector = new SudokuConnector(connection);
				connector.insertSolution(digitsToSolve, digitsSolved);
				connection.commit();
			} catch (final SQLException exception){
				try { connection.rollback(); } catch (final Exception e) {}
				throw exception;
			} finally {
				try { connection.close(); } catch (final Exception exception) {}
			}
		} catch (final SQLException exception) {
			throw new JdbcException(exception);
		}
	}

	
	public byte[] getSolution(byte[] digitsToSolve)
			throws NullPointerException, IllegalStateException, JdbcException {
		try {
			final Connection connection = this.dataSource.getConnection();
			try {
				connection.setAutoCommit(false);
				final SudokuConnector connector = new SudokuConnector(connection);
				final byte [] digitsSolved = connector.querySolution(digitsToSolve);
				connection.commit();
				return digitsSolved;
			} catch (final SQLException exception){
				try { connection.rollback(); } catch (final Exception e) {}
				throw exception;
			} finally {
				try { connection.close(); } catch (final Exception exception) {}
			}
		} catch (final SQLException exception) {
			throw new JdbcException(exception);
		}
	}

	@Override
	public boolean solutionExists(byte[] digitsToSolve)
			throws NullPointerException, IllegalStateException, JdbcException {
		try {
			final Connection connection = this.dataSource.getConnection();
			try {
				connection.setAutoCommit(false);
				final SudokuConnector connector = new SudokuConnector(connection);
				boolean solutionExists = connector.solutionExists(digitsToSolve);
				connection.commit();
				return solutionExists;
			} catch (final SQLException exception){
				try { connection.rollback(); } catch (final Exception e) {}
				throw exception;
			} finally {
				try { connection.close(); } catch (final Exception exception) {}
			}
		} catch (final SQLException exception) {
			throw new JdbcException(exception);
		}
	}

	/**
	 * Closes the receiver, thereby stopping it's SOAP endpoint.
	 */
	public void close() {
		this.endpoint.stop();
	}
	
	/**
	 * Application entry point. The given runtime parameters must be a SOAP service port,a
	 * SOAP service name, a JDBC connection URL, a database user-ID, a database password
	 * 
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if one of the given service URIs is malformed
	 * @throws JdbcException if none of the supported JDBC drivers is installed
	 * @throws WebServiceException if the given port is already in use
	 */
	public static void main(final String[] args) throws URISyntaxException, JdbcException, RemoteException {
		final long timeStamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final String serviceName = args[1];
		final URI soapServiceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		final URI jdbcConnectionURI = new URI(args[2]);

		final DataSource dataSource = createDataSource(jdbcConnectionURI, args[3], args[4]);
		final SoapSudokuServer server = new SoapSudokuServer(SOAPBinding.SOAP11HTTP_BINDING, soapServiceURI, dataSource);
		try {
			System.out.println("JAX-WS based shop server running.");
			System.out.println("Service URI is " + soapServiceURI + ", data source URL is " + args[2] + ", type \"quit\" to stop.");
			System.out.println("Startup time is " + (System.currentTimeMillis() - timeStamp) + "ms.");

			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(reader.readLine())); } catch (final IOException exception) {}
		} finally {
			server.close();
		}
	}
	
	
	
	/**
	 * Creates a new data source from the given arguments, using the Java
	 * Reflection API here to avoid static code dependency to the JDBC driver used.
	 * This allows easy extension for multiple database types to be supported, with
	 * only one JDBC driver being present during runtime!
	 * @param databaseURI the database URI
	 * @param alias the user alias
	 * @param password the user password
	 * @throws JdbcException if none of the supported JDBC drivers is installed
	 */
	public static DataSource createDataSource(final URI databaseURI, final String alias, final String password) throws JdbcException {

		try {
			final Class<?> dataSourceClass = Class.forName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource", true, Thread.currentThread().getContextClassLoader());
			final DataSource dataSource = (DataSource) dataSourceClass.newInstance();
			dataSourceClass.getMethod("setURL", String.class).invoke(dataSource, databaseURI.toASCIIString());
			dataSourceClass.getMethod("setCharacterEncoding", String.class).invoke(dataSource, "utf-8");
			dataSourceClass.getMethod("setUser", String.class).invoke(dataSource, alias);
			dataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, password);
			return dataSource;
		} catch (final ClassNotFoundException exeption) {
			// MySql driver is not installed, try next supported database type
		} catch (final NoSuchMethodException exception) {
			throw new AssertionError();
		} catch (final InstantiationException e) {
			throw new AssertionError();
		} catch (final IllegalAccessException exception) {
			throw new AssertionError();
		} catch (final InvocationTargetException exception) {
			if (exception.getCause() instanceof Error) throw (Error) exception.getCause();
			if (exception.getCause() instanceof RuntimeException) throw (RuntimeException) exception.getCause();
			throw new AssertionError();
		}

		throw new JdbcException("MySql JDBC driver not installed.");
	}
	
	
}
