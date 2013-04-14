FixMyStreet Android application fork
====================================

A fork of the original project (https://github.com/orjanv/fiksgatami). I have broken it 
totally, cleaned up parts of it and are in the process of adding some new features.

The fork project status as of now: *It does not work*. Please don't try to use it for 
submitting to fiksgatami. However, feel tree to download it and help me with testing 
and suggestions.

The Fix My Street directory contains a complete application that should open
directly in Eclipse.

To compile the project, you will need to add the following JAR dependencies.
 
- httpmime-4.1.1.jar: Makes the multipart messages work (Part of HttpClient 4.1.1 
  from http://hc.apache.org/downloads.cgi).
- osmdroid-android-3.0.9 OpenStreetMap for map interactions.
- sl4j-android-1.5.8 Required by osmdroid-android-3.0.9

Hopefully it will no longer be needed in future versions of Android. 

Note that the app has to be signed to go onto the Android Market
Contact anna@mysociety.org for signing a new british version
Contact hildenae@gmail.com for a new norwegian version
