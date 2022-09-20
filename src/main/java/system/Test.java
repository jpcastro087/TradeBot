package system;

import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import dbconnection.JDBCPostgres;
import utils.TradeBotUtil;


public class Test {
	public static void main(String[] args) throws ParseException {
		
		
		
//		System.out.println(String.format("%.7f", 0.00050499));
		
		ConfigSetup.USER_DATABASE = "postgres";
		ConfigSetup.PASS_DATABASE = "postgres";
		System.out.println(getSiguienteMontoByMoneda("MIRBUSD"));
//		
//		
//		JDBCPostgres.update("insert into monedamonto(moneda, monto) values(?,?)", "ETHBUSD", String.format("%.7f", 127.3) );
		
//        Double amount = 0.79d;
//        Double closePrice = 1990d;
//        Double monto = closePrice * amount;
//        String moneda = "BTCBUSD";
//        
//        JDBCPostgres.update("update monedamonto set monto = ? where moneda = ?", String.format("%.7f", monto), moneda);
		

		
		
		
//		Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2022-06-12 18:15:03");
		
		
//		System.out.println(date.getTime());
		
//		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
//		
//		
//		NumberFormat PERCENT_FORMAT = new DecimalFormat("0.000%");
//		
//		Number d = PERCENT_FORMAT.parse("1.33%");
//		Double ss = d.doubleValue();
//		System.out.println(ss * 100);
		
		
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
    
    
    private static Double getSiguienteMontoByMoneda(String pair) {
    	ResultSet rs = JDBCPostgres.getResultSet(" select monto monto from monedamonto where moneda = ? ", pair );
    	
    	JSONObject jsonObject = TradeBotUtil.resultSetToJSON(rs);
    	
    	Double mountoDouble = null;
    	if(null != jsonObject) {
    		String monto = jsonObject.getString("monto");
    		mountoDouble = Double.valueOf(monto);
    	}
    	
    	return mountoDouble;
    }

	
}
