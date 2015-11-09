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

/**
 * Interface to implement to listen to the events from the {@link HttpPoster}
 * Async task. All these callbacks are called on the main thread so updating the
 * UI is safe.
 *
 * @see HttpPoster
 */
public interface HttpListener {
  /**
   * Callback called whenever the connection is established with the POST end-
   * point.
   *
   * @param iInstance Unique Id of the HttpPost for tracking purpose.
   */
  void onStartTransfer(int iInstance);

  /**
   * Callback called everytime some data is written to the stream.
   *
   * @param iInstance Unique Id of the HttpPost for tracking purpose.
   * @param iProgress Progress represented from a scale to 0 to 100 as the
   *                  percentage of the POST request writing progress.
   */
  void onProgress(int iInstance, int iProgress);

  /**
   * Callback called if the HTTP Post fails.
   *
   * @param iInstance Unique Id of the HttpPost for tracking purpose.
   * @param lErrorCode The error code related to the POST failure. Either
   *                   #FAILURE_CONNECTION, #FAILURE_TRANSFER,
   *                   #FAILURE_RESPONSE, #FAILURE_MALFORMEDURL or
   *                   #FAILURE_FILE_NOT_FOUND.
   */
  void onFailure(int iInstance, long lErrorCode);

  /**
   * Callback called if the HTTP Post succeeds.
   *
   * @param iInstance Unique Id of the HttpPost for tracking purpose.
   * @param iResponseCode The HTTP Response code from the server to the POST
   *                      request.
   * @param sResponse The content of the server response.
   */
  void onResponse(int iInstance, int iResponseCode, String sResponse);
}