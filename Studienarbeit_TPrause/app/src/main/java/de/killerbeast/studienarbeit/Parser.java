package de.killerbeast.studienarbeit;

import android.content.SharedPreferences;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import de.killerbeast.studienarbeit.interfaces.Interface_Downloader;
import de.killerbeast.studienarbeit.interfaces.Interface_Parser;

public class Parser implements Interface_Downloader {

    private final Interface_Parser caller;
    private String coursename = "";

    public Parser(Interface_Parser caller) {

        this.caller = caller;

    }

    public void parse(String what, String... args) {

        switch (what) {
            //TODO add what to manager;
            case "degrees": downloadDegrees(); break;
            case "semester": downloadSemester(args); break;
            case "courses": downloadCourses(); break;
            case "news": downloadNews(); break;
            case "modulbook": downloadAllModulbook(args); break;
            case "person": downloadPersonSearch(args); break;
            case "sheduleChanges": downloadSheduleChanges(args); break;
            case "events": downloadEvents(); break;

        }

    }

    private void downloadSheduleChanges(String[] args) {

        DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
        String dateString = args[0];
        String course = Manager.getInstance().getSharedPreferences("settings").getString("degree_short", "0");
        String semester = Manager.getInstance().getSharedPreferences("settings").getString("semester_short", "0");
        try {

            Date firstDate = sdf.parse(dateString);

            for (int i = 0; i < 8; i++) {

                String checkDate = sdf.format(Objects.requireNonNull(firstDate));

                Downloader downloader = new Downloader(this, "sheduleChanges");
                downloader.download(String.format(Manager.getInstance().getContext().getResources().getString(R.string.url_sheduleChanges_template), course, semester, checkDate));

                Calendar c = Calendar.getInstance();
                c.setTime(firstDate);
                c.add(Calendar.DATE, 1);
                firstDate = c.getTime();

            }


        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void downloadNews(){

        String url = Manager.getInstance().getContext().getResources().getString(R.string.url_news);
        Downloader downloader = new Downloader(this, "news");
        downloader.download(url);

    }

    private void downloadCourses() {

        String url = String.format(Manager.getInstance().getContext().getResources().getString(R.string.url_shedule), Manager.getInstance().getSharedPreferences("settings").getString("degree_short", "0"), Manager.getInstance().getSharedPreferences("settings").getString("semester_short", "0"));
        Downloader downloader = new Downloader(this, "courses");
        downloader.download(url);

    }

    private void downloadDegrees(){

        String url = String.format(Manager.getInstance().getContext().getResources().getString(R.string.url_shedule), "0" , "0");
        Downloader downloader = new Downloader(this, "degrees");
        downloader.download(url);

    }

    private void downloadSemester(String[] args){

        String url = String.format(Manager.getInstance().getContext().getResources().getString(R.string.url_shedule), args[0] , "0");
        Downloader downloader = new Downloader(this, "semester");
        downloader.download(url);

    }

    private void downloadEvents() {

        String url = (Manager.getInstance().getContext().getResources().getString(R.string.url_events));

        Downloader downloader = new Downloader(this, "events");
        downloader.download(url);

    }

    @Override
    public void finishedDownload(String html, String mode) {

        switch (mode) {

            case "degrees": parseDegrees(html); break;
            case "semester": parseSemester(html); break;
            case "courses": parseCourses(html); break;
            case "news": parseNews(html); break;
            case "modulbook_all": parseModulbookAll(html); break;
            case "modulbook": parseModulbook(html); break;
            case "person_search": parsePersonSearch(html); break;
            case "sheduleChanges": parseSheduleChanges(html); break;
            case "events": parseEvents(html); break;

        }

    }


    private void parseSheduleChanges(String html) {

        if (!html.contains("Zu Ihrer Auswahl sind keine Termine vorhanden")) {

            Map<String, String> values = new LinkedHashMap<>();

            html = html.substring(html.indexOf("<table>", html.indexOf("twelve columns nop")));

            html = html.substring(0, html.indexOf("\\/div>"));

            while (html.contains("<\\/tr>")) {

                String degree;
                String coursename;
                String prof;
                String canceledEvent;
                String replacement;

                {

                    html = html.substring(html.indexOf("<td "));
                    degree = html.substring(html.indexOf(">") + 1, html.indexOf("<\\/td>"));
                    html = html.substring(html.indexOf("<\\/td>") + 6);

                    html = html.substring(html.indexOf("<td "));
                    coursename = html.substring(html.indexOf(">") + 1, html.indexOf("<\\/td>"));
                    html = html.substring(html.indexOf("<\\/td>") + 6);

                    html = html.substring(html.indexOf("<td "));
                    prof = html.substring(html.indexOf(">") + 1, html.indexOf("<\\/td>"));
                    html = html.substring(html.indexOf("<\\/td>") + 6);

                    html = html.substring(html.indexOf("<td "));
                    canceledEvent = html.substring(html.indexOf(">") + 1, html.indexOf("<\\/td>"));
                    html = html.substring(html.indexOf("<\\/td>") + 6);

                    html = html.substring(html.indexOf("<td "));
                    replacement = html.substring(html.indexOf(">") + 1, html.indexOf("<\\/td>"));
                    html = html.substring(html.indexOf("<\\/td>") + 6);

                    html = html.substring(html.indexOf("<\\/tr>") + 6);

                }

                {
                    coursename = coursename.replace("\\n ", "");
                    coursename = coursename.replace(" <br \\/>", "\\n");
                    coursename = coursename.replace("<br \\/>", "");
                }

                prof = prof.replace("\\n ", "");

                canceledEvent = canceledEvent.replace("\\n ", "");
                canceledEvent = canceledEvent.replaceAll("( *+)<br \\\\/>( *+)", ";");
                replacement = replacement.replace("\\n ", "");
                replacement = replacement.replaceAll("( *+)<br \\\\/>( *+)", ";");


                values.put(String.format("%s", values.size()), String.format("%s|%s|%s|%s|%s", degree, coursename, prof, canceledEvent, replacement));

            }

            caller.parsed(values, "sheduleChanges");

        }

    }

    private void parseNews(String html) {

        html = html.substring(html.indexOf("twelve columns nop"));

        html = html.substring(html.indexOf("</thead>")+8, html.indexOf("show-for-small"));

        Map<String, String> news = new TreeMap<>();

        while (html.contains("<tr>")) {

            try{

                String imageUrl;

                imageUrl = html.substring(html.indexOf("src=\"")+5, html.indexOf("\" ", html.indexOf("src=\"")+5));

                html = html.substring(html.indexOf("</td>")+5);

                String title;

                title = html.substring(html.indexOf("<b>")+3, html.indexOf("</b>"));


                html = html.substring(html.indexOf("<br />")+6);
                html = html.substring(html.indexOf("<br />")+6);

                String normalText;

                {

                    normalText = html.substring(0, html.indexOf("<a class"));

                    normalText = normalText.replace("<p>", "");
                    normalText = normalText.replace("</p>", "\n");
                    normalText = normalText.replace("<strong>", "");
                    normalText = normalText.replace("</strong>", "");
                    normalText = normalText.replace("<ul>", "");
                    normalText = normalText.replace("</ul>", "");
                    normalText = normalText.replace("<li>", "");
                    normalText = normalText.replace("</li>", "\n");
                    normalText = normalText.replace("<br />", "");
                    normalText = normalText.replace("&nbsp;", " ");
                    normalText = normalText.replace("&amp;", "&");
                    if (normalText.contains("href=\"http"))
                        normalText = normalText.replaceAll("<a (.*)href=\"(.*)\" (.*)>(.*)</a>", "$2");
                    else normalText = normalText.replaceAll("<a (.*)href=\"(.*)\" (.*)>(.*)</a>", "$4");


                }

                html = html.substring(html.indexOf("</tr>")+5);

                html = html.substring(html.indexOf("<div id")+7);
                html = html.substring(html.indexOf("\">")+2);

                String description ;

                {

                    description = html.substring(0, html.indexOf("</div>"));

                    description = description.replace("<br>", "");
                    description = description.replace("<br />", "");
                    description = description.replace("<p>", "");
                    description = description.replace("</p>", "\n");
                    description = description.replace("</a>", "</a> ");
                    description = description.replace("<strong>", "");
                    description = description.replace("</strong>", "");
                    description = description.replace("&nbsp;", " ");
                    description = description.replace("&amp;", "&");
                    if (description.contains("href=\"http"))
                        description = description.replaceAll("<a (.*)href=\"(.*)\" (.*)>(.*)</a>", "$2");
                    else description = description.replaceAll("<a (.*)href=\"(.*)\" (.*)>(.*)</a>", "$4");
                    description = description.replaceAll("<span(.*)>(.*)</span>", "");

                }

                html = html.substring(html.indexOf("</div>"));

                html = html.substring(html.indexOf("</tr>")+5);

                while (title.endsWith("\n")) title = title.substring(0,title.length()-1);
                while (normalText.endsWith("\n")) normalText = normalText.substring(0,normalText.length()-1);
                while (description.endsWith("\n")) description = description.substring(0,description.length()-1);

                News n = new News(title, normalText, description, imageUrl);

                news.put(news.size() + "", n.getCompressed());

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        caller.parsed(news, "news");

    }

    private void parseCourses(String html) {

        Map<String, String> courses = new LinkedHashMap<>();

        html = html.substring(html.indexOf("twelve columns nop"), html.indexOf("</div>", html.indexOf("twelve columns nop")));
        html = html.substring(html.indexOf("</table>")+ 8);

        int counter = 0;

        while (html.contains("<table>")){

            int dayA = html.indexOf("\">", html.indexOf("<th ")) + 2;
            int dayB = html.indexOf("</th>", dayA);
            String dayname = html.substring(dayA, dayB);

            if (dayname.equals("weitere Veranstaltungen")) dayname = "";

            html = html.substring(html.indexOf("</thead>", dayB));

            while (html.indexOf("<tr>") < html.indexOf("</table>") && html.contains("<tr>")) {

                String[] strings = new String[6];

                html = html.substring(html.indexOf("</td>") + 5);

                for (int i = 0; i < strings.length; i++) {

                    strings[i] = html.substring(html.indexOf("\">")+2, html.indexOf("</td>"));
                    html = html.substring(html.indexOf("</td>")+5);
                }

                strings[0] = strings[0].replace(" ", "");
                strings[1] = strings[1].replace(" ", "");
                strings[2] = strings[2].replace("<br>", "");
                strings[2] = strings[2].replace("<br />", "");
                strings[2] = strings[2].replace("(", "\n(");
                strings[3] = strings[3].replace("<p>", "");
                strings[3] = strings[3].replace("</p>", "");
                strings[3] = strings[3].replace("<br />", "\n& ");

                html = html.substring(html.indexOf("</tr>")+5);

                Course c = new Course(dayname, strings[0], strings[1], strings[2], strings[3], strings[5], strings[4]);

                courses.put(counter + "",c.saveCourse());
                counter++;

            }

            html = html.substring(html.indexOf("</table>") + 8);

        }

        caller.parsed(courses, "courses");

    }

    private void parseDegrees(String html) {

        Map<String, String> degrees = new TreeMap<>(parseValues("studiengang", html));
        caller.parsed(degrees, "degrees");

    }

    private void parseSemester(String html) {

        Map<String, String> semester = parseValues("semester", html);
        caller.parsed(semester, "semester");
    }

    private Map<String, String> parseValues(String item, String download) {

        Map<String, String> values = new TreeMap<>();

        int start = download.indexOf("<select name=\"tx_stundenplan_stundenplan[" + item + "]\"");
        int end = download.indexOf("</select>", start);
        String sub_download = download.substring(start, end);

        while (sub_download.contains("<option ")) {

            sub_download = sub_download.substring(sub_download.indexOf("<option "));
            String shortVal = sub_download.substring(sub_download.indexOf("\"")+1, sub_download.indexOf("\"", sub_download.indexOf("\"")+1));
            String longVal = sub_download.substring(sub_download.indexOf(">")+1, sub_download.indexOf("</option>"));

            if (shortVal.contains(" ")) shortVal = shortVal.replace(" ", "%20");
            if (shortVal.contains("#")) shortVal = shortVal.replace("#", "%23");

            values.put(shortVal, longVal);

            sub_download = sub_download.substring(sub_download.indexOf("</option>")+9);

        }

        return values;

    }

    private void downloadAllModulbook(String[] args) {

        coursename = args[0];
        SharedPreferences sp_settings = Manager.getInstance().getSharedPreferences("settings");
        String semesteryear = sp_settings.getString("semester", "");
        String semester = semesteryear.substring(0, semesteryear.indexOf(" - "));
        semesteryear = semesteryear.substring(semesteryear.indexOf(" - ") + 3);
        semesteryear = semesteryear.replace(" ", "%20");
        String url = String.format(Manager.getInstance().getContext().getResources().getString(R.string.url_modulbook_all), sp_settings.getString("degree_short", "0"), semesteryear, semester);
        Downloader downloader = new Downloader(this, "modulbook_all");
        downloader.download(url);

    }

    private void parseModulbookAll(String html) {

        html = html.substring(html.indexOf("twelve columns nop"));
        html = html.substring(html.indexOf("/thead"));

        Map<String, String> urls = new TreeMap<>();

        while (html.contains("</tr>")) {


            String url = html.substring(html.indexOf("href=\"") + 6, html.indexOf("\">"));
            String coursename = html.substring(html.indexOf("\">")+2, html.indexOf("</a>"));

            if (coursename.contains("- -")) coursename = coursename.substring(0, coursename.indexOf("- -"));
            if (coursename.contains("(")) coursename = coursename.substring(0, coursename.indexOf('('));
            coursename = coursename.trim();
            urls.put(coursename, url);
            html = html.substring(html.indexOf("</tr>")+5);

        }

        String modulbookurl = "";

        for (String key : urls.keySet()) {
            if (coursename.contains(key)) modulbookurl = urls.get(key);

        }

        if (!Objects.requireNonNull(modulbookurl).equals("")) {

            modulbookurl = Manager.getInstance().getContext().getResources().getString(R.string.url_fh_website)+ modulbookurl;

            modulbookurl = modulbookurl.replaceAll("&amp;", "&");

            Downloader downloader = new Downloader(this, "modulbook");
            downloader.download(modulbookurl);

        }


    }

    private void parseModulbook(String html) {

        html = html.substring(html.indexOf("twelve columns nop"));
        html = html.substring(html.indexOf("<tr>"));

        Map<String, String> values = new LinkedHashMap<>();

        while (html.contains("</tr>")) {

            String key = html.substring(html.indexOf("d\">")+3, html.indexOf("</td>"));
            html = html.substring(html.indexOf("</td>")+5);
            String value;
            if (key.equals("ECTS")) value = html.substring(html.indexOf("<td>")+4, html.indexOf("</td>"));
            else value = html.substring(html.indexOf("<p>")+3, html.indexOf("</p>"));
            html = html.substring(html.indexOf("</tr>")+5);
            value = value.replace("<br />", "\n");

            values.put(key, value);

        }

        caller.parsed(values, "modulbook");

    }

    private void downloadPersonSearch(String[] args) {

        String name = args[0];
        coursename = name;
        String url = Manager.getInstance().getContext().getResources().getString(R.string.url_person);
        Downloader downloader = new Downloader(this, "person_search");
        downloader.downloadWithParameter(url, name);

    }

    private void parsePersonSearch(String html) {

        Map<String, String> values = new LinkedHashMap<>();

        if (html.contains(Manager.getInstance().getContext().getResources().getString(R.string.person_not_found))) {

            values.put("0", null);

        } else {

            while (true) {

                if (html.contains("six mobile-one columns"))
                    html = html.substring(html.indexOf("six mobile-one columns"));
                else break;

                String img_url = html.substring(html.indexOf("src=\"") + 5, html.indexOf("\" ", html.indexOf("src=\"") + 5));
                if (img_url.contains("corner.gif")) img_url = "";
                html = html.substring(html.indexOf("<h4>"));
                String profName = html.substring(html.indexOf(">") + 1, html.indexOf("<br />"));
                if (!profName.equals(coursename))  continue;

                String faculity = html.substring(html.indexOf("<p>") + 3, html.indexOf("</p>"));
                faculity = faculity.replaceAll("<br />", "\n");

                html = html.substring(html.indexOf("</p>") + 4);

                String address = html.substring(html.indexOf("<p>") + 3, html.indexOf("</p>"));
                address = address.replaceAll("<br />", "\n");
                html = html.substring(html.indexOf("</p>") + 4);

                String room = html.substring(html.indexOf("<p>") + 3, html.indexOf("<br />"));
                if (room.length()>0) if (room.charAt(0) == ' ') room = room.substring(1);
                String telephone = "";
                String fax = "";
                if (room.contains("E-Mail")) {

                    html = room;
                    room = "";

                } else {

                    telephone = html.substring(html.indexOf("\">") + 2, html.indexOf("</a>"));
                    html = html.substring(html.indexOf("</a>"));

                    fax = html.substring(html.indexOf("<br />") + 6, html.indexOf("<br />", html.indexOf("<br />") + 6));
                    if (fax.length() > 0) if (fax.charAt(0) == ' ') fax = fax.substring(1);

                }
                html = html.substring(html.indexOf("E-Mail"));

                String email = html.substring(html.indexOf("\">") + 2, html.indexOf("</a>"));
                if (email.length()>0) {

                    String temp1 = email;

                    email = email.substring(0, email.indexOf('<'));
                    temp1 = temp1.substring(temp1.indexOf("</script>") + 9);

                    email = email + "@" + temp1.substring(0, temp1.indexOf("<span"));
                    temp1 = temp1.substring(temp1.indexOf("</span>") + 7);

                    email = email + temp1;

                }

                values.put("name", profName);
                values.put("kind", faculity);
                values.put("address", address);
                values.put("room", room);
                values.put("telephone", telephone);
                values.put("fax", fax);
                values.put("email", email);
                values.put("img", img_url);

            }

        }

        caller.parsed(values, "person");

    }

    private void parseEvents(String html) {

        Map<String, String> values = new LinkedHashMap<>();

        html = html.substring(html.indexOf("twelve columns nop"));

        html = html.substring(html.indexOf("<table>"), html.indexOf("show-for-small"));

        html = html.substring(html.indexOf("</thead>")+8);

        while (html.contains("</tr>")) {

            html = html.substring(html.indexOf("<tr"));

            String date = html.substring(html.indexOf("top\">") + 5, html.indexOf("</td>"));
                        html = html.substring(html.indexOf("</td>") + 5);
                        date = date.trim();

            String time_start = html.substring(html.indexOf("top\">") + 5, html.indexOf("</td>"));
                        html = html.substring(html.indexOf("</td>") + 5);
                        time_start = time_start.trim();

            String time_end = html.substring(html.indexOf("top\">") + 5, html.indexOf("</td>"));
                        html = html.substring(html.indexOf("</td>") + 5);
                        time_end = time_end.trim();


            String place = html.substring(html.indexOf("top\">") + 5, html.indexOf("</td>"));
                        html = html.substring(html.indexOf("</td>") + 5);
                        place = place.trim();

            String room = html.substring(html.indexOf("top\">") + 5, html.indexOf("</td>"));
                        html = html.substring(html.indexOf("</td>") + 5);
                        room = room.trim();


            String title = html.substring(html.indexOf("top\">") + 5, html.indexOf("<a class"));
                        html = html.substring(html.indexOf("</td>") + 5);
                        title = title.trim();

            html = html.substring(html.indexOf("</tr>")+5);

            html = html.substring(html.indexOf("<div"));

            String temp = html.substring(0, html.indexOf("</div>"));

            //html = html.substring(html.indexOf("</td>"));

            String description = "";

            while (temp.contains("</p>")) {

                String temp2 = temp.substring(temp.indexOf("<p"), temp.indexOf("</p>"));

                temp = temp.substring(temp.indexOf("</p>")+4);

                temp2 += "\n";

                description += temp2;

            }

            {
                if (date.contains("<br /> bis <br />")) date = date.replace("<br /> bis <br />", "bis");
                if (date.contains(" <br />")) date = date.replace(" <br />", ", ");

                if (time_start.contains("<br />"))        time_start = time_start.replace(" <br />", ", ");

                if (time_end.contains("<br /><br/>")) time_end = time_end.replace("<br /><br/> ", "");
                if (time_end.contains("<br />"))        time_end = time_end.replace(" <br />", ", ");

                if (place.contains(" <br />"))        place = place.replace(" <br />", ", ");

                if (title.contains("<br />"))        title = title.replace("<br />", "");

                if (room.contains(" <br />")) room = room.replace(" <br />", ", ");

            } //Cleanup

            String event_val = String.format("%s|%s|%s|%s|%s|%s|%s", date, time_start, time_end, place, room, title, description);

            values.put(""+ values.size(), event_val);

            html = html.substring(html.indexOf("</tr>", html.indexOf("<div class=\"clear\"></div>"))+5);


        }

        caller.parsed(values, "events");

    }

}
