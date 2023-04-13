package system;

import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import com.binance.api.client.domain.market.CandlestickInterval;

import dbconnection.JDBCPostgres;
import utils.TradeBotUtil;

public class Test {

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static void main(String[] args) throws ParseException {

//		System.out.println(ANSI_RED + "This text is red!" + ANSI_RESET);
//		
//		
//		CandlestickInterval a = CandlestickInterval.valueOf("FOUR_HOURLY");
//		
//		System.out.println(a);

//		System.out.println(String.format("%.7f", 0.00050499));

//		ConfigSetup.USER_DATABASE = "postgres";
//		ConfigSetup.PASS_DATABASE = "postgres";
//		System.out.println(getSiguienteMontoByMoneda("MIRBUSD"));
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

//		imprimirPorcentajeBajadaYSubida(20000, 25000, 2000, 1500, 250);

		int precioMinimo = 25000;
		int precioMaximo = 28500;
		int rangoLargo = 3000;
		int rangoMedio = 3000;
		int rangoCorto = 150;
		double porcentajeDineroRangoAlto = 6.0;
		double porcentajeDineroRangoMedio = 5.5;
		double porcentajeDineroRangoCorto = 5.0;

		imprimirPorcentajeBajadaYSubidaJSON(precioMinimo, precioMaximo, rangoLargo, rangoMedio, rangoCorto,
				porcentajeDineroRangoAlto, porcentajeDineroRangoMedio, porcentajeDineroRangoCorto);

	}

	private static int getCountTradesCloses24Hours(String pair) {
		ResultSet rs = JDBCPostgres.getResultSet("SELECT count(*) count FROM trade "
				+ " where to_timestamp(opentime / 1000) between (NOW() - INTERVAL '1 DAY') and current_timestamp "
				+ " and closetime is not null " + " and currency = ? ", pair);

		JSONObject countObj = TradeBotUtil.resultSetToJSON(rs);

		int countInt = countObj.getInt("count");

		return countInt;
	}

	private static Double getSiguienteMontoByMoneda(String pair) {
		ResultSet rs = JDBCPostgres.getResultSet(" select monto monto from monedamonto where moneda = ? ", pair);

		JSONObject jsonObject = TradeBotUtil.resultSetToJSON(rs);

		Double mountoDouble = null;
		if (null != jsonObject) {
			String monto = jsonObject.getString("monto");
			mountoDouble = Double.valueOf(monto);
		}

		return mountoDouble;
	}

	private static void imprimirPorcentajeBajadaYSubida(int minimo, int maximo, int rangoLargo, int rangoMedio,
			int rangoCorto) {

		for (int j = maximo; j > minimo; j = j - rangoLargo) {
			System.out.println();
			imprimirPorcentaje(j, j - rangoLargo, "LARGO");
			System.out.println();
			for (int k = j; k > minimo; k = k - rangoMedio) {
				System.out.println();
				imprimirPorcentaje(k, k - rangoMedio, "	Medio");
				System.out.println();
				for (int l = k; l > minimo; l = l - rangoCorto) {
					imprimirPorcentaje(l, l - rangoCorto, "		corto");
				}
			}
		}

	}

	private static void imprimirPorcentajeBajadaYSubidaJSON(int minimo, int maximo, int rangoLargo, int rangoMedio,
			int rangoCorto, double porcentajeDineroLargo, double porcentajeDineroMedio, double porcentajeDineroCorto) {

		int startPiso = 1;
		int sumRestoRangoCorto = 0;
		int sumRestoRangoMedio = 0;
		double sumaPorcentajeDinero = 0;
		for (int i = maximo; i > minimo; i = i - rangoCorto) {
			int currentMax = i;
			int currentMin = i - rangoCorto;
			imprimirPorcentajeJSON(currentMax, currentMin, startPiso++, porcentajeDineroCorto);
			sumRestoRangoCorto += currentMax - currentMin;
			
			sumaPorcentajeDinero += porcentajeDineroCorto;

			if (sumRestoRangoCorto >= rangoMedio) {
				imprimirPorcentajeJSON(maximo, currentMin, startPiso++, porcentajeDineroMedio);
				sumRestoRangoMedio += rangoMedio;
				sumRestoRangoCorto = 0;
				sumaPorcentajeDinero += porcentajeDineroMedio;
			}
			
			if (sumRestoRangoMedio >= rangoLargo) {
				imprimirPorcentajeJSON(maximo, currentMin, startPiso++, porcentajeDineroLargo);
				sumRestoRangoMedio = 0;
				sumaPorcentajeDinero += porcentajeDineroLargo;
			}

		}
		
		System.out.println("Suma porcentaje dinero: " + sumaPorcentajeDinero);

	}

	private static void imprimirPorcentaje(int maximo, int minimo, String descripcion) {
		double porcentajeSubida = (Double.valueOf(maximo) - Double.valueOf(minimo)) / Double.valueOf(minimo) * 100;
		double porcentajeBajada = (Double.valueOf(minimo) - Double.valueOf(maximo)) / Double.valueOf(maximo) * 100;
		String porcentajeSubidaFormateado = String.format("%.2f", new Object[] { porcentajeSubida });
		String porcentajeBajadaFormateado = String.format("%.2f", new Object[] { porcentajeBajada });
		System.out.println(descripcion + " " + maximo + "-" + minimo + " Subida: " + porcentajeSubidaFormateado
				+ "% Bajada: " + porcentajeBajadaFormateado + "% - ");
	}

	private static void imprimirPorcentajeJSON(int maximo, int minimo, int nroPiso, double takeProfit) {
		String porcentajeSubida = String.format("%.4f", new Object[] { getPorcentajeSubida(maximo, minimo) });
		String porcentajeBajada = String.format("%.4f", new Object[] { getPorcentajeBajada(maximo, minimo) });
		String takeProfitFormateado = String.format("%.4f", new Object[] { takeProfit / 100 });

		String json = "{\"nro\":" + nroPiso + ", \"porcentajeBajada\": " + porcentajeBajada + ", \"porcentajeDinero\":"
				+ takeProfitFormateado + ", \"takeProfit\":" + porcentajeSubida
				+ ", \"margen\":0.00, \"pair\":\"BTCBUSD\"},";

		System.out.println(json);
	}

	private static double getPorcentajeSubida(int maximo, int minimo) {
		double porcentajeSubida = (Double.valueOf(maximo) - Double.valueOf(minimo)) / Double.valueOf(minimo);
		String porcentajeSubidaFormateado = String.format("%.4f", new Object[] { porcentajeSubida });
		return Double.parseDouble(porcentajeSubidaFormateado);
	}

	private static double getPorcentajeBajada(int maximo, int minimo) {
		double porcentajeBajada = (Double.valueOf(minimo) - Double.valueOf(maximo)) / Double.valueOf(maximo);
		String porcentajeBajadaFormateado = String.format("%.4f", new Object[] { porcentajeBajada });
		return Double.parseDouble(porcentajeBajadaFormateado);
	}

}
