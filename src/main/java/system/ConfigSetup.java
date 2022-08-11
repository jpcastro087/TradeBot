package system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.TimeZone;

import com.binance.api.client.domain.general.RateLimit;
import com.binance.api.client.domain.general.RateLimitType;

import trading.BuySell;
import trading.CurrentAPI;
import trading.Trade;

public class ConfigSetup {
	private static final int REQUEST_LIMIT_FALLBACK = 1200;
	private static int REQUEST_LIMIT;
	public static String USER_DATABASE;
	public static String PASS_DATABASE;
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
		return getCurrenciesToTrack();
	}

	private static List<String> getCurrenciesToTrack() {
		return currencies;
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
				case "FIAT":
					fiat = arr[1].toUpperCase();
					break;
				case "user database":
					USER_DATABASE = String.valueOf(arr[1]);
					break;
				case "pass database":
					PASS_DATABASE = String.valueOf(arr[1]);
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
