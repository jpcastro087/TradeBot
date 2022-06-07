package system;

import java.util.List;


public class Test {
	public static void main(String[] args) {
		List<String> currenciesSetup = ConfigSetup.getCurrencies();
		
		
		for (int i = 0; i < currenciesSetup.size(); i++) {
			System.out.print(currenciesSetup.get(i) + ",");
			
			if(i % 30 == 0) {
				System.out.println();
			}
			
		}
		
	}
}
