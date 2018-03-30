package xiyj.ths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbWorker {
	static public Logger log = LoggerFactory.getLogger(DbWorker.class);

	public String _jdbcUrl = null;
	public String _jdbcUser = null;
	public String _jdbcPassword = null;

	public Connection _conn = null;

	public void close() {
		if (_conn != null)
			try {
				_conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	public void connect() throws SQLException {
		log.info("create connection : " + _jdbcUrl + " with user/pass");
		_conn = DriverManager.getConnection(_jdbcUrl, _jdbcUser, _jdbcPassword);
		_conn.setAutoCommit(false);
	}

	public static DbWorker create() throws SQLException {
		DbWorker db = new DbWorker();
		// db._jdbcUrl = .db._conn.close();.0wi
		// db.connect();

		return db;
	}

	public void runSqls(List<String> sqls) {
		for (String s : sqls) {
			log.info("run sql : " + s);
		}
	}

	public List<Date> getTradingMinutes() {
		List<Date> mins = new ArrayList<>();
		Date now = new Date();
		long base = now.getTime();
		log.info("base : " + base);
		base = (base / (60 * 1000)) * 60 * 1000;
		log.info("base after mod: " + base);

		for (int i = 0; i < 10; ++i) {
			long curr = base + i * 60 * 1000;
			log.info(String.format("curr : %d", curr));
			mins.add(new Date(curr));
		}

		return mins;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
