package xiyj.ths;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Ths.JDIBridge;

public class DownloadX {
	static public Logger log = LoggerFactory.getLogger(DownloadX.class);

	public static void p(String msg) {
		System.out.println(msg);
	}

	public String _prefix = null;
	public List<String> _codes = null;
	public String _columns = null;
	public int _group_size = 0;
	public int _timeout_ms = 0;

	public Set<Callable<String>> _cancelledWorker = new HashSet<>();
	public Map<String, String> _results = new HashMap<>();
	public Map<String, String> _stats = new HashMap<>();
	public List<String> _timeouts = new ArrayList<>();
	public int _totalTasks = 0;

	public Lock _resultLock = new ReentrantLock();

	public List<String> getResult() {
		_resultLock.lock();
		try {
			return new ArrayList<String>(_results.values());
		} finally {
			_resultLock.unlock();
		}
	}


	public void addResult(String id, String r, String stat) {
		_resultLock.lock();
		try {
			if (_results.containsKey(id)) {
				log.info("work " + id + " already done and set result");
				return;
			}
			_results.put(id, r);
			_stats.put(id, stat);
			log.info("result " + id + " added, remain tasks : " + (this._totalTasks - _results.size()));
		} finally {
			_resultLock.unlock();
		}
	}

	public boolean hasResult(String id) {
		_resultLock.lock();
		try {
			return _results.containsKey(id);
		} finally {
			_resultLock.unlock();
		}
	}

	public boolean isResultReady() {
		_resultLock.lock();
		try {
			return _results.size() == this._totalTasks;
		} finally {
			_resultLock.unlock();
		}
	}

	private DownloadX() {
	}

	public class DownloadException extends Exception {
		public DownloadException(String message) {
			super(message);
		}
	}

	public String taskId(int start) {
		return String.format("%05d_%05d", start, start + _group_size - 1);
	}

	public class DownloadTask {
		public int _start = 0;
		public String _id = null;
		public String _codeString = null;

		public DownloadTask(int start) {
			_start = start;
			// _id = String.format("%05d-%05d", _start, _start + _group_size - 1);
			_id = taskId(start);

			StringBuilder sb = new StringBuilder();
			int i = _start;
			sb.append(_codes.get(i));
			for (int j = i + 1; j < i + _group_size && j < _codes.size(); ++j)
				sb.append(",").append(_codes.get(j));
			_codeString = sb.toString();
		}

		public String id() {
			return _id;
		}

		public Callable<String> createWorker() {
			Callable<String> worker = new Callable<String>() {
				public String call() throws Exception {
					long start = System.currentTimeMillis();

					String code = _codeString;
					log.info("worker " + _id + " running, thread id " + Thread.currentThread().getId()
							+ ", download code " + code);
					String strResult = JDIBridge.THS_RealtimeQuotes(code, _columns);
					log.info("worker " + _id + " return length : " + strResult.length());
					// log.info("worker " + _id + " return : " + strResult);

					long end = System.currentTimeMillis();
					String stat = "worker " + _id + ", total execution time: " + (end - start);
					log.info(stat);

					addResult(_id, strResult, stat);
					return _id;
				}
			};

			return worker;
		}
	}

	public void run() throws DownloadException {
		long startRunMs = System.currentTimeMillis();
		log.info("download.run() at " + startRunMs);

		String pattern = "yyyy/MM/dd HH:mm:ss.SSS";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		String date_str = simpleDateFormat.format(new Date());
		log.info("group size " + _group_size + ", start at " + date_str);

		_stats.clear();
		_results.clear();

		this._totalTasks = 0;
		for (int i = 0; i < _codes.size(); i += _group_size) {
			++this._totalTasks;
		}
		log.info("total todo tasks : " + this._totalTasks);

		int NTHREDS = (_codes.size() / _group_size) + 1;
		ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

		Set<Future<String>> waitList = new HashSet<Future<String>>();
		Map<Future<String>, String> futureIds = new HashMap<>();
		Map<String, DownloadTask> idTasks = new HashMap<>();
		// Map<String, Long> idStarts = new HashMap<>();

		while (true) {
			long startMs = System.currentTimeMillis();
			for (int i = 0; i < _codes.size(); i += _group_size) {
				String id = this.taskId(i);

				if (this.hasResult(id)) // this one is done, skip
					continue;

				log.info("start task " + id);
				DownloadTask dt = idTasks.get(id);
				if (dt == null) {
					dt = new DownloadTask(i);
					idTasks.put(id, dt);
				}

				Callable<String> worker = dt.createWorker();
				Future<String> f = executor.submit(worker);

				waitList.add(f);
				futureIds.put(f, id);
				// idTasks.put(id, dt);
				// idStarts.put(id, startMs);
			}

			while (true) {
				try {
					Thread.sleep(100);

					List<Future<String>> futList = new ArrayList<>(waitList);
					for (Future<String> future : futList) {
						String id = futureIds.get(future);

						if (future.isDone()) {
							String id2;
							try {
								id2 = future.get();

								if (!id.equals(id2))
									throw new DownloadException("id != id2 return by worker");
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
							log.info("worker " + id + " done, removed from waitList and idTasks list");
							waitList.remove(future);
							idTasks.remove(id);
						}
					}

					if (this.isResultReady()) {
						log.info("result ready, done, break from inner loop");
						break;
					}

					long currentMs = System.currentTimeMillis();
					if (currentMs > this._timeout_ms + startMs) {
						this._timeouts.add("timeout, to next look");
						log.info("timeout, next loop to check");
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (this.isResultReady()) {
				log.info("result ready, done");
				break;
			}
		}

		executor.shutdown();
		long endRunMs = System.currentTimeMillis();
		log.info("Finished all threads at " + endRunMs);
		
		log.info(_prefix + ", download running time : " + (endRunMs - startRunMs));
		
		// String file = this._prefix.replaceAll(":",  "_").replaceAll(" ",  "_").replaceAll("/",  "_") + ".txt";
		String file = this._prefix + ".txt";
		log.info("write all result to file : " + file);
		FileWriter fw = null;
		try {
			fw = new FileWriter(file, true); // the true will append the new data
			for (String r : this._results.values()) {
				fw.write(r);
				fw.write("\n");
			}
		} catch (IOException ioe) {
			System.err.println("IOException: " + ioe.getMessage());
		} finally {
			if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	public static DownloadX createDownload(String prefix, List<String> codes, String columns, int group_size,
			int timeout_ms) {
		DownloadX dl = new DownloadX();
		dl._prefix = prefix;
		dl._codes = codes;
		// dl._codes = new ArrayList<>(codes);	// copy it 
		dl._columns = columns;
		dl._group_size = group_size;
		dl._timeout_ms = timeout_ms;

		log.info(String.format("Downoad %s, group_size %d, timeout %d\ncodes : %s\ncolumns : %s",
				prefix, group_size, timeout_ms, String.join(",", codes), columns));

		return dl;
	}

	public static void main(String[] args) {
	}
}
