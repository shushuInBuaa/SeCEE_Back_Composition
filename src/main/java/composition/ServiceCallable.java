package composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import dataStructure.Service;
import exception.InvalidServiceException;

public class ServiceCallable implements Callable<Object>{
	Service service;
	
	public ServiceCallable(Service service)
	{
		this.service=service;
	}
	
	public Object call() throws InvalidServiceException
	{
		Object result=null;
		String httpResult="";
		
		CloseableHttpClient client=HttpClients.createDefault();
		CloseableHttpResponse response=null;
		
		RequestConfig config=RequestConfig.custom().setConnectTimeout(35000)
				.setConnectionRequestTimeout(35000)
				.setSocketTimeout(60000)
				.build();
		
		try
		{
			URIBuilder builder=new URIBuilder(service.getURL());
			
			if(service.getParameters()!=null)
			{			
				builder.setParameter("params",service.getParameters());
			}
			HttpGet get=new HttpGet(builder.build());
			get.setConfig(config);
			get.addHeader("Content-Type","application/json;charset=utf-8");
			
			response=client.execute(get);
			HttpEntity entity=response.getEntity();
			httpResult=EntityUtils.toString(entity);
			//httpResult返回服务请求任务编号
			//轮询，调用getResult服务在数据库中查找相应服务请求任务编号的结果
			//一旦查找到了就停止工作，进行resultTypeConvert();
			
			result=resultTypeConvert(httpResult);
		}
		catch(Exception e)
		{
			System.out.println("服务"+service.getName()+"不可用");
			//e.printStackTrace();
			 
		}
		finally {
			try {
				response.close();
				client.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	public Object resultTypeConvert(String httpResult)
	{
		Object obj=null;
		
		JsonParser parser=new JsonParser();
		JsonObject jsonObj=(JsonObject)parser.parse(httpResult);
		
		HashMap<String, String> returnValuesSchema=parseReturnValue();
		
		Iterator<Entry<String,JsonElement>> iterator=jsonObj.entrySet().iterator();
		while(iterator.hasNext())
		{
			Entry<String,JsonElement> entry=iterator.next();
			String returnValueName=entry.getKey();
			JsonElement value=entry.getValue();
			
			if(returnValuesSchema.containsKey(returnValueName))
			{
				String type=returnValuesSchema.get(returnValueName);
				obj=returnResultByType(value,type);
			}	
		}
		
		return obj;
	}
	
	private HashMap<String, String> parseReturnValue()
	{
		HashMap<String, String> map=new HashMap<String, String>();
		
		JsonParser parser=new JsonParser();
		JsonArray returnValues=(JsonArray)parser.parse(service.getReturnValue());
		for(JsonElement elem:returnValues)
		{
			String name=((JsonObject)elem).get("name").getAsString();
			String type=((JsonObject)elem).get("type").getAsString();
			map.put(name, type);
		}
		
		return map;
	}
	
	private Object returnResultByType(JsonElement value, String type) 
	{
		//type的种类可以是int, double, float, List<T>, String, T只支持Integer, Double, Float, String和List<T>
		if(type.equals("int"))
		{
			return value.getAsInt();
		}
		else if(type.equals("double"))
		{
			return value.getAsDouble();
		}
		else if(type.equals("float"))
		{
			return value.getAsFloat();
		}
		else if(type.contains("List"))
		{
			String T=type.substring(type.indexOf("<"),type.lastIndexOf(">")-1);
			ArrayList result=new ArrayList();
			
			JsonArray values=value.getAsJsonArray();
			for(JsonElement _value:values)
			{
				result.add(returnResultByType(_value,T));
			}
			
			return result;
		}
		else if(type.equals("String"))
		{
			return type;
		}
		else
		{
			return null;
		}
	}
}
