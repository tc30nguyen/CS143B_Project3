import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Shell 
{
	BufferedReader inputFileReader;
	FileSystem fileSystem;
	Scanner input;

	public Shell()
	{
		inputFileReader = null;
		input = new Scanner(System.in);
	}

	public Shell(String inputFileName)
	{
		try 
		{
			inputFileReader = new BufferedReader(new FileReader(inputFileName));
			input = null;
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
	}

	public void run()
	{
		String currentLine = "";

		//run through System.input
		if(inputFileReader == null)
		{	
			while(input.hasNextLine() && !currentLine.equals("exit"))
			{
				currentLine = input.nextLine();
				runCommand(currentLine);
			}
			
			input.close();
		}
		
		//run through an input file
		else
		{
			try 
			{
				currentLine = inputFileReader.readLine();
				
				while(currentLine != null)
				{
					if(!currentLine.isEmpty())
						runCommand(currentLine);
					currentLine = inputFileReader.readLine();
				}
			}
			
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void runCommand(String currentLine)
	{
		Scanner lineScanner = new Scanner(currentLine);
		String command;
		if(lineScanner.hasNext())
		{
			command = lineScanner.next();
			switch(command)
			{
				//create: cr <name>
				case "cr":
					String name;
					if(lineScanner.hasNext())
					{
						name = lineScanner.next();
						if(fileSystem.create(name.toCharArray()))
							System.out.println(name + " created");
						else
							System.out.println("error");
					}
					else
						System.out.println("error");
					break;

				//delete: de <name>
				case "de":
					if(lineScanner.hasNext())
					{
						name = lineScanner.next();
						if(fileSystem.destroy(name.toCharArray()))
							System.out.println(name + " destroyed");
						else
							System.out.println("error");
					}
					else
						System.out.println("error");
					break;

				//open: op <name>
				case "op":
					int OFTIndex = -1;
					if(lineScanner.hasNext())
					{
						name = lineScanner.next();
						OFTIndex = fileSystem.open(name.toCharArray());
						if(OFTIndex != -1)
							System.out.println(name + " opened " + OFTIndex);
						else
							System.out.println("error");
					}
					else
						System.out.println("error");
					break;

				//close: cl <OFT index>
				case "cl":
					if(lineScanner.hasNextInt())
					{
						OFTIndex = lineScanner.nextInt();
						fileSystem.close(OFTIndex);
						System.out.println("file " + OFTIndex + " closed");
					}
					else
						System.out.println("error");
					break;

				//read: rd <OFT index> <bytesToRead>
				case "rd":
					char[] buffer;
					int bytesToRead = 0;
					if(lineScanner.hasNextInt())
					{
						OFTIndex = lineScanner.nextInt();
						if(lineScanner.hasNextInt())
						{
							bytesToRead = lineScanner.nextInt();
							buffer = new char[bytesToRead];
							bytesToRead = fileSystem.read(OFTIndex, buffer, bytesToRead);
							if(bytesToRead != -1)
							{
								//print output
								for(int i = 0; i < bytesToRead; i++)
									System.out.print(buffer[i]);
								System.out.print("\n");
							}
							else
								System.out.println("error");
						}
						else
							System.out.println("error");
					}
					else
						System.out.println("error");
					break;

				//write: wr <OFT index> <charToWrite> <bytesToWrite>
				case "wr":
					char toWrite = 0;
					int count = 0;
					if(lineScanner.hasNextInt())
					{
						OFTIndex = lineScanner.nextInt();
						if(lineScanner.hasNext())
							toWrite = lineScanner.next().charAt(0);
						else
							System.out.println("error");
						if(lineScanner.hasNextInt())
							count = lineScanner.nextInt();
						else
							System.out.println("error");
						buffer = new char[count];
						Arrays.fill(buffer, toWrite);
						count = fileSystem.write(OFTIndex, buffer, count);
						
						if(count == -1)
							System.out.println("error");
						else
							System.out.println(count + " bytes written");
					}
					else
						System.out.println("error");
					break;

				//seek: sk <OFT index> <position>
				case "sk":
					int filePosition = 0;
					if(lineScanner.hasNextInt())
					{
						OFTIndex = lineScanner.nextInt();
						if(lineScanner.hasNextInt())
						{
							filePosition = lineScanner.nextInt();
							fileSystem.lseek(OFTIndex, filePosition);
							System.out.println("current position is " + filePosition);
						}
						else
							System.out.println("error");
					}
					else
						System.out.println("error");
					break;

				//initialize: in OR in <inputFileName.txt>
				case "in":
					String fileName;
					if(lineScanner.hasNext())
					{;
						fileSystem = new FileSystem(lineScanner.next());
						System.out.println("disk restored");
					}
					else
					{
						fileSystem = new FileSystem();
						System.out.println("disk initialized");
					}
					break;

				//save: sv <outputFileName.txt>
				case "sv":
					if(lineScanner.hasNext())
					{
						fileName = lineScanner.next();
						fileSystem.save(fileName);
						System.out.println("disk saved");
					}
					break;
				
				//directory: dr
				case "dr":
					fileSystem.directory();
					break;

				//unhandled input
				default:
					System.out.println("error");
					break;
			}
		}
		
		lineScanner.close();
	}
}
