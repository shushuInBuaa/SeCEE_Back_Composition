package composition;

import java.io.File;
import java.io.FileInputStream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.*;

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
	
	public String[] getActivityParametersAndPropertiesByName(String activityName)
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
		if(results.hasNext())
		{
			QuerySolution result=results.next();
			return new String[]{result.get("parameters").toString(), result.get("properties").toString()};
		}
		return new String[] {"",""};
	}
}
