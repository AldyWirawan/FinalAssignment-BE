import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;


public class DatasetParser {
// untuk membuat dataset lingpipe
// belum dipakai~

	private Iterable<CSVRecord> records; // untuk menyimpan data dari CSV
	
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
}
