package de.htw.ds.sudoku;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import de.htw.ds.Namespaces;
import de.htw.ds.TypeMetadata;


/**
 * <p>Instances of this class generate and solve Sudoku riddles. Note that the riddles
 * use zero as a normal riddle digit. Therefore, dimension 2 riddles use digit
 * range [0-3], dimension 3 riddles (default) use range [0-8], dimension 4 riddles
 * use range [0-9, a-f], dimension 5 riddles use range [0-9, a-o], and dimension 6
 * riddles use range [0-9, a-z].</p>
 */
@TypeMetadata(copyright = "2012 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class Sudoku implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;


	public static enum ElementType {
		ROW, COLUMN, SECTOR
	}

	public static enum Command {
		CREATE, CHECK, SOLVE
	}

	private static final Random RANDOMIZER = new Random();
	private static final Set<Sudoku> EMPTY_SUDOKU_SET = Collections.unmodifiableSet(new HashSet<Sudoku>());

	private final SudokuPlugin plugin;
	private final byte dimension;
	private final short radix;
	private final byte[] digits;


	/**
	 * Public constructor.
	 * @param plugin the sudoku plugin implementing key algorithms
	 * @param dimension the square root of a valid row/column/segment length
	 * @throws NullPointerException if the given plugin is null
	 * @throws IllegalArgumentException if the given dimension is outside its range [2, 6]
	 */
	public Sudoku(final SudokuPlugin plugin, final byte dimension) {
		super();
		if (plugin == null) throw new NullPointerException();
		if (dimension < 2 || dimension * dimension > Character.MAX_RADIX) throw new IllegalArgumentException();
		plugin.setParent(this);

		this.plugin = plugin;
		this.dimension = dimension;
		this.radix = (short) (dimension * dimension);
		this.digits = new byte[this.radix * this.radix];
	}


	/**
	 * Returns a clone of the receiver, which is required for backtracking algorithms.
	 * @return the clone
	 */
	@Override
	public final Sudoku clone() {
		try {
			final SudokuPlugin plugin = (SudokuPlugin) this.plugin.getClass().newInstance();
			final Sudoku clone = new Sudoku(plugin, this.dimension);
			System.arraycopy(this.digits, 0, clone.digits, 0, this.digits.length);
			return clone;
		} catch (final Exception exception) {
			throw new InternalError("class " + this.plugin.getClass() + " probably lacks public default constructor");
		}
	}


	/**
	 * Returns the receiver's text representation.
	 * @return the text representation
	 */
	@Override
	public final String toString() {
		final StringWriter writer = new StringWriter();
		writer.write(this.getClass().getCanonicalName());
		writer.write('[');
		writer.write(Integer.toString(this.dimension));
		writer.write(']');
		writer.write("\n");

		final int areaSize = this.dimension * this.radix;
		for (int index = 0; index < this.digits.length; ++index) {
			final byte digit = this.digits[index];
			final char character = (digit == -1) ? '.' : Character.forDigit(digit, this.radix);
			writer.write(character);
			writer.write(' ');
			if (index % this.dimension == this.dimension - 1) writer.write(' ');
			if (index % this.radix == this.radix - 1) writer.write("\n");
			if (index % areaSize == areaSize - 1) writer.write("\n");
		}
		writer.write("\n");
		writer.write("\n");
		return writer.toString();
	}


	/**
	 * Returns the square root of the receiver's radix.
	 * @return the dimension
	 */
	public final byte getDimension() {
		return this.dimension;
	}


	/**
	 * Returns the square root of the receiver's digit count.
	 * @return the dimension
	 */
	public final short getRadix() {
		return this.radix;
	}


	/**
	 * Returns the digits, including -1 for unresolved digits.
	 * @return a digit array with radix square entries
	 */
	public final byte[] getDigits() {
		return this.digits;
	}


	/**
	 * Returns the digits of the element with the given type and index. Negative
	 * values represent unresolved digits.
	 * @param elementType the element type
	 * @param elementIndex the element index
	 * @return a digit array with radix entries.
	 * @throws NullPointerException if the given element type is null
	 * @throws IllegalArgumentException if the given element index is strictly
	 * 		negative or exceeds the receiver's radix
	 */
	public final byte[] getDigits(final ElementType elementType, final int elementIndex) {
		if (elementIndex < 0 || elementIndex >= this.radix) throw new IllegalArgumentException();

		final byte[] result = new byte[this.radix];
		switch (elementType) {
			case ROW: {
				System.arraycopy(this.digits, elementIndex * this.radix, result, 0, this.radix);
				break;
			}
			case COLUMN: {
				for (int rowIndex = 0; rowIndex < this.radix; ++rowIndex) {
					result[rowIndex] = this.digits[rowIndex * this.radix + elementIndex];
				}
				break;
			}
			case SECTOR: {
				final int position = (elementIndex / this.dimension * this.radix + elementIndex % this.dimension) * this.dimension;
				for (int index = 0; index < this.dimension; ++index) {
					System.arraycopy(this.digits, position + index * this.radix, result, index * this.dimension, this.dimension);
				}
				break;
			}
		}
		return result;
	}


	/**
	 * Sets the digits of the element with the given index to the given values.
	 * Negative values represent unresolved digits.
	 * @param elementType the element type
	 * @param elementIndex the element index
	 * @param digits a digit array with radix entries
	 * @throws NullPointerException if the given type or digits array is null
	 * @throws IllegalArgumentException if the given index is strictly negative or exceeds the
	 *		receiver's radix, or if the given digits length doesn't match the radix
	 */
	public final void setDigits(final ElementType elementType, final int elementIndex, final byte[] digits) {
		if (digits.length != this.radix || elementIndex < 0 || elementIndex >= this.radix) throw new IllegalArgumentException();

		switch (elementType) {
			case ROW: {
				System.arraycopy(digits, 0, this.digits, elementIndex * this.radix, this.radix);
				break;
			}
			case COLUMN: {
				for (int rowIndex = 0; rowIndex < this.radix; ++rowIndex) {
					this.digits[rowIndex * this.radix + elementIndex] = digits[rowIndex];
				}
				break;
			}
			case SECTOR: {
				final int baseIndex = (elementIndex / this.dimension * this.radix + elementIndex % this.dimension) * this.dimension;
				for (int index = 0; index < this.dimension; ++index) {
					System.arraycopy(digits, index * this.dimension, this.digits, baseIndex + index * this.radix, this.dimension);
				}
				break;
			}
		}
	}


	/**
	 * Fills the digits from the given digit text representations, with '.' characters
	 * representing unresolved digits.
	 * @param digitTexts the list of digit text representations
	 * @throws NullPointerException if the given digitTexts array is null
	 * @throws IllegalArgumentException if the given digitTexts array doesn't have the
	 *    same length as the receiver's digits, if it contains an element that is not a
	 *    single character string, or if it contains an element whose single character
	 *    is not a valid digit relative to the receiver's radix
	 */
	public void setDigits(final String... digitTexts) {
		if (digitTexts.length != this.getDigits().length) throw new IllegalArgumentException("Digit error, expecting " + this.getDigits().length + " digits but received " + digitTexts.length + "!");
		for (int index = 0; index < this.getDigits().length; ++index) {
			final String digitText = digitTexts[index];
			if (digitText.length() != 1) throw new IllegalArgumentException("Digit error, digits must be single character but \"" + digitText + "\" isn't!");

			final char character = digitText.toLowerCase().charAt(0);
			final byte digit = (byte) Character.digit(character, this.getRadix());
			if (digit == -1 && character != '.') throw new IllegalArgumentException("Digit error, character '" + digitText + "' isn't a valid digit!");
			this.getDigits()[index] = digit;
		}
	}


	/**
	 * Populates the receiver with a new riddle. The algorithm first creates a known
	 * riddle solution, and then performs random transformations that guarantee the
	 * riddle stays both valid and uniquely solvable. Said transformations must be
	 * highly effective in obfuscating the original into a totally different form,
	 * therefore digit swapping, area row swapping, and area column swapping are used.
	 */
	public final void populate() {
		byte digit = (byte) (this.radix - this.dimension - 1);
		for (int index = 0; index < this.digits.length; ++index) {
			if (index % this.radix == 0) digit += this.dimension;
			if (index % (this.dimension * this.radix) == 0) digit += 1;
			if (digit >= this.radix) digit -= this.radix;
			this.digits[index] = digit++;
		}

		for (int counter = 0; counter < this.digits.length; ++counter) {
			if (RANDOMIZER.nextBoolean()) { // swap two single digits
				final byte digit1 = (byte) RANDOMIZER.nextInt(this.radix);
				byte digit2 = digit1;
				while (digit2 == digit1)
					digit2 = (byte) RANDOMIZER.nextInt(this.radix);
				for (int index = 0; index < this.digits.length; ++index) {
					if (this.digits[index] == digit1) {
						this.digits[index] = digit2;
					} else if (this.digits[index] == digit2) {
						this.digits[index] = digit1;
					}
				}
			} else { // swap two area rows or area columns
				final ElementType elementType = RANDOMIZER.nextBoolean() ? ElementType.ROW : ElementType.COLUMN;
				final int index1 = RANDOMIZER.nextInt(this.radix);
				final int baseIndex = index1 / this.dimension * this.dimension;
				int index2 = index1;
				while (index2 == index1)
					index2 = baseIndex + RANDOMIZER.nextInt(this.dimension);
				final byte[] digits = this.getDigits(elementType, index1);
				this.setDigits(elementType, index1, this.getDigits(elementType, index2));
				this.setDigits(elementType, index2, digits);
			}
		}
	}


	/**
	 * Reduces the receiver into a riddle with relative maximum difficulty and exactly one possible
	 * solution. This is achieved by successively setting all digits that don't force
	 * multiple solutions to minus one.
	 * @throws IllegalStateException if the receiver already has more than one possible solution
	 */
	public final void reduce() {
		if (this.resolve().size() != 1) throw new IllegalStateException();

		final Set<Integer> indices = new HashSet<Integer>();
		for (int index = 0; index < this.digits.length; ++index)
			indices.add(index);
		while (!indices.isEmpty()) {
			final int index = (Integer) indices.toArray()[RANDOMIZER.nextInt(indices.size())];
			final byte digit = this.digits[index];
			this.digits[index] = -1;

			final Set<Sudoku> solutions = this.resolve();
			if (solutions.size() < 1) throw new AssertionError();
			if (solutions.size() > 1) this.digits[index] = digit;
			indices.remove(index);
		}
	}


	/**
	 * Resolves the riddle, i.e. replaces it's negative values with valid digits.
	 * Note that the receiver is not modified during this operation.
	 * @return a set of Sudoku representing the possible solutions.
	 */
	public final Set<Sudoku> resolve() {
		return this.clone().resolve(0);
	}


	/**
	 * Resolves the riddle, i.e. replaces it's negative values with valid digits.
	 * Note that the receiver may be modified during this operation.
	 * @param recursionDepth the recursion depth for analytic purposes
	 * @return a set of Sudoku representing the possible solutions.
	 */
	protected final Set<Sudoku> resolve(final int recursionDepth) {
		int pivotIndex = -1;
		Set<Byte> pivotAlternatives = null;
		for (int index = 0; index < this.getDigits().length; ++index) {
			if (this.getDigits()[index] < 0) {
				final Set<Byte> alternatives = this.getSolutions(index);
				if (alternatives.size() == 0) return EMPTY_SUDOKU_SET;
				if (alternatives.size() == 1) {
					this.getDigits()[index] = alternatives.iterator().next();
					index = -1;
				} else {
					final Set<Byte> solution = new HashSet<Byte>(alternatives);
					solution.removeAll(this.plugin.getAntiSolutions(ElementType.ROW, index));
					if (solution.isEmpty()) {
						solution.addAll(alternatives);
						solution.removeAll(this.plugin.getAntiSolutions(ElementType.COLUMN, index));
					}
					if (solution.isEmpty()) {
						solution.addAll(alternatives);
						solution.removeAll(this.plugin.getAntiSolutions(ElementType.SECTOR, index));
					}
					if (solution.isEmpty()) {
						if (pivotAlternatives == null || pivotAlternatives.size() > alternatives.size()) {
							pivotAlternatives = alternatives;
							pivotIndex = index;
						}
					} else {
						this.getDigits()[index] = solution.iterator().next();
						index = -1;
					}
				}
			}
		}

		if (pivotAlternatives == null) {
			final Set<Sudoku> result = new HashSet<Sudoku>();
			result.add(this);
			return result;
		}
		return this.plugin.resolve(recursionDepth, pivotIndex, pivotAlternatives);
	}


	/**
	 * Returns the cell solutions that are possible for the given digit index, given the value
	 * at that index is currently undefined (i.e. negative). This is performed by removing all
	 * digits of the given digit's row, column and sector from the set of possible solutions.
	 * @param digitIndex the digit index to be analyzed
	 * @return a set of possible values, or null if the value is already set
	 * @throws IllegalArgumentException if the given index is out of range
	 */
	protected final Set<Byte> getSolutions(final int digitIndex) {
		if (digitIndex < 0 || digitIndex >= this.getDigits().length) throw new IllegalArgumentException();
		if (this.getDigits()[digitIndex] >= 0) return null;

		final int rowIndex = digitIndex / this.getRadix();
		final int columnIndex = digitIndex % this.getRadix();
		final Set<Byte> result = new HashSet<Byte>();

		for (byte solution = 0; solution < this.getRadix(); ++solution) {
			result.add(solution);
		}

		for (int index = rowIndex * this.getRadix(), stop = (rowIndex + 1) * this.getRadix(); index < stop; ++index) {
			result.remove(this.getDigits()[index]);
		}

		for (int index = columnIndex; index < this.getDigits().length; index += this.getRadix()) {
			result.remove(this.getDigits()[index]);
		}

		final int baseStart = rowIndex / this.getDimension() * this.getDimension() * this.getRadix() + columnIndex / this.getDimension() * this.getDimension();
		for (int baseIndex = baseStart, baseStop = baseStart + this.getDimension() * this.getRadix(); baseIndex < baseStop; baseIndex += this.getRadix()) {
			for (int index = baseIndex, stop = baseIndex + this.getDimension(); index < stop; ++index) {
				result.remove(this.getDigits()[index]);
			}
		}

		return result;
	}


	/**
	 * Application entry point. The arguments given must be either a riddle dimension or
	 * a riddle's digits. In case a dimension is passed, the application populates a riddle
	 * with the given dimension, solves it afterwards, and finally displays both the riddle
	 * and it's sole solution. In case a riddle's digits are passed, the application tries
	 * to solve the riddle, and finally displays both the riddle and all (if any) possible
	 * solutions. Note that cell values that are no digits (like dots) are interpreted as
	 * negative values, i.e. values to be solved.
	 * @param args the arguments
	 * @throws URISyntaxException 
	 */
	
	/* arg[0] plugin classpath
	 * arg[1] mode; check or solve
	 * arg[2] sudoku dimension
	 * arg[3] sudoku digitsToSolve
	 * arg[4] SERVICE_URI
	 */
	
	public static void main(final String[] args) throws IOException, URISyntaxException {
		
		// SOAP stuff
		
		final URI SERVICE_URI = new URI(args[4]);
		final SoapSudokuService proxy = Namespaces.createDynamicSoapServiceProxy(SoapSudokuService.class, SERVICE_URI);

		// regular Sudoku stuff
		
		final long start = System.currentTimeMillis();
		final Sudoku sudoku;
		final Command command;
		try {
			final SudokuPlugin plugin = (SudokuPlugin) Class.forName(args[0], true, Thread.currentThread().getContextClassLoader()).newInstance();
			final byte dimension = Byte.parseByte(args[2]);

			/*No idea what this means, probably irrelevant & from old exercise:
			Local sudoku server only expected on port 9000 in case of SudokuPlugin3!*/
			sudoku = new Sudoku(plugin, dimension);
			command = Command.valueOf(args[1].toUpperCase());
		} catch (final Exception exception) {
			System.out.println("Parameter syntax: <pluginClass:String> <command:create|check|solve> <dimension:2-6> { <digits:0-9|a-z|.> } <SERVICE_URI>");
			System.out.println("Examples:");
			System.out.println("de.htw.ds.sudoku.SudokuPlugin0 create 3");
			System.out.println("de.htw.ds.sudoku.SudokuPlugin2 check 5");
			System.out.println("de.htw.ds.sudoku.SudokuPlugin1 solve 2  . 0 . .   . . 3 .   . . . 2   0 . . .");
			return;
		}

		if (command == Command.SOLVE) {
			try {
				final String[] digitTexts = new String[args.length - 3];
				System.arraycopy(args, 3, digitTexts, 0, digitTexts.length);
				sudoku.setDigits(digitTexts);
			} catch (final IllegalArgumentException exception) {
				System.out.println(exception.getMessage());
				return;
			}
		} else {
			
			// here the digitsToSolve are set up
			sudoku.populate();
			sudoku.reduce();
		}

		System.out.print(sudoku);

		if (command == Command.CHECK || command == Command.SOLVE) {
			
			//  check against proxy if solution exists
			byte [] digitsToSolve = sudoku.getDigits();
			try {
				if ( proxy.solutionExists(digitsToSolve) ) {
					//TODO format solution byte array to Sudoku solution type
					final Sudoku solution = proxy.getSolution(digitsToSolve);
				}
				// else use plugin to solve and print out
				else {
					for (final Sudoku solution : sudoku.resolve()) {
						System.out.print(solution);
						/*TODO populate a byte array with the solution,
						write to server as digitsSolved with key digitsToSolve*/
					}	
				}
			
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JdbcException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			
		}
		final long stop = System.currentTimeMillis();
		System.out.println(stop-start);
	}
}