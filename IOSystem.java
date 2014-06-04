import java.util.Arrays;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

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
	
	private void initializeLDisk(String inputFileName)
	{
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(inputFileName));
			
			for(int i = 0; i < numOfBlocks; i++)
			{
				String currentLine = reader.readLine();
				
				if(currentLine != null && !currentLine.isEmpty())
				{
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
				else {}
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

	/*public void initializeLDisk(String filename)
	{
		Reader reader = null;
		try 
		{
			reader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}

		BufferedReader buffer = new BufferedReader(reader);
		try 
		{
			for (int i = 0; i < numOfBlocks; i++)
			{
				char[] memArray = new char[blockLength];
				for (int j = 0; j < blockLength; j++)
				memArray[j] = buffer.readLine().charAt(0);
				iosystem.writeBlock(i, memArray);
			}
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void save(String inputFileName)
	{
		PrintWriter writer = null;
		try 
		{
			writer = new PrintWriter(inputFileName, "UTF-8");
			for (int i = 0; i < numOfBlocks; i++)
			{
				/*char[] memArray = new char[b];
				iosystem.readBlock(i, memArray);

				for(char c : memArray)
					writer.println(c);
				for(int j = 0; j < blockLength; j++)
				{
					System.out.println("i: " + i + ", j: " + j);
					writer.println(ldisk[i][j]);
				}
			}
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}

		writer.close();
	}*/
}