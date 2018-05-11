package composition;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;

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
	public HashMap<String, HashMap<String, RDFNode>> getInstantiationForAbstractService(String abstractServiceName)
	{
		HashMap<String, HashMap<String, RDFNode>> instantiationMap=new HashMap<String, HashMap<String, RDFNode>>();//<instatiation名称,<service名称,service自身>>
				
		String sparql=PREFIX+"select ?service ?serviceName ?instantiation "
				+ "where{"
				+ " ?obj rdf:type pro:AbstractService. "
				+ " ?obj pro:name '"+abstractServiceName+"'. "
				+ " ?obj pro:hasInstantiation ?instantiation. "
				+ " ?instantiation pro:hasPart ?service."
				+ " ?service pro:name ?serviceName"
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		while(results.hasNext())
		{
			QuerySolution result=results.next();
			if(instantiationMap.containsKey(result.get("instantiation").toString()))
			{
				instantiationMap.get(result.get("instantiation").toString()).put(result.get("serviceName").toString(), result.get("service"));
			}
			else
			{
				HashMap<String, RDFNode> map=new HashMap<String, RDFNode>();
				map.put(result.get("serviceName").toString(), result.get("service"));
				instantiationMap.put(result.get("instantiation").toString(), map);
			}
		}

		return instantiationMap;
	}
	
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
	
	public HashMap<String, HashMap<String, RDFNode>> getCompositionForAbstractService(String abstractServiceName)
	{
		HashMap<String, HashMap<String, RDFNode>> compositionMap=new HashMap<String, HashMap<String, RDFNode>>();//<instatiation名称,<service名称,service自身>>
				
		String sparql=PREFIX+"select ?service ?serviceName ?composition "
				+ "where{"
				+ " ?obj rdf:type pro:AbstractService. "
				+ " ?obj pro:name '"+abstractServiceName+"'. "
				+ " ?obj pro:hasComposition ?composition . "
				+ " ?instantiation pro:hasPart ?service."
				+ " ?service pro:name ?serviceName"
				+ "}";
		
		Query query=QueryFactory.create(sparql);
		QueryExecution qe=QueryExecutionFactory.create(query, model);
		ResultSet results=qe.execSelect();
		
		while(results.hasNext())
		{
			QuerySolution result=results.next();
			if(compositionMap.containsKey(result.get("composition").toString()))
			{
				compositionMap.get(result.get("composition").toString()).put(result.get("serviceName").toString(), result.get("service"));
			}
			else
			{
				HashMap<String, RDFNode> map=new HashMap<String, RDFNode>();
				map.put(result.get("serviceName").toString(), result.get("service"));
				compositionMap.put(result.get("composition").toString(), map);
			}
		}

		return compositionMap;
	}
	
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
