package com.huangjh.newdatedemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by carme on 2017/7/3.
 */

public class CalendarView extends LinearLayout implements View.OnTouchListener,
        Animation.AnimationListener, GestureDetector.OnGestureListener {
    private String TAG = "------TAG------";

    /**
     * 点击日历
     */
    public interface OnCalendarViewListener {
        void onCalendarItemClick(CalendarView view, Date date);
    }

    /**
     * 顶部控件所占高度
     */
    private final static int TOP_HEIGHT = 40;
    /**
     * 日历item中默认id从0xff0000开始
     */
    private final static int DEFAULT_ID = 0xff0000;

    // 判断手势用
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    // 屏幕宽度和高度
    private int screenWidth;
    private int widgetHeight;
    // 动画
    private Animation slideBottomIn;
    private Animation slideBottomOut;
    private Animation slideTopIn;
    private Animation slideTopOut;
    private ViewFlipper viewFlipper;
    private GestureDetector mGesture = null;

    /**
     * 上一月
     */
    private GridView gView1;
    /**
     * 当月
     */
    private GridView gView2;
    /**
     * 下一月
     */
    private GridView gView3;

    boolean bIsSelection = false;// 是否是选择事件发生
    private Calendar calStartDate = Calendar.getInstance();// 当前显示的日历
    private Calendar calSelected = Calendar.getInstance(); // 选择的日历
    private Calendar calNowDate = Calendar.getInstance();
    private CalendarGridViewAdapter gAdapter;
    private CalendarGridViewAdapter gAdapter1;
    private CalendarGridViewAdapter gAdapter3;

    private LinearLayout mMainLayout;
    private TextView mTitle; // 显示年月
    private TextView bottomTv;
    private int iMonthViewCurrentMonth = 0; // 当前视图月
    private int iMonthViewCurrentYear = 0; // 当前视图年

    private static final int caltitleLayoutID = 66; // title布局ID
    private static final int calLayoutID = 55; // 日历布局ID
    private Context mContext;

    /**
     * 标注日期
     */
    private final List<Date> markDates;

    private OnCalendarViewListener mListener;

    public CalendarView(Context context) {
        this(context, null);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        markDates = new ArrayList<Date>();
        init();
    }

    // 初始化相关工作
    protected void init() {
        // 得到屏幕的宽度
        screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

        // 手势操作
        mGesture = new GestureDetector(mContext, this);

        // 获取到当前日期
        UpdateStartDateForMonth();
        // 绘制界面
        setOrientation(LinearLayout.HORIZONTAL);
        mMainLayout = new LinearLayout(mContext);
        LinearLayout.LayoutParams main_params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mMainLayout.setLayoutParams(main_params);
        mMainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mMainLayout.setOrientation(LinearLayout.VERTICAL);
        addView(mMainLayout);

        //  顶部显示星期
        generateWeekGirdView();

        // 中间控件
        generateTopView();


        // 底部显示日历
        viewFlipper = new ViewFlipper(mContext);
        RelativeLayout.LayoutParams fliper_params = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        fliper_params.addRule(RelativeLayout.BELOW, caltitleLayoutID);
        mMainLayout.addView(viewFlipper, fliper_params);

        widgetHeight = viewFlipper.getResources().getDisplayMetrics().heightPixels;
        // 最下方的一条线条
        LinearLayout br = new LinearLayout(mContext);
        br.setBackgroundColor(Color.argb(0xff, 0xe3, 0xee, 0xf4));
        LinearLayout.LayoutParams params_br = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 3);
        mMainLayout.addView(br, params_br);

        generateBottomView();
        generateClaendarGirdView();
        // 滑动的动画
        slideTopIn = new TranslateAnimation(0, 0, widgetHeight, 0);
        slideTopIn.setDuration(400);
        slideTopIn.setAnimationListener(this);
        slideTopOut = new TranslateAnimation(0, 0, 0, -widgetHeight);
        slideTopOut.setDuration(400);
        slideTopOut.setAnimationListener(this);
        slideBottomIn = new TranslateAnimation(0, 0, -widgetHeight, 0);
        slideBottomIn.setDuration(400);
        slideBottomIn.setAnimationListener(this);
        slideBottomOut = new TranslateAnimation(0, 0, 0, widgetHeight);
        slideBottomOut.setDuration(400);
        slideBottomOut.setAnimationListener(this);

    }

    /**
     * 生成顶部控件
     */
    @SuppressWarnings("deprecation")
    private void generateTopView() {
        // 顶部显示上一个下一个，以及当前年月
        RelativeLayout top = new RelativeLayout(mContext);
        top.setBackgroundColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams top_params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                ViewUtil.dip2px(mContext, TOP_HEIGHT));
        top.setLayoutParams(top_params);
        mMainLayout.addView(top);
        mTitle = new TextView(mContext);
        android.widget.RelativeLayout.LayoutParams title_params = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
        mTitle.setLayoutParams(title_params);
        mTitle.setTextColor(Color.BLACK);
        mTitle.setTextSize(18);
        mTitle.setFocusableInTouchMode(true);
        mTitle.setMarqueeRepeatLimit(-1);
        mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        mTitle.setSingleLine(true);
        mTitle.setGravity(Gravity.CENTER);
        mTitle.setHorizontallyScrolling(true);
        mTitle.setText("2014年9月");
        top.addView(mTitle);

    }

    @SuppressWarnings("deprecation")
    private void generateBottomView() {
        RelativeLayout bottom = new RelativeLayout(mContext);
        bottom.setBackgroundColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams bottom_params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                ViewUtil.dip2px(mContext, TOP_HEIGHT));
        bottom.setLayoutParams(bottom_params);
        mMainLayout.addView(bottom);
        bottomTv = new TextView(mContext);
        android.widget.RelativeLayout.LayoutParams title_params = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
        bottomTv.setLayoutParams(title_params);
        bottomTv.setTextColor(Color.BLACK);
        bottomTv.setTextSize(18);
        bottomTv.setFocusableInTouchMode(true);
        bottomTv.setMarqueeRepeatLimit(-1);
        bottomTv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        bottomTv.setSingleLine(true);
        bottomTv.setGravity(Gravity.CENTER);
        bottomTv.setHorizontallyScrolling(true);
        bottomTv.setText("2014年9月");
        bottom.addView(bottomTv);
    }

    /**
     * 生成中间显示week
     */
    private void generateWeekGirdView() {
        GridView gridView = new GridView(mContext);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        gridView.setLayoutParams(params);
        gridView.setNumColumns(7);// 设置每行列数
        gridView.setGravity(Gravity.CENTER_VERTICAL);// 位置居中
        gridView.setVerticalSpacing(1);// 垂直间隔
        gridView.setHorizontalSpacing(1);// 水平间隔
        gridView.setBackgroundColor(getResources().getColor(R.color.wall_gray));

        int i = screenWidth / 7;
        int j = screenWidth - (i * 7);
        int x = j / 2;
        gridView.setPadding(x, 0, 0, 0);// 居中
        WeekGridAdapter weekAdapter = new WeekGridAdapter(mContext);
        gridView.setAdapter(weekAdapter);// 设置菜单Adapter
        mMainLayout.addView(gridView);
    }

    /**
     * 生成底部日历
     */
    private void generateClaendarGirdView() {
        Calendar tempSelected1 = Calendar.getInstance(); // 临时
        Calendar tempSelected2 = Calendar.getInstance(); // 临时
        Calendar tempSelected3 = Calendar.getInstance(); // 临时
        tempSelected1.setTime(calStartDate.getTime());
        tempSelected2.setTime(calStartDate.getTime());
        tempSelected3.setTime(calStartDate.getTime());
//
        gView1 = new CalendarGridView(mContext);
        tempSelected1.add(Calendar.MONTH, -1);
        gAdapter1 = new CalendarGridViewAdapter(mContext, tempSelected1,
                markDates);
        gView1.setAdapter(gAdapter1);// 设置菜单Adapter
        gView1.setId(calLayoutID);

        gView2 = new CalendarGridView(mContext);
        tempSelected2.add(Calendar.MONTH,1);
        gAdapter = new CalendarGridViewAdapter(mContext, tempSelected2,
                markDates);
        gView2.setAdapter(gAdapter);// 设置菜单Adapter
        gView2.setId(calLayoutID);

        gView3 = new CalendarGridView(mContext);
        tempSelected3.add(Calendar.MONTH, 1);
        gAdapter3 = new CalendarGridViewAdapter(mContext, tempSelected3,
                markDates);
        gView3.setAdapter(gAdapter3);// 设置菜单Adapter
        gView3.setId(calLayoutID);

        gView2.setOnTouchListener(this);
        gView1.setOnTouchListener(this);
        gView3.setOnTouchListener(this);

        if (viewFlipper.getChildCount() != 0) {
            viewFlipper.removeAllViews();
        }

        viewFlipper.addView(gView2);
//        viewFlipper.addView(gView3);
//        viewFlipper.addView(gView1);
        int tempMonthTop = calStartDate.get(Calendar.MONTH) + 2 ;
        if (tempMonthTop > 12 ){
            tempMonthTop = tempMonthTop - 12 ;
        }
        String title = calStartDate.get(Calendar.YEAR)
                + "-"
                + NumberHelper.LeftPad_Tow_Zero(tempMonthTop);
        mTitle.setText(title);

        int tempMonthBtm = calStartDate.get(Calendar.MONTH) + 3;
        int tempYear = calStartDate.get(Calendar.YEAR);
        if (tempMonthBtm > 12) {
            tempMonthBtm = tempMonthBtm - 12;
            tempYear = tempYear + 1;
        }
        String bottomString = tempYear
                + "-"
                + NumberHelper.LeftPad_Tow_Zero(tempMonthBtm);
        bottomTv.setText(bottomString);
//        }

    }

    // 上一个月
    private void setPrevViewItem() {
        iMonthViewCurrentMonth--;// 当前选择月--
        // 如果当前月为负数的话显示上一年
        if (iMonthViewCurrentMonth == 0) {
            iMonthViewCurrentMonth = 11;
            iMonthViewCurrentYear--;
        }
        calStartDate.set(Calendar.DAY_OF_MONTH, 1); // 设置日为当月1日
        calStartDate.set(Calendar.MONTH, iMonthViewCurrentMonth); // 设置月
        calStartDate.set(Calendar.YEAR, iMonthViewCurrentYear); // 设置年
    }

    // 下一个月
    private void setNextViewItem() {
        iMonthViewCurrentMonth++;
        if (iMonthViewCurrentMonth == 12) {
            iMonthViewCurrentMonth = 0;
            iMonthViewCurrentYear++;
        }
        calStartDate.set(Calendar.DAY_OF_MONTH, 1);
        calStartDate.set(Calendar.MONTH, iMonthViewCurrentMonth);
        calStartDate.set(Calendar.YEAR, iMonthViewCurrentYear);
    }

    // 根据改变的日期更新日历
    // 填充日历控件用
    private void UpdateStartDateForMonth() {
        calStartDate.set(Calendar.DATE, 1); // 设置成当月第一天
        iMonthViewCurrentMonth = calStartDate.get(Calendar.MONTH);// 得到当前日历显示的月
        Log.i(TAG, "UpdateStartDateForMonth: " + iMonthViewCurrentMonth);
        iMonthViewCurrentYear = calStartDate.get(Calendar.YEAR);// 得到当前日历显示的年

        // 星期一是2 星期天是1 填充剩余天数
        int iDay = 0;
        int iFirstDayOfWeek = Calendar.MONDAY;
        int iStartDay = iFirstDayOfWeek;
        if (iStartDay == Calendar.MONDAY) {
            iDay = calStartDate.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
            if (iDay < 0)
                iDay = 6;
        }
        if (iStartDay == Calendar.SUNDAY) {
            iDay = calStartDate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
            if (iDay < 0)
                iDay = 6;
        }
        calStartDate.add(Calendar.DAY_OF_WEEK, -iDay);
    }

    /**
     * 设置标注的日期
     *
     * @param markDates
     */
    public void setMarkDates(List<Date> markDates) {
        this.markDates.clear();
        this.markDates.addAll(markDates);
        gAdapter.notifyDataSetChanged();
        gAdapter1.notifyDataSetChanged();
        gAdapter3.notifyDataSetChanged();
    }

    /**
     * 设置点击日历监听
     *
     * @param listener
     */
    public void setOnCalendarViewListener(OnCalendarViewListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGesture.onTouchEvent(event);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // TODO Auto-generated method stub
        try {
            if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
                return false;
            if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                viewFlipper.setInAnimation(slideBottomIn);
                viewFlipper.setOutAnimation(slideBottomOut);
                viewFlipper.showNext();
                setNextViewItem();
                return true;
            } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                Log.i(TAG, "onFling: " + iMonthViewCurrentMonth);
                Log.i(TAG, "onFling: " + ((calNowDate.get(Calendar.MONTH))));
                if (iMonthViewCurrentMonth == ((calNowDate.get(Calendar.MONTH))-1)) {
                    return false;
                } else {
                    viewFlipper.setInAnimation(slideTopIn);
                    viewFlipper.setOutAnimation(slideTopOut);
                    viewFlipper.showPrevious();
                    setPrevViewItem();
                    return true;
                }

            }
        } catch (Exception e) {
            // nothing
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        // 得到当前选中的是第几个单元格
        int pos = gView2.pointToPosition((int) e.getX(), (int) e.getY());
        LinearLayout txtDay = (LinearLayout) gView2.findViewById(pos
                + DEFAULT_ID);
        if (txtDay != null) {
            if (txtDay.getTag() != null) {
                Date date = (Date) txtDay.getTag();
                calSelected.setTime(date);

                gAdapter.setSelectedDate(calSelected);
                gAdapter.notifyDataSetChanged();

                gAdapter1.setSelectedDate(calSelected);
                gAdapter1.notifyDataSetChanged();

                gAdapter3.setSelectedDate(calSelected);
                gAdapter3.notifyDataSetChanged();
                if (mListener != null)
                    mListener.onCalendarItemClick(this, date);
            }
        }
        return false;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        // TODO Auto-generated method stub
        generateClaendarGirdView();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAnimationStart(Animation animation) {
        // TODO Auto-generated method stub

    }
}


/**
 * 显示week的布局adapter
 */
class WeekGridAdapter extends BaseAdapter {

    final String[] titles = new String[]{"日", "一", "二", "三", "四", "五", "六"};
    private Context mContext;

    public WeekGridAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object getItem(int position) {
        return titles[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView week = new TextView(mContext);
        android.view.ViewGroup.LayoutParams week_params = new AbsListView.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        week.setLayoutParams(week_params);
        week.setPadding(0, 0, 0, 0);
        week.setGravity(Gravity.CENTER);
        week.setFocusable(false);
        week.setBackgroundColor(Color.TRANSPARENT);

        week.setText(getItem(position) + "");
        return week;
    }
}

/**
 * 显示日期的adapter
 */
class CalendarGridViewAdapter extends BaseAdapter {

    /**
     * 日历item中默认id从0xff0000开始
     */
    private final static int DEFAULT_ID = 0xff0000;
    private Calendar calStartDate = Calendar.getInstance();// 当前显示的日历
    private Calendar calSelected = Calendar.getInstance(); // 选择的日历

    /**
     * 标注的日期
     */
    private List<Date> markDates;

    private Context mContext;

    private Calendar calToday = Calendar.getInstance(); // 今日
    private ArrayList<java.util.Date> titles;

    private ArrayList<java.util.Date> getDates() {

        UpdateStartDateForMonth();

        ArrayList<java.util.Date> alArrayList = new ArrayList<java.util.Date>();

        for (int i = 1; i <= 42; i++) {

            alArrayList.add(calStartDate.getTime());
            calStartDate.add(Calendar.DAY_OF_MONTH, 1);
        }

        return alArrayList;
    }

    // construct
    public CalendarGridViewAdapter(Context context, Calendar cal, List<Date> dates) {
        calStartDate = cal;
        this.mContext = context;
        titles = getDates();
        this.markDates = dates;
    }

    public CalendarGridViewAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return titles.size();
    }

    @Override
    public Object getItem(int position) {
        return titles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("deprecation")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 整个Item
        LinearLayout itemLayout = new LinearLayout(mContext);
        itemLayout.setId(position + DEFAULT_ID);
        itemLayout.setGravity(Gravity.CENTER);
        itemLayout.setOrientation(1);
        itemLayout.setBackgroundColor(Color.WHITE);
        itemLayout.setPadding(0, 10, 0, 10);

        Date myDate = (Date) getItem(position);
        itemLayout.setTag(myDate);
        Calendar calCalendar = Calendar.getInstance();
        calCalendar.setTime(myDate);

        // 显示日期day
        TextView textDay = new TextView(mContext);// 日期
        LinearLayout.LayoutParams text_params = new LinearLayout.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
        textDay.setGravity(Gravity.CENTER_HORIZONTAL);
        int day = myDate.getDate(); // 日期
        textDay.setText(String.valueOf(day));
        textDay.setId(position + DEFAULT_ID);
        itemLayout.addView(textDay, text_params);


//        // 如果是当前日期则显示不同颜色
//        if (equalsDate(calToday.getTime(), myDate)) {
//            itemLayout.setBackgroundColor(Color.argb(0xff, 0x6d, 0xd6, 0x97));
//        }

        // 这里用于比对是不是比当前日期小，如果比当前日期小则显示浅灰色
        if (!CalendarUtil.compare(myDate, calToday.getTime())) {
            itemLayout.setBackgroundColor(Color.argb(0xff, 0xee, 0xee, 0xee));
//            textDay.setTextColor(Color.argb(0xff, 0xc0, 0xc0, 0xc0));
        } else {
            // 设置背景颜色
            if (equalsDate(calSelected.getTime(), myDate)) {
//                TODO 过18点置灰
//                if (calCalendar.getTime().getTime() - calCalendar.)
                // 选择的
                itemLayout.setBackgroundColor(Color.argb(0xff, 0xdc, 0xe2, 0xff));
            }
        }

        return itemLayout;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @SuppressWarnings("deprecation")
    private Boolean equalsDate(Date date1, Date date2) {
        if (date1.getYear() == date2.getYear()
                && date1.getMonth() == date2.getMonth()
                && date1.getDate() == date2.getDate()) {
            return true;
        } else {
            return false;
        }

    }

    // 根据改变的日期更新日历
    // 填充日历控件用
    private void UpdateStartDateForMonth() {
        calStartDate.set(Calendar.DATE, 1); // 设置成当月第一天

        // 星期一是2 星期天是1 填充剩余天数
        int iDay = 0;
        int iFirstDayOfWeek = Calendar.MONDAY;
        int iStartDay = iFirstDayOfWeek;
        if (iStartDay == Calendar.MONDAY) {
            iDay = calStartDate.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
            if (iDay < 0)
                iDay = 6;
        }
        if (iStartDay == Calendar.SUNDAY) {
            iDay = calStartDate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
            if (iDay < 0)
                iDay = 6;
        }
        calStartDate.add(Calendar.DAY_OF_WEEK, -iDay);
        calStartDate.add(Calendar.DAY_OF_MONTH, -1);// 周日第一位
    }

    public void setSelectedDate(Calendar cal) {
        calSelected = cal;
    }

}

/**
 * 用于生成日历展示的GridView布局
 */
class CalendarGridView extends GridView {

    /**
     * 当前操作的上下文对象
     */
    private Context mContext;

    /**
     * CalendarGridView 构造器
     *
     * @param context 当前操作的上下文对象
     */
    public CalendarGridView(Context context) {
        super(context);
        mContext = context;
        initGirdView();
    }

    /**
     * 初始化gridView 控件的布局
     */
    private void initGirdView() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);
        setNumColumns(7);// 设置每行列数
        setGravity(Gravity.CENTER_VERTICAL);// 位置居中
        setVerticalSpacing(1);// 垂直间隔
        setHorizontalSpacing(1);// 水平间隔
        setBackgroundColor(Color.argb(0xff, 0xe3, 0xee, 0xf4));

        int i = mContext.getResources().getDisplayMetrics().widthPixels / 7;
        int j = mContext.getResources().getDisplayMetrics().widthPixels
                - (i * 7);
        int x = j / 2;
        setPadding(x, 0, 0, 0);// 居中
    }
}


/**
 *
 */
class CalendarUtil {


    /**
     * 转换为2012年11月22日格式
     */
    private static SimpleDateFormat chineseDateFormat = new SimpleDateFormat(
            "yyyy年MM月dd日");
    /**
     * 转换为2012-11-22格式
     */
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd");


    /**
     * 把calendar转化为当前年月日
     *
     * @param calendar Calendar
     * @return 返回成转换好的 年月日格式
     */
    public static String getDay(Calendar calendar) {
        return simpleDateFormat.format(calendar.getTime());
    }

    /**
     * 用于比对二个日期的大小
     *
     * @param compareDate 将要比对的时间
     * @param currentDate 当前时间
     * @return true 表示大于当前时间 false 表示小于当前时间
     */
    public static boolean compare(Date compareDate, Date currentDate) {
        return chineseDateFormat.format(compareDate).compareTo(
                chineseDateFormat.format(currentDate)) >= 0;
    }


}

class NumberHelper {
    public static String LeftPad_Tow_Zero(int str) {
        java.text.DecimalFormat format = new java.text.DecimalFormat("00");
        return format.format(str);
    }
}