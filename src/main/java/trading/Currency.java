package trading;

import java.awt.Button;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

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
import utils.CollectionUtil;
import utils.TradeBotUtil;

public class Currency implements Closeable {
    public static int CONFLUENCE_TARGET;

    private final String pair;
    private Trade activeTrade;
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
    
    private LocalAccount localAccount;
    
    private boolean esAptaParaComprar;

    //Used for SIMULATION and LIVE
    public Currency(String coin, LocalAccount localAccount) throws Exception {
        this.pair = String.valueOf(coin) + ConfigSetup.getFiat();
        this.coin = coin;
        this.localAccount = localAccount;
        List<Candlestick> history = getHistoryPeriodosAtras(this.pair, CandlestickInterval.valueOf(ConfigSetup.UNIDAD_TIEMPO), ConfigSetup.CANTIDAD_PERIODOS);
        if (CollectionUtil.isNullOrEmpty(history)) return; 
        List<String> monedasActivas = ConfigSetup.getMonedasActivas();
        List<Double> closingPrices = (List<Double>)history.stream().map(candle -> Double.valueOf(Double.parseDouble(candle.getClose()))).collect(Collectors.toList());
        this.indicators.add(new RSI(closingPrices, 0));
        this.indicators.add(new MACD(closingPrices, 0, 0, 0));
        this.indicators.add(new DBB(closingPrices, 0));
        this.currentTime = System.currentTimeMillis();
        this.candleTime = ((Candlestick)history.get(history.size() - 1)).getCloseTime().longValue();
        this.currentPrice = Double.parseDouble(((Candlestick)history.get(history.size() - 1)).getClose());
        changeEsAptaParaComprar(history);
        if (this.esAptaParaComprar || monedasActivas.contains(coin)) {
        actualizarPisos(this.pair);
          if (!monedasActivas.contains(coin)) {
            System.out.println(String.valueOf(coin) + "Inicio Compra en Constructor Currency(String coin)");
            JSONObject infoPiso = ConfigSetup.getInfoPiso(Long.valueOf(1L), this.pair);
            Double porcentaje = Double.valueOf(infoPiso.getDouble("porcentajeDinero"));
//            BuySell.open(this, porcentaje, Long.valueOf(1L), "ES APTA PARA COMPRAR");
            System.out.println("+++++++++++Compró piso 1 porcentajeDinero:"+ porcentaje +" +++++++++++");
            System.out.println(String.valueOf(coin) + "Fin Compra en Constructor Venta en Currency(String coin)");
          } 
        } 
    }
    
    
    
    
    
    private void actualizarPisos(String pair) throws Exception {
    	
    	
    	
    	
    	List<JSONObject> pisos = ConfigSetup.getPisos(pair);
    	
    	if(!CollectionUtil.isNullOrEmpty(pisos)) {
    		for (JSONObject piso : pisos) {
    			Long nroPiso = piso.getLong("nro");
    			Trade tradePisoActual = getTradeByPairAndNroPiso(pair, nroPiso);
    			if(null != tradePisoActual) {
    				updatePiso(tradePisoActual, piso);
    				Trade tradePisoSiguiente = getTradeByPairAndNroPiso(pair, nroPiso+1);
    				if(null == tradePisoSiguiente) {
        				JSONObject siguientePiso = ConfigSetup.getInfoPiso(tradePisoActual.getPiso() + 1, pair);
        				if(null != siguientePiso) {
        					double porcentajePerdidaTradeActual = (currentPrice - tradePisoActual.getEntryPrice()) / tradePisoActual.getEntryPrice();
        					double porcentajeBajadaPisoSiguiente = getPorcentajeBajada(siguientePiso);
        					if(porcentajePerdidaTradeActual <= porcentajeBajadaPisoSiguiente) {
        		            	Double porcentajeDinero = siguientePiso.getDouble("porcentajeDinero");
        		            	Long nroPisoComprado = siguientePiso.getLong("nro");
        						System.out.println("+++++++++++Compró piso "+ nroPisoComprado + " porcentajeBajada:"+ porcentajeBajadaPisoSiguiente + " porcentajeDinero:"+ porcentajeDinero +" +++++++++++");
        	            		BuySell.open(this, porcentajeDinero, nroPisoComprado, "Abierto por ultimo piso");
        					}
        				}
    				}
    			}

			}

    	}
    	
    	//Recuperar todos los pisos de pisos.json
    	//Por cada piso							  
    		//Actualizar Piso					  
    		//Si el piso actual es distinto de 1
    			//Si el piso actual tiene abierto piso anterior en la base de datos
    				//Obtener el porcentaje de ganancia del piso anterior
    				//Si el porcentaje de ganancia del piso anterior es menor o igual al porcentaje indicado en porcentajeBajada del piso actual
    					//Abrir piso actual
    	
    	
    	
    	
//        JDBCPostgres.update("update trade set currentprice = ? where closetime is null and currency = ?", String.format("%.9f", currentPrice), pair);
//    	
//    	
//    	List<Trade> trades = getTradesByPair(pair);
//        
//        for (Trade trade : trades) {
//        	
//        	JSONObject infoPiso = ConfigSetup.getInfoPiso(trade.getPiso(), pair);
//        	trade.setTakeProfit(infoPiso.getDouble("takeProfit"));
//        	trade.update(currentPrice);//Update the active trade stop-loss and high values
//
//        	
//        	Trade tradeUltimoPiso = getTradeUltimoPisoByPair(pair);
//        	if(null != tradeUltimoPiso) {
//            	double entryPriceUltimoPiso = tradeUltimoPiso.getEntryPrice();
//            	double porcentajeContraUltimoPiso = (currentPrice - entryPriceUltimoPiso) / entryPriceUltimoPiso;
//            	DecimalFormat df = new DecimalFormat("#.###");
//            	df.setRoundingMode(RoundingMode.CEILING);
//            	porcentajeContraUltimoPiso = Double.valueOf(df.format(porcentajeContraUltimoPiso));
//            	
//            	
//            	JSONObject nextInfoPiso = ConfigSetup.getInfoPiso(tradeUltimoPiso.getPiso() + 1, pair);
//            	
//            	if(null != nextInfoPiso) {
//	            	Double porcentajeBajada = nextInfoPiso.getDouble("porcentajeBajada");
//	            	Double porcentajeDinero = nextInfoPiso.getDouble("porcentajeDinero");
//	            	
//	            	if(porcentajeContraUltimoPiso <= porcentajeBajada ) {
//	            		BuySell.open(this, porcentajeDinero, tradeUltimoPiso.getPiso()+1, "Abierto por ultimo piso");
//	            	}
//            	}
//        	}
//
//        	
//		}
    }
    
    
    private void updatePiso(Trade trade, JSONObject piso) {
    	if(null != trade) {
        	trade.setTakeProfit(piso.getDouble("takeProfit"));
        	trade.update(currentPrice);
    	}
    }
    
    private Double getPorcentajeBajada(JSONObject piso) {
    	return piso.getDouble("porcentajeBajada");
    }
    
    
    public boolean esAptaParaComprar() {
    	return this.esAptaParaComprar;
    }
    
    
    
    
    private List<Candlestick> getHistoryPeriodosAtras(String par, CandlestickInterval intervalo, Integer cantidadPeriodos) throws Exception{

    	Calendar cal = Calendar.getInstance();
    	cal.setTime(new Date());
    	
    	switch (intervalo) {
		case ONE_MINUTE:
	    	cal.add(Calendar.MINUTE, -cantidadPeriodos);
			break;
		case FIVE_MINUTES:
	    	cal.add(Calendar.MINUTE, -cantidadPeriodos * 5 );
			break;
		case FIFTEEN_MINUTES:
	    	cal.add(Calendar.MINUTE, -cantidadPeriodos * 15);
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
    	long fechaDesde =  dateBefore1000Periodos.getTime();
    	long fechaHasta =  new Date().getTime();
    	
    	
    	List<Candlestick> history = CurrentAPI.get().getCandlestickBars(par, intervalo, cantidadPeriodos, fechaDesde, fechaHasta);
    	return history;
    	
    }
    
    
    private void changeEsAptaParaComprar(List<Candlestick> history) throws Exception {
    	
        Double min = getMinimo(history);
        JSONObject infoPiso = ConfigSetup.getInfoPiso(Long.valueOf(1L), this.pair);
        Double margenMinimo = Double.valueOf(infoPiso.getDouble("margen"));
        Double minEsperado = Double.valueOf(min.doubleValue() + min.doubleValue() * margenMinimo.doubleValue());
        Double porcentajeABajarEsperado = Double.valueOf((minEsperado.doubleValue() - this.currentPrice) / this.currentPrice * 100.0D);
        Double max = getMaximo(history);
        Double porcentajeBajadaDesdeMaximo = Double.valueOf((this.currentPrice - max.doubleValue()) / max.doubleValue() * 100.0D);
        String pairFormateado = agregarEspaciosAlFinal(this.pair, Integer.valueOf(11 - this.pair.length()));
        String trackeo = String.valueOf(pairFormateado) + "Bajun " + String.format("%.2f", new Object[] { porcentajeBajadaDesdeMaximo }) + ", puede bajar un " + String.format("%.2f", new Object[] { porcentajeABajarEsperado });
        List<Trade> trades = getTradesByPair(this.pair);
        if (!CollectionUtil.isNullOrEmpty(trades))
          for (Trade trade : trades) {
            Double entryPrice = Double.valueOf(trade.getEntryPrice());
            Long piso = trade.getPiso();
            Double porcentajePiso = Double.valueOf((this.currentPrice - entryPrice.doubleValue()) / entryPrice.doubleValue() * 100.0D);
            if (porcentajePiso.doubleValue() < 0.0D) {
              trackeo = String.valueOf(trackeo) + " - Piso " + piso + " " + String.format("%.2f", new Object[] { porcentajePiso }) + "%";
              continue;
            } 
            trackeo = String.valueOf(trackeo) + " - Piso " + piso + " +" + String.format("%.2f", new Object[] { porcentajePiso }) + "%";
          }  
        if (this.currentPrice <= minEsperado.doubleValue()) {
          String fiatStr = ConfigSetup.getFiat();
          if (fiatStr.equals("BUSD") || fiatStr.equals("USDT")) {
            double fiatAmount = this.localAccount.getFiat();
            if (fiatAmount > 10.0D) {
              this.esAptaParaComprar = true;
            } else {
              List<String> monedasActivas = ConfigSetup.getMonedasActivas();
              if (!monedasActivas.contains(this.coin))
                trackeo = String.valueOf(trackeo) + " -- Quiere abrir pero no tiene fondos"; 
              this.esAptaParaComprar = false;
            } 
          } else {
            this.esAptaParaComprar = true;
          } 
        } 
        System.out.println(trackeo);
    	
    }
    
    
    
    

    
    
    
    
    private Candlestick getCandlestickInmediatamenteEnAlza(Candlestick candlestick, List<Candlestick> history, Double porcentajeSubida) {
    	Candlestick result = null;
    	Integer index = history.indexOf(candlestick);
    	
    	for(int i = index; i < history.size()-1; i++) {
    		Candlestick current = history.get(i);
    		Long closeTimeParam = candlestick.getCloseTime();
    		Long closeTimeCurrent = current.getCloseTime();
    		Double closePriceParam = Double.valueOf(candlestick.getClose());
    		Double closePriceCurrent = Double.valueOf(current.getClose());
    		if(closeTimeCurrent > closeTimeParam && closePriceCurrent > closePriceParam){
    			Double closePriceParamDouble = Double.valueOf(closePriceParam);
				Double closePriceCurrentDouble = Double.valueOf(closePriceCurrent);
				Double porcentajeDouble = ((closePriceCurrentDouble - closePriceParamDouble) / closePriceParamDouble) * 100;
    			if(porcentajeDouble > porcentajeSubida && i > 32) {
        			result = current;
        			break;
    			}
    		}
    	}
    	return result;
    }
    
    
    private Double getMinimo(List<Candlestick> history) {
        List<Candlestick> listaOrdenar = new ArrayList<>(history);
        List<Double> closePrices = (List<Double>)listaOrdenar.stream().map(candle -> Double.valueOf(Double.parseDouble(candle.getClose()))).collect(Collectors.toList());
        Double min = Double.valueOf(closePrices.stream().mapToDouble(Double::doubleValue).min().getAsDouble());
        return min;
     }
    
    private Double getMaximo(List<Candlestick> history) {
        List<Candlestick> listaOrdenar = new ArrayList<>(history);
        List<Double> closePrices = (List<Double>)listaOrdenar.stream().map(candle -> Double.valueOf(Double.parseDouble(candle.getClose()))).collect(Collectors.toList());
        Double max = Double.valueOf(closePrices.stream().mapToDouble(Double::doubleValue).max().getAsDouble());
        return max;
      }
    
    private String agregarEspaciosAlFinal(String parametro, Integer cantidadEspacios) {
        if (parametro != null && parametro.length() > 0)
          for (int i = 0; i < cantidadEspacios.intValue(); i++)
            parametro = String.valueOf(parametro) + "-";  
        return parametro;
     }
      
    
    
    private List<Double> getMinimos(List<Candlestick> history) throws Exception{
    	List<Double> minimos = new ArrayList<Double>();
		for (int i = 0; i < history.size(); i++) {
			Candlestick candlestick = history.get(i);
			String closePriceBaja = candlestick.getClose();
			Double closePriceBajaDouble = Double.valueOf(closePriceBaja);
			JSONObject infoPisoJson = ConfigSetup.getInfoPiso(1l, pair);
			Double takeProfit = infoPisoJson.getDouble("takeProfit");
			Candlestick candlestickAlza = getCandlestickInmediatamenteEnAlza(candlestick, history, takeProfit * 100);
			if(null != candlestickAlza) {
				minimos.add(closePriceBajaDouble);
			}
		}
    	return minimos;
    }
    
    
    private List<Double> getCloses(List<Candlestick> history) throws Exception{
    	List<Double> closes = new ArrayList<Double>();
		for (int i = 0; i < history.size(); i++) {
			Candlestick candlestick = history.get(i);
			String close = candlestick.getClose();
			Double closeDouble = Double.valueOf(close);
			closes.add(closeDouble);
		}
    	return closes;
    }
    
    
    private Double getPromedio(List<Double> numeros) {
    	Double suma = 0D;
    	Double promedio = 0D;
    	for (Double numero : numeros) {
    		suma += numero;
		}
    	promedio = suma / numeros.size();
    	return promedio;
    }
    
    
    
    
    private void imprimirPorcentajes(List<Candlestick> history) {
    	
    	
//    	if(!esVolatil(history)) return;
    	
    	List<Double> closePrices = history.stream().map(candle -> Double.parseDouble(candle.getClose())).collect(Collectors.toList());
    	List<Double> openPrices = history.stream().map(candle -> Double.parseDouble(candle.getOpen())).collect(Collectors.toList());
    	List<Double> highPrices = history.stream().map(candle -> Double.parseDouble(candle.getHigh())).collect(Collectors.toList());
    	List<Double> lowPrices = history.stream().map(candle -> Double.parseDouble(candle.getLow())).collect(Collectors.toList());
    	
    	
        boolean tradeActivo = false;
        Double precioCompra = null;
        String openTimeCompra = null;
        String closeTimeCompra = null;
        Double porcentajeGlobal = 0D;
    	for (int i = 120; i < history.size(); i++) {
    		List<Double> subListClose = closePrices.subList(0, i);
    		List<Double> subListOpen = openPrices.subList(0, i);
    		List<Double> subListHigh = highPrices.subList(0, i);
    		List<Double> subListLow = lowPrices.subList(0, i);
    		String openTime = new SimpleDateFormat("dd/MM HH:mm").format(new Date(history.get(i).getOpenTime()));
    		
        	Double promedioClose = getPromedioUltimasVelas(subListClose, 120);
        	Double promedioOpen = getPromedioUltimasVelas(subListOpen, 120);
        	Double promedioHigh = getPromedioUltimasVelas(subListHigh, 120);
        	Double promedioLow = getPromedioUltimasVelas(subListLow, 120);
        	Double promedioTotal = (promedioClose + promedioOpen + promedioHigh + promedioLow) / 4;
        	
			Double ultimaVela = closePrices.get(subListClose.size());
			Double porcentajeIncremento = ((ultimaVela-promedioTotal)/promedioTotal)*100;
			
			System.out.println(this.pair+" "+porcentajeIncremento.doubleValue() + " " + String.format("%.12f", ultimaVela) + " " + openTime  );
			
			
	    	if(porcentajeIncremento <= -999 || tradeActivo) {
	    		tradeActivo = true;
	    		if(null == precioCompra) precioCompra = ultimaVela;
	    		if(null == openTimeCompra) openTimeCompra = openTime;

	    		List<Candlestick> velasUnMinuto = CurrentAPI.get().getCandlestickBars(this.pair, CandlestickInterval.ONE_MINUTE, 1000, history.get(i).getOpenTime(), history.get(i).getCloseTime());
	    		
	    		for (Candlestick minuto : velasUnMinuto) {
	    			Double openUnMinuto = Double.parseDouble(minuto.getOpen());
	    			Double closeUnMinuto = Double.parseDouble(minuto.getClose());
	    			Double lowUnMinuto = Double.parseDouble(minuto.getLow());
	    			Double highUnMinuto = Double.parseDouble(minuto.getHigh());
	    			
	    			Double porcentajeOpen= ((openUnMinuto-precioCompra)/precioCompra)*100;
	    			Double porcentajeClose= ((closeUnMinuto-precioCompra)/precioCompra)*100;
	    			Double porcentajeLow= ((lowUnMinuto-precioCompra)/precioCompra)*100;
	    			Double porcentajeHigh= ((highUnMinuto-precioCompra)/precioCompra)*100;
	    			
	    			Double porcentajeGanacia = 5D;
	    			Double porcentajePerdida = -15D;
	    			
	    			boolean porcentajeOpenGanacia = porcentajeOpen >= porcentajeGanacia;
	    			boolean porcentajeCloseGanacia = porcentajeClose >= porcentajeGanacia;
	    			boolean porcentajeLowGanacia = porcentajeLow >= porcentajeGanacia;
	    			boolean porcentajeHighGanacia = porcentajeHigh >= porcentajeGanacia;
	    			
	    			boolean porcentajeOpenPerdida = porcentajeOpen <= porcentajePerdida;
	    			boolean porcentajeClosePerdida = porcentajeClose <= porcentajePerdida;
	    			boolean porcentajeLowPerdida = porcentajeLow <= porcentajePerdida;
	    			boolean porcentajeHighPerdida = porcentajeHigh <= porcentajePerdida;
	    			
	        		String openTimeUnMinuto = new SimpleDateFormat("dd/MM HH:mm:ss").format(new Date(minuto.getOpenTime()));
	        		String closeTimeUnMinuto = new SimpleDateFormat("dd/MM HH:mm:ss").format(new Date(minuto.getCloseTime()));
	        		
	        		
	    			if(porcentajeOpenPerdida) {
	    				 System.out.println(this.pair+" Open Perdió: "+ porcentajeOpen + "% Desde el " + openTimeCompra+ " Hasta el: "+closeTimeUnMinuto);
	    				 tradeActivo = false;
	    				 precioCompra = null;
	    				 openTimeCompra = null;
	    				 porcentajeGlobal += porcentajeOpen;
	    				 break;
	    			}
	    			if(porcentajeClosePerdida) {
	    				System.out.println(this.pair+" Close Perdió: "+ porcentajeClose + "% Desde el " + openTimeCompra+ " Hasta el: "+closeTimeUnMinuto);
	    				tradeActivo = false;
	    				precioCompra = null;
	    				openTimeCompra = null;
	    				porcentajeGlobal += porcentajeClose;
	    				break;
	    			} 
	    			if(porcentajeLowPerdida) {
	    				System.out.println(this.pair+" Low Perdió: "+ porcentajeLow + "% Desde el " + openTimeCompra+ " Hasta el: "+closeTimeUnMinuto);
	    				tradeActivo = false;
	    				precioCompra = null;
	    				openTimeCompra = null;
	    				porcentajeGlobal += porcentajeLow;
	    				break;
	    			} 
	    			if(porcentajeHighPerdida) {
	    				System.out.println(this.pair+" High Perdió: "+ porcentajeHigh + "% Desde el " + openTimeCompra+ " Hasta el: "+closeTimeUnMinuto);
	    				tradeActivo = false;
	    				precioCompra = null;
	    				openTimeCompra = null;
	    				porcentajeGlobal += porcentajeHigh;
	    				break;
	    			} 
	    			
	    			if(porcentajeOpenGanacia) {
	    				System.out.println(this.pair+" Open Ganó: "+ porcentajeOpen + "% Desde el: " + openTimeCompra + " Hasta el: "+closeTimeUnMinuto );
	    				tradeActivo = false;
	    				precioCompra = null;
	    				openTimeCompra = null;
	    				porcentajeGlobal += porcentajeOpen;
	    				break;
	    			}
	    			if(porcentajeCloseGanacia) {
	    				System.out.println(this.pair+" Close Ganó: "+ porcentajeClose+ "% Desde el " + openTimeCompra+ " Hasta el: "+closeTimeUnMinuto);
	    				tradeActivo = false;
	    				precioCompra = null;
	    				openTimeCompra = null;
	    				porcentajeGlobal += porcentajeClose;
	    				break;
	    			} 
	    			if(porcentajeLowGanacia) {
	    				System.out.println(this.pair+" Low Ganó: "+ porcentajeLow + "% Desde el " + openTimeCompra+ " Hasta el: "+closeTimeUnMinuto);
	    				tradeActivo = false;
	    				precioCompra = null;
	    				openTimeCompra = null;
	    				porcentajeGlobal += porcentajeLow;
	    				break;
	    			} 
	    			if(porcentajeHighGanacia) {
	    				System.out.println(this.pair+" High Ganó: "+ porcentajeHigh + "% Desde el " + openTimeCompra+ " Hasta el: "+closeTimeUnMinuto);
	    				tradeActivo = false;
	    				precioCompra = null;
	    				openTimeCompra = null;
	    				porcentajeGlobal += porcentajeHigh;
	    				break;
	    			} 
	    			
				}

	    	}
			
		}
    	
    	System.out.println("PORCENTAJE GLOBAL: "+ porcentajeGlobal);
    	
    }
    
    
    private boolean esVolatil(List<Candlestick> history) {
    	List<Double> porcentajesIncremento = getPorcentajesIncremento(history);
    	List<Integer> porcentajeEnteros = porcentajesIncremento.stream().map(p ->  p.intValue()).collect(Collectors.toList());
    	
    	boolean esVolatil = false;
    	esVolatil = esVolatil || porcentajeEnteros.contains(7);
    	esVolatil = esVolatil || porcentajeEnteros.contains(8);
    	esVolatil = esVolatil || porcentajeEnteros.contains(9);
    	esVolatil = esVolatil || porcentajeEnteros.contains(10);
    	esVolatil = esVolatil || porcentajeEnteros.contains(11);
    	esVolatil = esVolatil || porcentajeEnteros.contains(12);
    	
    	return esVolatil;
    	
    }
    
    
    private Double getTakeProfit(List<Candlestick> history) {
    	List<Double> closePrices = history.stream().map(candle -> Double.valueOf(candle.getClose())).collect(Collectors.toList());
    	List<Double> openPrices = history.stream().map(candle -> Double.valueOf(candle.getOpen())).collect(Collectors.toList());
    	List<Double> highPrices = history.stream().map(candle -> Double.valueOf(candle.getHigh())).collect(Collectors.toList());
    	List<Double> lowPrices = history.stream().map(candle -> Double.valueOf(candle.getLow())).collect(Collectors.toList());
    	
    	
    	Double promedioClose = getPromedioUltimasVelas(closePrices, 120);
    	Double promedioOpen = getPromedioUltimasVelas(openPrices, 120);
    	Double promedioHigh = getPromedioUltimasVelas(highPrices, 120);
    	Double promedioLow = getPromedioUltimasVelas(lowPrices, 120);
    	Double promedioTotal = (promedioClose + promedioOpen + promedioHigh + promedioLow) / 4;
    	
    	Double ultimaVela = closePrices.get(closePrices.size() - 1);
    	
    	Double porcentajeIncremento = ((ultimaVela-promedioTotal)/promedioTotal)*100;
    	
    	if(porcentajeIncremento.intValue() == -2) {
    		return 0.02d;
    	} else if(porcentajeIncremento <= -10) {
    		return 0.09d;
    	} else {
    		return 0.02d;
    	}
    	
    }
    
    
    
    private List<Double> getPorcentajesIncremento(List<Candlestick> history) {
    	List<Double> closePrices = history.stream().map(candle -> Double.parseDouble(candle.getClose())).collect(Collectors.toList());
    	List<Double> openPrices = history.stream().map(candle -> Double.parseDouble(candle.getOpen())).collect(Collectors.toList());
    	List<Double> highPrices = history.stream().map(candle -> Double.parseDouble(candle.getHigh())).collect(Collectors.toList());
    	List<Double> lowPrices = history.stream().map(candle -> Double.parseDouble(candle.getLow())).collect(Collectors.toList());
    	
    	List<Double> porcentajes = new ArrayList<Double>();
    	for (int i = 10; i < history.size(); i++) {
    		List<Double> subListClose = closePrices.subList(0, i);
    		List<Double> subListOpen = openPrices.subList(0, i);
    		List<Double> subListHigh = highPrices.subList(0, i);
    		List<Double> subListLow = lowPrices.subList(0, i);
    		
        	Double promedioClose = getPromedioUltimasVelas(subListClose, 10);
        	Double promedioOpen = getPromedioUltimasVelas(subListOpen, 10);
        	Double promedioHigh = getPromedioUltimasVelas(subListHigh, 10);
        	Double promedioLow = getPromedioUltimasVelas(subListLow, 10);
        	Double promedioTotal = (promedioClose + promedioOpen + promedioHigh + promedioLow) / 4;
        	
			Double ultimaVela = closePrices.get(subListClose.size()-1 );
			Double porcentajeIncremento = ((ultimaVela-promedioTotal)/promedioTotal)*100;
			porcentajes.add(porcentajeIncremento);
    	}
    	return porcentajes;
    }
    
    
    
    
    private boolean esAptaParaComprar(List<Candlestick> history) {
    	
    	if(!esVolatil(history)) return false;
    	
    	List<Double> closePrices = history.stream().map(candle -> Double.valueOf(candle.getClose())).collect(Collectors.toList());
    	List<Double> openPrices = history.stream().map(candle -> Double.valueOf(candle.getOpen())).collect(Collectors.toList());
    	List<Double> highPrices = history.stream().map(candle -> Double.valueOf(candle.getHigh())).collect(Collectors.toList());
    	List<Double> lowPrices = history.stream().map(candle -> Double.valueOf(candle.getLow())).collect(Collectors.toList());
    	
    	
    	Double promedioClose = getPromedioUltimasVelas(closePrices, 120);
    	Double promedioOpen = getPromedioUltimasVelas(openPrices, 120);
    	Double promedioHigh = getPromedioUltimasVelas(highPrices, 120);
    	Double promedioLow = getPromedioUltimasVelas(lowPrices, 120);
    	Double promedioTotal = (promedioClose + promedioOpen + promedioHigh + promedioLow) / 4;
    	
    	Double ultimaVela = closePrices.get(closePrices.size() - 1);
    	
    	Double porcentajeIncremento = ((ultimaVela-promedioTotal)/promedioTotal)*100;
    	
    	
    	//obtener el precio promedio de la moneda a 2 meses atras
    	//si el precio promedio a 2 meses supera cierto porcentaje con respecto al precio actual es apta para comprar
    	//Comprar y aplicar que venda cuando el porcentaje < (porcentaje - porcentaje / 3)
    	//Una vez que venda y realice un soporte en determinado precio, la moneda debe escanear obteniendo el promedio desde 
    	//la fecha de compra que hizo el quiebre al alza hasta el precio actual
    	//para evitar que vuelva a comprar dentro del soporte sin que haya subido.
    	
    	
    	if(porcentajeIncremento.intValue() == -2) {
    		return true;
    	}else{
    		return false;
    	}
    	
    	
    }
    
    
    private Double getPromedioUltimasVelas(List<Double> history, int cantVelas) {
    	Double promedio = 0d;
    	
    	int countDown = 0;
    	int count = 0;
    	
    	if(history.size() <= cantVelas) {
    		count = history.size();
    		countDown = history.size()-1;
    	} else {
    		count = cantVelas;
    		countDown = history.size()-2;
    	}
    		
    	if(history.size() > 1) {
    		for (int i = count; i > 0; i--) {
        		promedio += history.get(countDown--);
    		}
    	} else {
    		promedio = history.get(0);
    	}
    	promedio = promedio / count;
    	return promedio;
    }
    
    
    
    
    
    public void dispararThreadActualizador(String moneda) {
    	
        BinanceApiWebSocketClient client = CurrentAPI.getFactory().newWebSocketClient();
        //We add a websocket listener that automatically updates our values and triggers our strategy or trade logic as needed
        
        CacheClient.getMonedas().add(moneda);
        closeable = client.onAggTradeEvent(moneda, new BinanceApiCallback<AggTradeEvent>() {
            @Override
            public void onResponse(final AggTradeEvent response) {
            	try {
            		//Every message and the resulting indicator and strategy calculations is handled concurrently
                    //System.out.println(Thread.currentThread().getId());
                    double newPrice = Double.parseDouble(response.getPrice());
                    long newTime = response.getEventTime();
                    
                    if(null != activeTrade) {
            	        JDBCPostgres.update("update trade set currentprice = ? where closetime is null and currency = ?",
            	                String.format("%.9f", currentPrice),
            	                Currency.this.pair);
                    }

                    //We want to toss messages that provide no new information
                    if (currentPrice == newPrice && newTime <= candleTime) {
                        return;
                    }

                    if (newTime > candleTime) {
                        accept(new PriceBean(candleTime, currentPrice, true));
                        candleTime += 300000L;
                    }

                    accept(new PriceBean(newTime, newPrice));
                    
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
                accept(bean);
                bean = reader.readPrice();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void accept(PriceBean bean) {
    	try {
    		if(!ThreadLocker.isBlocked()) {
    	        //Make sure we dont get concurrency issues
    	        if (currentlyCalculating.get()) {
    	            System.out.println("------------WARNING, NEW THREAD STARTED ON " + pair + " MESSAGE DURING UNFINISHED PREVIOUS MESSAGE CALCULATIONS");
    	        }

    	        currentPrice = bean.getPrice();
    	        currentTime = bean.getTimestamp();

    	        if (bean.isClosing()) {
    	            indicators.forEach(indicator -> indicator.update(bean.getPrice()));
    	            if (Mode.get().equals(Mode.BACKTESTING)) {
    	                appendLogLine(system.Formatter.formatDate(currentTime) + "  ");
    	            }
    	        }

    	        if (!currentlyCalculating.get()) {
    	            currentlyCalculating.set(true);
    	            
    	            List<Trade> trades = getTradesByPair(pair);
    	            
    	            for (Trade trade : trades) {
    	            	
    	            	JSONObject infoPiso = ConfigSetup.getInfoPiso(trade.getPiso(), pair);
    	            	trade.setTakeProfit(infoPiso.getDouble("takeProfit"));
    	            	trade.update(currentPrice);//Update the active trade stop-loss and high values

    	            	
    	            	Trade tradeUltimoPiso = getTradeUltimoPisoByPair(pair);
    	            	if(null != tradeUltimoPiso) {
        	            	double entryPriceUltimoPiso = tradeUltimoPiso.getEntryPrice();
        	            	double porcentajeContraUltimoPiso = (currentPrice - entryPriceUltimoPiso) / entryPriceUltimoPiso;
        	            	
        	            	JSONObject nextInfoPiso = ConfigSetup.getInfoPiso(tradeUltimoPiso.getPiso() + 1, pair);
        	            	
        	            	if(null != nextInfoPiso) {
            	            	Double porcentajeBajada = nextInfoPiso.getDouble("porcentajeBajada");
            	            	Double porcentajeDinero = nextInfoPiso.getDouble("porcentajeDinero");
            	            	
            	            	if( porcentajeContraUltimoPiso <= porcentajeBajada ) {
            	            		BuySell.open(this, porcentajeDinero, tradeUltimoPiso.getPiso()+1, "Abierto por ultimo piso");
            	            	}
        	            	}
    	            	}

    	            	
					}
    	            
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
    
    
    private List<Trade> getTradesByPair(String pair){
    	
        ResultSet rs =
        JDBCPostgres.getResultSet("select * from trade where closetime is null and currency = ? order by piso", pair);
        List<JSONObject> tradesJson = TradeBotUtil.resultSetToListJSON(rs);
        
        List<Trade> trades = new ArrayList<Trade>();
        for (JSONObject jsonObject : tradesJson) {
        	
        	double entryPrice = Double.valueOf(jsonObject.getString("entryprice"));
        	double amount = Double.valueOf(jsonObject.getString("amount"));
        	long openTime = jsonObject.getLong("opentime");
        	double high = Double.valueOf(jsonObject.getString("high"));
        	long piso = jsonObject.getLong("piso");
        	
        	Trade trade = new Trade(this, entryPrice, amount, "");
        	trade.setOpenTime(openTime);
        	trade.setAmount(amount);
        	trade.setHigh(high);
        	trade.setPiso(piso);
        	trades.add(trade);
		}
        
        
        return trades;
    }
    
    private Trade getTradeUltimoPisoByPair(String pair) {
    	Trade trade = null;
        ResultSet rs =
        JDBCPostgres.getResultSet("select * from trade where closetime is null and currency = ? order by piso desc limit 1", pair);
        JSONObject tradeUltimoPiso = TradeBotUtil.resultSetToJSON(rs);
        
        if(null != tradeUltimoPiso) {
        	double entryPrice = Double.valueOf(tradeUltimoPiso.getString("entryprice"));
        	double amount = Double.valueOf(tradeUltimoPiso.getString("amount"));
        	long openTime = tradeUltimoPiso.getLong("opentime");
        	double high = Double.valueOf(tradeUltimoPiso.getString("high"));
        	long piso = tradeUltimoPiso.getLong("piso");
        	
        	trade = new Trade(this, entryPrice, amount, "");
        	trade.setOpenTime(openTime);
        	trade.setAmount(amount);
        	trade.setHigh(high);
        	trade.setPiso(piso);
        }

        
        return trade;
    }
    
    
    private Trade getTradeByPairAndNroPiso(String pair, Long piso) {
    	Trade trade = null;
        ResultSet rs =
        JDBCPostgres.getResultSet("select * from trade where closetime is null and currency = ? and piso = ?", pair, piso);
        JSONObject tradeUltimoPiso = TradeBotUtil.resultSetToJSON(rs);
        
        if(null != tradeUltimoPiso) {
        	double entryPrice = Double.valueOf(tradeUltimoPiso.getString("entryprice"));
        	double amount = Double.valueOf(tradeUltimoPiso.getString("amount"));
        	long openTime = tradeUltimoPiso.getLong("opentime");
        	double high = Double.valueOf(tradeUltimoPiso.getString("high"));
        	
        	trade = new Trade(this, entryPrice, amount, "");
        	trade.setOpenTime(openTime);
        	trade.setAmount(amount);
        	trade.setHigh(high);
        	trade.setPiso(piso);
        }

        
        return trade;
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

    public boolean hasActiveTrade() {
        return activeTrade != null;
    }

    public void setActiveTrade(Trade activeTrade) {
        this.activeTrade = activeTrade;
        
    }

    public Trade getActiveTrade() {
        return activeTrade;
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
        s.append(", hasActive: ").append(hasActiveTrade()).append(")");
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

    
}
