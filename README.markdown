STHLM Traveling
===============

Travel in Stockholm with your Android phone.

STHLM Traveling helps you plan your journeys using data from Stockholm Public 
Transport (SL, Stockholms Lokaltrafik). 

More about this project at <http://markupartist.com/sthlmtraveling> or follow 
me on [twitter](http://twitter.com/johanni) where I announce new versions.

The app is powered by two backend services. The journey planner in the app is
written in php and screen scrapes sl.se. The other one is written is Java and
hosted on Googles AppEnginge and is used for deviations and real time data.

Credits
-------
* Johan Walles for his great code in his SL app. Marker for the map is built
  from his code
* App icon, concepts & ideas Olof Brickarp
* Patches by [Morgan Christiansson, Screen Interaction AB](http://screeninteraction.com)
* Icons by [Fredrik Broman](http://fredrikbroman.com)
* Icons by [FatCow](http://www.fatcow.com/free-icons)
* Feedback, suggestions & test by [Swedroid users](http://swedroid.se)
* Also great thanks to my Twitter and Flickr followers for their suggestions 
  and feedback

Setting up the project
----------------------

Once forked and imported to eclipse you need to run "Fix Project Properties".
This is found under Android Tools. 

You then need to add the following class that holds the api endpoint and point 
out where the api is located.

    public class ApiSettings {
        public static String STHLM_TRAVELING_API_ENDPOINT = "";
    }

Run tests
---------

To be able to run the tests from Eclipse follow these instructions. These 
instructions assume that you alrady have STHLM Traveling setup as a project and
deployed. 

* Create a new Android Project.
* Choose create project from existing source. Choose tests/src as project root.
* Under project properties and Java Build Path choose Projects and add a 
  dependency to STHLM Traveling.
* If necessary run Fix Project Properties under Android Tools.
* Now you should be able to run the tests. Choose Run As Android JUnit Test.

To run the tests from a terminal just type in. Note, you must have both the 
application and the tests application deployed before.

    adb shell am instrument -w com.markupartist.sthlmtraveling.tests/android.test.InstrumentationTestRunner

License
-------
STHLM Traveling is open source and licensed under 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

### Exceptions

* Icons by Fredrik Broman is licensed under [CC Attribution 3.0](http://creativecommons.org/licenses/by/3.0/)
* SectionedAdapter released under [GNU General Public License](http://www.gnu.org/licenses/gpl.html)
* LabelMarker released under [GNU General Public License](http://www.gnu.org/licenses/gpl.html)
  Originally from Johan Walles [SL app](https://launchpad.net/sl),
* App icon by Olof Brickarp is copyright Olof Brickarp

Changelog
---------
* 1.7.2
    * New app icon by [Olof Brickarp](http://www.yay.se)
    * Improvements for hdpi screens
    * Bug fix for network problem that occurred when switching between different
      networks.
    * Bug fix for maps, that caused the my location indicator to not show up
      direct
* 1.5.2
    * Bug fix for transports without line numbers
    * Bug fix for line numbers, now Bus 123A will be parsed correct
    * Bug fix when my location sources is turned of in system settings
    * Added transport for the airport coach
* 1.5.1
    * Added line numbers in the routes view
    * Added a crash reporter
    * Added icon for boat
* 1.5.0
    * Added departures
    * Changed to use the Apache http client
* 1.4.2
    * Added support for setting arrival time
* 1.4.1
    * Added fallback on point on map when failed to determine "My location"
* 1.4.0
    * Point on map
    * Improved My location, now passing the real position to sl.se
    * Added support for lat and lng to favorites
    * Added retry button to the network problem dialog
    * Fixed bug with screen rotation when performing background jobs
    * Added support for named shortcuts
* 1.3.6
    * Fix for barcode generator
* 1.3.5
    * New app icon by [Olof Brickarp](http://www.yay.se)
    * Choose departure time from the planner
    * Shortcuts for routes
    * Share search via QR-code
    * Public intent for searching routes, other apps can now trigger a search
      for routes. See RoutesActivity for documentation
    * Reverse search from the routes list 
    * Show alert if we having problems with the network, closes issue [#6](http://github.com/johannilsson/sthlmtraveling/issues/#issue/6) 
    * Added start and end point to routes list
    * Moved actually searching to RoutesActivity
    * Moved planner code to own package
    * Replaced handlers with AsyncTask, now located in the tasks package
    * Fixed bug with menu for search did not launch the tabbed StartActivity, 
      patch by [Morgan Christiansson](http://www.screeninteraction.com/)
    * Moved all hard coded strings to strings.xml
* 1.3.0
    * Favorite routes
    * History for start and end point
    * Fixed force close when location is not available, closes issue [#4](http://github.com/johannilsson/sthlmtraveling/issues/#issue/4)
    * Compiling against 1.6
    * Tested for QVGA screens
    * Improved ui with icons from FatCow
    * Refactored search routes to a AsyncTask
    * Renamed SearchActivity to PlannerActivity, patch by [Morgan Christiansson](http://www.screeninteraction.com/)
* 1.2.2
    * Added Saltsjöbanan as an transport, closes issue [#2](http://github.com/johannilsson/sthlmtraveling/issues/#issue/2)
* 1.2.1
    * Added support for icons in routes list. Icons by 
      [Fredrik Broman](http://fredrikbroman.com)
    * Replaced earlier/later routes text with arrows, as suggested by 
      [fohlin](http://twitter.com/fohlin)
    * Added setup for unit tests and some tests
    * Fixed issue with "My Location" returning null instead of city sometimes
* 1.2.0
    * Added support for searching for earlier and later routes in the routes 
      view
    * Added support for changing the department time
* 1.1.0
    * Improved the ux by doing searches in the current activity
    * Added ApiSettings that holds the api endpoint
    * Introduced Planner groups all journey planning functionality
    * Renamed *Finder to *Parser and moved all http queries to Planner
    * Suffixed all activities to *Activity    
    * Renamed SimpleStopAdapter to AutoCompleteStopAdapter
    * Added dialog if no routes was found and a hint of why this occurred
* 1.0.0
    * Released on Android Market
    * Added about dialog
* 0.2
    * Added search based on the current postion
    * Fixed problems around finding stops, some searches did not return the 
      expected values
    * Made the search button bigger
* 0.1
    * Initial release
