import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentimentAnalysis {
// class untuk melakukan analisis sentimen pada twitter
// dapat menggunakan Lingpipe atau SentiWordNet
	
	// untuk SQL
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	
	// library Lingpipe dan SentiWordNet
	private SentimentClassifier sc = new SentimentClassifier(); // Lingpipe
	private SWN3 swn = new SWN3(); // SentiWordNet
	
	public void processTweetDatabase() {
	// melakukan sentimen analisis pada database twitter
	// memasukan hasil dari sentimen analisis berupa jumlah sentimen positif dan negatif ke dalam database
		prepareDatabaseConnection();
		String query = "select * from rawtweetcopy";
		try {
			// inisialisasi variabel pembantu
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			Date lastDate = (Date) formatter.parse("2015-08-03");
			cal.setTime(lastDate);
			int lastDay = cal.get(Calendar.DAY_OF_MONTH);
			Date currDate;
			
			// proses query
			ResultSet rs = statement.executeQuery(query);
			
			// variabel penyimpan jumlah analisis sentimen
			int pos[] = new int[100];
			int neg[] = new int[100];
			
			// inisiasi array
			for (int i = 0; i < 100; i++) {
				pos[i] = 0;
				neg[i] = 0;
			}
			
			while (rs.next()) {
				// iterasi tiap data yang diambil dari database twitter
				currDate = rs.getTimestamp("date");
				cal.setTime(currDate);
				if (cal.get(Calendar.DAY_OF_MONTH) != lastDay) {
				// jika hari yang diproses tidak sama
				// flush data + reset variable
					for (int i = 0; i < 84; i++) {
						preparedStatement = connect
								.prepareStatement("insert into TA.sentianal values (?, ?, ?, ?)");
						preparedStatement.setDate(1, new java.sql.Date(lastDate.getTime()));
						preparedStatement.setInt(2, i);
						preparedStatement.setInt(3, pos[i]);
						preparedStatement.setInt(4, neg[i]);
						preparedStatement.executeUpdate();
						pos[i] = 0;
						neg[i] = 0;
					}
					lastDay = cal.get(Calendar.DAY_OF_MONTH);
					lastDate = cal.getTime();
				}
				
				int hh = cal.get(Calendar.HOUR_OF_DAY);
				if (hh >= 9 && hh <= 16){
					/* LINGPIPE
					String result = sc.classify(removeUrl(rs.getString("text")));
					if (result.equals("Positive")) {
						System.out.println("pos");
						pos[((hh-9)*60+cal.get(Calendar.MINUTE))/5]++;
					} else if (result.equals("Negative")) {
						System.out.println("neg");
						neg[((hh-9)*60+cal.get(Calendar.MINUTE))/5]++;
					}
					*/
					/* Stanford
					int result = NLP.findSentiment(removeUrl(rs.getString("text")));
					if (result > 2) {
						pos[((hh-9)*60+cal.get(Calendar.MINUTE))/5]++;
					} else if (result < 2) {
						neg[((hh-9)*60+cal.get(Calendar.MINUTE))/5]++;
					}
					*/
					/* SWN */
					double result = swn.getScore(removeUrl(rs.getString("text")));
					if (result > 0) {
						pos[((hh-9)*60+cal.get(Calendar.MINUTE))/5]++;
						System.out.println("pos");
					} else if (result < 0) {
						neg[((hh-9)*60+cal.get(Calendar.MINUTE))/5]++;
						System.out.println("neg");
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove url from the string using regular expression
	 * 
	 * @param text
	 * @return URL free text
	 */
	private String removeUrl(String commentstr)
    {
        String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(commentstr);
        //int i = 0;
        while (m.find()) {
            commentstr = commentstr.replace(m.group(),"").trim();
            //i++;
        }
        return removeHTMLChar(commentstr);
    }

	/**
	 * Public tweets contains certain reserve character of HTML such as &amp,
	 * &quote This method cleans such HTMl reserve characters from the text
	 * using Regular Expression.
	 * 
	 * @param text
	 * @return
	 */

	public String removeHTMLChar(String text) {

		return text.replaceAll("&amp;", "&").replaceAll("&quot;", "\"")
				.replaceAll("&apos;", "'").replaceAll("&lt;", "<")
				.replaceAll("&gt;", ">");
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
	
	public void fillDummyDate() {
		// isi database dummydate untuk testing
		prepareDatabaseConnection();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date d = (Date) formatter.parse("2015-08-03");
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			cal.set(Calendar.HOUR_OF_DAY, 9);
			System.out.println(cal);
			statement.executeUpdate("truncate dummydate");
			for (int i = 0; i < 168; i++) {
				preparedStatement = connect
						.prepareStatement("insert into TA.dummydate values (?)");
				preparedStatement.setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()));
				preparedStatement.executeUpdate();
				cal.add(Calendar.MINUTE, 2);
				cal.add(Calendar.SECOND, 30);
				System.out.println(cal);
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testIdxCalc() {
		// fungsi untuk mencoba perhitungan sekmen waktu pada sentimen analisis
		// menggunakan database dummydate yang telah terisi sebelumnya
		prepareDatabaseConnection();
		String query = "select * from dummydate";
		try {
			// inisialisasi variabel pembantu
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			cal.setTime((Date) formatter.parse("2015-08-03"));
			int lastDay = cal.get(Calendar.DAY_OF_MONTH);
			Date currDate;
			
			// proses query
			ResultSet rs = statement.executeQuery(query);
			
			// variabel untuk mengetes ketepatan segmen waktu sesuai dengan formula yang ada
			int tes[] = new int[100];
			
			while (rs.next()) {
				// iterasi tiap data
				currDate = rs.getTimestamp("date");
				cal.setTime(currDate);
				if (cal.get(Calendar.DAY_OF_MONTH) == lastDay) {
					// jika hari yang diproses masih sama, masukan ke dalam data
					int hh = cal.get(Calendar.HOUR_OF_DAY);
					if (hh >= 9 && hh <= 16){
						// mengisi array tes dengan indeks segmen waktu
						tes[((hh-9)*60+cal.get(Calendar.MINUTE))/5]++;
					}
					
				}
			}
			
			for (int i = 0; i < 100; i++) {
				System.out.println(tes[i]);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String argv[]) {
		SentimentAnalysis sa = new SentimentAnalysis();
		sa.processTweetDatabase();
		
		/* testing
		sa.fillDummyDate();
		sa.testIdxCalc(); 
		*/
	}
}
