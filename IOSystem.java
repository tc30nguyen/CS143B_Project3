import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class IOSystem 
{
	private char[][] ldisk;
	private final int numOfBlocks;
	private final int blockLength;

	IOSystem(int numOfBlocks, int blockLength)
	{
		this.numOfBlocks = numOfBlocks;
		this.blockLength = blockLength;
		ldisk = new char[numOfBlocks][blockLength];
	}

	public void read_block(int index, char[] readTo)
	{
		char[] toRead = ldisk[index];
		readTo = Arrays.copyOf(toRead, toRead.length);
	}

	public void write_block(int index, char[] readFrom)
	{
		char[] toWrite = ldisk[index];
		toWrite = Arrays.copyOf(readFrom, toWrite.length);
	}

	public void save(String fileName)
	{
		try
		{
		File output = new File(fileName);
		if(!output.exists())
			output.createNewFile();

		StringBuilder sb = new StringBuilder();
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		for(int i = 0; i < numOfBlocks; i++)
		{
			for(int j = 0; j < blockLength; j++)
				sb.append(ldisk[i][j]);
			sb.append("\n");
		}

		bw.write(sb.toString());
		bw.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}