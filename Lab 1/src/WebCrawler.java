import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Thuan Tran
 * @version 1.0
 * @Date January 13th, 2018
 * This project is used to fulfill Lab 1 for CSS 490 Cloud Computing class
 * This project is about creating a web crawler for can hop N number of times from a starting URL and
 * print out the URL and html of the last hop
 */

public class WebCrawler {

    // URI is used because URL equals method is not good
    // Source: Stackoverflow
    public static Set<URI> visitedPages = new HashSet<URI>();

    /**
     * Main entry point of the program
     *
     * @param args A String array that has 2 values. First one is the URL and the second one is the number of hops
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid number of input. Need to specify only a given url and number of hops");
            return;
        }
        try {
            String previousURL = args[0];
            String currentURL = args[0];
            visitedPages.add(new URI(currentURL));
            int numHops = Integer.parseInt(args[1]);
            if (numHops < 0) {
                System.out.println("Invalid number of hops. Need to be positive");
            }
            while (numHops > 0) {
                currentURL = getNextURL(previousURL);
                // Could not advance any further. Stop the hop
                if (currentURL.equals(previousURL)) {
                    System.out.println("The page does not contains anymore absolute URL");
                    System.out.println("It stopped at " + numHops + "th hops");
                    break;
                }
                previousURL = currentURL; // remember the previous URL
                numHops--;
            }
            System.out.println("Final URL that was landed on: " + currentURL);
            System.out.println("HTML document of the page that was landed on:");
            Document website = Jsoup.connect(currentURL).get();
            System.out.println(website.toString());
        } catch (IOException invalidConnection) {
            System.out.println("Unable to make connection the last webpage that was visited");
        } catch (URISyntaxException invalidURL) {
            System.out.println("Invalid page format");
        } catch (NumberFormatException invalidNumberOfHops) {
            System.out.println("Only input integer that is number of hops");

        }
    }

    /**
     * This is a helper method that is used to get the next available URL from a given URL
     * It will compare next URL inside the page based on the URI
     *
     * @param currentURL current URL of the page we are at
     * @return the next URL to crawl
     */
    public static String getNextURL(String currentURL) {
        try {

            System.out.println(currentURL);
            // Get the HTML of the page given URL and get all query all absolute reference
            Document website = Jsoup.connect(currentURL).get();
            Elements absoluteURL = website.select("a[href]");
            for (int i = 0; i < absoluteURL.size(); i++) {
                String tempURL = absoluteURL.get(i).attr("abs:href");
                URI visitedPage = new URI(tempURL);
                if (!visitedPages.contains(visitedPage)) {
                    visitedPages.add(visitedPage);
                    return tempURL;
                }
            }
        } catch (IOException exception) {
            System.out.println("Unable to make connection to the website");
        } catch (URISyntaxException invalidURL) {
            System.out.println("Invalid page format");
        }
        // Only return this if there are no unique absolute reference in the page
        return currentURL;
    }
}
