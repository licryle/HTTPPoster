# HTTPPoster
A little library to easily HTTP POST variables &amp; files in Android.

# How to use?
## Android Studio Setup
In your Android Studio project, create a folder called libs, for example: ./AndroidStudioProject/MyProject/libs

Download the repository files and unpack either:
* At the root of ./AndroidStudioProjects/ then create a symbolic link from ./AndroidStudioProject/MyProject/libs/HttpPoster to ./AndroidStudioProject/HttpPoster/library
* In the directory "library" into the ./AndroidStudioProject/MyProject/libs/ directory and rename "library" folder to "HttpPost"

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
