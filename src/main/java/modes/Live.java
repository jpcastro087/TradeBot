package modes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

import dbconnection.JDBCPostgres;
import system.ConfigSetup;
import system.Formatter;
import trading.BuySell;
import trading.Currency;
import trading.CurrentAPI;
import trading.LocalAccount;
import trading.Trade;
import utils.CollectionUtil;
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

	public static void init(String... monedasParam) throws Exception {

		List<String> pairs = ConfigSetup.getTodasLasMonedas();

		for (String pair : pairs) {

			try {
				List<Candlestick> history = getHistoryPeriodosAtras(pair,
						CandlestickInterval.valueOf(ConfigSetup.UNIDAD_TIEMPO), ConfigSetup.CANTIDAD_PERIODOS);

				if (CollectionUtil.isNullOrEmpty(history))
					continue;

				System.out.println(pair);

				Candlestick velaMayorVolumen = getVelaMayorVolumen(history);
				if (null == velaMayorVolumen)
					continue;
				double precioVelaMayorVolumen = Double.valueOf(velaMayorVolumen.getHigh());
				Candlestick velaMenorVolumen = getVelaMenorVolumen(history, precioVelaMayorVolumen);

				double precioMayorVolumen = Double.valueOf(velaMayorVolumen.getClose());
				double precioMenorVolumen = Double.valueOf(velaMenorVolumen.getHigh());
				double precioActual = Double.valueOf(history.get(history.size() - 1).getClose());

				double porcentajeHastaMayorVolumen = ((precioMayorVolumen - precioActual) / precioActual) * 100;
				double porcentajeHastaMenorVolumen = ((precioMenorVolumen - precioActual) / precioActual) * 100;

				System.out.println("Moneda " + pair + " Porcentaje Hasta Mayor Volumen: " + porcentajeHastaMayorVolumen);
				System.out.println("Moneda " + pair + " Porcentaje Hasta Menor Volumen: " + porcentajeHastaMenorVolumen);

				JSONObject monedaVolumen = getMonedaVolumenByMoneda(pair);

				if (null == monedaVolumen) {
					insertMonedaVolumen(pair, porcentajeHastaMayorVolumen, porcentajeHastaMenorVolumen);
				} else {
					updateMonedaVolumen(pair, porcentajeHastaMayorVolumen, porcentajeHastaMenorVolumen);
				}
				
				
				Thread.sleep(500);
				
			} catch (Exception e) {
				System.out.println("Problema con " + pair);
			}

		}

	}

	public static void insertMonedaVolumen(String pair, double porcentajeMayorVolumen, double porcentajeMenorVolumen) {
		JDBCPostgres.create(
				"insert into monedavolumen (moneda,porcmayorvolumen, porcmenorvolumen) values(?,?,?); commit;", pair,
				porcentajeMayorVolumen, porcentajeMenorVolumen);
	}

	public static void updateMonedaVolumen(String pair, double porcentajeMayorVolumen, double porcentajeMenorVolumen) {
		JDBCPostgres.update(
				"update monedavolumen set porcmayorvolumen = ?, porcmenorvolumen= ? where moneda = ?; commit; ",
				porcentajeMayorVolumen, porcentajeMenorVolumen, pair);
	}

	public static JSONObject getMonedaVolumenByMoneda(String moneda) {
		ResultSet rs = JDBCPostgres.getResultSet("select * from monedavolumen where moneda = ?", moneda);
		JSONObject monedaVolumen = TradeBotUtil.resultSetToJSON(rs);

		return monedaVolumen;
	}

	private static Candlestick getVelaMayorVolumen(List<Candlestick> history) {
		Candlestick velaMayorVolumen = null;
		if (!CollectionUtil.isNullOrEmpty(history)) {
			double mayor = Double.valueOf(history.get(0).getVolume());
			for (Candlestick candlestick : history) {
				if (Double.valueOf(candlestick.getVolume()) <= mayor) {
					mayor = Double.valueOf(candlestick.getVolume());
					velaMayorVolumen = candlestick;
				}
			}
		}

		return velaMayorVolumen;
	}

	private static Candlestick getVelaMenorVolumen(List<Candlestick> history, double precioVelaMayorVolumen) {
		Candlestick velaMenorVolumen = null;
		if (!CollectionUtil.isNullOrEmpty(history)) {
			double menor = Double.valueOf(history.get(0).getVolume());
			for (Candlestick candlestick : history) {
				double precioVelaActual = Double.valueOf(candlestick.getHigh());
				if (Double.valueOf(candlestick.getVolume()) >= menor && precioVelaActual >= precioVelaMayorVolumen) {
					menor = Double.valueOf(candlestick.getVolume());
					velaMenorVolumen = candlestick;
				}
			}
		}

		return velaMenorVolumen;
	}

	private static List<Candlestick> getHistoryPeriodosAtras(String par, CandlestickInterval intervalo,
			Integer cantidadPeriodos) throws Exception {

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());

		switch (intervalo) {
		case ONE_MINUTE:
			cal.add(Calendar.MINUTE, -cantidadPeriodos);
			break;
		case FIVE_MINUTES:
			cal.add(Calendar.MINUTE, -cantidadPeriodos * 5);
			break;
		case FIFTEEN_MINUTES:
			cal.add(Calendar.MINUTE, -cantidadPeriodos * 15);
			break;
		case HALF_HOURLY:
			cal.add(Calendar.MINUTE, -cantidadPeriodos * 30);
			break;
		case HOURLY:
			cal.add(Calendar.HOUR, -cantidadPeriodos);
			break;
		case FOUR_HOURLY:
			cal.add(Calendar.HOUR, -cantidadPeriodos * 4);
			break;
		case DAILY:
			cal.add(Calendar.DATE, -cantidadPeriodos);
			break;
		default:
			throw new Exception("No existe el periodo, cargalo y no rompas las bolas");
		}

		Date dateBefore1000Periodos = cal.getTime();
		long fechaDesde = dateBefore1000Periodos.getTime();
		long fechaHasta = new Date().getTime();

		List<Candlestick> history = CurrentAPI.get().getCandlestickBars(par, intervalo, cantidadPeriodos, fechaDesde,
				fechaHasta);
		return history;

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
