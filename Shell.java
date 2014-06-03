import java.util.Arrays;
import java.util.Scanner;

public class Shell 
{
	FileSystem fileSystem;
	Scanner input;

	public Shell()
	{
		input = new Scanner(System.in);
	}

	public void run()
	{
		String currentLine = "";
		
		while(input.hasNextLine() && !currentLine.equals("exit"))
		{
			currentLine = input.nextLine();
			Scanner lineScanner = new Scanner(currentLine);
			String command;

			if(lineScanner.hasNext())
			{
				command = lineScanner.next();

				switch(command)
				{
					case "cd":
						String name;
						if(lineScanner.hasNext())
						{
							name = lineScanner.next();
							fileSystem.create(name.toCharArray());
							System.out.println("file " + name + " created");
						}
						else
							System.out.println("error");
						break;

					case "de":
						System.out.println("NOT DONE");
						break;

					case "op":
						//String name;
						int OFTIndex = -1;
						if(lineScanner.hasNext())
						{
							name = lineScanner.next();
							OFTIndex = fileSystem.open(name.toCharArray());
							System.out.println("file " + name + " opened, index=" + OFTIndex);
						}
						else
							System.out.println("error");
						break;

					case "cl":
						System.out.println("NOT DONE");
						break;

					case "rd":
						char[] buffer;
						int bytesToRead;
						//int OFTIndex = -1;
						if(lineScanner.hasNextInt())
						{
							OFTIndex = lineScanner.nextInt();
							if(lineScanner.hasNextInt())
							{
								bytesToRead = lineScanner.nextInt();
								buffer = new char[bytesToRead];
								bytesToRead = fileSystem.read(OFTIndex, buffer, bytesToRead);

								//print output
								System.out.print(bytesToRead + " bytes read: ");
								for(int i = 0; i < bytesToRead; i++)
									System.out.print(buffer[i]);
								System.out.print("\n");
							}
							else
								System.out.println("error");
						}
						else
							System.out.println("error");
						break;

					case "wr":
						//char[] buffer;
						char toWrite = 0;
						int count = 0;
						//int OFTIndex = -1;
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

							System.out.println(count + " bytes written");
						}
						else
							System.out.println("error");
						break;

					case "sk":
						//int OFTIndex = -1;
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

					case "in":
						String fileName;
						if(lineScanner.hasNext())
						{
							System.out.println("NOT DONE");
							//fileSystem = new FileSystem(lineScanner.next());
							//System.out.println("disk restored");
						}
						else
						{
							fileSystem = new FileSystem();
							System.out.println("disk initialized");
						}
						break;

					case "sv":
						//String fileName;
						if(lineScanner.hasNext())
						{
							fileName = lineScanner.next();
							fileSystem.save(fileName);
						}
						break;

					default:
						System.out.println("error");
						break;
				}
			}
			
			lineScanner.close();
		}
		
		input.close();
	}
}
