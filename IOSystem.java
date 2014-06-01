public class IOSystem 
{
	private char[][] ldisk;

	IOSystem(int numOfBlocks, int blockLength)
	{
		ldisk = new char[numOfBlocks][blockLength];
	}

	void read_block(int index, char[] readTo)
	{
		char[] toRead = ldisk[index];
		readTo = Arrays.copyOf(toRead, toRead.length);
	}

	void write_block(int index, char[] readFrom)
	{
		char[] toWrite = ldisk[index];
		toWrite = Arrays.copyOf(readFrom, toWrite.length);
	}
}
