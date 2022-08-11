package trading;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.AggTradeEvent;

import data.PriceBean;
import data.PriceReader;
import dbconnection.JDBCPostgres;
import indicators.DBB;
import indicators.Indicator;
import indicators.MACD;
import indicators.RSI;
import modes.Live;
import system.ConfigSetup;
import system.Formatter;
import system.Mode;

public class Currency implements Closeable {
    public static int CONFLUENCE_TARGET;

    private final String pair;
    private long candleTime;
    private final List<Indicator> indicators = new ArrayList<>();
    private final AtomicBoolean currentlyCalculating = new AtomicBoolean(false);

    private double currentPrice;
    private long currentTime;

    //Backtesting data
    private final StringBuilder log = new StringBuilder();
    private PriceBean firstBean;

    private Closeable apiListener;
    private Closeable closeable;
    private String coin;
    private List<Trade> activeTrades;
    
    private LocalAccount localAccount;

    //Used for SIMULATION and LIVE
    public Currency(String coin) {
        this.pair = coin + ConfigSetup.getFiat();
        this.coin = coin;

        dispararThreadActualizador(pair.toLowerCase());
        
        System.out.println("---SETUP DONE FOR " + this);
    }
    
    
    
    
    
    public void dispararThreadActualizador(String moneda) {
        BinanceApiWebSocketClient client = CurrentAPI.getFactory().newWebSocketClient();
        //We add a websocket listener that automatically updates our values and triggers our strategy or trade logic as needed
        closeable = client.onAggTradeEvent(moneda, new BinanceApiCallback<AggTradeEvent>() {
            @Override
            public void onResponse(final AggTradeEvent response) {
            	try {
            		//Every message and the resulting indicator and strategy calculations is handled concurrently
                    //System.out.println(Thread.currentThread().getId());
                    double newPrice = Double.parseDouble(response.getPrice());
                    
                    for (Trade trade : activeTrades) {
            	        JDBCPostgres.update("update trade set currentprice = ? where opentime = ?",
            	                String.format("%.9f", newPrice),
            	                trade.getOpenTime());
            	        currentPrice = newPrice;
            	        accept(trade, newPrice);
					}



                    
                    
				} catch (Exception e) {
					e.printStackTrace();
				} 
            	
            }

            @Override
            public void onFailure(final Throwable cause) {
            	System.err.println(cause.getLocalizedMessage());
        	    return;//Stop doing whatever I am doing and terminate
            }
        });
    }
    

    //Used for BACKTESTING
    public Currency(String pair, String filePath) {
        this.pair = pair;
        try (PriceReader reader = new PriceReader(filePath)) {
            PriceBean bean = reader.readPrice();

            firstBean = bean;
            List<Double> closingPrices = new ArrayList<>();
            while (bean.isClosing()) {
                closingPrices.add(bean.getPrice());
                bean = reader.readPrice();
            }
            //TODO: Fix slight mismatch between MACD backtesting and server values.
            indicators.add(new RSI(closingPrices, 14));
            indicators.add(new MACD(closingPrices, 12, 26, 9));
            indicators.add(new DBB(closingPrices, 20));
            while (bean != null) {
//                accept(bean);
                bean = reader.readPrice();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void accept(Trade trade, Double currentPrice) {
    	try {
    		if(!ThreadLocker.isBlocked() && !Live.isInitializing()) {
    	        //Make sure we dont get concurrency issues
    	        if (currentlyCalculating.get()) {
    	            System.out.println("------------WARNING, NEW THREAD STARTED ON " + pair + " MESSAGE DURING UNFINISHED PREVIOUS MESSAGE CALCULATIONS");
    	        }


    	        if (!currentlyCalculating.get()) {
    	            currentlyCalculating.set(true);
    	            
    	            this.currentPrice = currentPrice; 
    	            
    	            trade.update(currentPrice);
    	            
    	            trade.getProfit();
    	            
    	            currentlyCalculating.set(false);
    	        }
    		}
    	}catch(Exception e) {
    		currentlyCalculating.set(false);
    		if(e.getMessage().contains("current limit is 1200 request weight per 1 MINUTE")) {
    			System.err.println("ESPERANDO 1.5 MINUTOS PARA ARRANCAR DEVUELTA CON LOS THREADS NO OPERE POR FAVOR");
    			ThreadLocker.block();
    		} else {
    			e.printStackTrace();
    		}
    	}
    	

    }

    public int check() {
        return indicators.stream().mapToInt(indicator -> indicator.check(currentPrice)).sum();
    }

    public String getExplanations() {
        StringBuilder builder = new StringBuilder();
        for (Indicator indicator : indicators) {
            String explanation = indicator.getExplanation();
            if (explanation == null) explanation = "";
            builder.append(explanation.equals("") ? "" : explanation + "\t");
        }
        return builder.toString();
    }

    public String getPair() {
        return pair;
    }

    public double getPrice() {
        return currentPrice;
    }

    public long getCurrentTime() {
        return currentTime;
    }


    public void appendLogLine(String s) {
        log.append(s).append("\n");
    }

    public void log(String path) {
        List<Trade> tradeHistory = new ArrayList<>(BuySell.getAccount().getTradeHistory());
        try (FileWriter writer = new FileWriter(path)) {
            writer.write("Test ended " + system.Formatter.formatDate(LocalDateTime.now()) + " \n");
            writer.write("\n\nCONFIG:\n");
            writer.write(ConfigSetup.getSetup());
            writer.write("\n\nMarket performance: " + system.Formatter.formatPercent((currentPrice - firstBean.getPrice()) / firstBean.getPrice()));
            if (!tradeHistory.isEmpty()) {
                tradeHistory.sort(Comparator.comparingDouble(Trade::getProfit));
                double maxLoss = tradeHistory.get(0).getProfit();
                double maxGain = tradeHistory.get(tradeHistory.size() - 1).getProfit();
                int lossTrades = 0;
                double lossSum = 0;
                int gainTrades = 0;
                double gainSum = 0;
                long tradeDurs = 0;
                for (Trade trade : tradeHistory) {
                    double profit = trade.getProfit();
                    if (profit < 0) {
                        lossTrades += 1;
                        lossSum += profit;
                    } else if (profit > 0) {
                        gainTrades += 1;
                        gainSum += profit;
                    }
                    tradeDurs += trade.getDuration();
                }

                double tradePerWeek = 604800000.0 / (((double) currentTime - firstBean.getTimestamp()) / tradeHistory.size());

                writer.write("\nBot performance: " + system.Formatter.formatPercent(BuySell.getAccount().getProfit()) + "\n\n");
                writer.write(BuySell.getAccount().getTradeHistory().size() + " closed trades"
                        + " (" + system.Formatter.formatDecimal(tradePerWeek) + " trades per week) with an average holding length of "
                        + system.Formatter.formatDuration(Duration.of(tradeDurs / tradeHistory.size(), ChronoUnit.MILLIS)) + " hours");
                if (lossTrades != 0) {
                    writer.write("\nLoss trades:\n");
                    writer.write(lossTrades + " trades, " + system.Formatter.formatPercent(lossSum / (double) lossTrades) + " average, " + system.Formatter.formatPercent(maxLoss) + " max");
                }
                if (gainTrades != 0) {
                    writer.write("\nProfitable trades:\n");
                    writer.write(gainTrades + " trades, " + system.Formatter.formatPercent(gainSum / (double) gainTrades) + " average, " + system.Formatter.formatPercent(maxGain) + " max");
                }
                writer.write("\n\nClosed trades (least to most profitable):\n");
                for (Trade trade : tradeHistory) {
                    writer.write(trade.toString() + "\n");
                }
            } else {
                writer.write("\n(Not trades made)\n");
                System.out.println("---No trades made in the time period!");
            }
            writer.write("\n\nFULL LOG:\n\n");
            writer.write(log.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("---Log file generated at " + new File(path).getAbsolutePath());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(pair + " price: " + currentPrice);
        if (currentTime == candleTime)
            indicators.forEach(indicator -> s.append(", ").append(indicator.getClass().getSimpleName()).append(": ").append(system.Formatter.formatDecimal(indicator.get())));
        else
            indicators.forEach(indicator -> s.append(", ").append(indicator.getClass().getSimpleName()).append(": ").append(Formatter.formatDecimal(indicator.getTemp(currentPrice))));
        return s.toString();
    }

    @Override
    public int hashCode() {
        return pair.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != Currency.class) return false;
        return pair.equals(((Currency) obj).pair);
    }

    @Override
    public void close() throws IOException {
        if (Mode.get().equals(Mode.BACKTESTING) || Mode.get().equals(Mode.COLLECTION)) return;
        apiListener.close();
    }

	public String getCoin() {
		return coin;
	}

	public void setLocalAccount(LocalAccount localAccount) {
		this.localAccount = localAccount;
	}

	public List<Trade> getActiveTrades() {
		return activeTrades;
	}

	public void setActiveTrades(List<Trade> activeTrades) {
		this.activeTrades = activeTrades;
	}
    
}
