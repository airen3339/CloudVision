/*
 * Copyright (C) 2016. The Android Open Source Project.
 *
 *          yinglovezhuzhu@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.vrcvp.cloudvision.downloader;

import android.content.Context;
import android.util.Log;


import com.vrcvp.cloudvision.bean.DownloadLog;
import com.vrcvp.cloudvision.db.DownloadDBUtils;
import com.vrcvp.cloudvision.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * usage Downloader class
 *
 * @author yinglovezhuzhu@gmail.com
 */
public class Downloader {

    private static final String TAG = "DOWNLOADER";

    private static final int BUFFER_SIZE = 1024 * 64;

    private static final int RESPONSE_OK = 200;

    private static final int DEFAULT_END_SIZE = 1024 * 1024;

    private Context mContext;
    private boolean mStop = true; // The flag of stopped.
    private File mSaveFolder;
    private String mFileName; // saveLog file name;
    private File mSavedFile = null;
    private DownloadLog mDownloadLog; // The data downloadVideo state
    private String mUrl; // The url of the file which to downloadVideo.

    private boolean mNeedDownloadEnd = false; // Need download file end first

    /**
     * Constructor<br><br>
     *
     * @param context     Context??????
     * @param downloadUrl ????????????
     * @param needDownloadEnd ???????????????????????????
     * @param saveFolder  ????????????
     * @param fileName    ??????????????????????????????null????????????null??????????????????????????????????????????????????????????????????????????????????????????
     */
    public Downloader(Context context, String downloadUrl, boolean needDownloadEnd, File saveFolder, String fileName) {
        this.mContext = context;
        this.mUrl = downloadUrl;
        this.mNeedDownloadEnd = needDownloadEnd;
        this.mSaveFolder = saveFolder;
        this.mFileName = fileName;

        checkDownloadFolder(saveFolder);
    }

    /**
     * ????????????url
     * @param url ??????url??????
     */
    public void setUrl(String url) {
        this.mUrl = url;
    }

    /**
     * Download file???this method has network, don't use it on ui thread.
     *
     * @param listener      The listener to listen downloadVideo state, can be null if not need.
     * @param defaultSuffix ?????????????????????????????????????????????????????????????????????".mp4"
     * @return The size that downloaded.
     * @throws Exception The error happened when downloading.
     */
    public File download(String defaultSuffix, DownloadListener listener) throws Exception {
        if(StringUtils.isEmpty(mUrl)) {
            if(null != listener) {
                listener.onError(DownloadListener.CODE_EXCEPTION, "download url is empty");
            }
            return mSavedFile;
        }
        if (null != mDownloadLog && mDownloadLog.isLocked()) {
            Log.e(TAG, "File downloading now. url = " + mUrl);
            return mSavedFile;
        }
        mStop = false;

        mDownloadLog = DownloadDBUtils.getLogByUrl(mContext, mUrl);
        if (null != mDownloadLog
                && mDownloadLog.getDownloadedSize() >= mDownloadLog.getTotalSize()) {
            mSavedFile = new File(mDownloadLog.getSavedFile());
            DownloadDBUtils.deleteLog(mContext, mUrl);
            DownloadDBUtils.saveHistory(mContext, mDownloadLog);
            mDownloadLog.unlock();
            mStop = true;
            Log.w(TAG, "File downloadVideo finished!");
            return mSavedFile;
        }

        if(mStop) {
            return mSavedFile;
        }

        HttpURLConnection conn = null;
        RandomAccessFile randomFile = null;

        if (null == mDownloadLog) {
            // ??????????????????????????????????????????????????????
            int fileSize = 0;
            try {
                conn = getConnection(mUrl);
                conn.connect();
                Log.i(TAG, getResponseHeader(conn));
                if (conn.getResponseCode() == RESPONSE_OK) {
                    fileSize = conn.getContentLength();
                    // Throw a RuntimeException when got file size failed.
                    if (fileSize < 0) {
                        throw new RuntimeException("Can't get file size ");
                    }

                    if (StringUtils.isEmpty(mFileName)) {
                        final String filename = getFileName(conn, defaultSuffix);
                        // Create local file object according to local saved folder and local file name.
                        mSavedFile = new File(mSaveFolder, filename);
                    } else {
                        mSavedFile = new File(mSaveFolder, mFileName);
                    }

                    mDownloadLog = new DownloadLog(mUrl, 0, fileSize, mSavedFile.getPath());
                    DownloadDBUtils.saveLog(mContext, mDownloadLog);
                    if (mDownloadLog.getDownloadedSize() >= fileSize) {
                        // ??????????????????????????????????????????????????????
                        DownloadDBUtils.deleteLog(mContext, mUrl);
                        DownloadDBUtils.saveHistory(mContext, mDownloadLog);
                        mStop = true;
                        return mSavedFile;
                    }
                    mDownloadLog.lock();
                } else {
                    if (null != mDownloadLog) {
                        mDownloadLog.unlock();
                    }
                    mStop = true;
                    Log.w(TAG, "Server response error! Response code???" + conn.getResponseCode()
                            + "Response message???" + conn.getResponseMessage());
                    throw new RuntimeException("server response error, response code:" + conn.getResponseCode());
                }
            } catch (Exception e) {
                if (null != mDownloadLog) {
                    mDownloadLog.unlock();
                }
                mStop = true;
                Log.e(TAG, e.toString());
                throw new RuntimeException("Failed to connect the url:" + mUrl, e);
            } finally {
                if (null != conn) {
                    conn.disconnect();
                }
                conn = null;
            }

            if(mStop) {
                return mSavedFile;
            }

            try {
                randomFile = new RandomAccessFile(mSavedFile, "rw");
                if (fileSize > 0) {
                    randomFile.setLength(fileSize); // Set total size of the downloadVideo file.
                }
            } catch (Exception e) {
                if (null != mDownloadLog) {
                    mDownloadLog.unlock();
                }
                mStop = true;
                Log.e(TAG, e.toString(), e);// ????????????
                throw new Exception("Exception occur when downloading file\n", e);// Throw exception when some error happened when downloading.
            } finally {
                if (null != randomFile) {
                    try {
                        randomFile.close(); // Close the RandomAccessFile to make the settings effective
                        randomFile = null;
                    } catch (Exception e) {
                        Log.e(TAG, e.toString(), e);
                    }
                }
            }
        } else {
            mSavedFile = new File(mDownloadLog.getSavedFile());
            mSaveFolder = mSavedFile.getParentFile();
            mFileName = mSavedFile.getName();
        }

        if(mStop) {
            return mSavedFile;
        }

        // ???????????????????????????????????????????????????????????????????????????
        if(mNeedDownloadEnd) {
            int endSize = DEFAULT_END_SIZE;
            if(mDownloadLog.getTotalSize() < endSize) {
                endSize = mDownloadLog.getTotalSize();
            }
            downloadFileEnd(mContext, endSize);
        }

        if(mStop) {
            return mSavedFile;
        }

        if (null != listener) {
            listener.onProgressUpdate(mDownloadLog.getDownloadedSize(), mDownloadLog.getTotalSize());
        }

        InputStream inStream = null;
        try {
            conn = getConnection(mUrl);

            // Get the position of this thread start to downloadVideo.
            int startPos = mDownloadLog.getDownloadedSize();

            // Get the position of this thread end to downloadVideo.
            int endPos = mDownloadLog.getTotalSize();

            //Setting the rage of the data, it will return exact realistic size automatically,
            // if the size set to be is lager then realistic size.
            conn.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);

            // Get the input stream of the connection.
            Log.i(TAG, "Starts to downloadVideo from position " + startPos);
            randomFile = new RandomAccessFile(mSavedFile, "rw");
            // Make the pointer point to the position where start to downloadVideo.
            randomFile.seek(startPos);
            inStream = conn.getInputStream();
            // Set local cache size
            byte[] buffer = new byte[BUFFER_SIZE];
            int offset = 0;
            // The data is written to file until user stop downloadVideo or data is finished downloadVideo.
            while (!mStop && (offset = inStream.read(buffer)) != -1) {
                randomFile.write(buffer, 0, offset);
                mDownloadLog.setDownloadedSize(mDownloadLog.getDownloadedSize() + offset);
                if (null != listener) {
                    listener.onProgressUpdate(mDownloadLog.getDownloadedSize(), mDownloadLog.getTotalSize());
                }
            }
            // Update the range of this thread to database.
            DownloadDBUtils.updateLog(mContext, mDownloadLog);
            if (mDownloadLog.getDownloadedSize() >= mDownloadLog.getTotalSize()) {
                // ??????????????????????????????????????????????????????
                DownloadDBUtils.deleteLog(mContext, mUrl);
                mDownloadLog.setDownloadedSize(mDownloadLog.getTotalSize());
                DownloadDBUtils.saveHistory(mContext, mDownloadLog);
                mStop = true;
            }

            if (null != mDownloadLog) {
                mDownloadLog.unlock();
            }
        } catch (Exception e) {
            if (null != mDownloadLog) {
                mDownloadLog.unlock();
            }
            mStop = true;
            Log.e(TAG, e.toString());// ????????????
            throw new RuntimeException("Failed to downloadVideo file from " + mUrl, e);
        } finally {
            if (null != randomFile) {
                try {
                    randomFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null != conn) {
                conn.disconnect();
            }

        }
        return mSavedFile;
    }

    /**
     * Stop the downloadVideo
     */
    public synchronized void stop() {
        this.mStop = true;
        if (null != mDownloadLog) {
            mDownloadLog.unlock();
        }
    }

    /**
     * Get downloadVideo state is stopped or not.
     *
     * @return
     */
    public synchronized boolean isStop() {
        return this.mStop;
    }

    /**
     * ????????????????????????
     * @return
     */
    public synchronized boolean isFinished() {
        if(null == mDownloadLog) {
            return false;
        }
        return mDownloadLog.getDownloadedSize() >= mDownloadLog.getTotalSize();
    }

    /**
     * Get total file size
     *
     * @return
     */
    public int getFileSize() {
        return null == mDownloadLog ? 0 : mDownloadLog.getTotalSize();
    }

    /**
     * Gets save file
     *
     * @return save file
     */
    public File getSavedFile() {
        return mSavedFile;
    }


    /**
     * ?????????????????????????????????<br>
     * mp4??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ?????????????????????????????????????????????????????????
     *
     * @param context  Context??????
     * @param byteSize ????????????????????????????????????
     */
    private void downloadFileEnd(Context context, int byteSize) {
        if (null == mDownloadLog || mDownloadLog.isEndDownloaded()
                || mDownloadLog.getDownloadedSize() >= mDownloadLog.getTotalSize()) {
            return;
        }
        HttpURLConnection conn = null;
        RandomAccessFile outFile = null;
        InputStream inStream = null;
        try {
            conn = getConnection(mUrl);

            // ?????????????????????
            int startPos = mDownloadLog.getTotalSize() - byteSize;
            // ??????????????????????????????0
            startPos = startPos < 0 ? 0 : startPos;

            // ?????????????????????
            int endPos = mDownloadLog.getTotalSize();

            //Setting the rage of the data, it will return exact realistic size automatically,
            // if the size set to be is lager then realistic size.
            conn.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);

            // Get the input stream of the connection.
            Log.i(TAG, "Starts to downloadVideo from position " + startPos);
            outFile = new RandomAccessFile(mSavedFile, "rw");
            // Make the pointer point to the position where start to downloadVideo.
            outFile.seek(startPos);
            inStream = conn.getInputStream();
            // Set local cache size
            byte[] buffer = new byte[BUFFER_SIZE];
            int offset = 0;
            // The data is written to file until user stop downloadVideo or data is finished downloadVideo.
            while (!mStop && (offset = inStream.read(buffer)) != -1) {
                outFile.write(buffer, 0, offset);
            }
            mDownloadLog.setEndDownloaded(true);
            DownloadDBUtils.updateLog(context, mDownloadLog);
        } catch (Exception e) {
            Log.e(TAG, e.toString());// ????????????
            mDownloadLog.unlock();
            mStop = true;
            throw new RuntimeException("Failed to downloadVideo file from " + mUrl, e);
        } finally {
            if (null != outFile) {
                try {
                    outFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    /**
     * ??????HttpURLConnection??????
     *
     * @param downloadUrl ????????????
     * @return HttpURLConnection???????????????connect?????????
     */
    private HttpURLConnection getConnection(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(6 * 1000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "zh-CN");
        conn.setRequestProperty("Referer", downloadUrl);
        conn.setRequestProperty("Charset", "UTF-8");
        // Set agent.
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; "
                + "MSIE 8.0; Windows NT 5.2;"
                + " Trident/4.0; .NET CLR 1.1.4322;"
                + ".NET CLR 2.0.50727; " + ".NET CLR 3.0.04506.30;"
                + " .NET CLR 3.0.4506.2152; " + ".NET CLR 3.5.30729)");
        conn.setRequestProperty("Connection", "Keep-Alive");

        return conn;
    }

    /**
     * Check the downloadVideo folder, make new folder if it is not exist.
     *
     * @param folder ??????
     */
    private void checkDownloadFolder(File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Create folder failed: " + folder.getPath());
        }
    }

    /**
     * Get file name
     *
     * @param conn          HttpConnection object
     * @param defaultSuffix ???????????????????????????????????????".mp4"???
     * @return ????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private String getFileName(HttpURLConnection conn, String defaultSuffix) {
        String filename = mUrl.substring(mUrl.lastIndexOf("/") + 1);

        if (StringUtils.isEmpty(filename)) {// Get file name failed.
            for (int i = 0; ; i++) { // Get file name from http header.
                String mine = conn.getHeaderField(i);
                if (mine == null)
                    break; // Exit the loop when go through all http header.
                if ("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase(Locale.ENGLISH))) { // Get content-disposition header field returns, which may contain a file name
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase(Locale.ENGLISH)); // Using regular expressions query file name
                    if (m.find()) {
                        return m.group(1); // If there is compliance with the rules of the regular expression string
                    }
                }
            }
            filename = UUID.randomUUID() + defaultSuffix;// A 16-byte binary digits generated by a unique identification number
            // (each card has a unique identification number)
            // on the card and the CPU clock as the file name
        }
        return filename;
    }

    /**
     * Get HTTP response header field
     *
     * @param http HttpURLConnection object
     * @return HTTp response header field map.
     */
    private static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0; ; i++) {
            String fieldValue = http.getHeaderField(i);
            if (fieldValue == null) {
                break;
            }
            header.put(http.getHeaderFieldKey(i), fieldValue);
        }
        return header;
    }

    /**
     * Get HTTP response header field as a string
     *
     * @param conn HttpURLConnection object
     * @return HTTP response header field as a string
     */
    private static String getResponseHeader(HttpURLConnection conn) {
        Map<String, String> header = getHttpResponseHeader(conn);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            sb.append(key + entry.getValue());
        }
        return sb.toString();
    }
}
