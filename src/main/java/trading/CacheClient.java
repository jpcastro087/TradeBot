package trading;

import java.util.HashSet;
import java.util.Set;

public class CacheClient {

	private static Set<String> monedas;

	public static Set<String> getMonedas() {
		if(null == monedas) {
			monedas = new HashSet<String>();
		}
		return monedas;
	}
	
}
