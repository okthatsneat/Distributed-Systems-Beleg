package de.htw.ds.sudoku;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import de.htw.ds.TypeMetadata;


/**
 * <p>Single-threaded base implementation of the SudokuPlugin interface.</p>
 */
@TypeMetadata(copyright = "2012 Christoph Guttandin, Philipp Hofmann, Justin Evers", version = "0.1.0", authors = "Christoph Guttandin, Philipp Hofmann, Justin Evers")
public final class SudokuPlugin2 implements SudokuPlugin {
	private Sudoku parent = null;
	private static final int PROCESSOR_CUNT = Runtime.getRuntime().availableProcessors();


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
		final Semaphore indebtedSemaphore  = new Semaphore (1- cellAlternatives.size());
		final Set<Sudoku> result = new HashSet<Sudoku>();
		for (final byte alternative : cellAlternatives) {
			final Sudoku clone = this.parent.clone();
			clone.getDigits()[digitIndex] = alternative;
			// split into threads, excpet if recursion depth too deep
			
			// if abrage re: recursiondepth
			// start thread
			
			if (recursionDepth <= 3) {
			final SudokuReSolver reSolver = new SudokuReSolver(clone, result, recursionDepth,indebtedSemaphore );
			//finishSemaphore.acquireUninterruptibly();
			
			new Thread(reSolver).start();
			
			}
			
			else {
				result.addAll(clone.resolve(recursionDepth + 1)); // distributable!
			}
		}
		if (recursionDepth <= 3) indebtedSemaphore.acquireUninterruptibly();
		return result;
	}

	private static class SudokuReSolver implements Runnable {
		private final Sudoku clone;
		private final int recursionDepth;
		private final Set<Sudoku> result;
		private final Semaphore indebtedSemaphore;

		public SudokuReSolver(final Sudoku clone, final Set<Sudoku> result, final int recursionDepth, final Semaphore indebtedSemaphore) {
			this.clone = clone;
			this.recursionDepth = recursionDepth;
			this.result = result;
			this.indebtedSemaphore = indebtedSemaphore;
			
		}

		public void run() {
			try {
				
			result.addAll(clone.resolve(recursionDepth + 1)); // distributable!		
				
			} finally {
				indebtedSemaphore.release();
			}
		}
	}
	




}