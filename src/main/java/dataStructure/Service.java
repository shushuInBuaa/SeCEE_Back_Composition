package dataStructure;

public class Service {
	String name;
	String url;
	String parameters;
	String returnValue;
	
	public Service(String name, String url, String parameters, String returnValue)
	{
		this.name=name;
		this.url=url;
		this.parameters=parameters;
		this.returnValue=returnValue;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getURL()
	{
		return url;
	}
	
	public String getParameters()
	{
		return parameters;
	}
	
	public String getReturnValue()
	{
		return returnValue;
	}
}
