package modes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;

import dbconnection.JDBCPostgres;
import system.ConfigSetup;
import system.Formatter;
import trading.BuySell;
import trading.Currency;
import trading.CurrentAPI;
import trading.LocalAccount;
import trading.Trade;
import utils.TradeBotUtil;

public final class Live {
	private static LocalAccount localAccount;
	private static final List<Currency> currencies = new ArrayList<>();
	private static final File credentialsFile = new File("credentials.txt");
	private static boolean initializing;

	private Live() {
		throw new IllegalStateException("Utility class");
	}

	public static LocalAccount getAccount() {
		return localAccount;
	}

	public static List<Currency> getCurrencies() {
		return currencies;
	}

	public static void close() {
		for (Currency currency : currencies) {
			try {
				currency.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void init(String... monedasParam) throws InterruptedException {

		initializing = true;
		boolean fileFailed = true;
		if (credentialsFile.exists()) {
			try {
				List<String> strings = Files.readAllLines(credentialsFile.toPath());
				if (!((String) strings.get(0)).matches("\\*+")) {
					localAccount = new LocalAccount(strings.get(0), strings.get(1));
					fileFailed = false;
				} else {
					System.out.println("---credentials.txt has not been set up");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("---Failed to use credentials in credentials.txt");
			}
		} else {
			System.out.println("---credentials.txt file not detected!");
		}
		if (fileFailed) {
			String apiKey, apiSecret;
			Scanner sc = new Scanner(System.in);
			while (true) {
				System.out.println("Enter your API Key: ");
				apiKey = sc.nextLine();
				if (apiKey.length() == 64) {
					System.out.println("Enter your Secret Key: ");
					apiSecret = sc.nextLine();
					if (apiSecret.length() == 64)
						break;
					System.out.println("Secret API is incorrect, enter again.");
					continue;
				}
				System.out.println("Incorrect API, enter again.");
			}
			localAccount = new LocalAccount(apiKey, apiSecret);
		}
		System.out.println("Can trade: " + localAccount.getRealAccount().isCanTrade());
		System.out.println(String.valueOf(localAccount.getMakerComission()) + " Maker commission.");
		System.out.println(String.valueOf(localAccount.getBuyerComission()) + " Buyer commission");
		System.out.println(String.valueOf(localAccount.getTakerComission()) + " Taker comission");
		BuySell.setAccount(localAccount);
		String current = "";
		List<String> currenciesSetup = null;
		if (monedasParam != null && monedasParam.length > 0) {
			currenciesSetup = new ArrayList<String>(Arrays.asList(monedasParam));
		} else {
			currenciesSetup = ConfigSetup.getCurrencies();
		}
		List<String> monedasActivas = ConfigSetup.getMonedasActivas();
		monedasActivas.addAll(currenciesSetup);
		List<String> monedasActivasResult = monedasActivas.stream().distinct().collect(Collectors.toList());
		while (true) {
			try {
				long start = System.currentTimeMillis();
				for (String arg : monedasActivasResult) {
					try {
						current = arg;
						Currency balanceCurrency = new Currency(current, localAccount);
						if (!balanceCurrency.esAptaParaComprar() && !monedasActivasResult.contains(current))
							continue;
						ResultSet rs = JDBCPostgres.getResultSet(
								"select * from trade where closetime is null and currency = ? and piso = 1", balanceCurrency.getPair());
						JSONObject tradeDbJson = TradeBotUtil.resultSetToJSON(rs);
						Trade trade = null;
						if (tradeDbJson != null) {
							double entrypriceDB = tradeDbJson.getDouble("entryprice");
							long openTimeDB = tradeDbJson.getLong("opentime");
							double high = tradeDbJson.getDouble("high");
							double amount = tradeDbJson.getDouble("amount");
							trade = new Trade(balanceCurrency, entrypriceDB, amount,
									"Trade opened due to: Added based on live account\t");
							trade.setOpenTime(openTimeDB);
							trade.setHigh(high);
							balanceCurrency.setActiveTrade(trade);
						}
					} catch (Exception e) {
						System.out.println("---Could not add " + current + ConfigSetup.getFiat());
						e.printStackTrace();
						if (e.getMessage().contains("current limit is 1200 request weight per 1 MINUTE")) {
							System.out.println("Esperando 1 Minuto...");
							Thread.sleep(30000L);
						}
					}
				}
				localAccount.setStartingValue(localAccount.getTotalValue());
				long end = System.currentTimeMillis();
				float sec = (float) (end - start) / 1000.0F;
				System.out.println("tard" + sec + " seconds");
			} catch (Exception e) {
				System.out.println("---Could not add " + current + ConfigSetup.getFiat());
				System.out.println(e.getMessage());
			}
			initializing = false;
			Thread.sleep(2000L);
		}

	}

	public static void refreshWalletAndTrades() {
		for (AssetBalance balance : localAccount.getRealAccount().getBalances()) {
			if (balance.getFree().matches("0\\.0+"))
				continue;
			if (balance.getAsset().equals(ConfigSetup.getFiat())) {
				final double amount = Double.parseDouble(balance.getFree());
				if (localAccount.getFiat() != amount) {
					System.out.println("---Refreshed " + balance.getAsset() + " from "
							+ Formatter.formatDecimal(localAccount.getFiat()) + " to " + amount);
					System.out.println(balance.getLocked());
					localAccount.setFiat(amount);
				}
				continue;
			}
			for (Currency currency : currencies) {
				if ((balance.getAsset() + ConfigSetup.getFiat()).equals(currency.getPair())) {
					final double amount = Double.parseDouble(balance.getFree());
					if (!localAccount.getWallet().containsKey(currency)) {
						System.out.println("---Refreshed " + currency.getPair() + " from 0 to " + balance.getFree());
						localAccount.getWallet().replace(currency, amount);
					}
					if (localAccount.getWallet().get(currency) != amount) {
						System.out.println("---Refreshed " + currency.getPair() + " from "
								+ Formatter.formatDecimal(localAccount.getWallet().get(currency)) + " to "
								+ balance.getFree());
						System.out.println(balance.getLocked());
						localAccount.getWallet().replace(currency, amount);
					}
					if (currency.hasActiveTrade()) {
						if (currency.getActiveTrade().getAmount() > amount) {
							System.out.println("---Refreshed " + currency.getPair() + " trade from "
									+ Formatter.formatDecimal(currency.getActiveTrade().getAmount()) + " to "
									+ balance.getFree());
							currency.getActiveTrade().setAmount(amount);
						}
					}
					break;
				}
			}
		}
	}

	private static List<AssetBalance> getBalancesOrdenadosByMonedasActivas(List<AssetBalance> balances,
			List<String> monedasActivas) {
		List<AssetBalance> balancesOrdenados = new ArrayList<AssetBalance>();
		List<AssetBalance> balancesPrimeros = new ArrayList<AssetBalance>();
		if (null != monedasActivas && !monedasActivas.isEmpty()) {
			for (AssetBalance assetBalance : balancesPrimeros) {
				if (monedasActivas.contains(assetBalance.getAsset())) {
					balancesPrimeros.add(assetBalance);
				} else {
					balancesOrdenados.add(assetBalance);
				}
			}
			balancesOrdenados.addAll(balancesPrimeros);
			Collections.reverse(balancesOrdenados);
		}
		return balancesOrdenados;
	}

	public static boolean isInitializing() {
		return initializing;
	}
}
