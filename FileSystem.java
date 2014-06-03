import java.util.Arrays;


public class FileSystem 
{
	private class OpenFileEntry
	{
		private char[] buffer;
		private char[] fileDescriptor;
		private int currentPosition;
		private int descriptorIndex;

		public OpenFileEntry()
		{
			buffer = new char[MAX_FILE_SIZE];
			fileDescriptor = new char[FILE_DESCRIPTOR_SIZE];
			currentPosition = 0;
			descriptorIndex = -1;
		}

		//not sure if deals with max file size correctly atm
		public int writeFile(char[] memArea, int count)
		{
			int currentBlock = getCurrentDescriptorMapIndex();
			int bytesToWrite = 0;
			if(currentPosition + count >= fileDescriptor[0])
				count = fileDescriptor[0] - currentPosition;
			bytesToWrite = count;

			for(int i = 0; count > 0; count--, i++)
			{
				buffer[currentPosition % BLOCK_LENGTH] = memArea[i];
				currentPosition++;

				if(currentPosition % BLOCK_LENGTH == 0)
				{
					iosystem.write_block(getBlockIndex(currentBlock++), buffer);
					iosystem.read_block(getBlockIndex(currentBlock), buffer);
				}
			}

			fileDescriptor[0] += bytesToWrite;
			return bytesToWrite;
		}

		//handle case where currentPosition is > current file size
		public int readFile(char[] memArea, int count) //throws Exception
		{
			if(count >= MAX_FILE_SIZE)
				//throw new Exception();

			//limit the read count to the file's current size
			if(currentPosition + count >= fileDescriptor[0])
				count = fileDescriptor[0] - currentPosition;
			int numOfBytesRead = count;

			int currentBlock = getCurrentDescriptorMapIndex();

			//copy each requested char from the buffer into memArea
			for(int i = 0; count > 0; count--, i++)
			{
				memArea[i] = buffer[currentPosition % BLOCK_LENGTH];
				currentPosition++;

				//read the next ldisk block into the buffer when necessary
				if(currentPosition % BLOCK_LENGTH == 0)
				{
					iosystem.write_block(getBlockIndex(currentBlock++), buffer);
					iosystem.read_block(getBlockIndex(currentBlock), buffer);
				}
			}

			return numOfBytesRead;
		}
		
		public void close()
		{
			iosystem.write_block(getBlockIndex(getCurrentDescriptorMapIndex()), buffer);
		}

		private char getBlockIndex(int currentBlock)
		{
			if(fileDescriptor[currentBlock] == 0 || fileDescriptor[currentBlock] == -1)
			{
				char[] bitmap = new char[64];
				iosystem.read_block(0, bitmap);

				//search bitmap for an open block, skipping the reserved K blocks
				for(int i = K; i < bitmap.length; i++)
				{
					if(bitmap[i] == 0)
					{
						fileDescriptor[currentBlock] = (char) i;
						bitmap[i] = 1;

						//clear the newly allocated block to initialize
						char[] clearBlock = new char[BLOCK_LENGTH];
						iosystem.write_block(i, clearBlock);
					}
				}
			}
			
			return fileDescriptor[currentBlock];
		}

		private void getFileDescriptor()
		{
			char[] temp = new char[BLOCK_LENGTH];
			int descriptorLocationInBlock = descriptorIndex % BLOCK_LENGTH;
			iosystem.read_block(descriptorIndex / BLOCK_LENGTH, temp);

			for(int i = 0; i < FILE_DESCRIPTOR_SIZE; i++)
				fileDescriptor[i] = temp[descriptorLocationInBlock + i];
		}

		public int getFileDescriptorIndex()
		{
			return descriptorIndex;
		}

		public void setPosition(int pos)
		{
			int currentBlock = getCurrentDescriptorMapIndex();
			int newBlock = pos / BLOCK_LENGTH + 1;

			if(newBlock != currentBlock)
			{
				iosystem.write_block(fileDescriptor[currentBlock], buffer);
				iosystem.read_block(fileDescriptor[newBlock], buffer);
			}

			currentPosition = pos;
		}

		public void setNewFile(int fileDescriptorIndex)
		{
			descriptorIndex = fileDescriptorIndex;
			getFileDescriptor();
			currentPosition = 0;
			iosystem.read_block(fileDescriptor[1], buffer); //read ahead
		}
		
		private int getCurrentDescriptorMapIndex()
		{
			return currentPosition / BLOCK_LENGTH + 1;
		}
	}

	final int BLOCK_LENGTH = 64;
	final int DIRECTORY_ENTRY_SIZE = 5;
	final int FILE_DESCRIPTOR_SIZE = 4; //1 for file length, 3 for the file's max 3 disk blocks
	final int MAX_FILE_SIZE = 192;
	final int NUM_OF_BLOCKS = 64;
	final int K = 7; //first k blocks, reserved for the bitmap and file descriptors
	IOSystem iosystem;
	OpenFileEntry[] oft; //4 OpenFileEntrys, OpenFileEntry[0] is reserved for the directory file and 3 are can hold open files

	public FileSystem()
	{
		iosystem = new IOSystem(NUM_OF_BLOCKS, BLOCK_LENGTH);
		oft = new OpenFileEntry[4];
		oft[0] = new OpenFileEntry();
		oft[0].setNewFile(0);
	}

	//descriptorIndex is the index within the block, not the descriptor ID
	public void create(char[] symbolicFileName)
	{
		//get directory contents
		char[] directory = new char[MAX_FILE_SIZE];
		oft[0].setPosition(0);
		oft[0].readFile(directory, MAX_FILE_SIZE);

		//find free directory entry
		int openIndex = -1;
		for(int i = 0; i < directory.length && openIndex == -1; i += DIRECTORY_ENTRY_SIZE)
		{
			if(directory[i] == 0)
				openIndex = i;
		}

		//find free file descriptor, fill first disk mapping with -1 if found
		char descriptorIndex = 0;
		char[] fileDescriptorBlock = new char[64];
		for(int i = 0; i < 6 && descriptorIndex == -1; i++)
		{
			iosystem.read_block(i, fileDescriptorBlock);

			for(int j = 0; j < fileDescriptorBlock.length && descriptorIndex == -1; j += FILE_DESCRIPTOR_SIZE)
			{
				//if the file descriptor's first disk mapping is 0 (\uffff denotes empty allocated), it is free
				if(fileDescriptorBlock[j + 1] == 0)
				{
					descriptorIndex = (char) (i * BLOCK_LENGTH + j);
					fileDescriptorBlock[j + 1] = '\uffff';
				}
			}
		}

		//fill directory entry
		for(int i = 0; i < symbolicFileName.length; i++)
			directory[openIndex + i] = symbolicFileName[i];
		directory[openIndex + DIRECTORY_ENTRY_SIZE - 1] = descriptorIndex;
	}

	public boolean destroy(char[] symbolicFileName)
	{
		char[] temp = new char[BLOCK_LENGTH];
		char fileDescriptorIndex = 0;
		int directoryEntryIndex = searchDirectory(symbolicFileName);

		if(directoryEntryIndex == -1)
			return false;

		//get file descriptor index from the directory
		lseek(0, directoryEntryIndex + DIRECTORY_ENTRY_SIZE - 1);
		oft[0].readFile(temp, 1);
		fileDescriptorIndex = temp[0];

		//if the file is in the OFT, close it
		for(int i = 0; i < oft.length; i++)
		{
			if(oft[i] != null && fileDescriptorIndex == oft[i].getFileDescriptorIndex())
				close(i);
		}

		//clear the directory entry
		Arrays.fill(temp, (char) 0);
		lseek(0, directoryEntryIndex);
		oft[0].writeFile(temp, DIRECTORY_ENTRY_SIZE);

		//get disk mapping info from the file descriptor
		int fileDescriptorStart = fileDescriptorIndex % BLOCK_LENGTH;
		char[] diskmap = new char[FILE_DESCRIPTOR_SIZE - 1];
		iosystem.read_block((fileDescriptorIndex / BLOCK_LENGTH) + 1, temp);
		diskmap = Arrays.copyOfRange(temp, fileDescriptorStart + 1, fileDescriptorStart + FILE_DESCRIPTOR_SIZE - 1);
		
		//free file descriptor
		Arrays.fill(temp, (char) fileDescriptorStart, (char) (fileDescriptorStart + FILE_DESCRIPTOR_SIZE - 1), (char) 0);
		iosystem.write_block((fileDescriptorIndex / BLOCK_LENGTH) + 1, temp);

		//use diskmap info to find blocks allocated to this file and deallocate them in the bitmap
		iosystem.read_block(0, temp); //read bitmap into temp
		for(int i = 0; i < diskmap.length; i++)
		{
			if(diskmap[i] > 0)
				temp[diskmap[i]] = 0;
		}
		iosystem.write_block(0, temp); //write changes back into the iosystem
		
		return true;
	}

	//opens a file and into the oft. If no open slots are found, returns -1
	public int open(char[] symbolicFileName)
	{
		int directoryEntryIndex = 0;
		char fileDescriptorIndex = 0;
		int OFTIndex = -1;
		directoryEntryIndex = searchDirectory(symbolicFileName);

		if(directoryEntryIndex == -1)
			return -1;

		//read directory entry and get the file descriptor index
		lseek(0, directoryEntryIndex + DIRECTORY_ENTRY_SIZE - 1);
		char[] temp = new char[1];
		oft[0].readFile(temp, 1);
		fileDescriptorIndex = temp[0];

		//find free open file entry
		for(int i = 0; i < oft.length && OFTIndex == -1; i++)
		{
			if(oft[i] == null)
			{
				OFTIndex = i;
				oft[i] = new OpenFileEntry();
				oft[i].setNewFile(fileDescriptorIndex);
			}
		}

		return OFTIndex;
	}

	public void close(int OFTIndex)
	{
		oft[OFTIndex].close();
		oft[OFTIndex] = null;
	}

	public int read(int OFTIndex, char[] memArea, int count)
	{
		return oft[OFTIndex].readFile(memArea, count);
	}

	public int write(int OFTIndex, char[] memArea, int count)
	{
		return oft[OFTIndex].writeFile(memArea, count);
	}

	public void lseek(int OFTIndex, int position)
	{
		oft[OFTIndex].setPosition(position);
	}

	//return list of files by reading through the directory
	public void directory()
	{
		//read directory from memory
		char[] directory = new char[MAX_FILE_SIZE];
		oft[0].setPosition(0);
		oft[0].readFile(directory, MAX_FILE_SIZE);

		//iterate through directory, printing each file name
		for(int i = 0; i < MAX_FILE_SIZE; i++)
		{
			char current = directory[i];

			//if first name index is blank, assume there is no file and move on to the next entry
			if(current == 0 && i % DIRECTORY_ENTRY_SIZE == 0)
				i += DIRECTORY_ENTRY_SIZE;
			else
			{
				//print the current char if it's not blank. The last index in an entry is not reserved for name
				if(current != 0 && (i % DIRECTORY_ENTRY_SIZE) != (DIRECTORY_ENTRY_SIZE - 1))
					System.out.print(current);

				//if a blank is reached, assume name has ended and move on the next entry
				else
				{
					System.out.print(" ");
					i += DIRECTORY_ENTRY_SIZE - (i % DIRECTORY_ENTRY_SIZE);
				}
			}
		}
	}

	public void save(String fileName)
	{
		for(int i = 0; i < oft.length; i++)
			close(i);
		iosystem.save(fileName);
	}

	//find matching directory entry and return its index, returns -1 if no match is found
	private int searchDirectory(char[] symbolicFileName)
	{
		char[] directory = new char[MAX_FILE_SIZE];
		oft[0].setPosition(0);
		oft[0].readFile(directory, MAX_FILE_SIZE);

		for(int i = DIRECTORY_ENTRY_SIZE; i < directory.length; i += DIRECTORY_ENTRY_SIZE)
		{
			//first char matches, continue comparing name
			if(directory[i] == symbolicFileName[0])
			{
				boolean match = true;
				for(int j = 1; j < symbolicFileName.length && match; i++)
				{
					if(directory[j + i] != symbolicFileName[j])
						match = false;
				}
				if(match)
					return i;
			}
		}
		return -1;
	}
}