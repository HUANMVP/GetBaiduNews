import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class ReadTitle {

    static Connection connection;
    static int cNum = 0;
    static int tNum = 0;

    //连接数据库
    static {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/mytest", "root", "root");
            System.out.println("Success connect Mysql server!");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Success loading Mysql Driver!");
        } catch (Exception e) {
            System.out.print("Error loading Mysql Driver!");
            e.printStackTrace();
        }
        Document urlStr = getHtml("http://news.baidu.com/");
        getCategories(Objects.requireNonNull(urlStr));
        connection.close();
        System.out.println("there has "+cNum+" categories and "+tNum+" titles");
    }

    //获取网页源代码
    public static Document getHtml(String address) {
        try {
            //5000是设置连接超时时间，单位ms
            return Jsoup.connect(address).timeout(5000).get();
        } catch (Exception e) {
            System.out.println("与服务器连接发生异常错误 : " + e);
        }
        System.out.println("getting html file");
        return null;
    }

    //获取网页的分类信息
    private static void getCategories(Document urlStr) {
        Elements table = urlStr.select("div.menu-list");
        Elements table1 = table.select("li");
        Elements trs = table1.select("a");
        Set<String> set = new HashSet<>();
        for (Element tr : trs) {
            System.out.println("getting categories");
            String title = tr.text();//分类名
            String link = tr.attr("href");//分类链接
            if (set.add(title)) {
                cNum++;
                getTitle("http://news.baidu.com" + link, title);
            }
        }
    }

    //获取分类下的标题
    private static void getTitle(String address, String titleCategory) {
        Document doc = getHtml(address);
        Elements table = Objects.requireNonNull(doc).select("ul[class~=^ulist]");
        Elements tas = table.select("li");
        Elements trs = tas.select("a");
        Set<String> set = new HashSet<>();
        for (Element tr : trs) {
            System.out.println("getting titles");
            String link = tr.attr("href");//标题链接
            if (set.add(link)) {
                tNum++;
                //发布时间
                String time = Objects.requireNonNull(getHtml(link)).select("meta[itemprop=dateUpdate]").attr("content");
                //爬取时间
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateN = dateFormat.format(date);
                String come = "Unknown";
                insertToDatabase(titleCategory, tr.text(), time, dateN, come, link);
            }
        }
    }

    //插入数据到数据库
    private static void insertToDatabase(String titleCategory, String titleName, String publishTime, String getDate, String come, String link) {
        try {
            String sql = " INSERT INTO news_data(news_category,news_title,news_publishtime,news_gettime,news_come,news_link) VALUES( ' "
                    + titleCategory + " ',' " + titleName + " ',' " + publishTime + "','" + getDate + " ','" + come + "',' " + link + " ' ) ";
            Statement statement = connection.createStatement();//执行静态SQL语句
            statement.executeUpdate(sql); //插入数据
            statement.close();
            System.out.println("insert to database success");
        } catch (Exception e) {
            System.out.print("insert to database fail");
            e.printStackTrace();
        }
    }

}


