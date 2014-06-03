
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
			int currentBlock = getCurrentBlock();
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
					iosystem.write_block(fileDescriptor[currentBlock++], buffer);
					iosystem.read_block(fileDescriptor[currentBlock], buffer);
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

			int currentBlock = getCurrentBlock();

			//copy each requested char from the buffer into memArea
			for(int i = 0; count > 0; count--, i++)
			{
				memArea[i] = buffer[currentPosition % BLOCK_LENGTH];
				currentPosition++;

				//read the next ldisk block into the buffer when necessary
				if(currentPosition % BLOCK_LENGTH == 0)
				{
					iosystem.write_block(fileDescriptor[currentBlock++], buffer);
					iosystem.read_block(fileDescriptor[currentBlock], buffer);
				}
			}

			return numOfBytesRead;
		}

		private void getFileDescriptor()
		{
			char[] temp = new char[BLOCK_LENGTH];
			int descriptorLocationInBlock = descriptorIndex % BLOCK_LENGTH;
			iosystem.read_block(descriptorIndex / BLOCK_LENGTH, temp);

			for(int i = 0; i < FILE_DESCRIPTOR_SIZE; i++)
				fileDescriptor[i] = temp[descriptorLocationInBlock + i];
		}

		public void setPosition(int pos)
		{
			int currentBlock = getCurrentBlock();
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
		
		private int getCurrentBlock()
		{
			return currentPosition / BLOCK_LENGTH + 1;
		}
		
		public int getPosition()
		{
			return currentPosition;
		}
		
		public int getDescriptorIndex()
		{
			return descriptorIndex;
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

	public void destroy(char[] symbolicFileName)
	{

	}

	//opens a file and into the oft. If no open slots are found, returns -1
	public int open(char[] symbolicFileName)
	{
		char[] directory = new char[MAX_FILE_SIZE];
		char fileDescriptorIndex = 0;
		int OFTIndex = -1;

		//get directory contents
		oft[0].setPosition(0);
		oft[0].readFile(directory, MAX_FILE_SIZE);

		//find matching directory entry
		for(int i = DIRECTORY_ENTRY_SIZE; i < directory.length && fileDescriptorIndex == 0; i += DIRECTORY_ENTRY_SIZE)
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
					fileDescriptorIndex = (char) i;
			}
		}
		if(fileDescriptorIndex == 0)
			return -1;
			//throw FileNotFoundException();

		//find free open file entry
		for(int i = 0; i < oft.length && OFTIndex == -1; i++)
		{
			if(oft[i] == null)
			{
				OFTIndex = i;
				oft[i].setNewFile(fileDescriptorIndex);
			}
		}

		return OFTIndex;
	}

	public void close(int OFTIndex)
	{

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

	//public char[][] directory()
	{
		//print file names
	}

	public void save(String fileName)
	{
		for(int i = 0; i < oft.length; i++)
			close(i);
		iosystem.save(fileName);
	}
}