import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

public class StockData {
// kelas untuk mengambil dan menyimpan data ke database
// tanggung jawab kelas:
// 1. membuat url untuk Yahoo!
// 2. mengambil data CSV
// 3. melakukan parsing
// 4. memasukkan ke dalam database
	
	// ATRIBUT
	
	public static String historyFilename = "HistoricalData.csv"; // nama file data histori
	private URL url; // untuk menyimpan url data
	private Iterable<CSVRecord> records; // untuk menyimpan data dari CSV
	// variable untuk menampung data historis sementara
	// untuk SQL
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	
	// FUNGSI
	
	public StockData() {
	//ctor 
		
	}
	
	public URL getUrl() {
	// getter untuk atribut url
		return url;
	}
	
	public void getCSV(String stockSymbol) {
		// fungsi untuk membuat url sesuai parameter input
		// kemudian mengambil data csv dari url tersebut
		// asumsi data yang diambil selalu data 10 hari dengan interval per data 5 menit
		// data 10 hari merupakan data 10 hari terkini
			File file = new File(historyFilename);
			try {
				url = new URL("http://chartapi.finance.yahoo.com/instrument/1.0/" + stockSymbol + "/chartdata;type=quote;range=10d/csv");
				FileUtils.copyURLToFile(url, file);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	
	public void insertDataIntoDatabase() {
	// memasukkan data historis per 5 menit ke dalam database
	// data yang dimasukkan berasal dari csv
	// tidak menjamin keutuhan data -> akan diproses selanjutnya di prosedur berbeda
		csvParser();
		prepareDatabaseConnection();
		try {
			statement.executeUpdate("truncate datahistoris");
			for (CSVRecord record : records) {
				java.sql.Timestamp ts = new java.sql.Timestamp(Integer.parseInt(record.get("time"))*1000L);
				preparedStatement = connect
						.prepareStatement("insert into TA.datahistoris values (?, ?)");
				preparedStatement.setTimestamp(1, ts);
				preparedStatement.setDouble(2, Double.parseDouble(record.get("close")));
				preparedStatement.executeUpdate();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void segmentPrice() {
	// melakukan segmentasi datahistoris agar lebih mudah digunakan pada SVM
	// dari format data yang merupakan timestamp menjadi tanggal dan indeks waktu dari 0-83
	// indeks waktu merepresentasikan segmen waktu per 5 menit dari pembukaan hingga penutupan pasar saham
		prepareDatabaseConnection();
		String query = "select * from datahistoris";
		try {
			statement.executeUpdate("truncate segmented");
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			Date lastDate = (Date) formatter.parse("2015-08-03");
			cal.setTime(lastDate);
			int lastDay = cal.get(Calendar.DAY_OF_MONTH);
			Date currDate;
			
			// proses query
			ResultSet rs = statement.executeQuery(query);
			
			// variabel pembantu untuk menyimpan closeprice
			double[] closeprice = new double[83];
			
			// inisiasi array
			for (int i = 0; i < 83; i++) {
				closeprice[i] = 0;
			}
			
			while (rs.next()) {
				// iterasi tiap data
				currDate = rs.getTimestamp("time");
				cal.setTime(currDate);
				
				if (cal.get(Calendar.DAY_OF_MONTH) != lastDay) {
					// jika hari yang diproses tidak sama
					// flush data + reset variable
					
					// khusus tanggal 27 harus ditambal dengan data dari yahoofinance secara manual
					// karena tidak didapat dari API
					if (lastDay == 27) {
						closeprice[0] = 4345.67;
						closeprice[1] = 4345.74;
						closeprice[2] = 4333.85;
						closeprice[3] = 4337.22;
						closeprice[4] = 4341.93;
						closeprice[5] = 4344.53;
						closeprice[6] = 4346.21;
						closeprice[7] = 4347.24;
						closeprice[8] = 4349.07;
						closeprice[9] = 4347.01;
						closeprice[10] = 4345.77;
						closeprice[11] = 4345.87;
						closeprice[12] = 4347.25;
					}
					
					for (int i = 0; i < 83; i++) {
						if (closeprice[i] == 0)
							closeprice[i] = closeprice[i-1];
					}
					
					for (int i = 0; i < 83; i++) {
						preparedStatement = connect
								.prepareStatement("insert into TA.segmented values (?, ?, ?)");
						preparedStatement.setDate(1, new java.sql.Date(lastDate.getTime()));
						preparedStatement.setInt(2, i);
						preparedStatement.setDouble(3, closeprice[i]);
						preparedStatement.executeUpdate();
						closeprice[i] = 0;
					}
					lastDay = cal.get(Calendar.DAY_OF_MONTH);
					lastDate = cal.getTime();
				}
				
				int hh = cal.get(Calendar.HOUR_OF_DAY);
				closeprice[((hh-9)*60+cal.get(Calendar.MINUTE))/5] = rs.getDouble("closeprice");
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void SMA() {
		// fungsi untuk menghitung SMA dan memasukkan ke database SMA
			prepareDatabaseConnection();
			try {
				statement.executeUpdate("truncate SMAseg");
				
				// ambil seluruh data dari data historis yang telah disegmen
				String query = "select * from segmented";
				ResultSet rs = statement.executeQuery(query);
				
				ArrayList<Double> closePrices = new ArrayList<Double>();
				ArrayList<Date> dates = new ArrayList<Date>();
				ArrayList<Integer> segs = new ArrayList<Integer>();
				
				while (rs.next()){
					closePrices.add(rs.getDouble("closeprice"));
					dates.add(rs.getDate("date"));
					segs.add(rs.getInt("seg"));
				}
				
				int size = closePrices.size();
				
				// proses kalkulasi SMA
				for (int i = 0; i < size; i++) {
					
					// init SMA
					double SMA5 = 0;
					double SMA10 = 0;
					double SMA20 = 0;
					double SMA50 = 0;
					
					// SMA5
					if (i-4 >= 0) {
						for (int j = i-4; j <= i; j++) {
							SMA5 += closePrices.get(j);
						}
						SMA5 = SMA5/5;
			
						// SMA10
						if (i-9 >= 0) {
							for (int j = i-9; j <= i; j++) {
								SMA10 += closePrices.get(j);
							}
							SMA10 = SMA10/10;
							
							// SMA20
							if (i-19 >= 0) {
								for (int j = i-19; j <= i; j++) {
									SMA20 += closePrices.get(j);
								}
								SMA20 = SMA20/20;
								
								// SMA50
								if (i-49 >= 0) {
									for (int j = i-49; j <= i; j++) {
										SMA50 += closePrices.get(j);
									}
									SMA50 = SMA50/50;
								}
							}
						}
					}
					preparedStatement = connect
						.prepareStatement("insert into TA.SMAseg values (?, ?, ?, ?, ?, ?, ?)");
					preparedStatement.setDate(1, new java.sql.Date(dates.get(i).getTime()));
					preparedStatement.setInt(2, segs.get(i));
					preparedStatement.setDouble(3, SMA5);
					preparedStatement.setDouble(4, SMA10);
					preparedStatement.setDouble(5, SMA20);
					preparedStatement.setDouble(6, SMA50);
					preparedStatement.setDouble(7, closePrices.get(i));
					preparedStatement.executeUpdate();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	
	public void printParsedRecord() {
	// fungsi untuk print isi array sementara dates dan closePrices
	// setelah melakukan csvParse()
	// untuk pengetesan
		csvParser();
		int counter = 0;
		for (CSVRecord record : records) {
			java.sql.Timestamp ts = new java.sql.Timestamp(Integer.parseInt(record.get("time"))*1000L);
			System.out.println(counter + " " + ts + " " + record.get("close"));
			counter++;
		}
	}
	
	public void csvParser() {
	// melakukan parse terhadap csv data histori
		try {
			Reader in = new FileReader(historyFilename);
			records = CSVFormat.EXCEL.withHeader().parse(in);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void prepareDatabaseConnection() {
		// menyambungkan aplikasi dengan database mySQL
		try {
			Class.forName("com.mysql.jdbc.Driver");
			if (connect == null)
				connect = DriverManager.getConnection("jdbc:mysql://localhost/TA?" + "user=root&password=");
			statement = connect.createStatement();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		StockData stockData = new StockData();
		
		/* melakukan segmentasi
		 StockData.segmentPrice();
		 */
		
		/* kalkulasi SMA
		 stockData.SMA(); 
		 */
		
		
	}
}
