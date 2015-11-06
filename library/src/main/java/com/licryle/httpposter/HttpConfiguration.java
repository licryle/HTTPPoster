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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for the {@link HttpPoster}. Helps specify where, what
 * and under which conditions to send the HTTP POST request.
 *
 * ** Usage:
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
 *     this,  // If this class implements HttpListener
 *     null,  // Boundary for Entities - Optional
 *     15000  // Timeout in ms for the connection operation
 *     10000, // Timeout in ms for the reading operation
 * );
 *
 * This will send "lat=40.712784", "lon=-74.005941" as well as the encoded files
 * to the server using {@link HttpPoster}.
 *
 * @see HttpPoster
 */
public class HttpConfiguration {
  /** URL end point to send the HTTP Post request to. */
  protected URL _mEndPoint;
  /** Array of Key/Value pairs to be sent in the request. */
  protected HashMap<String, String> _mArgs;
  /** Array of files to be encoded and sent in the request. */
  protected ArrayList<File> _mFilePaths;
  /** Unique String to be used for the HTTP Boundary. It must *not* appear in
   * your files data. */
  protected String _sHTTPBoundary;
  /** Object implementing {@see HttpListener} to received callbacks calls. */
  protected HttpListener _mListener;
  /** Timeout in milliseconds for the connection. */
  protected int _iConnectTimeout;
  /** Timeout in mulliseconds for the reading of the response from the server.*/
  protected int _iReadTimeout;

  /**
   * Creates the configuration object for the HTTP POST request to be executed
   * by an {@link HttpPoster} instance.
   *
   * @param mEndPoint URL end point to send the HTTP Post request to.
   * @param mArgs Array of Key/Value pairs to be sent in the request.
   * @param mFilePaths Array of files to be encoded and sent in the request.
   * @param mListener Object implementing {@see HttpListener} to received
   *                  callbacks calls.
   * @param sHTTPBoundary Unique String to be used for the HTTP Boundary. It
   *                      must *not* appear in your files data.
   * @param iConnectTimeout Timeout in milliseconds for the connection.
   * @param iReadTimeout Timeout in mulliseconds for the reading of the response
   *                     from the server.
   */
  public HttpConfiguration(URL mEndPoint,
                           Map<String, String> mArgs,
                           ArrayList<File> mFilePaths,
                           HttpListener mListener,
                           String sHTTPBoundary,
                           int iConnectTimeout,
                           int iReadTimeout) {
    _mEndPoint = mEndPoint;
    _mArgs = new HashMap<String, String>(mArgs);
    _mFilePaths = new ArrayList<File>(mFilePaths);
    _mListener = mListener;

    _sHTTPBoundary = sHTTPBoundary;
    if (sHTTPBoundary == null || sHTTPBoundary.equals("")) {
      _sHTTPBoundary = "-------" + String.valueOf(Math.random() * 10e10)
          + "-------";
    }

    _iReadTimeout = iReadTimeout;
    _iConnectTimeout = iConnectTimeout;
  }

  public URL getEndPoint() { return _mEndPoint; }
  public ArrayList<File> getFiles() { return _mFilePaths; }
  public HashMap<String, String> getArgs() { return _mArgs; }
  public HttpListener getListener() { return _mListener; }
  public String getHTTPBoundary() { return _sHTTPBoundary; }
  public int getReadTimeout() { return _iReadTimeout; }
  public int getConnectTimeout() { return _iConnectTimeout; }
}