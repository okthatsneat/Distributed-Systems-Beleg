package de.htw.ds.sudoku;

import java.util.HashSet;
import java.util.Set;
import de.htw.ds.TypeMetadata;


/**
 * <p>Single-threaded base implementation of the SudokuPlugin interface.</p>
 */
@TypeMetadata(copyright = "2012 Sascha Baumeister, all rights reserved", version = "0.1.0", authors = "Sascha Baumeister")
public final class SudokuPlugin0 implements SudokuPlugin {
	private Sudoku parent = null;


	/**
	 * {@inheritDoc}
	 */
	public void setParent(final Sudoku parent) {
		this.parent = parent;
	}


	/**
	 * {@inheritDoc}
	 */
	public Set<Byte> getAntiSolutions(final Sudoku.ElementType elementType, final int digitIndex) {
		if (digitIndex < 0 || digitIndex >= this.parent.getDigits().length) throw new IllegalArgumentException();

		final int rowIndex = digitIndex / this.parent.getRadix();
		final int columnIndex = digitIndex % this.parent.getRadix();
		final Set<Byte> result = new HashSet<Byte>();

		switch (elementType) {
			case ROW: {
				for (int index = rowIndex * this.parent.getRadix(), stop = (rowIndex + 1) * this.parent.getRadix(); index < stop; ++index) {
					if (index != digitIndex) this.collectSolutions(index, result);
				}
				break;
			}
			case COLUMN: {
				for (int index = columnIndex; index < this.parent.getDigits().length; index += this.parent.getRadix()) {
					if (index != digitIndex) this.collectSolutions(index, result);
				}
				break;
			}
			case SECTOR: {
				final int baseStart = rowIndex / this.parent.getDimension() * this.parent.getDimension() * this.parent.getRadix() + columnIndex / this.parent.getDimension() * this.parent.getDimension();
				for (int baseIndex = baseStart, baseStop = baseStart + this.parent.getDimension() * this.parent.getRadix(); baseIndex < baseStop; baseIndex += this.parent.getRadix()) {
					for (int index = baseIndex, stop = baseIndex + this.parent.getDimension(); index < stop; ++index) {
						if (index != digitIndex) this.collectSolutions(index, result);
					}
				}
				break;
			}
		}
		return result;
	}


	/**
	 * Adds the cell value solutions that are possible for the given digit index
	 * to the given result set.
	 * @param digitIndex the digit index to be analyzed
	 * @param result the result set of possible cell values
	 * @throws ArrayIndexOutOfBoundsException if the given index is out of range
	 */
	private void collectSolutions(final int digitIndex, final Set<Byte> result) {
		if (this.parent.getDigits()[digitIndex] < 0) {
			result.addAll(this.parent.getSolutions(digitIndex));
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public Set<Sudoku> resolve(final int recursionDepth, final int digitIndex, final Set<Byte> cellAlternatives) {
		if (this.parent == null) throw new IllegalStateException();

		final Set<Sudoku> result = new HashSet<Sudoku>();
		for (final byte alternative : cellAlternatives) {
			final Sudoku clone = this.parent.clone();
			clone.getDigits()[digitIndex] = alternative;
			result.addAll(clone.resolve(recursionDepth + 1)); // distributable!
		}
		return result;
	}
}