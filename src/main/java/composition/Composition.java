package composition;

import java.util.HashMap;
import com.google.gson.*;

public class Composition {
	String activityName;
	String params;
	OWLHelper ontology;
	HashMap<String, String> parameters;
	HashMap<String, String> properties;
	
	public Composition(String activityName, String params)
	{
		this.activityName=activityName;
		this.params=params;
		this.ontology=new OWLHelper();
		
		this.parameters=new HashMap<String,String>();
		this.properties=new HashMap<String, String>();
		
		ontology.readOntologyFile("files/ontology.owl");
	}
	
	public void decompose()
	{
		seperatePropertiesAndParameters();
	}
	
	void seperatePropertiesAndParameters()
	{
		try
		{
			JsonParser parser=new JsonParser();

			JsonArray array=(JsonArray)parser.parse(params);
			for(JsonElement elem:array)
			{
				String name=((JsonObject)elem).get("name").toString();
				String value=((JsonObject)elem).get("value").toString();
				
				//ontology.getActivityParametersByName(activityName);
				System.out.print(ontology.getActivityParametersAndPropertiesByName(activityName));
			}
			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
