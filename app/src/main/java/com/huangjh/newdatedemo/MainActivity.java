package com.huangjh.newdatedemo;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{
    private Calendar mCalendar = Calendar.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CalendarView calendarView = (CalendarView) findViewById(R.id.calendar);
        //设置标注日期
        List<Date> markDates = new ArrayList<Date>();
        markDates.add(new Date());
        calendarView.setMarkDates(markDates);

        //设置点击操作
        calendarView.setOnCalendarViewListener(new CalendarView.OnCalendarViewListener() {

            @Override
            public void onCalendarItemClick(CalendarView view, Date date) {
                // TODO Auto-generated method stub
                if (CalendarUtil.compare(date,mCalendar.getTime())){
                    final SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
                    Toast.makeText(MainActivity.this, format.format(date), Toast.LENGTH_SHORT).show();
                }

            }

        });
    }
}
