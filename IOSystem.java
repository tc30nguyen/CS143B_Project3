import java.util.Arrays;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
	
	IOSystem(int numOfBlocks, int blockLength, String inputFileName)
	{
		this.numOfBlocks = numOfBlocks;
		this.blockLength = blockLength;
		ldisk = new char[numOfBlocks][blockLength];
		
		initializeLDisk(inputFileName);
	}

	public void read_block(int index, char[] readTo)
	{
		for(int i = 0; i < blockLength; i++)
			readTo[i] = ldisk[index][i];
	}

	public void write_block(int index, char[] readFrom)
	{
		ldisk[index] = Arrays.copyOf(readFrom, blockLength);
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
					sb.append((int) ldisk[i][j] + " ");
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
	
	//copy an input file into the new ldisk
	private void initializeLDisk(String inputFileName)
	{
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
			
			//iterate through input file by block (line) and copy contents into ldisk
			for(int i = 0; i < numOfBlocks; i++)
			{
				String currentLine = reader.readLine();
				if(currentLine != null && !currentLine.isEmpty())
				{
					//iterate through each line, convert the int back to char and copy contents into the current block
					Scanner lineScanner = new Scanner(currentLine);
					for(int j = 0; j < blockLength; j++)
					{
						if(lineScanner.hasNextInt())
						{
						char c = (char) lineScanner.nextInt();
						ldisk[i][j] = c;
						}
						else
							System.out.println("NO INT");
					}
					lineScanner.close();
				}
			}
		
			reader.close();
		}

		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}