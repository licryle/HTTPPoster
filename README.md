# HTTPPoster
A little library to easily HTTP POST variables &amp; files in Android.

This code is an merge of (this Stack Overflow post)[http://stackoverflow.com/a/19188010] and (this solution)[http://www.xyzws.com/javafaq/how-to-use-httpurlconnection-post-data-to-web-server/139] put in a nice AsyncTask, with configuration and a listener for all events.

It is also updated to the newest Android API, as HttpPost and HttpClient classes are deprecated in favor of HttpURLConnection.

This tiny library is composed of 3 classes:
- [HttpListener](https://github.com/licryle/HTTPPoster/blob/master/library/src/main/java/com/licryle/httpposter/HttpListener.java) - An interface you need to implement to listen for callbacks.
- [HttpConfiguration](https://github.com/licryle/HTTPPoster/blob/master/library/src/main/java/com/licryle/httpposter/HttpConfiguration.java) - A helper class holding the configuration &amp; content of the HTTP POST Request to execute. 
- [HttpPoster](https://github.com/licryle/HTTPPoster/blob/master/library/src/main/java/com/licryle/httpposter/HttpPoster.java) - The main class - the ASyncTask that will execute the POST requets based on the given [HttpConfiguration](https://github.com/licryle/HTTPPoster/blob/master/library/src/main/java/com/licryle/httpposter/HttpConfiguration.java), sending back signals through the [HttpListener](https://github.com/licryle/HTTPPoster/blob/master/library/src/main/java/com/licryle/httpposter/HttpListener.java).

# How to use?
## Android Studio Setup
In your Android Studio project, create a folder called libs, for example: ./AndroidStudioProject/MyProject/libs

Download the repository files and unpack either:
* At the root of ./AndroidStudioProjects/ then create a symbolic link from ./AndroidStudioProject/MyProject/libs/HttpPoster to ./AndroidStudioProject/HttpPoster/library
* The directory "library" into the ./AndroidStudioProject/MyProject/libs/ directory and rename the "library" folder to "HttpPost"

In your build.gradle Module file, add the dependency

```
dependencies {
   ...
    compile project(':libs:HTTPPoster')
}
```

## Quick usage
```
HashMap<String, String> mArgs = new HashMap<>();
mArgs.put("lat", "40.712784");
mArgs.put("lon", "-74.005941");

ArrayList<File> aFileList = getMyImageFiles();

HttpConfiguration mConf = new HttpConfiguration(
    "http://www.mysite.com/HttpPostEndPoint",
    mArgs,
    aFileList,
    this, // If this class implements HttpListener
    null,  // Boundary for Entities - Optional
    15000  // Timeout in ms for the connection operation
    10000, // Timeout in ms for the reading operation
);

new HttpPoster().execute(mConf);
```
