package samples.quickstart.impl;

import java.net.URL;

import samples.quickstart.StockQuoteService;
import samples.quickstart.StockQuoteServicePortType;

public class StockQuoteServiceClient {
    public static void main(String[] args) {
        try {
            StockQuoteService proxy = new StockQuoteService(
                                                            new URL(
                                                                    "file:/home/nilupa/Documents/experiment/StockQuoteService.wsdl"));
            StockQuoteServicePortType port = proxy.getStockQuoteServiceSOAP11Port();
            System.out.println(port.getPrice("symbol"));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
