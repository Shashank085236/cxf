package demo.jibx.server;

import java.util.Date;

public class TestServiceImpl implements TestService {

	public String testString(String context) {
		return "Hello " + context;
	}

	public Double testDouble(double dvalue) {
		return dvalue;
	}

	public Date testDate(Date date) {
		return date;
	}

}
