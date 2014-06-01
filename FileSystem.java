
public class FileSystem 
{
	private class OpenFileEntry //Open File Table
	{
		private char[] buffer;
		private int currentPosition;
		private int descriptorIndex;

		public OpenFileEntry()
		{
			buffer = new char[64];
			currentPosition = 0;
			descriptorIndex = -1;
		}

		public boolean write(char[] toWrite)
		{
			for(int i = currentPosition, int j = 0; i < buffer.length && j < toWrite.length; i++)
			{
				buffer[i] = toWrite[j];
			}
		}

		public char[] readFile(int memArea, count)
		{
			if(memArea >= 192)
				throw InvalidInputException();

			//count is limited by the 192 byte max file size
			if(memArea + count >= 192)
				count = 191 - memArea;
			char[] output = new char[count];

			int descriptorBlockIndex = descriptorIndex / BLOCK_LENGTH; //get ldisk index of file descriptor
			int descriptorLocationInBlock = descriptorIndex - descriptorBlockIndex * BLOCK_LENGTH;
			iosystem.read(descriptorIndex, buffer);
			char[] descriptor = Arrays.copyOfRange(buffer, locationInBlock, locationInBlock + FILE_DESCRIPTOR_SIZE);

			diskIndex = memArea / BLOCK_LENGTH;
			iosystem.read(descriptor[diskIndex], buffer);

			//iterate through each index of each necessary disk block
			for(int i = 0; i < count; i++)
			{
				int pos = (memArea + i) % BLOCK_LENGTH;
				output[i] = buffer[pos];
				
				if(pos % BLOCK_LENGTH == (BLOCK_LENGTH - 1))
				{
					descriptorIndex++;
					iosystem.read(descriptorIndex, buffer);
				}
			}
			return output;
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

	public char[] read(int OFTIndex, int memArea, int count)
	{
		return oft[OFTIndex].readFile(memArea, count);
	}

	public void write(int OFTIndex, int memArea, int count)
	{

	}

	public void lseek(int OFTIndex, int memArea)
	{

	}

	public void directory()
	{
		//print file names
	}
}