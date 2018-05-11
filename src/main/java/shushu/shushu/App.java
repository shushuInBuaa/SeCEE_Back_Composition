package shushu.shushu;

import static spark.Spark.get;

import composition.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	Composition run=new Composition("UserAmountCalculation","[{\"name\":\"string\",\"value\":\"3\"},{\"name\":\"string\",\"value\":\"3\"}]") ;
    	run.decompose();
    	
//    	get("/composition/:name",(req,res)->{
//			String name=req.params(":name");
//			return "I'm wang"+name;
//		});
    	
    
    }
}
