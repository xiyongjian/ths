package xiyj.ths;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Test {

	public static void p(String msg) {
		System.out.println(msg);
	}

	public static void main(String[] args) throws InterruptedException {
		test01();
	}

	public static void test01() throws InterruptedException {
		long millis = System.currentTimeMillis();
		p("curr millis : " + millis);
		p("slee 200 millis");
		Thread.sleep(200);
		millis = System.currentTimeMillis();
		p("curr millis : " + millis);

		String s = "2018\\03\\26_22_18_00.059_0";

		Date d01 = new Date();
		Thread.sleep(188);
		Date d02 = new Date();
		long diff = d02.getTime() - d01.getTime();
		p("d01 " + d01 + ", d02 " + d02);
		p("d01, d02 diff : " + diff);
		p("d01 == d02 : " + d01.equals(d02));
		
		p("done.");
	}

}
