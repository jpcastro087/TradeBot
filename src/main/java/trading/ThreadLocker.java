package trading;

import modes.Live;

public class ThreadLocker {

	private static boolean isBlocked;

	public static void reiniciarMoneda(Currency currency, LocalAccount localAccount) {
		String moneda = currency.getCoin();
		Trade trade = currency.getActiveTrade();
		currency.setActiveTrade(null);
		localAccount.closeTradeForThread(trade);
		Live.init(moneda);
	}

	public static boolean isBlocked() {
		return isBlocked;
	}

	public static void block() {
		ThreadLocker.isBlocked = true;
		while(isBlocked) {
			try {
				Thread.sleep(15000l);
				CurrentAPI.get().getPrice("BTCUSDT");
				ThreadLocker.isBlocked = false;
			} catch (InterruptedException e) {
			}catch (Exception e) {
				if(e.getMessage().contains("current limit is 1200 request weight per 1 MINUTE")) {
					isBlocked = true;
	    		} else {
	    			e.printStackTrace();
	    		}
			}
		}
		
		
	}

}
