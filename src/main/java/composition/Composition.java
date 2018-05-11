package composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

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
	
	public Composition(String activityName, String params)
	{
		this.activityName=activityName;
		this.params=params;
		this.ontology=new OWLHelper();
		
		this.parameters=new HashMap<String, Variable>();
		this.properties=new HashMap<String, Variable>();
		
		ontology.readOntologyFile("files/ontology.owl");
	}
	
	public void decompose()
	{
		seperatePropertiesAndParameters();
		String mainServiceName=ontology.getMainServiceNameByActivity(activityName);
		if(properties==null)
		{
			searchAndExecutePlan(mainServiceName);
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
				String name=((JsonObject)elem).get("name").toString();
				String type=((JsonObject)elem).get("type").toString();
				ontologyParameters.put(name, type);
			}
			
			for(JsonElement elem:arrayOntologyProperties)
			{
				String name=((JsonObject)elem).get("name").toString();
				String type=((JsonObject)elem).get("type").toString();
				ontologyProperties.put(name, type);
			}
			
			
			//解析用户传参
			JsonArray arrayUser=(JsonArray)parser.parse(params);
			for(JsonElement elem:arrayUser)
			{
				String name=((JsonObject)elem).get("name").toString();
				String value=((JsonObject)elem).get("value").toString();
				
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
	
	Object searchAndExecutePlan(String abstractServiceName)
	{
		Object result=new Object();	
		
		HashMap<String,HashMap<String, RDFNode>> candidateInstantiation=ontology.getInstantiationForAbstractService(abstractServiceName);
		HashMap<String,HashMap<String, RDFNode>> candidateComposition=ontology.getCompositionForAbstractService(abstractServiceName);
		
		HashMap<String, RDFNode> bestPlan=searchForTheBestPlan(candidateInstantiation,candidateComposition);
		
		//bestPlan中还有abstractservice就继续分解，递归
		if()
		
		return result;
	}
	
	HashMap<String, RDFNode> searchForTheBestPlan(HashMap<String,HashMap<String, RDFNode>> candidateInstantiation, HashMap<String,HashMap<String, RDFNode>> candidateComposition)
	{
		HashMap<String, RDFNode> bestPlan;

		//所有服务
		//查找可直接执行的Instantiation中优先级最高的
		
		HashMap<String, RDFNode> bestInstantiation=new HashMap<String, RDFNode>();
		int bestInstantiationPriority=-1;
		for(Entry e:candidateInstantiation.entrySet())
		{
			int priority=ontology.getPriorityForCompositionOrInstantiation(e.getKey().toString(),false);
			if(bestInstantiationPriority<priority)
			{
				bestInstantiationPriority=priority;
				bestInstantiation=(HashMap<String, RDFNode>)e.getValue();
			}
		}
		
		//查找需继续进行分解的Composition中优先级最高的
		HashMap<String, RDFNode> bestComposition=new HashMap<String, RDFNode>();
		int bestCompositionPriority=-1;
		for(Entry e:candidateComposition.entrySet())
		{
			int priority=ontology.getPriorityForCompositionOrInstantiation(e.getKey().toString(),true);
			if(bestCompositionPriority<priority)
			{
				bestCompositionPriority=priority;
				bestComposition=(HashMap<String, RDFNode>)e.getValue();
			}
		}
		
		if(bestCompositionPriority>bestInstantiationPriority)
			return bestComposition;
		else
			return bestInstantiation;
	}
	
	//如果有properties，那么就会从多个Instantiation中筛选出多个service
	Object searchForServiceFilteredByPropertiesAndExecute(String mainServiceName)
	{
		Object result=new Object();
		HashMap<String,HashMap<String, RDFNode>> candidateInstantiation=ontology.getInstantiationForAbstractService(mainServiceName);
		return result;
	}
	
	//执行plan列表中的服务，如果服务是多个，则需要按照combineService指出的方法进行结果合并
	Object execute()
	{
		Object result=new Object();
		
		return result;
	}
	
}
