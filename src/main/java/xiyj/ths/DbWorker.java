package xiyj.ths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbWorker implements AutoCloseable {
	static public Logger log = LoggerFactory.getLogger(DbWorker.class);

	// public String _jdbcUrl = "jdbc:mysql://127.0.0.1:3306/ths?trustServerCertificate=true&verifyServerCertificate=false&useSSL=true&requireSSL=true";
	public String _jdbcUrl = "jdbc:mysql://127.0.0.1:3306/ths";
	public String _jdbcUser = "ths";
	public String _jdbcPassword = "ths";

	public Connection _conn = null;

	@Override
	public void close() {
		log.info("Entering...");
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
		db.connect();
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

	public static void main(String[] args) throws THSException {
		// TODO Auto-generated method stub
		try (DbWorker db = DbWorker.create()) {
			PreparedStatement st = db._conn.prepareStatement("insert into t01 values (?, ?)");
			st.setString(1, "2018-10-10 11:22:33.444");
			st.setString(2,  "Hello, world");
			int ret = st.executeUpdate();
			log.info("insert return : " + ret);
			db._conn.commit();
		} catch (SQLException e) {
			throw new THSException("JDBC", e);
		}

	}

}
