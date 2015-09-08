import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class TweetData {
// kelas untuk mengambil dan menyimpan data Tweet ke dalam database
	
	// atribut
	private ConfigurationBuilder builder;
	private TwitterFactory factory;
	private Twitter twitter;
	private TwitterStream twitterStream;
	
	// untuk database
	private Connection connect = null;
	private PreparedStatement preparedStatement = null;
	
	// fungsi
	
	public TweetData() {
	// ctor
	// melakukan inisialisasi awal
		init();
	}
	
	public void init(){
	// menginisiasi twitter API
	// melakukan setting consumer key dan consumer secret
	// melakukan setting token akses
	// prosedur dirun saat inisialisasi class TweetData
		builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey("iSl6vmkExoKGTSNgR7fSnpTRD");
		builder.setOAuthConsumerSecret("y0HVJoG30fS54onuNVrADRwagPdT0wNerWbm5XFajkKYvtW59C");
		builder.setOAuthAccessToken("1349434058-9fwmZgln66neBbo1gSGEnOZLcTTe3Irt28Vqt75");
		builder.setOAuthAccessTokenSecret("vKc50s4oT5fwyYWk7euS4BTx9fp99L71qhi3WBpK6f8XI");
		Configuration configuration = builder.build();
		factory = new TwitterFactory(configuration);
		twitter = factory.getInstance();
		twitterStream = new TwitterStreamFactory(configuration).getInstance();
		if (connect == null)
			try {
				connect = DriverManager.getConnection("jdbc:mysql://localhost/TA?" + "user=root&password=");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public void TwitterStreaming(){
	// mencari random tweet dengan filter tertentu
	// masukan ke dalam database rawtweet
		
		StatusListener statusListener = new StatusListener() {

			public void onException(Exception arg0) {
				// TODO Auto-generated method stub
				
			}

			public void onDeletionNotice(StatusDeletionNotice arg0) {
				// TODO Auto-generated method stub
				
			}

			public void onScrubGeo(long arg0, long arg1) {
				// TODO Auto-generated method stub
				
			}

			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub
				
			}

			public void onStatus(Status status) {
				// TODO Auto-generated method stub

				//console output
				//System.out.println("created at " + status.getCreatedAt());
				
				try {
					preparedStatement = connect
							.prepareStatement("insert into TA.rawtweetnew values (?, ?)");
					preparedStatement.setTimestamp(1, new java.sql.Timestamp(status.getCreatedAt().getTime()));
					preparedStatement.setNString(2, status.getText());
					preparedStatement.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

			public void onTrackLimitationNotice(int arg0) {
				// TODO Auto-generated method stub
				
			}

	         
		};

		FilterQuery fq = new FilterQuery();        
		
		fq.language(new String[] { "en" });
		
		String keywords[] = {"tes"};

		fq.track(keywords);        
		
		twitterStream.addListener(statusListener);
		// jika ingin menggunakan filter
		// dalam hal ini keywordnya adalah tes
		//twitterStream.filter(fq);
	    
		// lakukan stream dengan filter bahasa -> bahasa inggris saja
		twitterStream.sample("en");
	}
	
	public static void main(String args[]) throws TwitterException, IOException {
		TweetData td = new TweetData();
		td.init();
		td.TwitterStreaming();
	}
}
