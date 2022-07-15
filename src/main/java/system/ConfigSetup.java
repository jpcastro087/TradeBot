package system;

import com.binance.api.client.domain.general.RateLimit;
import com.binance.api.client.domain.general.RateLimitType;
import com.binance.api.client.domain.market.TickerStatistics;

import dbconnection.JDBCPostgres;
import indicators.MACD;
import indicators.RSI;
import modes.Simulation;
import trading.BuySell;
import trading.Currency;
import trading.CurrentAPI;
import trading.Trade;
import utils.TradeBotUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.*;

import org.json.JSONObject;

public class ConfigSetup {
	private static final int REQUEST_LIMIT_FALLBACK = 1200;
	private static int REQUEST_LIMIT;
    public static String USER_DATABASE;
    public static String PASS_DATABASE;
    private static Double PORCENTAJE_DESDE;
    private static Double PORCENTAJE_HASTA;
    public static String TIEMPO_REINICIO;
    public static boolean COMPRA_DE_CUALQUIER_MANERA;
    public static boolean ESCANER_CURRENCY_TO_TRACK;
    private static List<String> currenciesNotTrack;
	private static List<String> currencies;

	private static final StringBuilder setup = new StringBuilder();

	private static String fiat;

	public ConfigSetup() {
		throw new IllegalStateException("Utility class");
	}

	public static String getSetup() {
		return setup.toString();
	}

	public static List<String> getCurrencies() {
		
		if(ESCANER_CURRENCY_TO_TRACK) {
			return getCurrenciesToTrack();
		} else {
			return getTodasLasMonedas();
		}
		
	}
	
	
	private static List<String> getCurrenciesToTrack(){
		return currencies;
	}
	
	
	
	
	private static List<String> getTodasLasMonedas(){
		List<TickerStatistics> pricesStatistics = CurrentAPI.get().getAll24HrPriceStatistics();
		List<String> currencies = new ArrayList<String>();
		for (int i = 0; i < pricesStatistics.size(); i++) {
			TickerStatistics t = pricesStatistics.get(i);
			String symbol = t.getSymbol();
			if (symbol.endsWith(fiat) && !symbol.endsWith("UP") && !symbol.endsWith("DOWN")) {
				Double porcentaje = Double.valueOf(t.getPriceChangePercent());
				if ( PORCENTAJE_DESDE <= porcentaje && PORCENTAJE_HASTA >= porcentaje) {
					currencies.add(symbol.substring(0, symbol.length() - fiat.length()));
				}
			}
		}
		
		currencies.removeAll(currenciesNotTrack);
		List<String> monedasActivas = getMonedasActivas();
		currencies.removeAll(monedasActivas);
		currencies.addAll(monedasActivas);

		return currencies;
	}
	
	

	private static List<String> getMonedasActivas() {
		List<String> result = new ArrayList<String>();
		ResultSet rs = JDBCPostgres.getResultSet("select currency from trade where closetime is null");
		
		List<JSONObject> jsonObjects = TradeBotUtil.resultSetToListJSON(rs);
		for (JSONObject jsonObject : jsonObjects) {
			String currency = jsonObject.getString("currency");
			if(currency.endsWith(fiat)) {
				currency = currency.substring(0, currency.length()-4);
				result.add(currency);
			}
		}
		return result;
	}

	public static int getRequestLimit() {
		return REQUEST_LIMIT;
	}

	public static String getFiat() {
		return fiat;
	}

	public static void readConfig() {
		System.out.println("---Getting server rate limit");
		try {
			Optional<RateLimit> found = Optional.empty();
			for (RateLimit rateLimit : CurrentAPI.get().getExchangeInfo().getRateLimits()) {
				if (rateLimit.getRateLimitType().equals(RateLimitType.REQUEST_WEIGHT)) {
					found = Optional.of(rateLimit);
					break;
				}
			}
			REQUEST_LIMIT = found.map(RateLimit::getLimit).orElse(REQUEST_LIMIT_FALLBACK);
		} catch (Exception e) {
			System.out.println("Could not read value from server, using fallback value");
			REQUEST_LIMIT = REQUEST_LIMIT_FALLBACK;
		}
		Formatter.getSimpleFormatter().setTimeZone(TimeZone.getDefault());
		System.out.println("Rate limit set at " + REQUEST_LIMIT + " request weight per minute");
		System.out.println("---Reading config...");
		int items = 0;
		File file = new File("config.txt");
		if (!file.exists()) {
			System.out.println("No config file detected!");
			new Scanner(System.in).nextLine();
			System.exit(1);
		}
		try (FileReader reader = new FileReader(file); BufferedReader br = new BufferedReader(reader)) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.isBlank() && !line.isEmpty()) {
					setup.append(line).append("\n");
				} else {
					continue;
				}
				String[] arr = line.strip().split(":");
				if (arr.length != 2)
					continue;
				items++;
				switch (arr[0]) {
				case "MACD change indicator":
					MACD.SIGNAL_CHANGE = (Double.parseDouble(arr[1]));
					break;
				case "RSI positive side minimum":
					RSI.POSITIVE_MIN = Integer.parseInt(arr[1]);
					break;
				case "RSI positive side maximum":
					RSI.POSITIVE_MAX = Integer.parseInt(arr[1]);
					break;
				case "RSI negative side minimum":
					RSI.NEGATIVE_MIN = Integer.parseInt(arr[1]);
					break;
				case "RSI negative side maximum":
					RSI.NEGATIVE_MAX = Integer.parseInt(arr[1]);
					break;
				case "Simulation mode starting value":
					Simulation.STARTING_VALUE = Integer.parseInt(arr[1]);
					break;
				case "Currencies to track":
					currencies = new ArrayList<String>(Arrays.asList(arr[1].toUpperCase().split(", ")));
					break;
				case "Percentage of money per trade":
					BuySell.MONEY_PER_TRADE = Double.parseDouble(arr[1]);
					break;
				case "Trailing SL":
					Trade.TRAILING_SL = Double.parseDouble(arr[1]);
					break;
				case "Take profit":
					Trade.TAKE_PROFIT = Double.parseDouble(arr[1]);
					break;
				case "Confluence":
					Currency.CONFLUENCE_TARGET = Integer.parseInt(arr[1]);
					break;
				case "Close confluence":
					Trade.CLOSE_CONFLUENCE = Integer.parseInt(arr[1]);
					break;
				case "Use confluence to close":
					Trade.CLOSE_USE_CONFLUENCE = Boolean.parseBoolean(arr[1]);
					break;
				case "FIAT":
					fiat = arr[1].toUpperCase();
					break;
				case "aumentar porcentaje de trades exitosos en 24hr":
					BuySell.AUMENTAR_PORCENTAJE_TRADES_EXITOSOS_24HR = Boolean.parseBoolean(arr[1]);
					break;
				case "cantidad trades exitosos necesarios":
					BuySell.CANTIDAD_TRADES_EXITOSOS_NECESARIOS = Integer.parseInt(arr[1]);
					break;
				case "porcentaje para trades exitosos":
					BuySell.PORCENTAJE_PARA_TRADES_EXITOSOS = Double.parseDouble(arr[1]);
					break;
				case "cantidad trades permitidos":
					BuySell.CANTIDAD_TRADES_ACTIVOS_PERMITIDOS = Integer.parseInt(arr[1]);
					break;
				case "user database":
					USER_DATABASE = String.valueOf(arr[1]);
					break;
				case "pass database":
					PASS_DATABASE = String.valueOf(arr[1]);
					break;
				case "porcentaje desde":
					PORCENTAJE_DESDE = Double.valueOf(arr[1]);
					break;
				case "porcentaje hasta":
					PORCENTAJE_HASTA = Double.valueOf(arr[1]);
					break;
				case "tiempo de reinicio":
					TIEMPO_REINICIO = String.valueOf(arr[1]);
					break;
				case "comprar de cualquier manera":
					COMPRA_DE_CUALQUIER_MANERA = Boolean.valueOf(arr[1]);
					break;
				case "escanear del listado currencies to track":
					ESCANER_CURRENCY_TO_TRACK = Boolean.valueOf(arr[1]);
					break;
				case "Currencies to not track":
					currenciesNotTrack = Collections.unmodifiableList(Arrays.asList(arr[1].toUpperCase().split(", ")));
					break;
				default:
					items--;
					break;
				}
			}
			if (items < 12) { // 12 is the number of configuration elements in the file.
				throw new ConfigException("Config file has some missing elements.");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ConfigException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
