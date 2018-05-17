package composition;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;

import dataStructure.Service;

public class OWLHelper {
	String exNs="http://www.semanticweb.org/shushu/ontologies/2018/2/untitled-ontology-257#";
	String PREFIX="PREFIX pro:<http://www.semanticweb.org/shushu/ontologies/2018/2/untitled-ontology-257#> "
			+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
	OntModel model;
	
	public void readOntologyFile(String path)
	{
		FileInputStream in;
		OntModel ontology=ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);		
		
		try {
			File file=new File(path);
			in=new FileInputStream(file);
			ontology.read(in,null);
			in.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		this.model=ontology;
	}
	
	//根据活动名称获取其属性和参数
	public HashMap<String, String> getActivityParametersAndPropertiesByName(String activityName)
	{
		String sparql=PREFIX+"select ?properties ?parameters "
				+ "where{"
				+ " ?obj pro:name '"+activityName+"'. "
				+ " ?obj rdf:type pro:Activity. "
				+ " ?obj pro:properties ?properties. "
				+ " ?obj pro:parameters ?parameters"
				+ "}";

		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		HashMap<String, String> map=new HashMap<String, String>();
		if(results.hasNext())
		{
			QuerySolution result=results.next();
			map.put("parameters", result.get("parameters").toString());
			map.put("properties", result.get("properties").toString());
		}
		return map;
	}
	
	//寻找是否有实例化关系
	public HashMap<RDFNode, Integer> getInstantiationNodeForAbstractService(String abstractServiceName)
	{
		HashMap<RDFNode, Integer> instantiationMap=new HashMap<RDFNode, Integer>();//<instatiation名称,<service名称,service自身>>
				
		String sparql=PREFIX+"select ?priority ?instantiation "
				+ "where{"
				+ " ?obj rdf:type pro:AbstractService. "
				+ " ?obj pro:name '"+abstractServiceName+"'. "
				+ " ?obj pro:hasInstantiation ?instantiation. "
				+ " ?instantiation pro:priority ?priority"
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		while(results.hasNext())
		{
			QuerySolution result=results.next();
			String priority=result.get("priority").toString();
			instantiationMap.put(result.get("instantiation"), Integer.valueOf(priority.substring(0, priority.indexOf("^"))));
		}

		return instantiationMap;
	}
	
	public ArrayList<String> getServiceByComponentsNode(RDFNode components)
	{
		ArrayList<String> services=new ArrayList<String>();
		
		String sparql=PREFIX+"select ?serviceName "
				+ "where{"
				+ "<"+components.toString()+"> pro:hasPart ?service. "
				+ "?service pro:name ?serviceName"
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		while(results.hasNext())
		{
			QuerySolution result=results.next();
			services.add(result.get("serviceName").toString());
		}
		
		return services;
	}
	
	public ArrayList<Service> getServicesByInstantiationNode(RDFNode instantiation)
	{
		ArrayList<Service> services=new ArrayList<Service>();
		
		String sparql=PREFIX+"select ?url ?returnValue ?name ?parameters "
				+ "where{"
				+ "<"+instantiation.toString()+"> pro:hasPart ?service. "
				+ "?service pro:URL ?url."
				+ "?service pro:returnValue ?returnValue."
				+ "?service pro:name ?name. "
				+ "?service pro:parameters ?parameters "
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		while(results.hasNext())
		{
			QuerySolution result=results.next();
			services.add(new Service(result.get("name").toString(),result.get("url").toString(),result.get("parameters").toString(),result.get("returnValue").toString()));
		}
		
		return services;
	}
	

//  public HashMap<String, HashMap<String, RDFNode>> getDetailedInstantiationForAbstractService(String abstractServiceName)
//	{
//		HashMap<String, HashMap<String, RDFNode>> instantiationMap=new HashMap<String, HashMap<String, RDFNode>>();//<instatiation名称,<service名称,service自身>>
//				
//		String sparql=PREFIX+"select ?service ?serviceName ?instantiation "
//				+ "where{"
//				+ " ?obj rdf:type pro:AbstractService. "
//				+ " ?obj pro:name '"+abstractServiceName+"'. "
//				+ " ?obj pro:hasInstantiation ?instantiation. "
//				+ " ?instantiation pro:hasPart ?service."
//				+ " ?service pro:name ?serviceName"
//				+ "}";
//		
//		Query query=QueryFactory.create(sparql);
//		QueryExecution qe=QueryExecutionFactory.create(query, model);
//		ResultSet results=qe.execSelect();
//		
//		while(results.hasNext())
//		{
//			QuerySolution result=results.next();
//			if(instantiationMap.containsKey(result.get("instantiation").toString()))
//			{
//				instantiationMap.get(result.get("instantiation").toString()).put(result.get("serviceName").toString(), result.get("service"));
//			}
//			else
//			{
//				HashMap<String, RDFNode> map=new HashMap<String, RDFNode>();
//				map.put(result.get("serviceName").toString(), result.get("service"));
//				instantiationMap.put(result.get("instantiation").toString(), map);
//			}
//		}
//
//		return instantiationMap;
//	}
 
	
	public String getMainServiceNameByActivity(String activityName)
	{
		String sparql=PREFIX+"select ?service ?serviceName "
				+ "where{"
				+ " ?obj rdf:type pro:Activity. "
				+ " ?obj pro:name '"+activityName+"'. "
				+ " ?obj pro:mainService ?service. "
				+ " ?service pro:name ?serviceName"
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		if(results.hasNext())
		{
			QuerySolution result=results.next();
			return result.get("serviceName").toString();
		}
		return "";
	}
	
	public HashMap<RDFNode, Integer> getComponentsNodeForAbstractService(String abstractServiceName)
	{
		HashMap<RDFNode, Integer> componentsMap=new HashMap<RDFNode, Integer>();//<instatiation名称,<service名称,service自身>>
				
		String sparql=PREFIX+"select ?priority ?components "
				+ "where{"
				+ " ?obj rdf:type pro:AbstractService. "
				+ " ?obj pro:name '"+abstractServiceName+"'. "
				+ " ?obj pro:hasComponents ?components . "
				+ " ?component pro:priority ?priority."
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		while(results.hasNext())
		{
			QuerySolution result=results.next();
			String priority=result.get("priority").toString();
			componentsMap.put(result.get("components"), Integer.valueOf(priority.substring(0, priority.indexOf("^"))));
		}

		return componentsMap;
	}
	
//	public HashMap<String, HashMap<String, RDFNode>> getDetailedCompositionForAbstractService(String abstractServiceName)
//	{
//		HashMap<String, HashMap<String, RDFNode>> compositionMap=new HashMap<String, HashMap<String, RDFNode>>();//<instatiation名称,<service名称,service自身>>
//				
//		String sparql=PREFIX+"select ?service ?serviceName ?composition "
//				+ "where{"
//				+ " ?obj rdf:type pro:AbstractService. "
//				+ " ?obj pro:name '"+abstractServiceName+"'. "
//				+ " ?obj pro:hasComposition ?composition . "
//				+ " ?instantiation pro:hasPart ?service."
//				+ " ?service pro:name ?serviceName"
//				+ "}";
//		
//		Query query=QueryFactory.create(sparql);
//		QueryExecution qe=QueryExecutionFactory.create(query, model);
//		ResultSet results=qe.execSelect();
//		
//		while(results.hasNext())
//		{
//			QuerySolution result=results.next();
//			if(compositionMap.containsKey(result.get("composition").toString()))
//			{
//				compositionMap.get(result.get("composition").toString()).put(result.get("serviceName").toString(), result.get("service"));
//			}
//			else
//			{
//				HashMap<String, RDFNode> map=new HashMap<String, RDFNode>();
//				map.put(result.get("serviceName").toString(), result.get("service"));
//				compositionMap.put(result.get("composition").toString(), map);
//			}
//		}
//
//		return compositionMap;
//	}
	 
	
	public boolean isAbstractService(String serviceName)
	{
		String sparql=PREFIX+"select ?service "
				+ "where{"
				+ " ?service rdf:type pro:AbstractService. "
				+ " ?service pro:name '"+serviceName+"'"
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		if(results.hasNext())
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isComposition(RDFNode node)
	{
		String sparql=PREFIX+"select ?type "
				+ "where{"
				+ " <"+node.toString()+"> rdf:type ?type "
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query,model);
		ResultSet results=qe.execSelect();
		
		if(results.hasNext())
		{
			String type=results.next().toString();
			if(type.contains("Component"))
				return true;
			else
				return false;
		}
		
		return false;
	}
	
	public int getPriorityForCompositionOrInstantiation(String name, boolean isComposition)
	{
		int priority=-1;
		
		String sparql;
		if(isComposition)
			sparql=PREFIX+"select ?priority "
					+ "where{"
					+ " ?obj rdf:type pro:Composition. "
					+ " ?obj pro:name '"+name+"'. "
					+ " ?obj pro:priority ?priority"
					+ "}";
		else
			sparql=PREFIX+"select ?priority "
					+ "where{"
					+ " ?obj rdf:type pro:Instantiation. "
					+ " ?obj pro:name '"+name+"'. "
					+ " ?obj pro:priority ?priority"
					+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		if(results.hasNext())
		{
			priority=Integer.getInteger(results.next().get("priority").toString());
		}
		return priority;
	}
}
