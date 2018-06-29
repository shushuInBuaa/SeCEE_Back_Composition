package shushu.shushu;

import static spark.Spark.get;

import composition.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args) throws Exception
    {   	
    	get("/composition/:name/:params",(req,res)->{
			String name=req.params(":name");//"ETCTurnoverCalculation"
			String params=req.params(":params");//"[{\"name\":\"startTime\",\"value\":\"3\"},{\"name\":\"endTime\",\"value\":\"3\"}]"
			//[%7B"name":"startTime","value":"3"%7D,%7B"name":"endTime","value":"3"%7D]
			
			Composition run=new Composition(name,params) ;
	    	Object result=run.decompose();
			
	    	return result;
	    	
		});
    }
}
