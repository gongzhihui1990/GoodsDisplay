package koolpos.cn.goodsdisplayer.api;

import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import koolpos.cn.goodproviderservice.service.aidl.IGPService;
import koolpos.cn.goodsdisplayer.mvcModel.AidlResponse;
import koolpos.cn.goodsdisplayer.mvcModel.GoodType;
import koolpos.cn.goodsdisplayer.mvcModel.Goods;
import koolpos.cn.goodsdisplayer.util.Loger;

/**
 * Created by Administrator on 2017/5/14.
 */

public class AidlApi {
    private IGPService service;
    public AidlApi(IGPService service){
        this.service=service;
    }
    public List<GoodType> getTypeList() throws JSONException{
        JSONObject request=new JSONObject();
        request.put("action","local/get/getTypeList");
        AidlResponse response =proxyPost(request.toString());
        String data=response.getData();
        List<String> typeList =  new Gson().fromJson(data,
                new TypeToken<List<String>>() {
                }.getType());
        List<GoodType> goodTypeList=new ArrayList<>();
        for (String type:typeList) {
            goodTypeList.add(new GoodType(type));
        }
        return goodTypeList;
    }
    public List<Goods> getListByType(String type) throws JSONException {
        JSONObject request=new JSONObject();
        request.put("action","local/get/getListByType");
        request.put("type",type);
        AidlResponse response = proxyPost(request.toString());
        List<Goods> goodsList =  new Gson().fromJson(response.getData(),
                new TypeToken<List<Goods>>() {
                }.getType());
        return goodsList;
    }
    //                case "local/get/getListByType":
//                    String type = reqJson.optString("type");
//                    response.setData(getListByType(type));
//                    break;
    private AidlResponse proxyPost(String request){
        String response = "";
        try {
            response = service.proxyPost(request);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Loger.d("AidlResponse:"+response.toString());
        return new Gson().fromJson(response,AidlResponse.class);
    }
}
