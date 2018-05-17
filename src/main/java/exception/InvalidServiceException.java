package exception;

public class InvalidServiceException extends Exception{
	public static final long serialVersionUID=1L;
	
	public InvalidServiceException(String message)
	{
		super(message);
	}
}
