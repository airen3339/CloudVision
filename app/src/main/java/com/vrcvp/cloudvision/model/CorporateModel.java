package com.vrcvp.cloudvision.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vrcvp.cloudvision.Config;
import com.vrcvp.cloudvision.bean.req.QueryCorporateReq;
import com.vrcvp.cloudvision.bean.req.QueryProductReq;
import com.vrcvp.cloudvision.bean.req.QueryVideoReq;
import com.vrcvp.cloudvision.bean.resp.QueryCorporateResp;
import com.vrcvp.cloudvision.bean.resp.QueryProductResp;
import com.vrcvp.cloudvision.bean.resp.QueryVideoResp;
import com.vrcvp.cloudvision.db.HttpCacheDBUtils;
import com.vrcvp.cloudvision.http.HttpAsyncTask;
import com.vrcvp.cloudvision.http.HttpStatus;
import com.vrcvp.cloudvision.utils.DataManager;
import com.vrcvp.cloudvision.utils.NetworkManager;
import com.vrcvp.cloudvision.utils.StringUtils;

/**
 * Corporate Model class
 * Created by yinglovezhuzhu@gmail.com on 2016/9/19.
 */
public class CorporateModel implements ICorporateModel {

    private Context mContext;

    private HttpAsyncTask<QueryCorporateResp> mQueryCorporateTask;

    public CorporateModel(Context context) {
        this.mContext = context;
    }


    @Override
    public void queryCorporateInfo(final HttpAsyncTask.Callback<QueryCorporateResp> callback) {
        final String url = Config.API_CORPORATE_INFO;
        final QueryCorporateReq reqParam = new QueryCorporateReq();
        reqParam.setEnterpriseId(DataManager.getInstance().getCorporateId());
        reqParam.setToken(DataManager.getInstance().getToken());
        final Gson gson = new Gson();
        final String key = gson.toJson(reqParam);
        if(NetworkManager.getInstance().isNetworkConnected()) {
            mQueryCorporateTask = new HttpAsyncTask<>();
            mQueryCorporateTask.doPost(url, reqParam,
                    QueryCorporateResp.class, new HttpAsyncTask.Callback<QueryCorporateResp>() {
                        @Override
                        public void onPreExecute() {
                            if(null != callback) {
                                callback.onPreExecute();
                            }
                            mQueryCorporateTask = null;
                        }

                        @Override
                        public void onCanceled() {
                            if(null != callback) {
                                callback.onCanceled();
                            }
                            mQueryCorporateTask = null;
                        }

                        @Override
                        public void onResult(QueryCorporateResp result) {
                            // ??????????????????????????????????????????????????????????????????
                            if(HttpStatus.SC_OK == result.getHttpCode()) {
                                HttpCacheDBUtils.saveHttpCache(mContext, url, key, gson.toJson(result));
                            }

                            if(null != callback) {
                                callback.onResult(result);
                            }
                            mQueryCorporateTask = null;
                        }
                    });
        } else {
            QueryCorporateResp result;
            String data = HttpCacheDBUtils.getHttpCache(mContext, url, key);
            if(StringUtils.isEmpty(data)) {
                // ??????
                if(null != callback) {
                    result = new QueryCorporateResp(HttpStatus.SC_CACHE_NOT_FOUND, "Cache not found");
                    callback.onResult(result);
                }
            } else {
                try {
                    // ????????????
                    result = new Gson().fromJson(data, QueryCorporateResp.class);
                    if(null != callback) {
                        callback.onResult(result);
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    if(null != callback) {
                        result = new QueryCorporateResp(HttpStatus.SC_CACHE_NOT_FOUND, "Cache not found");
                        callback.onResult(result);
                        // ?????????????????????????????????
                        HttpCacheDBUtils.deleteHttpCache(mContext, url, key);
                    }
                }
            }
        }
    }

    @Override
    public void cancelQueryCorporateInfo() {
        if(null != mQueryCorporateTask) {
            mQueryCorporateTask.cancel();
        }
    }

    @Override
    public void queryRecommendedProduct(final HttpAsyncTask.Callback<QueryProductResp> callback) {
        final String url = Config.API_RECOMMENDED_PRODUCT;
        final QueryProductReq reqParam = new QueryProductReq();
        reqParam.setEnterpriseId(DataManager.getInstance().getCorporateId());
        reqParam.setToken(DataManager.getInstance().getToken());
        final Gson gson = new Gson();
        final String key = gson.toJson(reqParam);
        if(NetworkManager.getInstance().isNetworkConnected()) {
            new HttpAsyncTask<QueryProductResp>().doPost(url, reqParam,
                    QueryProductResp.class, new HttpAsyncTask.Callback<QueryProductResp>() {
                        @Override
                        public void onPreExecute() {
                            if(null != callback) {
                                callback.onPreExecute();
                            }
                        }

                        @Override
                        public void onCanceled() {
                            if(null != callback) {
                                callback.onCanceled();
                            }
                        }

                        @Override
                        public void onResult(QueryProductResp result) {
                            // ??????????????????????????????????????????????????????????????????
                            if(HttpStatus.SC_OK == result.getHttpCode()) {
                                HttpCacheDBUtils.saveHttpCache(mContext, url, key, gson.toJson(result));
                            }

                            if(null != callback) {
                                callback.onResult(result);
                            }
                        }
                    });
        } else {
            QueryProductResp result;
            String data = HttpCacheDBUtils.getHttpCache(mContext, url, key);
            if(StringUtils.isEmpty(data)) {
                // ??????
                if(null != callback) {
                    result = new QueryProductResp(HttpStatus.SC_CACHE_NOT_FOUND, "Cache not found");
                    callback.onResult(result);
                }
            } else {
                try {
                    // ????????????
                    result = new Gson().fromJson(data, QueryProductResp.class);
                    if(null != callback) {
                        callback.onResult(result);
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    if(null != callback) {
                        result = new QueryProductResp(HttpStatus.SC_CACHE_NOT_FOUND, "Cache not found");
                        callback.onResult(result);
                        // ?????????????????????????????????
                        HttpCacheDBUtils.deleteHttpCache(mContext, url, key);
                    }
                }
            }
        }
    }

    @Override
    public void cancelQueryRecommendedProduct() {

    }

    @Override
    public void queryRecommendedVideo(final HttpAsyncTask.Callback<QueryVideoResp> callback) {
        final String url = Config.API_RECOMMENDED_VIDEO;
        final QueryVideoReq reqParam = new QueryVideoReq();
        reqParam.setEnterpriseId(DataManager.getInstance().getCorporateId());
        reqParam.setToken(DataManager.getInstance().getToken());
        final Gson gson = new Gson();
        final String key = gson.toJson(reqParam);
        if(NetworkManager.getInstance().isNetworkConnected()) {
            new HttpAsyncTask<QueryVideoResp>().doPost(url, reqParam,
                    QueryVideoResp.class, new HttpAsyncTask.Callback<QueryVideoResp>() {
                        @Override
                        public void onPreExecute() {
                            if(null != callback) {
                                callback.onPreExecute();
                            }
                        }

                        @Override
                        public void onCanceled() {
                            if(null != callback) {
                                callback.onCanceled();
                            }
                        }

                        @Override
                        public void onResult(QueryVideoResp result) {
                            // ??????????????????????????????????????????????????????????????????
                            if(HttpStatus.SC_OK == result.getHttpCode()) {
                                HttpCacheDBUtils.saveHttpCache(mContext, url, key, gson.toJson(result));
                            }

                            if(null != callback) {
                                callback.onResult(result);
                            }
                        }
                    });
        } else {
            QueryVideoResp result;
            String data = HttpCacheDBUtils.getHttpCache(mContext, url, key);
            if(StringUtils.isEmpty(data)) {
                // ??????
                if(null != callback) {
                    result = new QueryVideoResp(HttpStatus.SC_CACHE_NOT_FOUND, "Cache not found");
                    callback.onResult(result);
                }
            } else {
                try {
                    // ????????????
                    result = new Gson().fromJson(data, QueryVideoResp.class);
                    if(null != callback) {
                        callback.onResult(result);
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    if(null != callback) {
                        result = new QueryVideoResp(HttpStatus.SC_CACHE_NOT_FOUND, "Cache not found");
                        callback.onResult(result);
                        // ?????????????????????????????????
                        HttpCacheDBUtils.deleteHttpCache(mContext, url, key);
                    }
                }
            }
        }
    }

    @Override
    public void cancelQueryRecommendedVideo() {

    }
}
