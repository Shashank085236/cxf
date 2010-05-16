package demo.jibx.server;

import java.util.Date;


public interface TestService {
	
	public String testString(String context);
	
	public Double testDouble(double dvalue);
	
	public Date testDate(Date date);

}
