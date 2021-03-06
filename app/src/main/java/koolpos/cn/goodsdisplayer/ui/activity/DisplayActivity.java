package koolpos.cn.goodsdisplayer.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import koolpos.cn.goodproviderservice.service.aidl.IGPService;
import koolpos.cn.goodsdisplayer.MyApplication;
import koolpos.cn.goodsdisplayer.R;
import koolpos.cn.goodsdisplayer.api.AidlApi;
import koolpos.cn.goodsdisplayer.mvcModel.GoodType;
import koolpos.cn.goodsdisplayer.mvcModel.Goods;
import koolpos.cn.goodsdisplayer.ui.fragment.DisplayGoodGroupFragment;
import koolpos.cn.goodsdisplayer.ui.widget.BounceBackViewPager;
import koolpos.cn.goodsdisplayer.ui.widget.FixedSpeedScroller;
import koolpos.cn.goodsdisplayer.util.AndroidUtils;
import koolpos.cn.goodsdisplayer.util.Loger;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DisplayActivity extends BaseActivity implements DisplayGoodGroupFragment.OnFragmentInteractionListener {

    @BindView(R.id.grid_content)
    BounceBackViewPager gridContent;
    @BindView(R.id.list_content_title)
    RecyclerView listContentTitle;
    private ViewPagerAdapter gridAdapter;
    private GoodTypeAdapter typeAdapter;

    private AidlApi aidlApi;
    private IGPService gpService;
    ServiceConnection connection =new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            gpService= IGPService.Stub.asInterface(iBinder);
            aidlApi =new AidlApi(gpService);
            try {
                JSONObject request=new JSONObject();
                request.put("action","local/get/getTypeList");
                String response =gpService.proxyPost(request.toString());
                typeAdapter.setData(aidlApi.getTypeList());
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        gridAdapter =new ViewPagerAdapter(getSupportFragmentManager());
        typeAdapter =new GoodTypeAdapter(gridAdapter);
        //Content布局
        //TODO
        gridContent.setAdapter(gridAdapter);
        Glide.with(MyApplication.getContext()).resumeRequests();
        gridContent.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (0==position&&0==positionOffsetPixels&&0==positionOffsetPixels){
                    Glide.with(DisplayActivity.this).resumeRequests();
                }else{
                    Glide.with(DisplayActivity.this).pauseRequests();
                }
            }

            @Override
            public void onPageSelected(int position) {
                Loger.d("onPageSelected");
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Loger.d("onPageScrollStateChanged");
                Glide.with(DisplayActivity.this).resumeRequests();
                switch (state) {
                    // 闲置中
                    case ViewPager.SCROLL_STATE_IDLE:
                        // “偷梁换柱”
//                        if (gridContent.getCurrentItem() == 0) {
//                            gridContent.setCurrentItem(gridAdapter.getCount()-1, false);
//                        } else if (gridContent.getCurrentItem() == gridAdapter.getCount()) {
//                            gridContent.setCurrentItem(1, false);
//                        }
                        if (gridContent.getCurrentItem() == 0) {
                            gridContent.setCurrentItem(gridAdapter.getCount()-1, false);
                        } else if (gridContent.getCurrentItem() == gridAdapter.getCount()-1) {
                            gridContent.setCurrentItem(1, false);
                        }
                        mCurrentPage=gridContent.getCurrentItem();
                        break;
                }
            }
        });
        //Title布局
        GridLayoutManager titleLayoutManager = new GridLayoutManager(this,1);
        titleLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        listContentTitle.setLayoutManager(titleLayoutManager);
        listContentTitle.setAdapter(typeAdapter);
        Intent serviceIntent=new Intent(IGPService.class.getName());
        serviceIntent= AndroidUtils.getExplicitIntent(getBaseContext(),serviceIntent);
        //serviceIntent.setPackage("koolpos.cn.goodproviderservice");//这里你需要设置你应用的包名
        boolean bindService=bindService(serviceIntent,connection, Context.BIND_AUTO_CREATE);
        Loger.d("bindService "+bindService);
        start();
        gridContent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //监听ViewPager的触摸事件，当用户按下的时候取消注册，当用户手抬起的时候再注册
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        stop();
                        break;
                    case MotionEvent.ACTION_UP:
                        stop();
                        reStart();
                        break;
                }
                return false;
            }});
    }

    @Override
    protected void onDestroy() {
        unbindService(connection);
        stop();
        super.onDestroy();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
    private int mCurrentPage=0;
    private boolean isAutoPlay=true;
    private Disposable mViewPagerSubscribe;
    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
    private boolean stateStop=true;
    private void controlViewPagerSpeedStop(){
        if (stateStop){
            return;
        }stateStop=true;
        try {
            Field mField;
            mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            Scroller mScroller = new Scroller(getBaseContext(), sInterpolator);
            mField.set(gridContent, mScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //设置滚动速度
    private void controlViewPagerSpeed() {
        if (!stateStop){
            return;
        }stateStop=false;
        try {
            Field mField;
            mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            FixedSpeedScroller  mScroller = new FixedSpeedScroller(getBaseContext(),
                    new AccelerateInterpolator());
            mScroller.setmDuration(1500); // 1000ms
            mField.set(gridContent, mScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //开始轮播
    public void start()  {
        mViewPagerSubscribe = Observable.interval(3, 3, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        controlViewPagerSpeed();
                        if (gridAdapter.getData() != null && gridAdapter.getData().size() > 0 && isAutoPlay) {
                            mCurrentPage++;
                            gridContent.setCurrentItem(mCurrentPage);
                        }
                    }
                });
    }
    //重新轮播
    public void reStart()  {
        mViewPagerSubscribe = Observable.interval(6, 3, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        controlViewPagerSpeed();
                        if (gridAdapter.getData() != null && gridAdapter.getData().size() > 1 && isAutoPlay) {
                            mCurrentPage++;
                            gridContent.setCurrentItem(mCurrentPage);
                        }
                    }
                });
    }
    //结束、暂停轮播
    public void stop() {
        controlViewPagerSpeedStop();
        if(!mViewPagerSubscribe.isDisposed()) {
            mViewPagerSubscribe.dispose();
        }
    }
    class ViewPagerAdapter extends FragmentStatePagerAdapter {

        private final int GroupSize =20;

        List<Goods[]> goodsGroup =new ArrayList<>();

        public  List<Goods[]> getData(){
            return goodsGroup;
        }
        public ViewPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public int getCount() {
            if (goodsGroup!=null&&goodsGroup.size()>=2){
                return goodsGroup.size()+2;
            }

            if(goodsGroup!=null&&goodsGroup.size()==1){
                return 1;
            }

            return 0;
        }

        @Override
        public Fragment getItem(int position) {
            Goods[] goods = null;

            if (getCount()==1){
                goods = goodsGroup.get(position);
            }else {
                if (position == 0) {// 将最前面一页设置成本来最后的那页
                    goods = goodsGroup.get(getData().size()-1);
                } else if (position == getData().size() + 1) {// 将最后面一页设置成本来最前的那页
                    goods = goodsGroup.get(0);
                } else {
                    goods = goodsGroup.get(position-1);
                }
            }
            DisplayGoodGroupFragment fragment = DisplayGoodGroupFragment.newInstance(goods);
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        public void setDataByType(String type) {
            goodsGroup.clear();
                try {
                    List <Goods> dataList = aidlApi.getListByType(type);
                    Loger.d("dataList size:"+dataList.size());
                    int dataListSizeNow=dataList.size();
                    int index=0;
                    //新建数组的大小
                    int itemSize = dataListSizeNow < GroupSize ? dataListSizeNow : GroupSize;
                    Loger.d("初始单页的大小:"+itemSize);
                    Goods[] group = new Goods[itemSize];
                    for (Goods item:dataList){
                        //赋值项到单页数组
                        group[index]=item;
                        if (index == group.length-1){//到达单页数组最后一项
                            //添加单页数组
                            goodsGroup.add(group);
                            //新建单页的大小
                            itemSize = dataListSizeNow < GroupSize ? dataListSizeNow -1 : GroupSize;
                            Loger.d("新建单页的大小:"+itemSize);
                            group = new Goods[itemSize];
                            index = 0;
                        }else{
                            index++;
                        }
                        dataListSizeNow--;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            Loger.d("页大小:" + goodsGroup.size());
            notifyDataSetChanged();
        }
    }

    public class GoodTypeAdapter extends RecyclerView.Adapter<GoodTypeAdapter.GoodTypeViewHolder>{

        private List<GoodType> data=new ArrayList<>();
        private int curIndex = 0;
        private ViewPagerAdapter mGridAdapter;
        private GoodTypeAdapter(ViewPagerAdapter gridAdapter) {
            this.mGridAdapter=gridAdapter;
        }

        public void setData(List<GoodType> data) {
            this.data = data;
            myNotifyDataSetChanged();
        }

        public void myNotifyDataSetChanged(){
            mGridAdapter.setDataByType(data.get(curIndex).getTypeName());
            notifyDataSetChanged();
        }
        @Override
        public GoodTypeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_good_title,parent,false);
            return new GoodTypeViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final GoodTypeViewHolder holder, int position) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (curIndex!=holder.getAdapterPosition()){
                        curIndex =holder.getAdapterPosition();
                        myNotifyDataSetChanged();
                    }
                }
            });
            holder.good_type.setSelected(curIndex == position);
            holder.good_type.setText(data.get(position).getTypeName());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class GoodTypeViewHolder extends RecyclerView.ViewHolder{
            TextView good_type;
            private GoodTypeViewHolder(View itemView){
                super(itemView);
                good_type= (TextView) itemView.findViewById(R.id.good_type);
            }
        }
    }
}
