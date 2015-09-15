package com.licryle.httpposter;

/**
 * Created by licryle on 9/15/15.
 */
public interface HttpListener {
  void onStartTransfer(int iInstance);
  void onProgress(int iInstance, int iProgress);
  void onFailure(int iInstance, Long lErrorCode);
  void onResponse(int iInstance, int iResponseCode, String mResponse);
}