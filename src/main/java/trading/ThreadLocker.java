package trading;

public class ThreadLocker {

	private static boolean isBlocked;


	public static boolean isBlocked() {
		return isBlocked;
	}

	public static void block() {
		ThreadLocker.isBlocked = true;
		while(isBlocked) {
			try {
				Thread.sleep(90000l);
				CurrentAPI.get().getPrice("BTCUSDT");
				ThreadLocker.isBlocked = false;
				System.out.println("YA SE PUEDE OPERAR");
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
