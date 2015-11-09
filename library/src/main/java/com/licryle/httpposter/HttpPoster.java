/*
 * Copyright (C) 2015 - Cyrille Berliat <cyrille.berliat+github@gmail.com>
 *
 * Licensed under the GNU General Public  License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.licryle.httpposter;

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
import java.util.Iterator;
import java.util.Map;

/**
 * Main class of the library. Orchestrate the Async HTTP Posting. Only the first
 * argument/HttpConfiguration passed in execute() will be acted on.
 *
 * How to use:
 * - Implement the HttpListener into a class,
 * - Instanciate a {@link HttpConfiguration},
 * - Instanciate a {@link HttpPoster},
 * - Call execute on the {@link HttpConfiguration} object.
 *
 * ** Usage example:
 * HashMap<String, String> mArgs = new HashMap<>();
 * mArgs.put("lat", "40.712784");
 * mArgs.put("lon", "-74.005941");
 *
 * ArrayList<File> aFileList = getMyImageFiles();
 *
 * HttpConfiguration mConf = new HttpConfiguration(
 *     "http://www.mysite.com/HttpPostEndPoint",
 *     mArgs,
 *     aFileList,
 *     this, // If this class implements HttpListener
 *     null,  // Boundary for Entities - Optional
 *     15000  // Timeout in ms for the connection operation
 *     10000, // Timeout in ms for the reading operation
 * );
 *
 * new HttpPoster().execute(mConf);
 *
 * @see com.licryle.httpposter.HttpConfiguration
 * @see com.licryle.httpposter.HttpListener
 *
 * This code is an merge of {@see http://stackoverflow.com/a/19188010} and
 * {@see http://www.xyzws.com/javafaq/how-to-use-httpurlconnection-post-data-to-
 * web-server/139} put in a nice AsyncTask, with configuration and a listener
 * for all events.
 *
 * It is also updated to the newest Android API, as HttpPost and HttpClient
 * classes are deprecated in favor of HttpURLConnection.
 */
public class HttpPoster extends AsyncTask<HttpConfiguration, Integer,
    Long> {
  /*****************************************************************************
   ****************************** Public Constants *****************************
   ****************************************************************************/

  /** A failure ocurred when trying to open the connection. */
  public static final long FAILURE_CONNECTION = -1;
  /** A failure ocurred when trying to post the request. */
  public static final long FAILURE_TRANSFER = -2;
  /** A failure ocurred when retrieving the reponse from the post request. */
  public static final long FAILURE_RESPONSE = -3;
  /** A file passed in paramater couldn't be accessed. */
  private static final long FAILURE_FILE_READ = -4;

  /** The POST request was successful and we retrieved the answer correctly. */
  public static final long SUCCESS = 0;

  /*****************************************************************************
   **************************** Protected Variables ****************************
   ****************************************************************************/

  /** The response from the server following the HTTPPost request. */
  protected String _sResponse = "";
  /** The response code from the server following the HTTPPost request. 200 is a
   *  success. */
  protected int _iResponseCode = 0;
  /** The listener object to which we will dispatch events. */
  protected HttpListener _mListener = null;

  /** Static Unique ID so we can associate each request with a unique id for the
   * listener's callbacks. */
  protected static int UNIQUE_ID = 0;

  /** Current Unique Id of the POST Request. */
  protected int _iInstanceId = 0;


  /*****************************************************************************
   ************************** Protected Helper Classes *************************
   ****************************************************************************/

  /** HttpPost implementation of HttpEntity to allow for a
   * _ProgressiveOutputStream to be called in place of the generic OutputStream.
   * This way, we can track the progress of the posting.
   *
   * @see com.licryle.httpposter.HttpPoster._ProgressiveOutputStream
   */
  protected class _ProgressiveEntity implements HttpEntity {
    /** Original Entity we want to send in the POST request */
    protected HttpEntity _mEntity;
    /** Poster object we want to inform progress of. */
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
      Log.d("HttpPoster", "_ProgressiveEntity.In");
      _mEntity.writeTo(new _ProgressiveOutputStream(
          mOutstream, _mEntity.getContentLength(), _mPoster));
      Log.d("HttpPoster", "_ProgressiveEntity.Out");
    }

  }

  /** _ProgressiveOutputStream is an implementation of DataOutputStream that
   * keeps track of the number of bytes written in the stream.
   *
   * Only called by _ProgressiveEntity.
   *
   * @see com.licryle.httpposter.HttpPoster._ProgressiveEntity
   */
  protected class _ProgressiveOutputStream extends DataOutputStream {
    /** Number of bytes we already sent in the stream. */
    protected long _lTotalSent;
    /** Total number of bytes we want to send. */
    protected long _lTotalSize;
    /** Poster object we want to inform progress of. */
    protected HttpPoster _mPoster;
    /** Progress from 0 to 100 of the transfer. Used to pace progress sending.*/
    protected int _iProgress;

    public _ProgressiveOutputStream(OutputStream proxy, long total,
                                    HttpPoster mPoster) {
      super(proxy);
      _lTotalSent = 0;
      _lTotalSize = total;
      _mPoster = mPoster;
      _iProgress = 0;
    }

    /**
     * Override of the write method from the base class so we can track how many
     * bytes we sent in the stream. Once written, dispatches progress using
     * _dispatchOnProgress as an int from 0 to 100 corresponding to the
     * percentage of data sent, only when the value changes to avoid spamming
     * progress signals.
     *
     * @param aBytes Full array of bytes to be sent in the stream.
     * @param iStart Starting index in aBytes weere we want to start sending.
     * @param iCount Number of bytes to write in the stream.
     * @throws IOException Whenever the write fails, raises an IO Exception.
     */
    @Override
    public void write(byte[] aBytes, int iStart, int iCount) throws IOException {
      _lTotalSent += iCount;

      out.write(aBytes, iStart, iCount);

      int iOldProgress = _iProgress;
      _iProgress = (int) ((_lTotalSent / (float) _lTotalSize) * 100);

      if (_iProgress != iOldProgress) {
        _mPoster.publishProgress(_iProgress);
      }
    }
  }

  /*****************************************************************************
   **************************** AsyncTask Overrides ****************************
   ****************************************************************************/

  @Override
  protected void onPreExecute() {
    _iInstanceId = ++UNIQUE_ID;

    Log.d("HttpPoster",
        String.format("onPreExecute: Now executing instance %d", _iInstanceId));
  }

  /**
   * Executes on the background thread to perform the HTTP Post request. This
   * function initialize the Listener, builds the Entity to send to the server,
   * then process the request. Warning: only the first {link HttpConfiguration}
   * is processed.
   *
   * @param mConf {@link HttpConfiguration} of the post request to be processed.
   *                                       Only the first element is acted upon.
   * @return The result of the HTTP Post request. Either #SUCCESS,
   * #FAILURE_CONNECTION, #FAILURE_TRANSFER, #FAILURE_RESPONSE,
   * #FAILURE_MALFORMEDURL or #FAILURE_FILE_READ.
   */
  @Override
  protected Long doInBackground(HttpConfiguration... mConf) {
    _mListener = mConf[0].getListener();

    try {
      _ProgressiveEntity mEntity = _buildEntity(mConf[0]);

      return _httpPost(mConf[0], mEntity);
    } catch (IOException e) {
      e.printStackTrace();
      return FAILURE_FILE_READ;
    }
  }

  @Override
  protected void onProgressUpdate(Integer... iProgress) {
    _dispatchOnProgress(iProgress[0]);
  }

  @Override
  protected void onPostExecute(Long lResult) {
    Log.d("HttpPoster",
        String.format("onPostExecute: Finished executing instance %d",
            _iInstanceId));

    if (lResult == SUCCESS || lResult == FAILURE_RESPONSE) {
      _dispatchOnSuccess(_sResponse);
    } else {
      _dispatchOnFailure(lResult);
    }
  }

  /*****************************************************************************
   ************************** HttpListener Dispatchers *************************
   ****************************************************************************/

  /**
   * Will call the {@link HttpListener#onStartTransfer} callback should the
   * _mListener object be non-null.
   *
   * @see HttpListener#onStartTransfer
   */
  protected void _dispatchOnStartTransfer() {
    if (_mListener != null) {
      _mListener.onStartTransfer(_iInstanceId);
    }
  }

  /**
   * Will call the {@link HttpListener#onProgress} callback should the
   * _mListener object be non-null.
   *
   * @param iProgress The process of the upload on a scale of 0 to 100.
   *
   * @see HttpListener#onProgress
   */
  protected void _dispatchOnProgress(int iProgress) {
    if (_mListener != null) {
      _mListener.onProgress(_iInstanceId, iProgress);
    }
  }

  /**
   * Will call the {@link HttpListener#onResponse} callback should the
   * _mListener object be non-null.
   *
   * @param sResponse The response from the server.
   *
   * @see HttpListener#onResponse
   */
  protected void _dispatchOnSuccess(String sResponse) {
    if (_mListener != null) {
      _mListener.onResponse(_iInstanceId, _iResponseCode, sResponse);
    }

    _iResponseCode = 0;
  }

  /**
   * Will call the {@link HttpListener#onFailure} callback should the
   * _mListener object be non-null.
   *
   * @param lError The error code related to the POST failure. Either
   *               #FAILURE_CONNECTION, #FAILURE_TRANSFER, #FAILURE_RESPONSE,
   *               #FAILURE_MALFORMEDURL or #FAILURE_FILE_READ.
   *
   * @see HttpListener#onFailure
   */
  protected void _dispatchOnFailure(long lError) {
    if (_mListener != null) {
      _mListener.onFailure(_iInstanceId, lError);
    }
  }

  /*****************************************************************************
   **************************** Core Poster Functions **************************
   ****************************************************************************/

  /**
   * Builds the {@link _ProgressiveEntity} that we will send to the server.
   * Takes for input an {@link HttpConfiguration} that contains the Post
   * variables and File Names to send.
   *
   * @param mConf Configuration of the POST request to be processed.
   * @return A ProgressiveEntity which progress can be tracked as we send it to
   * the server.
   *
   * @throws IOException When a file in the list of files from
   * {@link HttpConfiguration#getFiles()} cannot be read.
   *
   * @see {@link com.licryle.httpposter.HttpPoster._ProgressiveEntity}
   * @see {@link com.licryle.httpposter.HttpConfiguration}
   */
  protected _ProgressiveEntity _buildEntity(HttpConfiguration mConf)
      throws IOException{
    Log.d("HttpPoster",
        String.format("_buildEntity: Entering for Instance %d", _iInstanceId));

    /********* Build request content *********/
    MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();
    mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    mBuilder.setBoundary(mConf.getHTTPBoundary());

    int iFileNb = 0;
    Iterator mFiles = mConf.getFiles().iterator();
    while (mFiles.hasNext()) {
      final File mFile = (File) mFiles.next();

      try {
        mBuilder.addBinaryBody("file_" + iFileNb, mFile,
            ContentType.DEFAULT_BINARY, mFile.getName());
      } catch(Exception e) {
        throw new IOException();
      }

      iFileNb++;
    }

    Iterator mArgs = mConf.getArgs().entrySet().iterator();
    while (mArgs.hasNext()) {
      Map.Entry mPair = (Map.Entry) mArgs.next();

      mBuilder.addTextBody((String) mPair.getKey(), (String) mPair.getValue(),
          ContentType.MULTIPART_FORM_DATA);
    }

    Log.d("HttpPoster",
        String.format("_buildEntity: Leaving for Instance %d", _iInstanceId));

    return new _ProgressiveEntity(mBuilder.build(), this);
  }

  /**
   * Opens a connection to the POST end point specified in the mConf
   * {@link HttpConfiguration} and sends the content of mEntity. Attempts to
   * read the answer from the server after the POST Request.
   * 
   * @param mConf The {@link HttpConfiguration} of the request.
   * @param mEntity The Entity to send in the HTTP Post. Should be built using
   *                {@link #_buildEntity}.
   *
   * @return The result of the HTTP Post request. Either #SUCCESS,
   * #FAILURE_CONNECTION, #FAILURE_TRANSFER, #FAILURE_RESPONSE,
   * #FAILURE_MALFORMEDURL.
   *
   * @see HttpConfiguration
   * @see _ProgressiveEntity
   */
  protected Long _httpPost(HttpConfiguration mConf,
                           _ProgressiveEntity mEntity) {
    Log.d("HttpPoster",
        String.format("_httpPost: Entering Instance %d", _iInstanceId));

    /******** Open request ********/
    try {
      HttpURLConnection mConn =
          (HttpURLConnection) mConf.getEndPoint().openConnection();

      mConn.setRequestMethod("POST");
      mConn.setRequestProperty("Content-Type",
          "multipart/form-data; boundary=" + mConf.getHTTPBoundary());

      mConn.setDoInput(true);
      mConn.setDoOutput(true);
      mConn.setUseCaches(false);
      mConn.setReadTimeout(mConf.getReadTimeout());
      mConn.setConnectTimeout(mConf.getConnectTimeout());
      mConn.setInstanceFollowRedirects(false);

      mConn.connect();

      Log.d("HttpPoster",
          String.format("_httpPost: Connected for Instance %d", _iInstanceId));

      try {
        /********** Write request ********/
        _dispatchOnStartTransfer();

        Log.d("HttpPoster",
            String.format("_httpPost: Sending for Instance %d", _iInstanceId));

        mEntity.writeTo(mConn.getOutputStream());
        mConn.getOutputStream().flush();
        mConn.getOutputStream().close();

        _iResponseCode = mConn.getResponseCode();
        try {
          Log.d("HttpPoster",
              String.format("_httpPost: Reading for Instance %d",
                  _iInstanceId));

          _readServerAnswer(mConn.getInputStream());
          return SUCCESS;
        } catch (IOException e) {
          return FAILURE_RESPONSE;
        }
      } catch (Exception e) {
        Log.d("HTTPParser", e.getMessage());
        e.printStackTrace();

        return FAILURE_TRANSFER;
      } finally {
        if (mConn != null) {
          Log.d("HttpPoster",
              String.format("_httpPost: Disconnecting Instance %d",
                  _iInstanceId));

          mConn.disconnect();
        }
      }
    } catch (Exception e) {
      return FAILURE_CONNECTION;
    }
  }

  /**
   * Reads the content of the InputStream, typically the answer from the server
   * in this context, and stores it in {@link #_sResponse}.
   *
   * @param mInputStream Any valid InputStream.
   * @throws IOException When the InputStream can't be read.
   */
  protected void _readServerAnswer(InputStream mInputStream)
      throws IOException {
    BufferedReader mRd =
        new BufferedReader(new InputStreamReader(mInputStream));

    String sLine = "";
    String sContent = "";

    while ((sLine = mRd.readLine()) != null)
    {
      sContent += sLine + "\n";
    }

    _sResponse = sContent;
  }
}