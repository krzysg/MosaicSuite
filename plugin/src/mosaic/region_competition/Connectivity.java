package mosaic.region_competition;

import java.util.Iterator;

public class Connectivity implements Iterable<Point>
{
	private int VDim;				// dimension
	private int VCellDim;			// connectivity
	private int m_NeighborhoodSize;	// complete neighborhood
	
	private int size;				// m_NumberOfNeighbors
	static Point[] neighborsP;
	static int[] neighborsOfs;
	
	public Connectivity(int VDim, int VCellDim) 
	{
		this.VDim = VDim;
		this.VCellDim = VCellDim;
		m_NeighborhoodSize = (int)Math.pow(3, VDim);
		size = ComputeNumberOfNeighbors();
		
		neighborsP = new Point[size];
		neighborsOfs = new int[size];
		
		initOffsets();
		
	}

	private void initOffsets()
	{
		int currentNbNeighbors = 0;

		for(int i = 0; i < m_NeighborhoodSize; ++i) {
			Point p = OffsetToPoint(i);

			int numberOfZeros = countZeros(p);

			if(numberOfZeros != VDim && numberOfZeros >= VCellDim) 
			{
				neighborsP[currentNbNeighbors] = p;
				neighborsOfs[currentNbNeighbors] = i;
				currentNbNeighbors++;
			}
		}

	}
	
	public Point OffsetToPoint(int offset)
	{
		
		//TODO dublicated in unitCube
		
		int remainder = offset;
		Point p = Point.PointWithDim(this.VDim);

		for(int i = 0; i < this.VDim; ++i) {
			p.x[i] = remainder % 3;
			remainder -= p.x[i];
			remainder /= 3;
			p.x[i]--;
		}

		return p;
	}
	
	
	private int countZeros(Point p)
	{
		int count = 0;
		for(int i: p.x)
		{
			if(i==0) count++;
		}
		return count;
	}
	
	
	private int ComputeNumberOfNeighbors()
	{
		int numberOfNeighbors = 0;
		for(int i = VCellDim; i <= VDim - 1; ++i) 
		{
			numberOfNeighbors += 
				factorialrec(VDim)/(factorialrec(VDim - i) * factorialrec(i)) * (1<<(VDim-i));
		}

		return numberOfNeighbors;
	}

	private int factorialrec(int n)
	{
		int fac=1;
		for(int i=1; i<=n; i++)
		{
			fac=i*fac;
		}
		return fac;

	}
	
	
	@Override
	public Iterator<Point> iterator() {
		return new OfsIterator();
	}
	
	/**
	 * Iterates through the neighbors of Point point
	 */
	Iterable<Point> getNeighborIterable(Point point)
	{
		return new ConnectivityNeighborIterable(point);
	}
	
	/**
	 * Iterates through all neighbors and returns offsets
	 */
	class OfsIterator implements Iterator<Point> {
		
		private int cursor=0;
		
		@Override
		public boolean hasNext() {
			if (cursor < size)
				return true;
			else
				return false;
		}

		@Override
		public Point next() {
			Point result = neighborsP[cursor];
			cursor++;
			return result;
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			// do nothing
		}

	}
	
	/**
	 * Iterates through neighbors and returns Points 
	 * @author Stephan
	 */
	class ConnectivityNeighborIterable implements Iterable<Point>
	{
		Point point;
		public ConnectivityNeighborIterable(Point p) 
		{
			point = p;
		}
		@Override
		public Iterator<Point> iterator() 
		{
			return new NeighborIterator();
		}
		
		
		//TODO now thats ugly. inner inner class inheriting from inner class
		class NeighborIterator extends OfsIterator
		{
			@Override
			public Point next() {
				Point ofs = super.next();
				return point.add(ofs);
			}
		}
		
	}

	public int GetNeighborhoodSize()
	{
		return m_NeighborhoodSize;
	}
	
	public int Dimension()
	{
		return this.VDim;
	}
	
	/**
	 * 
	 * @param ofs Point representing offset to midpoint
	 * @return 
	 */
	boolean isInNeighborhood(Point ofs)
	{
		for(Point p:this)
		{
			if(ofs.equals(p))
			{
				return true;
				
			}
		}
		return false;
	}
	
	boolean areNeighbors(Point p1, Point p2)
	{
		return isInNeighborhood(p1.sub(p2));
	}
	
	
}

