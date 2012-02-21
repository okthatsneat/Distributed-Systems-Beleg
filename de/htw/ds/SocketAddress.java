package de.htw.ds;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;


/**
 * <p>This class is an alternative for {@link java.net.InetSocketAddress} that
 * can be serialized throughout the network, without failing upon deserialization.
 * The problem with {@link java.net.InetSocketAddress} is that it's instances do
 * serialize the binary address state and the local host name, but not the
 * host's domain. Therefore, if the binary address doesn't have a global scope
 * (most ISPs only issue link local addresses), and the socket-address is sent
 * to a machine in another domain, then the deserialized address becomes
 * unresolvable.</p>
 * <p>Instances of this class consist of a canonical host name and a port,
 * similarly to the socket-address information carried in URLs. They can be
 * serialized, and then sent through the network without risk of failure due to
 * the segmented binary address state of the internet.</p>
 * <p>When creating instances of this class, it is possible to provide textual
 * host names that are not canonical host names, but local ones, or IP addresses.
 * This is intentional to allow the creation of virtual socket-addresses, but may
 * cause {@link #getAddress()} to fail, and should be avoided whenever the
 * socket-addresses are intended to be transported over the internet.</p>
 */
@TypeMetadata(copyright = "2009-2011 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class SocketAddress implements Cloneable, Serializable, Comparable<SocketAddress> {
	private static final long serialVersionUID = -2431264614572396822L;
	public static final int BINARY_IP4_ADDRESS_LENGTH = 4;
	public static final int BINARY_IP4_SOCKET_ADDRESS_LENGTH = BINARY_IP4_ADDRESS_LENGTH + 2;
	public static final int BINARY_IP6_ADDRESS_LENGTH = 16;
	public static final int BINARY_IP6_SOCKET_ADDRESS_LENGTH = BINARY_IP6_ADDRESS_LENGTH + 2;
	public static enum ConversionMode { KEEP, FORCE_IP4, FORCE_IP6 }

	private String hostName;
	private final int port;
	private transient InetAddress address;


	/**
	 * Constructs an instance from the local host and the given port. If the local
	 * host can be resolved, the resulting socket-address is resolved, too.
	 * Otherwise, "127.0.0.1" is used as host name.
	 * @param port the port
	 * @throws IllegalArgumentException if the given port is outside range [0, 65535]
	 * @see #isResolved()
	 */
	public SocketAddress(final int port) {
		super();
		if (port < 0 || port > 65535) throw new IllegalArgumentException();

		this.port = port;
		try {
			this.address = InetAddress.getLocalHost();
			this.hostName = this.address.getCanonicalHostName();
		} catch (final UnknownHostException exception) {
			this.hostName = "127.0.0.1";
		}
	}


	/**
	 * Constructs an unresolved instance from the given host name and port.
	 * Note that the given host name may be a canonical host name, a simple
	 * host name, or an IP address.
	 * @param port the port
	 * @throws NullPointerException if the given host name is null
	 * @throws IllegalArgumentException if the given port is outside range [0, 65535]
	 * @see #isResolved()
	 */
	public SocketAddress(final String hostName, final int port) {
		super();
		if (port < 0 || port > 65535) throw new IllegalArgumentException();

		this.port = port;
		this.hostName = hostName;
	}


	/**
	 * Constructs an instance from the given address and port. The resulting socket
	 * address is resolved if and only if the given address was already resolved
	 * using a DNS lookup.
	 * @param address the IP address, or null for the local address
	 * @param port the port
	 * @throws NullPointerException if the given address is null
	 * @throws IllegalArgumentException if the given port is outside range [0, 65535]
	 * @see #isResolved()
	 */
	public SocketAddress(final InetAddress address, final int port) {
		super();
		if (port < 0 || port > 65535) throw new IllegalArgumentException();

		this.port = port;

		// note that the result is not guaranteed to be canonical if the given address was
		// not created using a DNS lookup!
		this.hostName = address.getCanonicalHostName();

		// Store address only if it is resolved, i.e. if it's creation involved a DNS lookup.
		// Using Java Reflection API to determine this, as there seems to be no other way to
		// determine this without causing a DNS lookup.
		try {
			final Field field = InetAddress.class.getDeclaredField("hostName");
			field.setAccessible(true);
			if (field.get(address) != null) {
				this.address = address;
			}
		} catch (final Exception exception) {
			// do nothing
		}
	}


	/**
	 * Constructs an instance from the given textual socket-address representation in
	 * the form "host:port". Note that the host portion is optional, i.e. both "80"
	 * and ":80" will be recognized as valid socket-address representations targeting
	 * the local host. Additionally, the host portion may be bracketed if it consists
	 * of an IPv6 address, as defined in RFC 2732. The resulting socket-address will
	 * be unresolved, unless the given socket-address representation omitted the host
	 * portion, and the local host can be resolved.
	 * @throws NullPointerException if the given socket-address text is null
	 * @throws IllegalArgumentException if the given socket-address's port is
	 *    not a number, or outside range [0, 65535]
	 * @see #isResolved()
	 */
	public SocketAddress(final String socketAddressText) {
		super();

		final int colonOffset = socketAddressText.lastIndexOf(':');
		try {
			this.port = Integer.parseInt(socketAddressText.substring(colonOffset + 1));
		} catch (final NumberFormatException exception) {
			throw new IllegalArgumentException();
		}
		if (this.port < 0 || this.port > 65535) throw new IllegalArgumentException();

		if (colonOffset < 1) {
			try {
				this.address = InetAddress.getLocalHost();
				this.hostName = this.address.getCanonicalHostName();
			} catch (final UnknownHostException exception) {
				this.hostName = "127.0.0.1";
			}
		} else if (socketAddressText.charAt(0) == '[' && socketAddressText.charAt(colonOffset - 1) == ']' && socketAddressText.indexOf(':') < colonOffset) {
			this.hostName = socketAddressText.substring(1, colonOffset - 1);
		} else {
			this.hostName = socketAddressText.substring(0, colonOffset);
		}
	}


	/**
	 * Creates an unresolved instance from the given binary socket-address.
	 * @param binarySocketAddress the binary socket-address
	 * @throws IllegalArgumentException if the given byte array doesn't have a length of 6 (IPv4) or 18 (IPv6)
	 * @see #isResolved()
	 */
	public SocketAddress(final byte[] binarySocketAddress) {
		super();

		if (binarySocketAddress.length != BINARY_IP4_SOCKET_ADDRESS_LENGTH && binarySocketAddress.length != BINARY_IP6_SOCKET_ADDRESS_LENGTH) {
			throw new IllegalArgumentException();
		}

		this.port = ((binarySocketAddress[binarySocketAddress.length - 2] & 0xFF) << 8) 
        		  | ((binarySocketAddress[binarySocketAddress.length - 1] & 0xFF) << 0);

		final byte[] binaryAddress = new byte[binarySocketAddress.length - 2];
		System.arraycopy(binarySocketAddress, 0, binaryAddress, 0, binaryAddress.length);
		try {
			this.hostName = InetAddress.getByAddress(binaryAddress).getCanonicalHostName();
		} catch (final UnknownHostException exception) {
			throw new AssertionError();
		}
	}


	/**
	 * Returns true if the host name is resolved into an address, false otherwise.
	 * Resolved socket-addresses can provide a cached address, and their host name is
	 * guaranteed to be canonical. Note that a socket-address is unresolved once it
	 * is deserialized, even if it was resolved before serialization. This way, if
	 * the socket-address is deserialized in a new domain, it can resolve to a
	 * different IP address if necessary, similarly to URLs and URIs.
	 * @return true if the receiver is resolved, false otherwise
	 * @see #resolve()
	 * @see #getAddress()
	 */
	public boolean isResolved() {
		return this.address != null;
	}


	/**
	 * Resets the receiver's cached address by performing a DNS lookup on
	 * it's host name. Also, resets it's host name using a reverse DNS lookup
	 * on the resolved address, to ensure that all resolved socket-addresses
	 * report canonical host names. Returns true if this succeeds, false
	 * otherwise. Note that a previously resolved socket-address may become
	 * unresolved if this operation fails.
	 * @return true if the receiver is resolved, false otherwise
	 * @see #isResolved()
	 * @see #getAddress()
	 */
	public boolean resolve() {
		synchronized(this) {
			try {
				this.address = InetAddress.getByName(this.hostName);
				this.hostName = this.address.getCanonicalHostName();
				return true;
			} catch (final UnknownHostException exception) {
				this.address = null;
				return false;
			}
		}
	}


	/**
	 * Returns the host name. Note that the host name is guaranteed to be
	 * canonical once the receiver is resolved. Otherwise it may be
	 * canonical or not, depending on the data that was provided during
	 * construction.
	 * @return the canonical host name if the receiver is resolved, a
	 *    normal host name or host address otherwise
	 * @see #isResolved()
	 * @see #resolve()
	 */
	public String getHostName() {
		return this.hostName;
	}


	/**
	 * Returns the port
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}


	/**
	 * Resolves the receiver if necessary, and returns a resolved host address.
	 * @return the host address
	 * @throws UnknownHostException if the receiver cannot be resolved
	 * @see #isResolved()
	 * @see #resolve()
	 */
	public InetAddress getAddress() throws UnknownHostException {
		synchronized(this) {
			if (this.address == null) this.resolve();
			if (this.address == null) throw new UnknownHostException(this.hostName);
			return this.address;
		}
	}


	/**
	 * Resolves the receiver if necessary, and converts it into a binary socket-address.
	 * The resulting byte array contains N-2 address bytes, followed by 2 port bytes,
	 * both as two-complements in big endian order (i.e. high bytes first). If the given
	 * mode is KEEP, then 6 bytes will be returned if the underlying address is IPv4, and
	 * 18 bytes	if the underlying address is IPv6. If the given mode is FORCE_IP6, then
	 * 18 bytes will be returned in any case, using an IPv4-mapped IPv6 address (RFC 4291)
	 * if necessary. If the given mode mode is FORCE_IP4, then 6 bytes will be returned,
	 * but this is only possible if the underlying address is either an IPv4 address, or
	 * an IPv4-mapped IPv6 address.
	 * @param mode the conversion mode
	 * @return the binary socket-address
	 * @throws NullPointerException if the given mode is null
	 * @throws UnknownHostException if the receiver cannot be resolved, or if mode is
	 *    FORCE_IP4 and the underlying address is IPv6, but not IPv4-mapped
	 * @see #isResolved()
	 */
	public byte[] toBinarySocketAddress(final ConversionMode mode) throws UnknownHostException {
		if (mode == null) throw new NullPointerException();

		final byte[] rawBinaryAddress = this.getAddress().getAddress();
		final byte[] binarySocketAddress = (mode == ConversionMode.KEEP && rawBinaryAddress.length == BINARY_IP4_ADDRESS_LENGTH) || mode == ConversionMode.FORCE_IP4
			? new byte[BINARY_IP4_SOCKET_ADDRESS_LENGTH]
			: new byte[BINARY_IP6_SOCKET_ADDRESS_LENGTH];

		if (rawBinaryAddress.length == BINARY_IP4_ADDRESS_LENGTH) {
			if (binarySocketAddress.length == BINARY_IP4_SOCKET_ADDRESS_LENGTH) {
				System.arraycopy(rawBinaryAddress, 0, binarySocketAddress, 0, rawBinaryAddress.length);
			} else {
				binarySocketAddress[10] = (byte) 0xFF;
				binarySocketAddress[11] = (byte) 0xFF;
				System.arraycopy(rawBinaryAddress, 0, binarySocketAddress, 12, rawBinaryAddress.length);
			}
		} else {
			if (binarySocketAddress.length == BINARY_IP4_SOCKET_ADDRESS_LENGTH) {
				for (int index = 0; index < 10; ++index) {
					if (rawBinaryAddress[index] != (byte) 0x00) throw new UnknownHostException();
				}
				for (int index = 10; index < 12; ++index) {
					if (rawBinaryAddress[index] != (byte) 0xFF) throw new UnknownHostException();
				}
				System.arraycopy(rawBinaryAddress, 12, binarySocketAddress, 0, rawBinaryAddress.length - 12);
			} else {
				System.arraycopy(rawBinaryAddress, 0, binarySocketAddress, 0, rawBinaryAddress.length);
			}
		}

		binarySocketAddress[binarySocketAddress.length - 2] = (byte) ((this.port >> 8) & 0xFF);
		binarySocketAddress[binarySocketAddress.length - 1] = (byte) ((this.port >> 0) & 0xFF);
		return binarySocketAddress;		
	}


	/**
	 * Returns the equivalent {@link java.net.InetSocketAddress}.
	 * @return the equivalent socket-address
	 */
	public InetSocketAddress toInetSocketAddress() {
		return this.address == null
			? new InetSocketAddress(this.hostName, this.port)
			: new InetSocketAddress(this.address, this.port);
	}


	/**
	 * Returns a base URI (a URL) with the given optional scheme, optional user info, the receiver's
	 * host name, the receiver's port, and the given path. If the given path is null, then "/" is
	 * assumed. If the given path is relative, an absolute path is assumed. Note that the scheme,
	 * user info, path, and the receiver's canonical host name must conform to RFC 2396.
	 * @param scheme the scheme, or null
	 * @param userInfo the user info, or null
	 * @param path the path
	 * @return the base URI
	 * @throws URISyntaxException if the given scheme, user info or path contains illegal characters,
	 *    as defined in RFC 2396
	 */
	public URI toBaseURI(final String scheme, final String userInfo, final String path) throws URISyntaxException {
		final String globalPath = (path == null) ? "/" : (path.startsWith("/") ? path : "/" + path);
		final String encodedHostName = this.hostName.contains(":") ? "[" + this.hostName + "]" : this.hostName;
		return new URI(scheme, userInfo, encodedHostName, this.port, globalPath, null, null);
	}


   /**
     * Compares this socket-address with the specified socket-address for order.
     * Returns a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object. The comparison
     * is performed on the two host names first, then on the two ports. Note that
     * the host name may change once a socket-address is resolved, which requires
     * care when using socket-addresses in sorted collections.
     * @param socketAddress the socket-address to compare to
     * @return -1 if the receiver is less than, +1 if the receiver is greater than, or 0
     *    if the receiver is equal to the given socket-address
     * @see #equals(Object)
	 * @see #isResolved()
 	 */
	public int compareTo(final SocketAddress socketAddress) {
		final int compare = this.hostName.compareTo(socketAddress.hostName);
		if (compare != 0) return compare;
		if (this.port < socketAddress.port) return -1;
		if (this.port > socketAddress.port) return  1;
		return 0;
	}


	/**
	 * Returns a clone.
	 * @return a cloned socket-address
	 */
	@Override
	public SocketAddress clone() {
		try {
			return (SocketAddress) super.clone();
		} catch (final CloneNotSupportedException exception) {
			throw new AssertionError();
		}
	}


	/**
	 * Returns a hash-code based on the receiver's fields, as required for field
	 * based equality checks. Note that a socket-address's hash-code may change
	 * when it is resolved, which requires care when using socket-addresses in
	 * hashed collections.
	 * @return a socket-address
	 * @see #compareTo(SocketAddress)
	 * @see #equals(Object)
	 * @see #isResolved()
	 */
	@Override
	public int hashCode() {
		return this.hostName.hashCode() ^ this.port;
	}


	/**
	 * Returns true if the receiver and the given object are equal, false otherwise.
	 * socket-addresses are equal if they share equals canonical host names and ports.
	 * Note that equal socket-addresses are guaranteed to have the same hash-code.
	 * @param object the object to compare to
	 * @return true if both objects are equal, false otherwise
	 * @see #compareTo(SocketAddress)
	 * @see #hashCode()
	 * @see #isResolved()
	 */
	@Override
	public boolean equals (final Object object) {
		if (!(object instanceof SocketAddress)) return false;
		return this.compareTo((SocketAddress) object) == 0;
	}


	/**
	 * Returns a textual representation of this socket-address, by combining the
	 * host name and the port, separated by a colon. If the host name is an
	 * IPv6 address, then it is enclosed in brackets, as defined in RFC 2732.
	 * @return a textual socket-address representation
	 */
	@Override
	public String toString() {
		return this.hostName.contains(":")
			? "[" + this.hostName + "]:" + this.port
			: this.hostName + ":" + this.port;
	}


	/**
	 * Returns the local address, or the localhost/127.0.0.1
	 * loopback address if the former cannot be determined.
	 * @return the local host address
	 * @see java.net.InetAddress#getLocalHost()
	 */
	public static InetAddress getLocalAddress() {
		try {
			return InetAddress.getLocalHost();
		} catch (final UnknownHostException exception) {
			try {
				return InetAddress.getByAddress("localhost", new byte[] {127, 0, 0, 1});
			} catch (final UnknownHostException nestedException) {
				throw new AssertionError();
			}
		}
	}
}