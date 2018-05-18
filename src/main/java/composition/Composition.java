package composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.jena.rdf.model.RDFNode;

import com.google.gson.*;
import dataStructure.*;

public class Composition {
	String activityName;
	String params;
	OWLHelper ontology;
	HashMap<String, Variable> parameters;
	HashMap<String, Variable> properties;
	HashMap<String, String> plan; 
	String combineService;
	ArrayList<RDFNode> usedPlan;//已使用过的Instantiation或Composition，一旦复合服务没有响应则需要更换没有使用过的Instantiation和Composition
	HashMap<String, Boolean> noPlan=new HashMap<String,Boolean>();
	
	public Composition(String activityName, String params)
	{
		this.activityName=activityName;
		this.params=params;
		this.ontology=new OWLHelper();
		
		this.parameters=new HashMap<String, Variable>();
		this.properties=new HashMap<String, Variable>();
		this.usedPlan=new ArrayList<RDFNode>();
		
		ontology.readOntologyFile("files/ontology.owl");
	}
	
	public void decompose() throws Exception
	{
		boolean noPlan=false;
		seperatePropertiesAndParameters();
		String mainServiceName=ontology.getMainServiceNameByActivity(activityName);
		if(properties.size()==0)
		{
			System.out.println("————————————启动组合————————————————");
			Object result=searchAndExecutePlan(mainServiceName);
			while(result==null&&!noPlan)
			{
				//重新组合
				if(result==null)
					System.out.println("（不一定对）方案不可用："+usedPlan.get(usedPlan.size()-1));
				System.out.println("重新组合");
				result=searchAndExecutePlan(mainServiceName);
			}
			
			if(noPlan)
			{
				System.out.println("没有方案了！");
			}
			else
			{
				System.out.println("最终结果为: "+result);
			}
		}
		else
		{
			//从所有Instantiation中挑选相应符合标准的service
			searchForServiceFilteredByPropertiesAndExecute(mainServiceName);
			
		}

	}
	
	//将用户传参中混合着的properties和parameters分开
	void seperatePropertiesAndParameters()
	{
		try
		{
			JsonParser parser=new JsonParser();

			
			//解析activity相应的parameters和properties
			HashMap<String, String> map=ontology.getActivityParametersAndPropertiesByName(activityName);
			String parameters=map.get("parameters");
			String properties=map.get("properties");
			HashMap<String, String> ontologyParameters=new HashMap();
			HashMap<String, String> ontologyProperties=new HashMap();
			
			JsonArray arrayOntologyParameters=(JsonArray)parser.parse(parameters);
			JsonArray arrayOntologyProperties=(JsonArray)parser.parse(properties);
			for(JsonElement elem:arrayOntologyParameters)
			{
				String name=((JsonObject)elem).get("name").getAsString();
				String type=((JsonObject)elem).get("type").getAsString();
				ontologyParameters.put(name, type);
			}
			
			for(JsonElement elem:arrayOntologyProperties)
			{
				String name=((JsonObject)elem).get("name").getAsString();
				String type=((JsonObject)elem).get("type").getAsString();
				ontologyProperties.put(name, type);
			}
			
			
			//解析用户传参
			JsonArray arrayUser=(JsonArray)parser.parse(params);
			for(JsonElement elem:arrayUser)
			{
				String name=((JsonObject)elem).get("name").getAsString();
				String value=((JsonObject)elem).get("value").getAsString();
				
				if(ontologyParameters.containsKey(name))
					this.parameters.put(name, new Variable(name, ontologyParameters.get(name),value));
				else if(ontologyProperties.containsKey(name))
					this.properties.put(name, new Variable(name, ontologyParameters.get(name),value));
			}
			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	Object searchAndExecutePlan(String abstractServiceName) throws Exception
	{
		Object result=null;	
		
		HashMap<RDFNode, Integer> candidateInstantiation=ontology.getInstantiationNodeForAbstractService(abstractServiceName);
		HashMap<RDFNode, Integer> candidateComposition=ontology.getComponentsNodeForAbstractService(abstractServiceName);//error: 为空
		
		RDFNode bestPlan=searchForTheBestPlan(candidateInstantiation,candidateComposition);
		usedPlan.add(bestPlan);
		
		//bestPlan中还有abstractservice就继续分解，递归
		if(bestPlan!=null)
		{	
			noPlan.put(abstractServiceName, true);
			System.out.println("采用方案"+bestPlan.toString());
			
			if(ontology.isComposition(bestPlan))
			{
				ArrayList<String> abstractServices=ontology.getServiceByComponentsNode(bestPlan);
				ArrayList<Object> results=new ArrayList<Object>();
				for(String abstractService: abstractServices)
				{
					Object _result=searchAndExecutePlan(abstractService);
					
					//需要重新进行组合
					while(_result==null&&!noPlan.get(abstractServiceName))
					{
						if(_result==null)
							System.out.println("（不一定对）方案不可用："+usedPlan.get(usedPlan.size()-1));
						System.out.println("---------重新组合----------");
						searchAndExecutePlan(abstractService);
					}
					
					if(_result==null)
					{
						System.out.println("该方案存在不可用的子方案，需要重新组合");
						return null;
					}
					
					results.add(_result);
				}
				
				//combine的形式有两种，一个是求和，一个是求并集
				
				if(results.size()>0)
				{
					result=combineResult(results);
				}
				else
					noPlan.put(abstractServiceName, true);
				
				return result;
			}
			else
			{
				ArrayList<Service> entityServices=ontology.getServicesByInstantiationNode(bestPlan);
				ArrayList<FutureTask> futures=new ArrayList<FutureTask>();
				ArrayList<Object> resultsList=new ArrayList<Object>();
				
				Gson gson=new Gson();
				String params="";
				
				if(parameters.size()!=0)
					params=gson.toJson(parameters);
				else 
					params=null;
				ExecutorService pool=Executors.newCachedThreadPool();
							
				if(entityServices.size()>0)
				{
					for(Service service:entityServices)
					{
						ServiceCallable callable=new ServiceCallable(service);
						FutureTask<Object> task=new FutureTask<Object>(callable);
						futures.add(task);
						pool.submit(task);
					}
					
					for(FutureTask task:futures)
					{
						Object obj=task.get();
						if(obj!=null)
						{
							//right
							resultsList.add(obj);
						}
						else
						{
							//wrong
							System.out.println("有服务不可用");
							return null;//有服务不可用，出现异常，因此没有返回
						}
					}
					
					if(resultsList.size()!=futures.size())
					{
						System.out.println("有服务不可用，提交的服务请求和返回的结果数量不一致");
						return null;//有服务不可用，出现异常，因此没有返回
					}

					//combine的形式有两种，一个是求和，一个是求并集
					if(resultsList.size()>0)
					{
						result=combineResult(resultsList);
					}
					else
						System.out.println("该Instantiation的服务没有返回任何结果");;
				}
				else
				{
					System.out.println("该Instantiation没有服务");;
				}
			}
		}
		else
		{
			noPlan.put(abstractServiceName,true);
		}
		
		return result;
	}
	
	RDFNode searchForTheBestPlan(HashMap<RDFNode, Integer> candidateInstantiation, HashMap<RDFNode, Integer> candidateComposition)
	{
		//所有服务
		//查找可直接执行的Instantiation中优先级最高的
		
		RDFNode bestInstantiation=null;
		int bestInstantiationPriority=Integer.MAX_VALUE;
		for(Entry e:candidateInstantiation.entrySet())
		{
			int priority=(Integer)(e.getValue());
			RDFNode plan=(RDFNode)(e.getKey());
			if(bestInstantiationPriority>priority&&!usedPlan.contains(plan))
			{
				bestInstantiationPriority=priority;
				bestInstantiation=plan;
			}
		}
		
		//查找需继续进行分解的Composition中优先级最高的
		RDFNode bestComposition=null;
		int bestCompositionPriority=Integer.MAX_VALUE;
		for(Entry e:candidateComposition.entrySet())
		{
			int priority=(Integer)(e.getValue());
			RDFNode plan=(RDFNode)e.getKey();
			if(bestCompositionPriority>priority&&!usedPlan.contains(plan))
			{
				bestCompositionPriority=priority;
				bestComposition=plan;
			}
		}
		
		if(bestCompositionPriority<bestInstantiationPriority)
			return bestComposition;
		else
			return bestInstantiation;
	}
	
	Object combineResult(ArrayList<Object> resultList)
	{
		//所有numerical类型结果支持相加ADD
		//所有类型支持返回Map<地区，结果>MAP
		//所有类型支持求并集UNION
		
		
		if(resultList.size()==0)
		{
			System.out.println("结果list不合法，没有元素");
			return null;
		}
		
		Class type=resultList.get(0).getClass();
		
		if(type.equals(Integer.class))
		{
			Integer result=0;
			
			for(Object _result:resultList)
			{
				if(_result instanceof Integer)
					result+=(Integer)_result;
				else
				{
					System.out.println("结果列表中混入了多种类型的结果");
					return null;
				}
			}
			
			return result;
		}
		else if(type.equals(Double.class) ) 
		{
			Double result=0.0;
			
			for(Object _result:resultList)
			{
				if(_result instanceof Double)
					result+=(Double)_result;
				else
				{
					System.out.println("结果列表中混入了多种类型的结果");
					return null;
				}
			}
			
			return result;
		}
		else if(type.equals(Float.class))
		{
			Float result=0f;
			
			for(Object _result:resultList)
			{
				if(_result instanceof Float)
					result+=(Float)_result;
				else
				{
					System.out.println("结果列表中混入了多种类型的结果");
					return null;
				}
			}
			
			return result;
		}
		else if(type.equals(String.class))
		{
			String result="";
			
			for(Object _result:resultList)
			{
				if(_result instanceof String)
					result+=(String)_result;
				else
				{
					System.out.println("结果列表中混入了多种类型的结果");
					return null;
				}
			}
			
			return result;
		}
		else if(type.equals(List.class))
		{
			List<Object> list=new ArrayList();
			
			return list;
		}
		else
		{
			System.out.println("类型不对");
			return null;
		}
	}
	
	//如果有properties，那么就会从多个Instantiation中筛选出多个service
	Object searchForServiceFilteredByPropertiesAndExecute(String mainServiceName)
	{
		Object result=new Object();
		//HashMap<String,HashMap<String, RDFNode>> candidateInstantiation=ontology.getInstantiationForAbstractService(mainServiceName);
		return result;
	}	
}
