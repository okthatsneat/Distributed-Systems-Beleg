package de.htw.ds;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;


/**
 * <p>This static class provides methods to convert Java packages into equivalent
 * URI's, and URI's into packages. The mapping interprets a package name as a
 * canonical host name in reversed component order, and vice versa.</p> 
 */
public final class Namespaces {
	private static final Pattern DOT_PATTERN = Pattern.compile("\\.");


	/**
	 * Private constructor preventing instantiation.
	 */
	private Namespaces() {
		super();
	}


	/**
	 * If the given package is null or empty, <tt>[scheme:]///</tt> is returned to
	 * represent Java's virtual default package. Otherwise returns a URI
	 * containing the given scheme, the component-reversed package name as host
	 * name, and <tt>"/"</tt> as path.
	 * @param scheme the resulting URI's scheme, or <tt>null</tt> for undefined
	 * @param pkg the package
	 * @return the package's equivalent URI, or <tt>[scheme:]///</tt> representing
	 *     the default package
	 * @throws IllegalArgumentException if the given package cannot be converted
	 *     into a valid URI
	 */
	public static URI toURI(final String scheme, final Package pkg) {
		try {
			if (pkg == null) return new URI(scheme, null, "/", null);
			return new URI(scheme, reverseComponents(pkg.getName()), "/", null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException(pkg.getName());
		}
	}


	/**
	 * If the given URI's is <tt>[scheme:]///</tt>, <tt>null</tt> is returned to
	 * represent Java's virtual default package. Otherwise, this method
	 * returns a Java package that has the component-reversed host of the URI
	 * as it's name. Note that it fails if no matching package is found by the
	 * current thread's class loader.
	 * @param uri the URI
	 * @return the URI's equivalent package, or <tt>null</tt> representing the
	 *     default package
	 * @throws NullPointerException if the given URI is not <tt>[scheme:]///</tt>,
	 *     and the URI, it's host, or it's path is <tt>null</tt>
	 * @throws IllegalArgumentException if the given URI is not <tt>[scheme:]///</tt>,
	 *    and it's host is empty, it's port isn't <tt>-1</tt>, it's path doesn't equal
	 *    "/", or the URI's host cannot be converted into a package accessible by
	 *    the current thread's class loader
	 * @see java.xml.namespace.QName
	 */
	public static Package toPackage(final URI uri) {
		final boolean nullHost = (uri.getHost() == null);
		final boolean nullPath = (uri.getPath() == null);
		final boolean legalHost = !"".equals(uri.getHost());
		final boolean legalPort = (uri.getPort() == -1);
		final boolean legalPath = "/".equals(uri.getPath());

		if ("///".equals(uri.getSchemeSpecificPart()) && nullHost && legalPort && legalPath) return null;
		if (nullHost || nullPath) throw new NullPointerException();
		if (!legalHost || !legalPort || !legalPath) throw new IllegalArgumentException(uri.toString());

		final Package pkg = Package.getPackage(reverseComponents(uri.getHost()));
		if (pkg == null) throw new IllegalArgumentException(uri.toString());
		return pkg;
	}


	/**
	 * Splits the given test into components divided by dots, reverses the component's
	 * order, reassembles the reversed components into a dot-divided string, and
	 * returns the result.
	 * @param text the dot-divided text to be reversed
	 * @return the reversed dot-divided text
	 * @throws NullPointerException if the given text is null
	 */
	private static String reverseComponents(final String text) {
		final String[] components = DOT_PATTERN.split(text);
		if (components.length < 2) return text;

		final StringWriter writer = new StringWriter();
		writer.write(components[components.length - 1]);
		for (int index = components.length - 2; index >= 0; --index) {
			writer.write('.');
			writer.write(components[index]);
		}

		return writer.toString();
	}


	/**
	 * Creates a new dynamically created SOAP proxy for the given service interface and service URI.
	 * The method assumes that the service contains a single port per interface, and that the
	 * qualified SOAP service name consists of the reversed components of the service interface's
	 * package, and it's simple name.
	 * @param <T> the service interface type
	 * @param serviceInterface the service interface
	 * @param serviceURI the service URI
	 * @return a dynamically created service proxy instance
	 * @throws IllegalArgumentException if the given service URI cannot be used to construct a valid URL
	 * @throws javax.xml.ws.WebServiceException if the given service URI doesn't point to a running service,
	 *    or if the SOAP service name cannot be deduced from the service interface.
	 */
	public static <T> T createDynamicSoapServiceProxy(final Class<T> serviceInterface, final URI serviceURI) {
		final URI soapPackageNamespace = Namespaces.toURI("http", serviceInterface.getPackage());
		final QName soapServiceName = new QName(soapPackageNamespace.toASCIIString(), serviceInterface.getSimpleName());
		final T proxy = Namespaces.createDynamicSoapServiceProxy(serviceInterface, serviceURI, soapServiceName, null);
		return proxy;
	}


	/**
	 * Returns a dynamically created proxy for the given service interface, service URI, service name,
	 * and optional port name. The algorithm first builds a WSDL query URL from the given service URI,
	 * and loads the WSDL from there. Then it looks up the given SOAP service name within the WSDL,
	 * and creates a Service proxy factory from it's definitions. Finally, the factory creates a proxy
	 * fitting the given service interface, selecting from the available ports by the given SOAP port
	 * name. If the given SOAP port name is <tt>null</tt>, then the proxy is created from any available
	 * SOAP port. Note that it is a sign of utter stupidity in the JAX-WS community to require a SOAP
	 * service name when each WSDL document can only contain one service element ...
	 * @param <T> the service interface type
	 * @param serviceInterface the service interface
	 * @param serviceURI the service URI
	 * @param soapServiceName the qualified SOAP service name
	 * @param soapServicePortName the qualified SOAP port name, or null
	 * @return a dynamically created service proxy instance
	 * @throws IllegalArgumentException if the given service URI cannot be used to construct a valid URL
	 * @throws javax.xml.ws.WebServiceException if the given service URI doesn't point to a running service,
	 *    or if the given SOAP service name is not found within the WSDL queried from said service
	 */
	public static <T> T createDynamicSoapServiceProxy(final Class<T> serviceInterface, final URI serviceURI, final QName soapServiceName, final QName soapServicePortName) {
		final String query = (serviceURI.getQuery() == null)
			? "wsdl"
			: serviceURI.getQuery() + "&wsdl";


		final URL wsdlURL;
		try {
			final URI wsdlURI = new URI(serviceURI.getScheme(), serviceURI.getUserInfo(), serviceURI.getHost(), serviceURI.getPort(), serviceURI.getPath(), query, serviceURI.getFragment());
			wsdlURL = wsdlURI.toURL();
		} catch (final Exception exception) {
			// transformation error can only come from unsuitable service URI
			throw new IllegalArgumentException(serviceURI.toASCIIString());
		}


		final Service proxyFactory = Service.create(wsdlURL, soapServiceName);
		final T proxy = (soapServicePortName == null)
			? proxyFactory.getPort(serviceInterface)
			: proxyFactory.getPort(soapServicePortName, serviceInterface);
		return proxy;
	}
}