package trading;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import com.binance.api.client.domain.account.Account;

import dbconnection.JDBCPostgres;
import modes.Live;
import system.ConfigSetup;
import system.Formatter;
import system.Mode;
import utils.TradeBotUtil;

public class LocalAccount {
    private final String username;
    private Account realAccount;

    //To give the account a specific final amount of money.
    private double fiatValue;
    private double startingValue;
    private final ConcurrentHashMap<Currency, Double> wallet;
    private final List<Trade> tradeHistory;
    private final List<Trade> activeTrades;
    private double makerComission;
    private double takerComission;
    private double buyerComission;

    /**
     * Wallet value will most probably be 0 at first, but you could start
     * with an existing wallet value as well.
     */
    public LocalAccount(String username, double startingValue) {
        this.username = username;
        this.startingValue = startingValue;
        fiatValue = startingValue;
        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
    }

    public LocalAccount(String apiKey, String secretApiKey) {
        CurrentAPI.login(apiKey, secretApiKey);
        username = "";
        wallet = new ConcurrentHashMap<>();
        tradeHistory = new ArrayList<>();
        activeTrades = new CopyOnWriteArrayList<>();
        realAccount = CurrentAPI.get().getAccount();
        if (!realAccount.isCanTrade()) {
            System.out.println("Can't trade!");
        }
        makerComission = realAccount.getMakerCommission(); //Maker fees are
        // paid when you add liquidity to our order book
        // by placing a limit order below the ticker price for buy, and above the ticker price for sell.
        takerComission = realAccount.getTakerCommission();//Taker fees are paid when you remove
        // liquidity from our order book by placing any order that is executed against an order on the order book.
        buyerComission = realAccount.getBuyerCommission();

        //Example: If the current market/ticker price is $2000 for 1 BTC and you market buy bitcoins starting at the market price of $2000, then you will pay the taker fee. In this instance, you have taken liquidity/coins from the order book.
        //
        //If the current market/ticker price is $2000 for 1 BTC and you
        //place a limit buy for bitcoins at $1995, then
        //you will pay the maker fee IF the market/ticker price moves into your limit order at $1995.
        fiatValue = Double.parseDouble(realAccount.getAssetBalance(ConfigSetup.getFiat()).getFree());
        System.out.println("---Starting FIAT: " + Formatter.formatDecimal(fiatValue) + " " + ConfigSetup.getFiat());
    }

    public Account getRealAccount() {
        return realAccount;
    }

    //All backend.Trade methods
    public List<Trade> getActiveTrades() {
        return activeTrades;
    }

    public List<Trade> getTradeHistory() {
        return tradeHistory;
    }

    public void setStartingValue(double startingValue) {
        this.startingValue = startingValue;
    }

    public void openTrade(Trade trade) {
        activeTrades.add(trade);
        JDBCPostgres.create("insert into trade (opentime, entryprice, amount, total, high, currency) values (?,?,?,?,?,?)",
                trade.getOpenTime(),
                String.format("%.7f", trade.getEntryPrice()),
                String.format("%.5f", trade.getAmount()),
                String.format("%.7f", trade.getAmount() * trade.getEntryPrice()),
                String.format("%.7f", trade.getHigh()),
                trade.getCurrency().getPair());
    }

    public void closeTrade(Trade trade) {
        activeTrades.remove(trade);
        JDBCPostgres.update("update trade set closetime = ?, closeprice = ? where opentime = ?",
                trade.getCloseTime(),
                String.format("%.7f", trade.getClosePrice()),
                trade.getOpenTime());
        tradeHistory.add(trade);
    }
    
    
    //All the get methods.
    public String getUsername() {
        return username;
    }

    public double getFiat() {
        return fiatValue;
    }

    public void setFiat(double fiatValue) {
        this.fiatValue = fiatValue;
    }

    public double getTotalValue() {
        double value = 0;
        ResultSet rs = JDBCPostgres.getResultSet(""
        		+ "select currentprice, amount from trade "
        		+ "where closetime is null "
        		+ "and currentprice is not null "
        		+ "and amount is not null");
        List<JSONObject> jsonObjects = TradeBotUtil.resultSetToListJSON(rs);
        for (JSONObject jsonObject : jsonObjects) {
        	double amount = jsonObject.getDouble("amount");
        	double price = jsonObject.getDouble("currentprice");
        	value += price * amount;
		}
        return value + fiatValue;
    }
    
    

    
    
    public double getTotalActualMonedasActivas() {
        double value = 0;
        String monedasActivas = getMonedasActivasAsString();
        monedasActivas = monedasActivas.equals("") ? "'NOROMPASYESTAMOS'" : monedasActivas;
        ResultSet rs = JDBCPostgres.getResultSet(
        		"select currency, amount from trade "
        		+ " where closetime is null "
        		+ " and currency in (" + monedasActivas + ")");
        
        List<JSONObject> jsonObjects = TradeBotUtil.resultSetToListJSON(rs);
        for (JSONObject jsonObject : jsonObjects) {
        	String currency = jsonObject.getString("currency");
        	Double amount = jsonObject.getDouble("amount");
        	double price = getPryce(currency);
        	
        	if(price == 0) {
        		value += Double.valueOf(CurrentAPI.get().getPrice(currency).getPrice()) * amount;
        	} else{
        		value += price * amount;
        	}
		}
        return value ;
    }
    
    private Double getPryce(String currency) {
    	List<Currency> currencies = Live.getCurrencies();
    	for (Currency currentCurrency : currencies) {
			if(currentCurrency.getPair().equals(currency)) {
				return currentCurrency.getPrice();
			}
		}
    	return 0D;
    }
    
    public double getTotalInicial(){
    	ResultSet rs =
        JDBCPostgres.getResultSet(
        		"select coalesce(sum(cast( entryprice as float ) * cast(amount as float)), 0) as total "
        		+ " from trade "
        		+ " where closetime is null ");
    	
    	JSONObject js = TradeBotUtil.resultSetToJSON(rs);
    	double total = js.getDouble("total");
    	
        return total + fiatValue;
    }
    
    
    public double getTotalInicialMonedasActivas(){
    	String monedasActivas = getMonedasActivasAsString();
    	monedasActivas = monedasActivas.equals("") ? "'NOROMPASYESTAMOS'" : monedasActivas;
    	ResultSet rs =
        JDBCPostgres.getResultSet(
        		"select coalesce(sum(cast( entryprice as float ) * cast(amount as float)),0) as total "
        		+ " from trade "
        		+ " where closetime is null "
        		+ " and currency in (" + monedasActivas + ")");
    	
    	JSONObject js = TradeBotUtil.resultSetToJSON(rs);
    	double total = js.getDouble("total");
    	
        return total;
    }
    
    private String getMonedasActivasAsString(){
    	String monedas = "";
    	if(wallet.entrySet().size() > 0) {
        	for (Map.Entry<Currency, Double> entry : wallet.entrySet()) {
                Currency currency = entry.getKey();
                monedas += "'"+currency.getPair()+"',";
            }
        	monedas = monedas.substring(0, monedas.length()-1);
    	}
    	return monedas;
    }

    public void addToFiat(double amount) {
        fiatValue += amount;
    }

    /**
     * Method has backend.Currency names as keys and the amount of certain currency as value.
     * i.e {"BTCUSDT : 3.23}
     *
     * @return
     */
    public ConcurrentHashMap<Currency, Double> getWallet() {
        return wallet;
    }

    /**
     * Method will calculate current profit off of all the active trades
     *
     * @return returns the sum of all the percentages wether the profit is below 0 or above.
     */
    public double getProfit() {
    	double totalActualMonedasActivas = getTotalActualMonedasActivas();
    	double totalInicial = getTotalInicial();
    	double totalInicialMonedasActivas = getTotalInicialMonedasActivas();
        return ((100 * totalActualMonedasActivas / totalInicial) - ( 100 * totalInicialMonedasActivas / totalInicial)) / 100;
    }


    //All wallet methods

    /**
     * Method allows to add currencies to wallet hashmap.
     *
     * @param key   Should be the name of the currency ie "BTCUSDT"
     * @param value The amount how much was bought.
     */
    public void addToWallet(Currency key, double value) {
        if (wallet.containsKey(key)) {
            wallet.put(key, wallet.get(key) + value);
        } else {
            wallet.put(key, value);
        }

    }

    /**
     * Method allows to remove values from keys.
     **/
    public void removeFromWallet(Currency key, double value) {
        wallet.put(key, wallet.get(key) - value);
    }

    public double getMakerComission() {
        return makerComission;
    }

    public double getTakerComission() {
        return takerComission;
    }

    public double getBuyerComission() {
        return buyerComission;
    }
}
