
public class Driver 
{
	public static void main(String[] args)
	{
		Shell shell;
		if(args.length == 0)
			shell = new Shell();
		else
			shell = new Shell(args[0]);
		
		shell.run();
	}
}
