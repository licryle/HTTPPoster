package com.licryle.httpposter; /**
 * Created by licryle on 9/7/15.
 */

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

public class HttpPoster extends AsyncTask<HttpConfiguration, Integer,
    Long> {
  public static final long FAILURE_CONNECTION = -1;
  public static final long FAILURE_TRANSFER = -2;
  public static final long FAILURE_RESPONSE = -3;
  public static final long FAILURE_MALFORMEDURL = -4;
  public static final long SUCCESS = 0;

  protected String _sResponse = "";
  protected int _iResponseCode = 0;
  protected HttpListener _mListener = null;

  protected static int UNIQUE_ID = 0;
  protected int _iInstanceId = 0;

  protected class _ProgressiveEntity implements HttpEntity {
    protected HttpEntity _mEntity;
    protected HttpPoster _mPoster;

    public _ProgressiveEntity(HttpEntity mEntity, HttpPoster mPoster) {
      _mEntity = mEntity;
      _mPoster = mPoster;
    }

    @Override
    public void consumeContent() throws IOException {
      _mEntity.consumeContent();
    }

    @Override
    public InputStream getContent() throws IOException,
        IllegalStateException {
      return _mEntity.getContent();
    }

    @Override
    public Header getContentEncoding() {
      return _mEntity.getContentEncoding();
    }

    @Override
    public long getContentLength() {
      return _mEntity.getContentLength();
    }

    @Override
    public Header getContentType() {
      return _mEntity.getContentType();
    }

    @Override
    public boolean isChunked() {
      return _mEntity.isChunked();
    }

    @Override
    public boolean isRepeatable() {
      return _mEntity.isRepeatable();
    }

    @Override
    public boolean isStreaming() {
      return _mEntity.isStreaming();
    } // CONSIDER put a _real_ delegator into here!

    @Override
    public void writeTo(OutputStream mOutstream) throws IOException {
      Log.i("HttpPoster", "_ProgressiveEntity.In");
      _mEntity.writeTo(new _ProgressiveOutputStream(
          mOutstream, _mEntity.getContentLength(), _mPoster));
      Log.i("HttpPoster", "_ProgressiveEntity.Out");
    }

  }

  protected class _ProgressiveOutputStream extends DataOutputStream {
    long totalSent;
    long totalSize;
    protected HttpPoster _mPoster;

    public _ProgressiveOutputStream(OutputStream proxy, long total,
                                    HttpPoster mPoster) {
      super(proxy);
      totalSent = 0;
      totalSize = total;
      _mPoster = mPoster;
    }

    public void write(byte[] bts, int st, int end) throws IOException {
      totalSent += end;

      _mPoster._dispatchOnProgress(
          (int) ((totalSent / (float) totalSize) * 100));

      out.write(bts, st, end);
    }
  }

  @Override
  protected void onPreExecute() {
    _iInstanceId = ++UNIQUE_ID;
  }

  @Override
  protected Long doInBackground(HttpConfiguration... mConf) {
    _mListener = mConf[0].getListener();

    _ProgressiveEntity mEntity = _BuildEntity(mConf[0]);

    return _Transfer(mConf[0], mEntity);
  }

  @Override
  protected void onPostExecute(Long lResult) {
    if (lResult == SUCCESS || lResult == FAILURE_RESPONSE) {
      _dispatchOnSuccess(_sResponse);
    } else {
      _dispatchOnFailure(lResult);
    }
  }

  protected void _dispatchOnStartTransfer() {
    if (_mListener != null) {
      _mListener.onStartTransfer(_iInstanceId);
    }
  }

  protected void _dispatchOnProgress(int iProgress) {
    if (_mListener != null) {
      _mListener.onProgress(_iInstanceId, iProgress);
    }
  }

  protected void _dispatchOnSuccess(String sResponse) {
    if (_mListener != null) {
      _mListener.onResponse(_iInstanceId, _iResponseCode, sResponse);
    }

    _iResponseCode = 0;
  }

  protected void _dispatchOnFailure(long lError) {
    if (_mListener != null) {
      _mListener.onFailure(_iInstanceId, lError);
    }
  }

  protected _ProgressiveEntity _BuildEntity(HttpConfiguration mConf) {
    /********* Build request content *********/
    MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();
    mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    mBuilder.setBoundary(mConf.getHTTPBoundary());

    int iFileNb = 0;
    Iterator mFiles = mConf.getFiles().iterator();
    while (mFiles.hasNext()) {
      final File mFile = new File((String) mFiles.next());

      mBuilder.addBinaryBody("file_" + iFileNb, mFile,
          ContentType.DEFAULT_BINARY, mFile.getName());

      iFileNb++;
    }

    Iterator mArgs = mConf.getArgs().entrySet().iterator();
    while (mArgs.hasNext()) {
      Map.Entry mPair = (Map.Entry) mArgs.next();

      mBuilder.addTextBody((String) mPair.getKey(), (String) mPair.getValue(),
          ContentType.MULTIPART_FORM_DATA);
    }


    return new _ProgressiveEntity(mBuilder.build(), this);
  }

  protected Long _Transfer(HttpConfiguration mConf, _ProgressiveEntity mEntity) {
    /******** Open request ********/
    URL mUrl = null;
    try {
      mUrl = new URL(mConf.getEndPoint());
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return  FAILURE_MALFORMEDURL;
    }

    try {
      HttpURLConnection mConn = (HttpURLConnection) mUrl.openConnection();

      mConn.setRequestMethod("POST");
      mConn.setRequestProperty("Content-Type",
          "multipart/form-data; boundary=" + mConf.getHTTPBoundary());

      mConn.setDoInput(true);
      mConn.setDoOutput(true);
      mConn.setUseCaches(false);
      mConn.setReadTimeout(10000);
      mConn.setConnectTimeout(15000);
      mConn.setInstanceFollowRedirects(false);

      mConn.connect();


      try {
        /********** Write request ********/
        _dispatchOnStartTransfer();
        mEntity.writeTo(mConn.getOutputStream());
        mConn.getOutputStream().flush();
        mConn.getOutputStream().close();

        _iResponseCode = mConn.getResponseCode();

        try {
          _getContent(mConn.getInputStream());
          return SUCCESS;
        } catch (IOException e) {
          return FAILURE_RESPONSE;
        }
      } catch (Exception e) {
        Log.d("HTTPParser", e.getMessage() + e.getStackTrace().toString());

        return FAILURE_TRANSFER;
      } finally {
        if (mConn != null) {
          mConn.disconnect();
        }
      }
    } catch (Exception e) {
      return FAILURE_CONNECTION;
    }
  }

  protected void _getContent(InputStream response) throws IOException {
    BufferedReader mRd = new BufferedReader(new InputStreamReader(response));
    String sBody = "";
    String sContent = "";

    while ((sBody = mRd.readLine()) != null)
    {
      sContent += sBody + "\n";
    }

    _sResponse = sContent;
  }
}