package modes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import com.binance.api.client.domain.account.AssetBalance;

import dbconnection.JDBCPostgres;
import system.ConfigSetup;
import system.Formatter;
import trading.BuySell;
import trading.Currency;
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

    public static void init(String... monedasParam) {
    	initializing = true;
        if (credentialsFile.exists()) {
            //TODO: This try block doesn't work
            try {
                final List<String> strings = Files.readAllLines(credentialsFile.toPath());
                if (!strings.get(0).matches("\\*+")) {
                    localAccount = new LocalAccount(strings.get(0), strings.get(1));
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

        //This doesn't seem to do anything
        //localAccount.getRealAccount().setUpdateTime(1);
        System.out.println("Can trade: " + localAccount.getRealAccount().isCanTrade());
        System.out.println(localAccount.getMakerComission() + " Maker commission.");
        System.out.println(localAccount.getBuyerComission() + " Buyer commission");
        System.out.println(localAccount.getTakerComission() + " Taker comission");
        BuySell.setAccount(localAccount);

        //TODO: Open price for existing currencies
        String current = "";
        
        List<String> currenciesSetup = null;
        if(null != monedasParam && monedasParam.length > 0) {
        	currenciesSetup = new ArrayList<String>(Arrays.asList(monedasParam));
        } else {
        	currenciesSetup = ConfigSetup.getCurrencies();
        }
        try {
            for (String asset : currenciesSetup) {
            	try {
                    Currency balanceCurrency = new Currency(asset);
                    balanceCurrency.setLocalAccount(localAccount);
                    
                    ResultSet rs =
                    JDBCPostgres.getResultSet("select * from trade where closetime is null and currency = ?", balanceCurrency.getPair());
                    List<JSONObject> tradeDbJsons = TradeBotUtil.resultSetToListJSON(rs);
                    
                    //TENGO QUE OBTENER EL AMOUNT SUMANDO TODOS LOS PISOS COMPRADOS
                    double amount = 0d;
                    localAccount.getWallet().put(balanceCurrency, amount);
                    List<Trade> trades = new ArrayList<Trade>();
                    for (JSONObject tradeDbJson : tradeDbJsons) {
                    	if (null != tradeDbJson) {
                        	double entrypriceDB = tradeDbJson.getDouble("entryprice");
                            long openTimeDB = tradeDbJson.getLong("opentime");
                            double high = tradeDbJson.getDouble("high");
                            double currentamount = tradeDbJson.getDouble("amount");
                            Trade trade = new Trade(balanceCurrency, entrypriceDB, currentamount, "Trade opened due to: Added based on live account\t");
                            trade.setOpenTime(openTimeDB);
                            trade.setHigh(high);
                            trades.add(trade);
                            System.out.println("Added an active trade of " + currentamount + " " + current + " at " + Formatter.formatDecimal(trade.getEntryPrice()) + " based on existing balance in account");
                        }
					}
                    balanceCurrency.setActiveTrades(trades);
                    currencies.add(balanceCurrency);
            	}catch(Exception e) {
                    System.out.println("---Could not add " + current + ConfigSetup.getFiat());
                    System.out.println(e.getLocalizedMessage());
                    if(e.getMessage().contains("current limit is 1200 request weight per 1 MINUTE")) {
                        System.out.println("Esperando 1 Minuto...");
                        Thread.sleep(60000);
                    }
                    continue;
            	}
                
            }
            localAccount.setStartingValue(localAccount.getTotalValue());
        } catch (Exception e) {
            System.out.println("---Could not add " + current + ConfigSetup.getFiat());
            System.out.println(e.getMessage());
        }
        initializing = false;
    }

    public static void refreshWalletAndTrades() {
        for (AssetBalance balance : localAccount.getRealAccount().getBalances()) {
            if (balance.getFree().matches("0\\.0+")) continue;
            if (balance.getAsset().equals(ConfigSetup.getFiat())) {
                final double amount = Double.parseDouble(balance.getFree());
                if (localAccount.getFiat() != amount) {
                    System.out.println("---Refreshed " + balance.getAsset() + " from " + Formatter.formatDecimal(localAccount.getFiat()) + " to " + amount);
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
                        System.out.println("---Refreshed " + currency.getPair() + " from " + Formatter.formatDecimal(localAccount.getWallet().get(currency)) + " to " + balance.getFree());
                        System.out.println(balance.getLocked());
                        localAccount.getWallet().replace(currency, amount);
                    }
                    break;
                }
            }
        }
    }
    
    
    public static boolean isInitializing() {
    	return initializing;
    }
}
