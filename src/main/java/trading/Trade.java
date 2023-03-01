package trading;

import dbconnection.JDBCPostgres;
import system.Formatter;

public class Trade {
	private double high;

	public static double TRAILING_SL;

	public static double TAKE_PROFIT;

	public static boolean CLOSE_USE_CONFLUENCE;

	public static int CLOSE_CONFLUENCE;

	private long openTime;

	private double entryPrice;

	private final Currency currency;

	private double amount;

	private double closePrice;

	private long closeTime;

	private String explanation;
	
	private Long piso;
	
	private Double takeProfit;

	public Trade(Currency currency, double entryPrice, double amount, String explanation) {
		this.currency = currency;
		this.entryPrice = entryPrice;
		this.high = entryPrice;
		this.amount = amount;
		this.explanation = explanation;
		this.openTime = currency.getCurrentTime();
		this.closePrice = -1.0D;
	}

	public String getExplanation() {
		return this.explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}

	public void setClosePrice(double closePrice) {
		this.closePrice = closePrice;
	}

	public double getEntryPrice() {
		return this.entryPrice;
	}

	public double getClosePrice() {
		return this.closePrice;
	}

	public Currency getCurrency() {
		return this.currency;
	}

	public double getAmount() {
		return this.amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public void setCloseTime(long closeTime) {
		this.closeTime = closeTime;
	}

	public long getCloseTime() {
		return this.closeTime;
	}

	public long getOpenTime() {
		return this.openTime;
	}

	public void setOpenTime(long openTime) {
		this.openTime = openTime;
	}

	public void setEntryPrice(double entryPrice) {
		this.entryPrice = entryPrice;
	}

	public boolean isClosed() {
		return (this.closePrice != -1.0D);
	}

	public double getHigh() {
		return this.high;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public double getProfit() {
		if (this.closePrice == -1.0D)
			return (this.currency.getPrice() - this.entryPrice) / this.entryPrice;
		return (this.closePrice - this.entryPrice) / this.entryPrice;
	}

	public double getHighProfit() {
		return (this.high - this.entryPrice) / this.entryPrice;
	}

	public long getDuration() {
		return this.closeTime - this.openTime;
	}

	public Long getPiso() {
		return piso;
	}

	public void setPiso(Long piso) {
		this.piso = piso;
	}
	
	public Double getTakeProfit() {
		return takeProfit;
	}

	public void setTakeProfit(Double takeProfit) {
		this.takeProfit = takeProfit;
	}

	public void update(double newPrice) {
		
		if (newPrice > this.high) {
			this.high = newPrice;
			JDBCPostgres.update("update trade set high = ? where opentime = ?", new Object[] {
					String.format("%.7f", new Object[] { Double.valueOf(this.high) }), Long.valueOf(getOpenTime()) });
		}
		
		if (getProfit() > getTakeProfit()) {
			this.explanation = String.valueOf(this.explanation) + "Closed due to: Take profit";
			BuySell.close(this);
			System.out.println("-------------Vendió a "+getTakeProfit()+"% -------------");
			return;
		}

	}

	public String toString() {
		return   this.currency.getPair() + " " + Formatter.formatDecimal(this.amount) + "\n" + "open: "
				+ Formatter.formatDate(this.openTime) + " at " + this.entryPrice + "\n"
				+ (isClosed() ? ("close: " + Formatter.formatDate(this.closeTime) + " at " + this.closePrice)
						: ("current price: " + this.currency.getPrice()))
				+ "\n" + "high: " + this.high + ", profit: " + Formatter.formatPercent(getProfit()) + "\n"
				+ this.explanation + "\n";
	}

	public String toString2() {
		return this.currency.getPair() + " " + Formatter.formatDecimal(this.amount) + " - " + "open: "
				+ Formatter.formatDate(this.openTime) + " at " + this.entryPrice + " - " + "current price: "
				+ this.currency.getPrice() + " profit: " + Formatter.formatPercent(getProfit());
	}
}
