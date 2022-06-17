package system;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import dbconnection.JDBCPostgres;
import utils.TradeBotUtil;


public class Test {
	public static void main(String[] args) {
		
		int count = getCountTradesCloses24Hours("");
		
		System.out.println(count);
		
		
		
	}
	
	
    private static int getCountTradesCloses24Hours(String pair) {
    	ResultSet rs = JDBCPostgres.getResultSet("SELECT count(*) count FROM trade " + 
    			" where to_timestamp(opentime / 1000) between (NOW() - INTERVAL '1 DAY') and current_timestamp " + 
    			" and closetime is not null " + 
    			" and currency = ? ", pair );
    	
    	JSONObject countObj = TradeBotUtil.resultSetToJSON(rs);
    	
    	int countInt = countObj.getInt("count");
    	
    	return countInt;
    }

	
}
