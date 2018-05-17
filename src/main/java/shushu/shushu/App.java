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
    	Composition run=new Composition("ETCTurnoverCalculation","[{\"name\":\"startTime\",\"value\":\"3\"},{\"name\":\"endTime\",\"value\":\"3\"}]") ;
    	run.decompose();
    	
//    	get("/composition/:name",(req,res)->{
//			String name=req.params(":name");
//			return "I'm wang"+name;
//		});
    	
    
    }
}
