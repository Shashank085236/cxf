package samples.quickstart.impl;

import samples.quickstart.StockQuoteServicePortType;

public class StockQuoteServicePortTypeImpl implements StockQuoteServicePortType {

    public Double getPrice(String symbol) {
        return Double.valueOf(1.0);
        
    }

    public void update(String symbol, Double price) {
        System.out.println("Symbol : " + symbol + " Price : "  + price);
    }
    
    

}
