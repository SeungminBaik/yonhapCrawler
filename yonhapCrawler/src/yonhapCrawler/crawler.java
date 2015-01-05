package yonhapCrawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class crawler {
	private static String[] topics = {"politics","northkorea","economy","stock","it","society","local",
		"entertainment","culture","sports","international","compatriot","section"};
	private static String url = "";
	private static String date = "2015/01/05";
	private static JSONObject article = new JSONObject(); //Content, ImageList, DateTime, URL
	private static JSONArray articleImageList = new JSONArray(); // List of Image

	public static void main(String[] args) throws IOException,SocketTimeoutException{

		File dirDate = new File("C:/Users/еб©М/Desktop/",date.replaceAll("/", ""));
		dirDate.mkdir();
		//Iterate crawling for all topics
		for(int topicCnt=0; topicCnt<13; topicCnt++){
			int articleNum = 1;
			File dirTopic = new File(dirDate.getAbsoluteFile(),topics[topicCnt]);
			dirTopic.mkdir();
			url = "http://www.yonhapnews.co.kr/"+topics[topicCnt]+"/index.html";

			try {
				//Make List of HyperLink in the given topic page.
				//Prevent same HyperLink(Because Title, snippet, picture have same link).
				Document parentDoc = Jsoup.connect(url).get();
				Elements linksWithRedun = parentDoc.select("a[href]");
				Elements links = new Elements();
				for(Element link: linksWithRedun){
					int redunChecker = 0;
					for(Element link2: links){
						if(link2.attr("href").equals(link.attr("href"))) redunChecker++;
					}
					if(redunChecker == 0) links.add(link);
				}
				//Find all articles in given date
				for(Element link: links){
					
					FileWriter fw = new FileWriter(dirTopic.getAbsolutePath()+"/"+topics[topicCnt]+"_"+articleNum+".json");
					
					String articleLink = link.attr("href");
					
					if(articleLink.contains(date)){
						Document doc = Jsoup.connect(articleLink.toString()).get();
						Element publishDate = doc.getElementsByClass("pblsh").first();
						String pblshDate = publishDate.text().subSequence(0, 10).toString();	
						article.put("url",articleLink);
						article.put("datetime",pblshDate);
						Element wholeArticleContent = doc.getElementsByClass("article").first();

						//Process an article into structured format
						//Refine content

						Elements surplusContent = wholeArticleContent.select("div");
						for(Element surplus: surplusContent){
							surplus.remove();
						}

						//Content of article
						String[] tempContent = surplusContent.toString().split("<p class=");
						Document tidy = new Document("");
						tidy.append(tempContent[0].replaceAll("<p></p>","").replaceAll("</p>", "</p>/n"));
						String content = tidy.text();
						article.put("images",articleImageList);
						article.put("content",content);
						
						boolean isImageTag = false;				
						int imgCounter = 0;
						JSONObject[] articleImage = new JSONObject[10];
						
						for(Element surplus: surplusContent){
							
							String imageInfo = surplus.html().toString();
							
							if(imageInfo.startsWith("<img src")==true){
								articleImage[imgCounter] = new JSONObject();
								String[] temp = imageInfo.split("\"");
								String imageSource = temp[1];
								articleImage[imgCounter].put("url",imageSource);
								isImageTag = true;
								imgCounter++;
							}
							if(imageInfo.startsWith("<strong>") == true && isImageTag == true){
								Document tidyTag = new Document("");
								tidyTag.append(imageInfo);
								String imgTag = tidyTag.text();
								articleImage[imgCounter-1].put("tag",imgTag);
								isImageTag = false;
								articleImageList.add(articleImage[imgCounter-1]);
							}
						}
						fw.write(article.toString()+"\n");
						//System.out.println(article);

						for(int cnt=0; cnt<imgCounter; cnt++){
							articleImage[cnt].clear();
						}
						articleImageList.clear();
						article.clear();
						fw.flush();
						fw.close();
						articleNum++;
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
