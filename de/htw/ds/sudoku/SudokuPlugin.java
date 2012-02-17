	package de.htw.ds.sudoku;

import java.util.Set;
import de.htw.ds.TypeMetadata;
import de.htw.ds.sudoku.Sudoku.ElementType;


/**
 * <p>Instances of this interface serve as plugins for Sudoku riddles. Note that the
 * riddles use zero as a normal riddle digit. Therefore, dimension 2 riddles use digit
 * range [0-3], dimension 3 riddles (default) use range [0-8], dimension 4 riddles
 * use range [0-9, a-f], dimension 5 riddles use range [0-9, a-o], and dimension 6
 * riddles use range [0-9, a-z].</p>
 * <p>Implementations must feature a public default constructor to allow dynamic
 * instantiation by the Java reflection API!</p>
 */
@TypeMetadata(copyright = "2012 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public interface SudokuPlugin {

	/**
	 * Contains the integer binary logarithm of the number of available
	 * processors/processor cores. This value can be very useful for
	 * decision-making when performing binary or quasi-binary recursions.
	 */
	@SuppressWarnings("all")
	static final byte LOG2_PROCESSOR_COUNT = (byte) (Integer.SIZE - Integer.numberOfLeadingZeros(Runtime.getRuntime().availableProcessors()) - 1);


	/**
	 * Sets the receiver's parent.
	 * @param sudoku a sudoku
	 */
	void setParent(Sudoku sudoku);


	/**
	 * Returns the solutions that are possible for the given row/column/sector
	 * element defined by the given digit index, except for the ones at the
	 * given digit index.
	 * @param elementType the element type
	 * @param digitIndex the digit index to be analyzed
	 * @return a set of possible cell values
	 * @throws NullPointerException if the given type is null
	 * @throws IllegalArgumentException if the given digit index is out of range
	 */
	Set<Byte> getAntiSolutions(ElementType elementType, int digitIndex);


	/**
	 * Resolves the riddle, i.e. replaces it's negative values with valid digits.
	 * This method is called whenever there are two or more possible solutions to
	 * a single cell. Note that the receiver is not modified during this operation.
	 * @param recursionDepth the recursion depth for analytic purposes
	 * @param digitIndex the digit index of the cell to modify
	 * @param cellAlternatives the cell value alternatives for the given cell index
	 * @return the set of Sudoku representing the possible solutions
	 */
	Set<Sudoku> resolve(int recursionDepth, int digitIndex, Set<Byte> cellAlternatives);
}