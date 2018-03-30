package xiyj.ths;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	static public Logger log = LoggerFactory.getLogger(Utils.class);
	
	public static String formatDate(Date d, String fmt) {
		// String pattern = "yyyy/MM/dd HH:mm:ss.SSS";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(fmt);
		return simpleDateFormat.format(d);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		log.info("current : " + formatDate(new Date(), "yyyyMMdd_HHmmss_SSS"));

	}

}
