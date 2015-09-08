import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;


public class SVM {
// kelas untuk mempersiapkan data SVM dan menjalankan training serta testing
	
	// atribut
	// untuk SQL
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	
	// arraylist untuk menampung semua data yang dibutuhkan
	// diisi dari database
	private ArrayList<Date> dates = new ArrayList<Date>();
	private ArrayList<Integer> segs = new ArrayList<Integer>();
	private ArrayList<Double> closePrices = new ArrayList<Double>();
	private ArrayList<Double> SMA5s = new ArrayList<Double>();
	private ArrayList<Double> SMA10s = new ArrayList<Double>();
	private ArrayList<Double> SMA20s = new ArrayList<Double>();
	private ArrayList<Double> SMA50s = new ArrayList<Double>();
	private ArrayList<Double> SMA520s = new ArrayList<Double>();
	private ArrayList<Double> SMA550s = new ArrayList<Double>();
	private ArrayList<Double> SMA1020s = new ArrayList<Double>();
	private ArrayList<Double> SMA1050s = new ArrayList<Double>();
	private ArrayList<Boolean> labels = new ArrayList<Boolean>();
	
	// fungsi
	
	public SVM() {
	// ctor
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
	
	public void prelim() {
	// melakukan aksi preliminary terhadap data
	// mengambil data dari SMA yang telah disegmentasi dan dimasukkan ke dalam variabel
		prepareDatabaseConnection();
		try {
			resultSet = statement.executeQuery("select * from SMAseg");
			while (resultSet.next()) {
				double SMA5;
				double SMA10;
				double SMA20;
				double SMA50 = resultSet.getDouble("SMA50");
				
				if (SMA50 != 0) {
					SMA5 = resultSet.getDouble("SMA5");
					SMA10 = resultSet.getDouble("SMA10");
					SMA20 = resultSet.getDouble("SMA20");
					dates.add(resultSet.getDate("date"));
					segs.add(resultSet.getInt("seg"));
					closePrices.add(resultSet.getDouble("closeprice"));
					SMA5s.add(SMA5);
					SMA10s.add(SMA10);
					SMA20s.add(SMA20);
					SMA50s.add(SMA50);
					SMA520s.add(SMA5 - SMA20);
					SMA550s.add(SMA5 - SMA50);
					SMA1020s.add(SMA10 - SMA20);
					SMA1050s.add(SMA10 - SMA50);
					//dapat semua data yang sudah dicut!
				}
				
			}
		}  catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public void addLabelExt(int distance) {
	// menambahkan label prediksi dengan melihat harga
	// parameter input adalah distance yaitu 0 jika prediksi dilakukan terhadap 5 menit berikutnya
	// dan angka lain bila prediksi dilakukan terhadap 1 jam berikutnya
	// extra cut -> memotong segmen 70++
		
		int mod;
		
		if (distance == 0)
			// prediksi 5 menit ke depan
			mod = 1;
		else
			// prediksi 1 jam ke depan
			mod = 12;
		
		int random = 0;
		
		for (int i = 0; i < closePrices.size()-mod; i++) {
			if (closePrices.get(i) > closePrices.get(i+mod)) {
				labels.add(false);
			} else if (closePrices.get(i) < closePrices.get(i+mod)) {
				labels.add(true);
			} else {
				if (random % 2 == 0) {
					labels.add(true);
				}
				else {
					labels.add(false);
				}
				random++;
			}
		}
	}
	
	public void addLabel(int distance) {
	// menambahkan label prediksi dengan melihat harga
	// parameter input adalah distance yaitu 0 jika prediksi dilakukan terhadap 5 menit berikutnya
	// dan angka lain bila prediksi dilakukan terhadap 1 jam berikutnya
	// extra cut -> memotong segmen 70++
		
		int mod;
		
		if (distance == 0)
			// prediksi 5 menit ke depan
			mod = 1;
		else
			// prediksi 1 jam ke depan
			mod = 12;
		
		int random = 0;
		
		for (int i = 0; i < closePrices.size()-mod; i++) {
			if (closePrices.get(i) > closePrices.get(i+mod)) {
				labels.add(false);
			} else if (closePrices.get(i) < closePrices.get(i+mod)) {
				labels.add(true);
			} else {
				if (random % 2 == 0) {
					labels.add(true);
				}
				else {
					labels.add(false);
				}
				random++;
			}
		}
		
		// membetulkan label sebelum ganti hari
		if (distance != 0) {
			int start = 23;
			int diminish = 11;
			while (start < labels.size()) {
				if (closePrices.get(start) > closePrices.get(start+diminish))
					labels.set(start, false);
				else if (closePrices.get(start) < closePrices.get(start+diminish))
					labels.set(start, true);
				else {
					if (random % 2 == 0) {
						labels.set(start,true);
					}
					else {
						labels.set(start,false);
					}
					random++;
				}
				start++;
				diminish--;
				if (diminish == 0) {
					start = start+72;
					diminish = 11;
				}
			}
		}
		
	}
	
	public void fillSVMData(int dist){
	// mengisi data SVM
	// data yang diisi merupakan data yang telah diambil sebelumnya
	// data jumlah analisis sentimen diambil langsung dari database
		prelim();
		addLabel(dist);
		prepareDatabaseConnection();
		
		int mod = 0;
		if (dist == 0) {
			mod = 1;
		} else {
			mod = 12;
		}
		try {
			int pos;
			int neg;
			statement.executeUpdate("truncate SVM");
			for (int i = 0; i < closePrices.size()-mod; i++) {
				boolean SMA520P = SMA520s.get(i) >= 0 ? true : false;
				boolean SMA550P = SMA550s.get(i) >= 0 ? true : false;
				boolean SMA1020P = SMA1020s.get(i) >= 0 ? true : false;
				boolean SMA1050P = SMA1050s.get(i) >= 0 ? true : false;
				
				// tes print query
				//System.out.println("select * from sentianalSWN where date='"+dates.get(i)+"' AND seg="+segs.get(i));
				
				ResultSet rs = statement.executeQuery("select * from sentianalSWN where date='"+dates.get(i)+"' AND seg="+segs.get(i));
				pos = 0;
				neg = 0;
				if (rs.next()) {
					pos = rs.getInt("pos");
					neg = rs.getInt("neg");
				}
				
				preparedStatement = connect
						.prepareStatement("insert into TA.SVM values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				preparedStatement.setDouble(1, SMA520s.get(i));	
				preparedStatement.setDouble(2, SMA550s.get(i));
				preparedStatement.setDouble(3, SMA1020s.get(i));
				preparedStatement.setDouble(4, SMA1050s.get(i));
				preparedStatement.setBoolean(5, SMA520P);
				preparedStatement.setBoolean(6, SMA550P);
				preparedStatement.setBoolean(7, SMA1020P);
				preparedStatement.setBoolean(8, SMA1050P);
				preparedStatement.setInt(9, pos);
				preparedStatement.setInt(10, neg);
				preparedStatement.setBoolean(11, labels.get(i));
				preparedStatement.setInt(12, segs.get(i));
				preparedStatement.executeUpdate();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public void cutDataExt(int dist){
		// buang yang memiliki data sentimen analisis sedikit sekali atau tidak ada
		// limitnya adalah 200
		// menghasilkan data yang siap dipakai SVM
		// extra cut -> memotong segmen 70++
		prepareDatabaseConnection();
		try {
			statement.executeUpdate("truncate SVMcut");
			ResultSet rs = statement.executeQuery("select * from SVM");
			int pos;
			int neg;
			int seg;
			if (dist == 0) {
				while (rs.next()) {
					pos = rs.getInt("pos");
					neg = rs.getInt("neg");
					if ((pos > 200) && (neg > 200)) {
						preparedStatement = connect
								.prepareStatement("insert into TA.SVMcut values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						preparedStatement.setDouble(1, rs.getDouble("SMA520"));	
						preparedStatement.setDouble(2, rs.getDouble("SMA550"));
						preparedStatement.setDouble(3, rs.getDouble("SMA1020"));
						preparedStatement.setDouble(4, rs.getDouble("SMA1050"));
						preparedStatement.setBoolean(5, rs.getBoolean("SMA520P"));
						preparedStatement.setBoolean(6, rs.getBoolean("SMA550P"));
						preparedStatement.setBoolean(7, rs.getBoolean("SMA1020P"));
						preparedStatement.setBoolean(8, rs.getBoolean("SMA1050P"));
						preparedStatement.setInt(9, pos);
						preparedStatement.setInt(10, neg);
						preparedStatement.setBoolean(11, rs.getBoolean("label"));
						preparedStatement.executeUpdate();
					}
				}
			} else {
				while (rs.next()) {
					pos = rs.getInt("pos");
					neg = rs.getInt("neg");
					seg = rs.getInt("seg");
					if ((pos > 200) && (neg > 200) && (seg<71)) {
						preparedStatement = connect
								.prepareStatement("insert into TA.SVMcut values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						preparedStatement.setDouble(1, rs.getDouble("SMA520"));	
						preparedStatement.setDouble(2, rs.getDouble("SMA550"));
						preparedStatement.setDouble(3, rs.getDouble("SMA1020"));
						preparedStatement.setDouble(4, rs.getDouble("SMA1050"));
						preparedStatement.setBoolean(5, rs.getBoolean("SMA520P"));
						preparedStatement.setBoolean(6, rs.getBoolean("SMA550P"));
						preparedStatement.setBoolean(7, rs.getBoolean("SMA1020P"));
						preparedStatement.setBoolean(8, rs.getBoolean("SMA1050P"));
						preparedStatement.setInt(9, pos);
						preparedStatement.setInt(10, neg);
						preparedStatement.setBoolean(11, rs.getBoolean("label"));
						preparedStatement.executeUpdate();
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void cutData(){
		// buang yang memiliki data sentimen analisis sedikit sekali atau tidak ada
		// limitnya adalah 200
		// menghasilkan data yang siap dipakai SVM
		prepareDatabaseConnection();
		try {
			statement.executeUpdate("truncate SVMcut");
			ResultSet rs = statement.executeQuery("select * from SVM");
			int pos;
			int neg;
			while (rs.next()) {
				pos = rs.getInt("pos");
				neg = rs.getInt("neg");
				if ((pos > 200) && (neg > 200)) {
					preparedStatement = connect
							.prepareStatement("insert into TA.SVMcut values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					preparedStatement.setDouble(1, rs.getDouble("SMA520"));	
					preparedStatement.setDouble(2, rs.getDouble("SMA550"));
					preparedStatement.setDouble(3, rs.getDouble("SMA1020"));
					preparedStatement.setDouble(4, rs.getDouble("SMA1050"));
					preparedStatement.setBoolean(5, rs.getBoolean("SMA520P"));
					preparedStatement.setBoolean(6, rs.getBoolean("SMA550P"));
					preparedStatement.setBoolean(7, rs.getBoolean("SMA1020P"));
					preparedStatement.setBoolean(8, rs.getBoolean("SMA1050P"));
					preparedStatement.setInt(9, pos);
					preparedStatement.setInt(10, neg);
					preparedStatement.setBoolean(11, rs.getBoolean("label"));
					preparedStatement.executeUpdate();
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void cutMore() {
		// potong data negatif
		// agar data balance
		prepareDatabaseConnection();
		try {
			statement.executeUpdate("truncate SVMcutmore");
			ResultSet rs = statement.executeQuery("select * from SVMcut");
			int cutcounter = 0;
			while (rs.next()) {
				if (cutcounter < 140) {
					if (rs.getBoolean("label")) {
						preparedStatement = connect
								.prepareStatement("insert into TA.SVMcutmore values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						preparedStatement.setDouble(1, rs.getDouble("SMA520"));	
						preparedStatement.setDouble(2, rs.getDouble("SMA550"));
						preparedStatement.setDouble(3, rs.getDouble("SMA1020"));
						preparedStatement.setDouble(4, rs.getDouble("SMA1050"));
						preparedStatement.setBoolean(5, rs.getBoolean("SMA520P"));
						preparedStatement.setBoolean(6, rs.getBoolean("SMA550P"));
						preparedStatement.setBoolean(7, rs.getBoolean("SMA1020P"));
						preparedStatement.setBoolean(8, rs.getBoolean("SMA1050P"));
						preparedStatement.setInt(9, rs.getInt("pos"));
						preparedStatement.setInt(10, rs.getInt("neg"));
						preparedStatement.setBoolean(11, rs.getBoolean("label"));
						preparedStatement.executeUpdate();
					} else {
						cutcounter++;
					}
				} else {
					preparedStatement = connect
							.prepareStatement("insert into TA.SVMcutmore values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					preparedStatement.setDouble(1, rs.getDouble("SMA520"));	
					preparedStatement.setDouble(2, rs.getDouble("SMA550"));
					preparedStatement.setDouble(3, rs.getDouble("SMA1020"));
					preparedStatement.setDouble(4, rs.getDouble("SMA1050"));
					preparedStatement.setBoolean(5, rs.getBoolean("SMA520P"));
					preparedStatement.setBoolean(6, rs.getBoolean("SMA550P"));
					preparedStatement.setBoolean(7, rs.getBoolean("SMA1020P"));
					preparedStatement.setBoolean(8, rs.getBoolean("SMA1050P"));
					preparedStatement.setInt(9, rs.getInt("pos"));
					preparedStatement.setInt(10, rs.getInt("neg"));
					preparedStatement.setBoolean(11, rs.getBoolean("label"));
					preparedStatement.executeUpdate();
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void makeDataFile(int dist){
	// membuat data yang siap dipakai SVM
		prepareDatabaseConnection();
			try {
				String query;
				if (dist != 0)
					query = "select * from SVMtrain";
				else
					query = "select * from SVMtrain5m";
				resultSet = statement.executeQuery(query);
				FileWriter f = new FileWriter(new File("SVMdata.data"));
				while (resultSet.next()) {
					Double d = resultSet.getDouble("SMA520");
					//double SMA520 = d;
					int SMA520 = d.intValue();
					d = resultSet.getDouble("SMA550");
					//double SMA550 = d;
					int SMA550 = d.intValue();
					d = resultSet.getDouble("SMA1020");
					//double SMA1020 = d;
					int SMA1020 = d.intValue();
					d = resultSet.getDouble("SMA1050");
					//double SMA1050 = d;
					int SMA1050 = d.intValue();
					int SMA520P;
					int SMA550P;
					int SMA1020P;
					int SMA1050P;
					int label;
					if (resultSet.getBoolean("SMA520P"))
						SMA520P = 1;
					else
						SMA520P = 0;
					if (resultSet.getBoolean("SMA550P"))
						SMA550P = 1;
					else
						SMA550P = 0;
					if (resultSet.getBoolean("SMA1020P"))
						SMA1020P = 1;
					else
						SMA1020P = 0;
					if (resultSet.getBoolean("SMA1050P"))
						SMA1050P = 1;
					else
						SMA1050P = 0;
					if (resultSet.getBoolean("label"))
						label = 1;
					else
						label = 0;
					resultSet.getBoolean("SMA550P");
					
					f.write("" + label + " " + SMA520 + " " + SMA550 + " " + SMA1020 + " " + SMA1050 + " " 
								+ SMA520P + " " + SMA550P + " " + SMA1020P + " " + SMA1050P + " "
								+ resultSet.getInt("pos") + " " + resultSet.getInt("neg") + " " +"\n");
				}
				f.close();
				if (dist != 0)
					query = "select * from SVMtest";
				else
					query = "select * from SVMtest5m";
				resultSet = statement.executeQuery(query);
				f = new FileWriter(new File("SVMdataTest.data"));
				while (resultSet.next()) {
					Double d = resultSet.getDouble("SMA520");
					//double SMA520 = d;
					int SMA520 = d.intValue();
					d = resultSet.getDouble("SMA550");
					//double SMA550 = d;
					int SMA550 = d.intValue();
					d = resultSet.getDouble("SMA1020");
					//double SMA1020 = d;
					int SMA1020 = d.intValue();
					d = resultSet.getDouble("SMA1050");
					//double SMA1050 = d;
					int SMA1050 = d.intValue();
					int SMA520P;
					int SMA550P;
					int SMA1020P;
					int SMA1050P;
					int label;
					if (resultSet.getBoolean("SMA520P"))
						SMA520P = 1;
					else
						SMA520P = 0;
					if (resultSet.getBoolean("SMA550P"))
						SMA550P = 1;
					else
						SMA550P = 0;
					if (resultSet.getBoolean("SMA1020P"))
						SMA1020P = 1;
					else
						SMA1020P = 0;
					if (resultSet.getBoolean("SMA1050P"))
						SMA1050P = 1;
					else
						SMA1050P = 0;
					if (resultSet.getBoolean("label"))
						label = 1;
					else
						label = 0;
					resultSet.getBoolean("SMA550P");
					
					f.write("" + label + " " + SMA520 + " " + SMA550 + " " + SMA1020 + " " + SMA1050 + " " 
								+ SMA520P + " " + SMA550P + " " + SMA1020P + " " + SMA1050P + " "
								+ resultSet.getInt("pos") + " " + resultSet.getInt("neg") + " " +"\n");
				}
				f.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public void makeDataFileAll(int dist){
		// membuat data yang siap dipakai SVM
			prepareDatabaseConnection();
				try {
					String query;
					if (dist == 0) {
						query = "select * from svmcut5m";
					} else {
						query = "select * from svmcut";
					}
					resultSet = statement.executeQuery(query);
					FileWriter f = new FileWriter(new File("SVMdata.data"));
					while (resultSet.next()) {
						Double d = resultSet.getDouble("SMA520");
						//double SMA520 = d;
						int SMA520 = d.intValue();
						d = resultSet.getDouble("SMA550");
						//double SMA550 = d;
						int SMA550 = d.intValue();
						d = resultSet.getDouble("SMA1020");
						//double SMA1020 = d;
						int SMA1020 = d.intValue();
						d = resultSet.getDouble("SMA1050");
						//double SMA1050 = d;
						int SMA1050 = d.intValue();
						int SMA520P;
						int SMA550P;
						int SMA1020P;
						int SMA1050P;
						int label;
						if (resultSet.getBoolean("SMA520P"))
							SMA520P = 1;
						else
							SMA520P = 0;
						if (resultSet.getBoolean("SMA550P"))
							SMA550P = 1;
						else
							SMA550P = 0;
						if (resultSet.getBoolean("SMA1020P"))
							SMA1020P = 1;
						else
							SMA1020P = 0;
						if (resultSet.getBoolean("SMA1050P"))
							SMA1050P = 1;
						else
							SMA1050P = 0;
						if (resultSet.getBoolean("label"))
							label = 1;
						else
							label = 0;
						resultSet.getBoolean("SMA550P");
						
						f.write("" + label + " " + SMA520 + " " + SMA550 + " " + SMA1020 + " " + SMA1050 + " " 
									+ SMA520P + " " + SMA550P + " " + SMA1020P + " " + SMA1050P + " "
									+ resultSet.getInt("pos") + " " + resultSet.getInt("neg") + " " +"\n");
					}
					f.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	
	public void SMAccuracy() {
	// print akurasi simple moving average
		prepareDatabaseConnection();
		try {
			resultSet = statement.executeQuery("select * from svm");
			int total = 0;
			int SMA520 = 0;
			int SMA550 = 0;
			int SMA1020 = 0;
			int SMA1050 = 0;
			while (resultSet.next()) {
				total++;
				if (resultSet.getBoolean("SMA520P") == resultSet.getBoolean("label"))
					SMA520++;
				if (resultSet.getBoolean("SMA550P") == resultSet.getBoolean("label"))
					SMA550++;
				if (resultSet.getBoolean("SMA1020P") == resultSet.getBoolean("label"))
					SMA1020++;
				if (resultSet.getBoolean("SMA1050P") == resultSet.getBoolean("label"))
					SMA1050++;
			}
			double akurasiSMA520 = (double) SMA520 / (double) total * 100;
			double akurasiSMA550 = (double) SMA550 / (double) total * 100;
			double akurasiSMA1020 = (double) SMA1020 / (double) total * 100;
			double akurasiSMA1050 = (double) SMA1050 / (double) total * 100;
			System.out.println("AKURASI SMA");
			System.out.println("TOTAL DATA : "+ total);
			System.out.println("SMA520  : BENAR " + SMA520 + " AKURASI " + akurasiSMA520 + "%");
			System.out.println("SMA550  : BENAR " + SMA550 + " AKURASI " + akurasiSMA550 + "%");
			System.out.println("SMA1020 : BENAR " + SMA1020 + " AKURASI " + akurasiSMA1020 + "%");
			System.out.println("SMA1050 : BENAR " + SMA1050 + " AKURASI " + akurasiSMA1050 + "%");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void cleanCut() {
	// memotong data menjadi data latih dan data uji
	// ratio 80:20
	// membuat agar data latih balance
		prepareDatabaseConnection();
		try {
			resultSet = statement.executeQuery("select * from svmcut");
			int upcounter = 0;
			int dwcounter = 0;
			while (resultSet.next()) {
				if (resultSet.getBoolean("label")) {
					if (upcounter < 377) {
						preparedStatement = connect
								.prepareStatement("insert into TA.SVMtrain values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						preparedStatement.setDouble(1, resultSet.getDouble("SMA520"));	
						preparedStatement.setDouble(2, resultSet.getDouble("SMA550"));
						preparedStatement.setDouble(3, resultSet.getDouble("SMA1020"));
						preparedStatement.setDouble(4, resultSet.getDouble("SMA1050"));
						preparedStatement.setBoolean(5, resultSet.getBoolean("SMA520P"));
						preparedStatement.setBoolean(6, resultSet.getBoolean("SMA550P"));
						preparedStatement.setBoolean(7, resultSet.getBoolean("SMA1020P"));
						preparedStatement.setBoolean(8, resultSet.getBoolean("SMA1050P"));
						preparedStatement.setInt(9, resultSet.getInt("pos"));
						preparedStatement.setInt(10, resultSet.getInt("neg"));
						preparedStatement.setBoolean(11, resultSet.getBoolean("label"));
						preparedStatement.executeUpdate();	
						upcounter++;
					} else {
						preparedStatement = connect
								.prepareStatement("insert into TA.SVMtest values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						preparedStatement.setDouble(1, resultSet.getDouble("SMA520"));	
						preparedStatement.setDouble(2, resultSet.getDouble("SMA550"));
						preparedStatement.setDouble(3, resultSet.getDouble("SMA1020"));
						preparedStatement.setDouble(4, resultSet.getDouble("SMA1050"));
						preparedStatement.setBoolean(5, resultSet.getBoolean("SMA520P"));
						preparedStatement.setBoolean(6, resultSet.getBoolean("SMA550P"));
						preparedStatement.setBoolean(7, resultSet.getBoolean("SMA1020P"));
						preparedStatement.setBoolean(8, resultSet.getBoolean("SMA1050P"));
						preparedStatement.setInt(9, resultSet.getInt("pos"));
						preparedStatement.setInt(10, resultSet.getInt("neg"));
						preparedStatement.setBoolean(11, resultSet.getBoolean("label"));
						preparedStatement.executeUpdate();	
					}
				} else {
					if (dwcounter < 377) {
						preparedStatement = connect
								.prepareStatement("insert into TA.SVMtrain values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						preparedStatement.setDouble(1, resultSet.getDouble("SMA520"));	
						preparedStatement.setDouble(2, resultSet.getDouble("SMA550"));
						preparedStatement.setDouble(3, resultSet.getDouble("SMA1020"));
						preparedStatement.setDouble(4, resultSet.getDouble("SMA1050"));
						preparedStatement.setBoolean(5, resultSet.getBoolean("SMA520P"));
						preparedStatement.setBoolean(6, resultSet.getBoolean("SMA550P"));
						preparedStatement.setBoolean(7, resultSet.getBoolean("SMA1020P"));
						preparedStatement.setBoolean(8, resultSet.getBoolean("SMA1050P"));
						preparedStatement.setInt(9, resultSet.getInt("pos"));
						preparedStatement.setInt(10, resultSet.getInt("neg"));
						preparedStatement.setBoolean(11, resultSet.getBoolean("label"));
						preparedStatement.executeUpdate();	
						dwcounter++;
					} else {
						preparedStatement = connect
								.prepareStatement("insert into TA.SVMtest values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						preparedStatement.setDouble(1, resultSet.getDouble("SMA520"));	
						preparedStatement.setDouble(2, resultSet.getDouble("SMA550"));
						preparedStatement.setDouble(3, resultSet.getDouble("SMA1020"));
						preparedStatement.setDouble(4, resultSet.getDouble("SMA1050"));
						preparedStatement.setBoolean(5, resultSet.getBoolean("SMA520P"));
						preparedStatement.setBoolean(6, resultSet.getBoolean("SMA550P"));
						preparedStatement.setBoolean(7, resultSet.getBoolean("SMA1020P"));
						preparedStatement.setBoolean(8, resultSet.getBoolean("SMA1050P"));
						preparedStatement.setInt(9, resultSet.getInt("pos"));
						preparedStatement.setInt(10, resultSet.getInt("neg"));
						preparedStatement.setBoolean(11, resultSet.getBoolean("label"));
						preparedStatement.executeUpdate();	
					}
				}
				
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void printUpDown() {
	// print data berlabel naik dan data berlabel turun untuk mengecek keseimbangan data
		prepareDatabaseConnection();
		int pos = 0;
		int neg = 0;
		int total = 0;
		try {
			resultSet = statement.executeQuery("select * from svmcutmore");
			while (resultSet.next()) {
				total++;
				if (resultSet.getBoolean("label"))
					pos++;
				else
					neg++;
			}
			System.out.println ("TOTAL DATA = "+total);
			System.out.println ("POSITIF = "+pos);
			System.out.println ("NEGATIF = "+neg);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		SVM svm = new SVM();
		
		svm.SMAccuracy();
		
		//svm.makeDataFile(1);
		
		//svm.cleanCut();
		
		//svm.makeDataFileAll(1);
		
		/* isi
		svm.fillSVMData(0);
		*/
		
		/* cut
		svm.cutData(0);
		*/
		
		/* cutmore *HARDCODED JUMLAHNYA
		svm.cutMore();
		 */
		
		/* buat data train dan test untuk SVM
		svm.makeDataFile(0);
		*/
		
		/* print akurasi SMA
		svm.SMAccuracy();
		 */
		
		/* cek balance *UBAH DATABASENYA DULU!
		svm.printUpDown();
		 */
		
	}
}
