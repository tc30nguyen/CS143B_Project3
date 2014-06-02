
public class FileSystem 
{
	private class OpenFileEntry //Open File Table
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
		public boolean writeFile(char[] memArea, int count)
		{
			int currentBlock = getCurrentBlock();
			if(currentPosition + count >= fileDescriptor[0])
				count = fileDescriptor[0] - currentPosition;

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

			fileDescriptor[0] += count;
		}

		//handle case where currentPosition is > current file size
		public void readFile(int[] memArea, count)
		{
			if(count >= MAX_FILE_SIZE)
				throw InvalidInputException();

			//limit the read count to the file's current size
			if(currentPosition + count >= fileDescriptor[0])
				count = fileDescriptor[0] - currentPosition;

			//read the file's first ldisk block into the buffer
			/*int currentBlock = getCurrentBlock;
			iosystem.read_block(fileDescriptor[currentBlock], buffer);*/

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
		}

		private int getCurrentBlock()
		{
			return currentPosition / BLOCK_LENGTH + 1;
		}

		private void getFileDescriptor()
		{
			char[] temp = new char[BLOCK_LENGTH];
			int descriptorLocation = ((descriptorIndex - 1) * FILE_DESCRIPTOR_SIZE);
			int descriptorLocationInBlock = descriptorLocation % BLOCK_LENGTH;
			iosystem.read(descriptorLocation / BLOCK_LENGTH, temp);

			for(int i = 0; i < FILE_DESCRIPTOR_SIZE; i++)
				fileDescriptor[i] = temp[descriptorLocationInBlock + i];
		}

		public char[] getBuffer()
		{
			return buffer;
		}

		public void writeBuffer(char[] toWrite)
		{
			for(int i = currentPosition, j = ; i < t)
		}

		public int getPosition()
		{
			return currentPosition;
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

		public int getDescriptorIndex()
		{
			return descriptorIndex;
		}

		public void setDescriptorIndex(int index)
		{
			descriptorIndex = index;
		}
	}

	final int BLOCK_LENGTH = 64;
	final int FILE_DESCRIPTOR_SIZE = 4; //1 for file length, 3 for the file's max 3 disk blocks
	final int MAX_FILE_SIZE = 192;
	final int NUM_OF_BLOCKS = 64;
	int k; //first k blocks, reserved for the bitmap and file descriptors
	IOSystem iosystem;
	OpenFileEntry[] oft; //4 OpenFileEntrys, OpenFileEntry[0] is reserved for the directory file and 3 are can hold open files

	public FileSystem(int k)
	{
		this.k = k;
		iosystem = new IOSystem(NUM_OF_BLOCKS, BLOCK_LENGTH);
		oft = new OpenFileEntry[4];
		oft[0].setDescriptorIndex(0);
	}

	public void create(char[] symbolicFileName)
	{
		char[] buffer = Arrays.copyOf()
	}

	public void destroy(char[] symbolicFileName)
	{

	}

	public void open(char[] symbolicFileName)
	{

	}

	public void close(int OFTIndex)
	{

	}

	public void read(int OFTIndex, int[] memArea, int count)
	{
		oft[OFTIndex].readFile(memArea, count);
	}

	public void write(int OFTIndex, int[] memArea, int count)
	{
		oft[OFTIndex].writeFile(memArea, count);
	}

	public void lseek(int OFTIndex, int position)
	{
		oft[OFTIndex].setPosition(position);
	}

	public void directory()
	{
		//print file names
	}
}