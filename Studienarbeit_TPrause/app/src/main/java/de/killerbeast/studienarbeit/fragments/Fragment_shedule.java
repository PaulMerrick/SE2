package de.killerbeast.studienarbeit.fragments;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.killerbeast.studienarbeit.activities.Activity_showCourse;
import de.killerbeast.studienarbeit.Course;
import de.killerbeast.studienarbeit.Manager;
import de.killerbeast.studienarbeit.Parser;
import de.killerbeast.studienarbeit.R;
import de.killerbeast.studienarbeit.SocketListener;
import de.killerbeast.studienarbeit.interfaces.Interface_Fragmenthandler;
import de.killerbeast.studienarbeit.interfaces.Interface_Parser;
import de.killerbeast.studienarbeit.interfaces.Interface_SocketListener;

import static android.view.View.GONE;

public class Fragment_shedule extends Fragment implements Interface_Parser, Interface_SocketListener {

    private View view;
    private Interface_Fragmenthandler parent;
    private SharedPreferences sp_courses;
    private AlertDialog alertDialog;
    private boolean sheduleScrolling = false;
    private CardView currentCourse;
    private SocketListener socketListener;
    private boolean buildFinished = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shedule, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

    }

    public static Fragment_shedule newInstance(Interface_Fragmenthandler parent) {

        Fragment_shedule fragment = new Fragment_shedule();
        fragment.setParent(parent);
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;

    }

    private void setParent(Interface_Fragmenthandler parent) {

        this.parent = parent;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
        super.onViewCreated(view,savedInstance);

        this.view = view;
        sp_courses = Manager.getInstance().getSharedPreferences("courses");
        if (Manager.getInstance().getSharedPreferences("settings").getBoolean("downloadShedule", true)) downloadCourses();
        else showCourses();

    }

    @SuppressLint("InflateParams")
    private void downloadCourses(){

        Context cw = new ContextThemeWrapper(Manager.getInstance().getContext(), R.style.dialogMenu);
        AlertDialog.Builder downloading = new AlertDialog.Builder(cw);
        LayoutInflater inflater = getLayoutInflater();
        downloading.setView(inflater.inflate(R.layout.dialog_downloading, null));
        alertDialog = downloading.create();
        alertDialog.show();

        Parser parser = new Parser(this);
        parser.parse("courses");

    }

    public void parsed(Map<String, String> values, String mode) {

        if (mode.equals("sheduleChanges")) {

            applySheduleChanges(values);

        } else {

            Map<String, ?> oldCoursesMap = new LinkedHashMap<>(sp_courses.getAll());

            ArrayList<Course> oldCourses = new ArrayList<>();

            for (Object s : oldCoursesMap.values())
                oldCourses.add(new Course((String) s));

            Map<String, String> temp = new LinkedHashMap<>();

            for (Course c : oldCourses) {

                if (!c.isShown()) {

                    String edited = c.saveCourse().replace("|false", "|true");
                    for (String string : values.values()) {

                        if (string.equals(edited)) {

                            String key = "";

                            for (String string2 : values.keySet()) {

                                if (Objects.equals(values.get(string2), edited)) {

                                    key = string2;
                                    break;

                                }

                            }

                            temp.put(key, c.saveCourse());

                        }

                    }

                }

            }

            for (String key : temp.keySet()) values.remove(key);
            values.putAll(temp);

            SharedPreferences.Editor editor = sp_courses.edit();
            editor.clear();

            for (String s : values.keySet()) editor.putString(s, values.get(s));

            editor.apply();

            alertDialog.dismiss();

            Manager.getInstance().getSharedPreferences("settings").edit().putBoolean("downloadShedule", false).apply();

            parent.updateFragment(Manager.STRING_FRAGMENT_SHEDULE);

        }

    }

    private void applySheduleChanges(Map<String, String> values) {

        //Log.wtf("SheduleChange::", values.values().toString());


        for (String s : values.values()) {

            String[] info = s.split("\\|"); //0 = degree, 1 = coursename,  2 = prof, 3 = canceled, 4 = replace;

            String date = info[3].split(";")[0];
            String startTime = info[3].split(";")[1];

            String newdate = "";
            String newroom = "";
            String newstartTime = "";

            if (!info[4].equals(";")) {

                newdate = info[4].split(";")[0];
                newstartTime = info[4].split(";")[1];
                newroom = info[4].split(";")[2];

            }
            // First convert to Date. This is one of the many ways.

            try {
                Date temp = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN).parse(date);
                String dayOfWeek = new SimpleDateFormat("EEEE", Locale.GERMANY).format(Objects.requireNonNull(temp));
                //Log.wtf("day of changed Shedule:", dayOfWeek);

                while (!buildFinished){
                    try{
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LinearLayout ll_shedule = view.findViewById(R.id.ll_shedule);

                LinearLayout cv_conainer = null;

                for (int i = 0; i < ll_shedule.getChildCount(); i++) {

                    LinearLayout ll = (LinearLayout)ll_shedule.getChildAt(i);
                    TextView tv = (TextView)ll.getChildAt(0);
                    if (tv.getText().equals(dayOfWeek)) {

                        ScrollView sv = (ScrollView) ll.getChildAt(1);
                        cv_conainer = (LinearLayout)sv.getChildAt(0);

                    }

                }

                if (cv_conainer != null) {

                    for (int j = 0; j < cv_conainer.getChildCount(); j++){

                        View v = cv_conainer.getChildAt(j);

                        if (v instanceof CardView) {

                            CardView cv = v.findViewById(R.id.cv_cardview_shedule);
                            CardView temp1 = (CardView)cv.getChildAt(0);
                            LinearLayout cv_ll = (LinearLayout) temp1.getChildAt(0);

                            String cv_prof = ((TextView)cv.findViewById(R.id.tv_cardview_shedule_prof)).getText().toString();
                            String cv_time = ((TextView)cv.findViewById(R.id.tv_cardview_shedule_time)).getText().toString().split(" - ")[0];

                            cv_prof = cv_prof.replace("\n", "");
                            info[2] = info[2].replace("\\n", "");

                            boolean test = cv_prof.contains(info[2]) && startTime.contains(cv_time);
                            if (test) {



                                String changed = "";
                                TextView tv_time = cv_ll.findViewById(R.id.tv_cardview_shedule_time);
                                TextView tv_room_kind = cv_ll.findViewById(R.id.tv_cardview_shedule_room_kind);
                                String[] roominfo = tv_room_kind.getText().toString().split(" \\| ");
                                TextView tv_changes = cv_ll.findViewById(R.id.tv_cardview_shedule_changes);

                                String time = tv_time.getText().toString().split(" - ")[0];
                                if (!newstartTime.contains(time) && !info[4].equals(";")) {

                                    changed += String.format("%s%s\n", Manager.getInstance().getContext().getResources().getString(R.string.shedule_changed_time), newstartTime);
                                    tv_time.setTextColor(ContextCompat.getColor(Manager.getInstance().getContext(), R.color.cardviewChangeText));
                                    tv_time.setPaintFlags(tv_time.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                                }
                                if (!roominfo[0].equals(newroom) && !info[4].equals(";")) {

                                    changed += String.format("%s | %s\n", newroom, roominfo[1]);
                                    tv_room_kind.setTextColor(ContextCompat.getColor(Manager.getInstance().getContext(), R.color.cardviewChangeText));
                                    tv_room_kind.setPaintFlags(tv_room_kind.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                                }

                                if (!date.equals(newdate) && !info[4].equals(";")) {

                                    changed += newdate + "\n";

                                }

                                if (!changed.equals("") || info[4].equals(";")) {

                                    if (info[4].equals(";")) changed = Manager.getInstance().getContext().getResources().getString(R.string.shedule_changed_canceld);
                                    if (changed.endsWith("\n")) changed = changed.substring(0, changed.length()-1);
                                    tv_changes.setText(changed);
                                    tv_changes.setVisibility(View.VISIBLE);


                                }


                            }

                        }

                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

    }

    private ArrayList<Course> createCourses() {

        ArrayList<Course> courses = new ArrayList<>();

        int counter = 0;

        while (!sp_courses.getString("" + counter, "").equals("")) courses.add(new Course(sp_courses.getString(""+ counter++, "")));

        return courses;

    }

    @SuppressLint("ClickableViewAccessibility")
    private void showCourses(){

        checkIfSheduleChanged();

        int deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

        HorizontalScrollView sv = view.findViewById(R.id.sv_shedule); //Container linear layout
        sv.setSmoothScrollingEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sv.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {

                int width = sv.getChildAt(0).getWidth();

                int days = width / deviceWidth;
                //Log.wtf("scroll ",   "o");

                if (!sheduleScrolling) {

                    //Log.wtf("Scrolled to ", scrollX + ", " + scrollY);
                    // Log.wtf("DeviceWidth", deviceWidth * 0.5 + "");



                    for (int i = 0; i < days; i++) {

                        int under = (int)((deviceWidth*(i-1)) + (deviceWidth*0.5));
                        int above = (int)((deviceWidth*i) + (deviceWidth*0.5));

                        if (above > scrollX && under < scrollX) {

                            if (Manager.getInstance().getSharedPreferences("settings").getBoolean("animations", true)) {

                                ObjectAnimator animator = ObjectAnimator.ofInt(sv, "scrollX", deviceWidth * i);

                                animator.setDuration(500);
                                animator.start();
                                animator.addUpdateListener(animation -> {

                                    int val = (int) animation.getAnimatedValue();
                                    sheduleScrolling = true;
                                    try {

                                        requireActivity().runOnUiThread(() -> sv.setScrollX(val));

                                    } catch (Exception e) {
                                        //Activity was closed while Animator was working
                                        animator.end();
                                        e.printStackTrace();

                                    }
                                    sheduleScrolling = false;

                                });

                            } else {

                                sheduleScrolling = true;
                                int finalI = i;
                                try {
                                    requireActivity().runOnUiThread(() -> sv.setScrollX(deviceWidth * finalI));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                sheduleScrolling = false;

                            }

                            //Log.wtf("scroll to", i + "");
                            break;
                        }

                    }

                }

            });
        } // magnetic scrolling

        sv.setOnTouchListener((v, event) -> {

            String e = event.toString();

            if (e.contains("ACTION_UP")) {

                sheduleScrolling = false;
                sv.scrollBy(+1,0);

            } else sheduleScrolling = true;

            return false;

        }); //helper for magnetic scrolling

        LinearLayout ll_main = view.findViewById(R.id.ll_shedule); //MainContainer for all days

        ArrayList<Course> courses = createCourses();

        Map<String, LinearLayout> days = new LinkedHashMap<>();

        for (Course c : courses) {

            if (!days.containsKey(c.getDay())) {

                LinearLayout ll = new LinearLayout(Manager.getInstance().getContext());
                ll.setGravity(Gravity.CENTER_HORIZONTAL);
                TextView tv = new TextView(Manager.getInstance().getContext());
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tv.setText(c.getDay());
                tv.setTextColor(ContextCompat.getColor(Manager.getInstance().getContext(), R.color.normalText));
                tv.setTextSize(26);
                tv.setPadding(0,0,0,20);

                ScrollView sv1 = new ScrollView(Manager.getInstance().getContext());
                sv1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                LinearLayout ll1 = new LinearLayout(Manager.getInstance().getContext());
                ll1.setOrientation(LinearLayout.VERTICAL);
                ll1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ll1.setGravity(Gravity.CENTER_HORIZONTAL);
                sv1.addView(ll1);
                ll.addView(tv);
                ll.addView(sv1);
                ll.setOrientation(LinearLayout.VERTICAL);
                ll.setLayoutParams(new LinearLayout.LayoutParams(deviceWidth, ViewGroup.LayoutParams.MATCH_PARENT));
                days.put(c.getDay(), ll);

            }

        }

        String dayname = "";

        for (Course c : courses) {

                if (c.isShown()) {

                    CardView cv = new CardView(Manager.getInstance().getContext());
                    CardView.inflate(Manager.getInstance().getContext(), R.layout.cardview_shedule, cv);
                    cv.setLayoutParams(new CardView.LayoutParams( (int)(deviceWidth - (deviceWidth*0.1)), CardView.LayoutParams.WRAP_CONTENT));
                    ((TextView)cv.findViewById(R.id.tv_cardview_shedule_name)).setText(c.getCourseName());
                    ((TextView)cv.findViewById(R.id.tv_cardview_shedule_time)).setText(c.getTimeFormatted());
                    ((TextView)cv.findViewById(R.id.tv_cardview_shedule_prof)).setText(String.format("%s %s", Manager.getInstance().getContext().getResources().getString(R.string.with),c.getProfessor()));
                    ((TextView)cv.findViewById(R.id.tv_cardview_shedule_room_kind)).setText(String.format("%s | %s", c.getRoomNumber(), c.getKind()));

                    cv.setRight(40);
                    cv.setCardElevation(20);
                    cv.setRadius(20);


                    Calendar calendar = Calendar.getInstance();
                    int daycount = calendar.get(Calendar.DAY_OF_WEEK);

                    switch (daycount) {

                        case Calendar.MONDAY:
                            dayname = "Montag";
                            break;
                        case Calendar.TUESDAY:
                            dayname = "Dienstag";
                            break;
                        case Calendar.WEDNESDAY:
                            dayname = "Mittwoch";
                            break;
                        case Calendar.THURSDAY:
                            dayname = "Donnerstag";
                            break;
                        case Calendar.FRIDAY:
                            dayname = "Freitag";
                            break;
                        case Calendar.SATURDAY:
                            dayname = "Samstag";
                            break;
                        case Calendar.SUNDAY:
                            dayname = "Sonntag";
                            break;

                    }

                    if (dayname.equals(c.getDay())) {

                        try {

                            Date time1 = new SimpleDateFormat("HH:mm", Locale.GERMAN).parse(c.getStartTime());
                            Calendar calendar1 = Calendar.getInstance();
                            calendar1.setTime(Objects.requireNonNull(time1));
                            calendar1.add(Calendar.DATE, 1);

                            Date time2 = new SimpleDateFormat("HH:mm", Locale.GERMAN).parse(c.getEndTime());
                            Calendar calendar2 = Calendar.getInstance();
                            calendar2.setTime(Objects.requireNonNull(time2));
                            calendar2.add(Calendar.DATE, 1);
                            String date = Calendar.getInstance().getTime().toString();
                            date = date.replaceAll("(.*) (.*) (.*) (.*) (.*) (.*)", "$4");
                            Date time3 = new SimpleDateFormat("HH:mm:ss", Locale.GERMAN).parse(date);

                            Calendar calendar3 = Calendar.getInstance();
                            calendar3.setTime(Objects.requireNonNull(time3));
                            calendar3.add(Calendar.DATE, 1);

                            Date start = calendar1.getTime();
                            Date end = calendar2.getTime();
                            Date current = calendar3.getTime();

                            if (current.after(start) && current.before(end)) {
                                ((CardView)cv.findViewById(R.id.cv_cardview_shedule)).setCardBackgroundColor(ContextCompat.getColor(Manager.getInstance().getContext(), R.color.cardBackgroundHighlited));
                                checkIfChatIsActive(cv, c);

                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }

                    cv.setOnClickListener(v -> startActivityForResult(new Intent().setClass(Manager.getInstance().getContext(), Activity_showCourse.class).putExtra("course", c.saveCourse()), 5));

                    SharedPreferences sp_settings = Manager.getInstance().getSharedPreferences("settings");
                    if (!sp_settings.getString("courseFilters", "").equals("")) {

                        String[] filter = sp_settings.getString("courseFilters", "").split("\\|");

                        switch (filter[0]) {

                            case "coursename":
                                if (!c.getCourseName().equals(filter[1])) cv.setVisibility(GONE);
                                break;
                            case "professor":
                                if (!c.getProfessor().equals(filter[1])) cv.setVisibility(GONE);
                                break;
                            case "starttime":
                                if (!c.getStartTime().equals(filter[1])) cv.setVisibility(GONE);
                                break;
                            case "endtime":
                                if (!c.getEndTime().equals(filter[1])) cv.setVisibility(GONE);
                                break;
                            case "room":
                                if (!c.getRoomNumber().equals(filter[1])) cv.setVisibility(GONE);
                                break;
                            case "kind":
                                if (!c.getKind().equals(filter[1])) cv.setVisibility(GONE);
                                break;

                        }

                    }

                    if (cv.getVisibility() == View.VISIBLE) {

                        Space sp = new Space(Manager.getInstance().getContext());
                        sp.setMinimumHeight(60);

                        LinearLayout temp = (LinearLayout) ((ScrollView)Objects.requireNonNull(days.get(c.getDay())).getChildAt(1)).getChildAt(0);

                        temp.addView(cv);
                        temp.addView(sp);

                    }

                }

            }

        for (LinearLayout ll : days.values()) {

            ScrollView temp1 = (ScrollView) ll.getChildAt(1);
            LinearLayout temp2 = (LinearLayout) temp1.getChildAt(0);

            if (temp2.getChildCount() > 0)
                ll_main.addView(ll);

        }


        int scrollposition = 0;

        for (LinearLayout ll : days.values()) {

            if (((TextView)ll.getChildAt(0)).getText().toString().equals(dayname)) {

                scrollposition *= -1;
                break;

            } else scrollposition--;

        }
            //Scroll

        if (Manager.getInstance().getSharedPreferences("settings").getBoolean("animations", true)) {

           ObjectAnimator animator = ObjectAnimator.ofInt(sv, "scrollX", (deviceWidth * scrollposition));

           animator.setDuration(500);
           animator.start();
           animator.addUpdateListener(animation -> {

               int val = (int) animation.getAnimatedValue();
               sheduleScrolling = true;
               Manager.getInstance().runOnUiThread(() -> sv.setScrollX(val));
               sheduleScrolling = false;

           });

        } else {

            sheduleScrolling = true;
            int finalScrollposition = scrollposition;
            requireActivity().runOnUiThread(() -> sv.setScrollX(deviceWidth * finalScrollposition));
            sheduleScrolling = false;

        }

        buildFinished = true;

    }

    private void checkIfSheduleChanged() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date date = calendar.getTime();
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
        String firstDayOfWeek = f.format(date);

        Parser parser = new Parser(this);
        parser.parse("sheduleChanges", firstDayOfWeek);

    }

    private void checkIfChatIsActive(CardView cv, Course c) {


        if (Manager.getInstance().isInternetAvaiable())

        try {

            currentCourse = cv;

            Socket socket = new Socket(Manager.STRING_SERVER_IP, Manager.INTEGER_SERVER_PORT);

            socketListener = new SocketListener(this, socket);

            new Thread(socketListener).start();

            String checkIfChatIsActive = String.format("%s|//isChatActive?", c.getCourseIdentification());
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bw.write(checkIfChatIsActive + "\n");
            bw.flush();
            //Log.wtf("ask", "if chat avaiable");

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public void received(String received) {

       // Log.wtf("is chat active?", received);
        if (received.contains("Chat is active")) {

            requireActivity().runOnUiThread(()->currentCourse.findViewById(R.id.tv_cardview_shedule_chatActive).setVisibility(View.VISIBLE));

        }
        socketListener.disconnect();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Log.wtf("request", ""+ requestCode);
        if (resultCode == Manager.REQUEST_UPDATE_SHEDULE_FRAGMENT) {
            parent.updateFragment(Manager.STRING_FRAGMENT_SHEDULE);
            parent.updateContext();
        }

    }

}
