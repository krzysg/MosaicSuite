package mosaic.core.ipc;

public interface Outdata<E>
{
	/**
	 * 
	 * To write CSV files every base class given as parameters to InterPluginsCSV
	 * has to implements this class, for itself
	 * 
	 * Example InterPluginsCSV<A> csv
	 * 
	 * In order to do
	 * 
	 * Vector<S> out
	 * Write(.... outdata ....)
	 * 
	 * "A" has to implement Outdata<S>
	 * 
	 * same thing if S = A
	 * 
	 * Vector<A> out
	 * Write(.... outdata ....)
	 * 
	 * "A" has to implement Outdata<A>
	 * 
	 * the method implement a conversion from S to A
	 * in the case of S = A the conversion is trivial, but must be 
	 * specified
	 * 
	 * @param r Object to convert
	 */
	
	void setData(E r);
}

