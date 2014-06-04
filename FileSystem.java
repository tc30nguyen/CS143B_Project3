import java.util.Arrays;

public class FileSystem 
{
	private class OpenFileEntry
	{
		private char[] buffer;
		private char[] fileDescriptor;
		int[] DEBUGGING = new int[4];
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
			int currentDescriptorIndex = getCurrentDescriptorMapIndex();
			if(currentPosition + count >= MAX_FILE_SIZE)
				count = MAX_FILE_SIZE - currentPosition;
			for(int i = 0; i < count; i++, currentPosition++)
			{
				if(i != 0 && currentPosition % BLOCK_LENGTH == 0)
				{
					iosystem.write_block(getBlockIndex(currentDescriptorIndex++), buffer);
					iosystem.read_block(getBlockIndex(currentDescriptorIndex), buffer);
				}

				buffer[currentPosition % BLOCK_LENGTH] = memArea[i];
			}
			
			fileDescriptor[0] += count;
			updateFileDescriptor();
			return count;
		}

		//handle case where currentPosition is > current file size
		public int readFile(char[] memArea, int count) //throws Exception
		{
			if(count > MAX_FILE_SIZE)
				return -1;

			//limit the read count to the file's current size
			getFileDescriptor();
			if(currentPosition + count >= fileDescriptor[0])
				count = fileDescriptor[0] - currentPosition;
			int numOfBytesRead = count;

			int currentBlock = getCurrentDescriptorMapIndex();

			//copy each requested char from the buffer into memArea
			for(int i = 0; count > 0; count--, i++, currentPosition++)
			{
				//read the next ldisk block into the buffer when necessary
				if(i != 0 && currentPosition % BLOCK_LENGTH == 0)
				{
					iosystem.write_block(getBlockIndex(currentBlock++), buffer);
					iosystem.read_block(getBlockIndex(currentBlock), buffer);
				}

				memArea[i] = buffer[currentPosition % BLOCK_LENGTH];
			}

			return numOfBytesRead;
		}
		
		public void close()
		{
			iosystem.write_block(getBlockIndex(getCurrentDescriptorMapIndex()), buffer);
			
			//write file descriptor to disk
			int descriptorBlock = descriptorIndex / BLOCK_LENGTH + 1;
			int descriptorLocationInBlock = descriptorIndex % BLOCK_LENGTH;
			iosystem.read_block(descriptorBlock, buffer);
			for(int i = 0; i < FILE_DESCRIPTOR_SIZE; i++)
				buffer[descriptorLocationInBlock + i] = fileDescriptor[i];
			iosystem.write_block(descriptorBlock, buffer);
		}
		
		public void writeToDisk()
		{
			int currentDescriptorIndex = getCurrentDescriptorMapIndex();
			iosystem.write_block(getBlockIndex(currentDescriptorIndex), buffer);
		}

		//return the block mapped to a currentDescriptorIndex, allocating one if it is not mapped
		private char getBlockIndex(int currentDescriptorIndex)
		{
			if(fileDescriptor[currentDescriptorIndex] == 0 || fileDescriptor[currentDescriptorIndex] == '\uffff')
			{
				fileDescriptor[currentDescriptorIndex] = 0;
				char[] bitmap = new char[64];
				iosystem.read_block(0, bitmap);

				//search bitmap for an open block, skipping the reserved K blocks
				for(int i = K; i < bitmap.length && fileDescriptor[currentDescriptorIndex] == 0; i++)
				{
					if(bitmap[i] == 0)
					{
						fileDescriptor[currentDescriptorIndex] = (char) i;
						DEBUGGING[currentDescriptorIndex] = i; //REMOVE AFTERWARDS
						bitmap[i] = 1;
						iosystem.write_block(0, bitmap);
						
						//clear the newly allocated block to initialize
						char[] clearBlock = new char[BLOCK_LENGTH];
						iosystem.write_block(i, clearBlock);
						
						//write the updated file descriptor to disk
						updateFileDescriptor();
					}
				}
			}
			
			return fileDescriptor[currentDescriptorIndex];
		}
		
		private void updateFileDescriptor()
		{
			char[] temp = new char[64];
			int blockIndex = descriptorIndex / BLOCK_LENGTH + 1;
			iosystem.read_block(blockIndex, temp);
			int indexInBlock = descriptorIndex % BLOCK_LENGTH;
			for(int j = 0; j < FILE_DESCRIPTOR_SIZE; j++)
				temp[indexInBlock + j] = fileDescriptor[j];
			iosystem.write_block(blockIndex, temp);
		}

		private void getFileDescriptor()
		{
			char[] temp = new char[BLOCK_LENGTH];
			int descriptorLocationInBlock = descriptorIndex % BLOCK_LENGTH;
			iosystem.read_block(descriptorIndex / BLOCK_LENGTH + 1, temp);

			for(int i = 0; i < FILE_DESCRIPTOR_SIZE; i++)
				fileDescriptor[i] = temp[descriptorLocationInBlock + i];
		}

		public int getFileDescriptorIndex()
		{
			return descriptorIndex;
		}

		public void setPosition(int pos)
		{
			int currentDescriptorIndex = getCurrentDescriptorMapIndex();
			int newDescriptorIndex = pos / BLOCK_LENGTH + 1;
			
			//if changing blocks, first write current block to disk
			if(newDescriptorIndex != currentDescriptorIndex)
			{
				iosystem.write_block(fileDescriptor[currentDescriptorIndex], buffer);
				iosystem.read_block(fileDescriptor[newDescriptorIndex], buffer);
			}

			currentPosition = pos;
		}

		public void setNewFile(int fileDescriptorIndex)
		{
			descriptorIndex = fileDescriptorIndex;
			getFileDescriptor();
			currentPosition = 0;
			iosystem.read_block(getBlockIndex(1), buffer); //read ahead
		}
		
		//returns descriptor map index, rewinds to 0 if position reaches max file size
		private int getCurrentDescriptorMapIndex()
		{
			int currentDescriptorIndex = currentPosition / BLOCK_LENGTH + 1;
			
			//rewind to 0 if reached max file size
			if(currentDescriptorIndex == (MAX_FILE_SIZE / BLOCK_LENGTH + 1))
			{
				currentDescriptorIndex--;
				iosystem.write_block(fileDescriptor[currentDescriptorIndex], buffer);

				currentPosition = 0;
				currentDescriptorIndex = 1;
				iosystem.read_block(fileDescriptor[currentDescriptorIndex], buffer);
			}

			return currentDescriptorIndex;
		}
	}

	private final int BLOCK_LENGTH = 64;
	private final int DIRECTORY_ENTRY_SIZE = 5;
	private final int FILE_DESCRIPTOR_SIZE = 4; //1 for file length, 3 for the file's max 3 disk blocks
	private final int MAX_FILE_SIZE = 192;
	private final int NUM_OF_BLOCKS = 64;
	private final int K = 7; //first k blocks, reserved for the bitmap and file descriptors
	private IOSystem iosystem;
	private OpenFileEntry[] oft; //4 OpenFileEntrys, OpenFileEntry[0] is reserved for the directory file and 3 are can hold open files

	public FileSystem()
	{
		iosystem = new IOSystem(NUM_OF_BLOCKS, BLOCK_LENGTH);
		oft = new OpenFileEntry[4];
		oft[0] = new OpenFileEntry();
		oft[0].setNewFile(0);
	}
	
	public FileSystem(String inputFile)
	{
		iosystem = new IOSystem(NUM_OF_BLOCKS, BLOCK_LENGTH, inputFile);
		oft = new OpenFileEntry[4];
		oft[0] = new OpenFileEntry();
		oft[0].setNewFile(0);
	}

	//descriptorIndex is the index within the block, not the descriptor ID
	public boolean create(char[] symbolicFileName)
	{
		//get directory contents
		char[] directory = new char[MAX_FILE_SIZE];
		oft[0].setPosition(0);
		oft[0].readFile(directory, MAX_FILE_SIZE);

		//check if file already exists
		if(searchDirectory(symbolicFileName) != -1)
			return false;
		
		//find free directory entry
		char openIndex = '\uffff';
		for(int i = 0; i < directory.length && openIndex == '\uffff'; i += DIRECTORY_ENTRY_SIZE)
		{
			//assume empty entry if null name
			if(directory[i] == 0)
				openIndex = (char) i;
		}
		
		//if cannot find a free directory entry
		if(openIndex == '\uffff')
			return false;

		//iterate through each block of descriptors to find free file descriptor, fill first disk mapping with -1 if found
		char descriptorIndex = 0;
		char[] fileDescriptorBlock = new char[64];
		for(int i = 1; i < K && descriptorIndex == 0; i++)
		{
			iosystem.read_block(i, fileDescriptorBlock);
			for(int j = 0; j < fileDescriptorBlock.length && descriptorIndex == 0; j += FILE_DESCRIPTOR_SIZE)
			{
				//if the file descriptor's first disk mapping is 0 (\uffff denotes empty allocated), it is free
				if(fileDescriptorBlock[j + 1] == 0)
				{
					descriptorIndex = (char) (i * BLOCK_LENGTH + j);
					fileDescriptorBlock[j + 1] = '\uffff';
					iosystem.write_block(i, fileDescriptorBlock);
				}
			}
		}

		//fill directory entry
		for(int i = 0; i < symbolicFileName.length; i++)
			directory[openIndex + i] = symbolicFileName[i];
		directory[openIndex + DIRECTORY_ENTRY_SIZE - 1] = descriptorIndex;
		oft[0].writeFile(directory, directory.length);
		oft[0].writeToDisk();
		
		return true;
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
		if(oft[OFTIndex] != null)
			oft[OFTIndex].close();
		oft[OFTIndex] = null;
	}

	public int read(int OFTIndex, char[] memArea, int count)
	{
		if(oft[OFTIndex] == null)
			return -1;
		else
			return oft[OFTIndex].readFile(memArea, count);
	}

	public int write(int OFTIndex, char[] memArea, int count)
	{
		if(oft[OFTIndex] == null)
			return -1;
		else
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
				i += DIRECTORY_ENTRY_SIZE - 1;
			else
			{
				//print the current char if it's not blank. The last index in an entry is not reserved for name
				if(current != 0 && (i % DIRECTORY_ENTRY_SIZE) != (DIRECTORY_ENTRY_SIZE - 1))
					System.out.print(current);

				//if a blank is reached, assume name has ended and move on the next entry
				else
				{
					System.out.print(" ");
					i += (DIRECTORY_ENTRY_SIZE - 1) - (i % DIRECTORY_ENTRY_SIZE);
				}
			}
		}
		System.out.print("\n");
	}

	public void save(String fileName)
	{
		for(int i = 1; i < oft.length; i++)
			close(i);
		iosystem.save(fileName);
		oft[0] = new OpenFileEntry();
		oft[0].setNewFile(0);
	}

	//find matching directory entry and return its index, returns -1 if no match is found
	private int searchDirectory(char[] symbolicFileName)
	{
		char[] directory = new char[MAX_FILE_SIZE];
		oft[0].setPosition(0);
		oft[0].readFile(directory, MAX_FILE_SIZE);

		for(int i = 0; i < directory.length; i += DIRECTORY_ENTRY_SIZE)
		{
			//first char matches, continue comparing name
			if(directory[i] == symbolicFileName[0])
			{
				boolean match = true;
				for(int j = 1; j < symbolicFileName.length && match; j++)
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