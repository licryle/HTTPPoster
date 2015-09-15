package com.licryle.httpposter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by licryle on 9/15/15.
 */
public class HttpConfiguration {
  protected String _mEndPoint;
  protected HashMap<String, String> _mArgs;
  protected ArrayList<String> _mFilePaths;
  protected String _sHTTPBoundary;
  protected HttpListener _mListener;

  public HttpConfiguration(String mEndPoint,
                           Map<String, String> mArgs,
                           ArrayList<String> mFilePaths,
                           HttpListener mListener,
                           String sHTTPBoundary) {
    _mEndPoint = mEndPoint;
    _mArgs = new HashMap<String, String>(mArgs);
    _mFilePaths = new ArrayList<String>(mFilePaths);
    _mListener = mListener;

    _sHTTPBoundary = sHTTPBoundary;
    if (sHTTPBoundary == null || sHTTPBoundary.equals("")) {
      _sHTTPBoundary = "-------" + String.valueOf(Math.random() * 10e10)
          + "-------";
    }
  }

  public String getEndPoint() { return _mEndPoint; }
  public ArrayList<String> getFiles() { return _mFilePaths; }
  public HashMap<String, String> getArgs() { return _mArgs; }
  public HttpListener getListener() { return _mListener; }
  public String getHTTPBoundary() { return _sHTTPBoundary; }
}