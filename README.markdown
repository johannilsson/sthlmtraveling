![Logo for STHLM Traveling for Android](http://markupartist.com/sthlmtraveling/logo.png)

STHLM Traveling
===============

Travel in Stockholm with your Android phone.

STHLM Traveling helps you plan your journeys using data from Stockholm Public 
Transport (SL, Stockholms Lokaltrafik).

Vote for features at http://bit.ly/sthlmt

More about this project at <http://markupartist.com/sthlmtraveling> or follow 
me on twitter at [@johanni](http://twitter.com/johanni) or [@sthlmtraveling](http://twitter.com/sthlmtraveling)
where new versions is announced.

Some screen shots can be found at [Flickr](http://www.flickr.com/photos/johannilsson/tags/sthlmtraveling/).

The app is powered by a backend service written i Java and hosted at Google
app engine. Unfortunately that application is not open source since it's using
closed API's that is provided by SL.

<a href="http://flattr.com/thing/332993/STHLM-Traveling" target="_blank">
<img src="http://api.flattr.com/button/flattr-badge-large.png" alt="Flattr this" title="Flattr this" border="0" /></a> 

Credits
-------
* App icon, concepts & ideas Olof Brickarp
* Icons by [Fredrik Broman](http://fredrikbroman.com)
* Feedback, suggestions & test by [Swedroid users](http://swedroid.se)
* Also great thanks to my Twitter and Flickr followers for their suggestions 
  and feedback

Developers
----------

If you want to contribute to this project just send me a email, tweet or just
fork the app. I will do my best help you out if you have any problems.

Please follow the [Code Style Guidelines for Contributors](http://source.android.com/source/code-style.html)
for Android except for line length that should be kept to 80 colums where possible.  

### Dependencies

* [Android ActionBar](https://github.com/johannilsson/android-actionbar) - mimic-native-api
* [View Pager Indicator](http://viewpagerindicator.com/) - v2.2.2

### Run tests

To be able to run the tests from Eclipse follow these instructions. Assumes
that you already have STHLM Traveling setup as a project and deployed.

* Create a new Android Project.
* Choose create project from existing source. Choose tests/src as project root.
* Under project properties and Java Build Path choose Projects and add a 
  dependency to STHLM Traveling.
* If necessary run Fix Project Properties under Android Tools.
* Now you should be able to run the tests. Choose Run As Android JUnit Test.

To run the tests from a terminal: Note, you must have both the 
application and the tests application deployed before.

    adb shell am instrument -w com.markupartist.sthlmtraveling.tests/android.test.InstrumentationTestRunner

Changelog
---------
* 2.4.0
    * Add swipe between tabs and port to fragments by [Robert Johansson](https://twitter.com/#!/likebobby).
    * Add support for Arlanda Express.
    * Add support for intermediate stops.
    * Add Holo theme for API level 14+.
    * Various style fixes.
    * Add proper default selection in departures.
    * Add proper name of My Location when used as destination.
    * Add support for using the keyboard software search button from departure
      search.
    * Fixed bug in departure when no Site was selected.
    * Improve validation of user input.
* 2.3.10
    * Adapted deviation to new API.
    * Adapted traffic status to new API.
    * Fixed loading indicator in deviation.
    * Fixed some translations.
* 2.3.9
    * Adapted departures to new API.
    * Time displayed for departures now comes directly from SL which should
      give more accurate times.
    * Removed red indicator in departures for "now" because it did not work.
* 2.3.8
    * Renamed transport types for TRAIN and TRAM to work with the new API.
* 2.3.7
    * New API for sites and journey planner.
    * Initial support for intermediate stops (not available yet).
    * Restyled tabs on the front activities.
* 2.3.6
    * Removed ads
* 2.3.5
    * Added ads
* 2.3.4
    * New SMS prices
    * Fixed issue with SMS now showing
    * Enabled nearby sites as an experimental function, available through a
      shortcut.
* 2.3.3
    * Fixed bug with alternative stops not being accepted even if it was
      checked.
* 2.3.2
    * Fixed crash in Deviations that could occurr during network problems.
    * Possible fix for not finding any routes.
    * Excluded sites with geo data for the via field.
    * Fixed problem with via site history being mixed with the departure site
      history.
    * Fixed crash in favorites that occured when favoriting a journey with no
      transports.
* 2.3.1
    * Fixed crash related to migration of favorites to the new format.
* 2.3.0
    * New journey provider that holds starred journey, including traffic modes
      and other search criterias. This replaces the favorites database.
    * Added support for via to the journey planner.
    * Added possibility to search for different transport modes and alternative
      stops.
    * Added a action bar.
    * Updated icons.
    * Added journey history, visible below the journey planner.
    * Changed target sdk version to 10 (2.3.3) and min sdk version to 4 (1.6).
    * Moved files from the assets directory (that shouldn't be there) into the
      extras directory.
* 2.2.2
    * Last selected transport mode for a specific site is now automatically
      selected for Departures.
* 2.2.1
    * Fixed bug that crashed the app when changing transport type in the
      departure activity before the data was loaded.
* 2.2.0
    * New departures view. Departures are now grouped by transport type.
    * Departures for metros are pulled from a separate api and should now be
      based on realtime data.
    * If the departure time is not based on realtime data it's now displayed
      as the time for departure instead of eg. "2 min" in the departure view.
    * Fixed bug when "Now" was not respected in the journey planner.
    * Added traffic line to the traffic status view.
    * The detailed journey view now displays the correct time in the header.
    * UI tweaks.
* 2.1.0
    * Changed to use json instead of xml for getting site data from the backend.
    * Mapped the bus icon for närtrafiken.  
    * Added Flurry Analytics v1.24. We need analytics to be able to improve
      the application and know how users use it.
    * Using default Theme in preferences because [issue](http://twitter.com/johanni/status/26117527135) with Sony Ericsson devices.
    * Date of Trip added to the detailed view.
    * New arrows and clock icons in the journey planner by [Sara Smedman](http://joforik.net/).
    * Spanish translation by Fredrik Jönsson.
    * SMS tickets is back, now with price and extra information. Patch by [Adam Nybäck](http://anyro.se/).
    * Increased text sizes and margins.
    * Invalid encoding for origin and destination name in the detailed
      result, closes [#25](http://github.com/johannilsson/sthlmtraveling/issues#issue/25).
    * Replaced the token MY_LOCATION with a localized string in the detailed
      result, closes [#35](http://github.com/johannilsson/sthlmtraveling/issues#issue/35). 
    * Target sdk version is now 8 (2.2) min sdk version is still 3 (1.5).
    * Possibility to change install location (Install on SD card), closes [#34](http://github.com/johannilsson/sthlmtraveling/issues#issue/34).
    * Make sure tasks are canceled when user leaves the RoutesActivity, closes [#27](http://github.com/johannilsson/sthlmtraveling/issues#issue/27).
    * Fixed bug when restoring GetEarlierRoutesTask, closes [#17](http://github.com/johannilsson/sthlmtraveling/issues#issue/17).
    * Fixed issue with MY_LOCATION being added to the text input when selecting
      an item from the history, thanks droidgren. Closes [#25](http://github.com/johannilsson/sthlmtraveling/issues#issue/25)
    * Improved error handling when the DeparturesActivity got restored after an
      orientation change. This closes [#16](http://github.com/johannilsson/sthlmtraveling/issues#issue/16) and as a side effect is also closes [#11](http://github.com/johannilsson/sthlmtraveling/issues#issue/11).  
* 2.0.0
    * Search history is visible in the "get" dialog direct
    * Address search integrated in auto completion
    * Reworked history to allow storage of latitude and longitude
    * Various layout fixes 
    * New traffic status view
    * Fixed my location bug
    * Complete rewrite of the journey planner, new backend etc
    * Various icons has been replaced creds to Olof Brickarp
    * Changed to use SL standard icons for transports (with their permission)
    * Switched to use the light theme
* 1.8.1
    * Reverted version of 1.8.0, just upped version to be able to release it
* 1.8.0
    * Reworked detailed view
    * Added buy sms-ticket from the detailed view
    * Added possibility to show stop on a map from the detailed view
    * New marker for text on the map view
    * The hardware search button will bring up the planner view
    * Various layout fixes
* 1.7.7
    * Added possibility to share deviations
    * Added possibility to share routes
    * Search improvements, if the start or end point matches a name in the
      list of suggestions re-query direct instead of showing the suggestions
      for the end user
    * Fixed NullPointerException that occurred when searching and the
      communication failed, closes #10.
* 1.7.6
    * Fixed issue with changing locale on Android 2.0.x, closes #9
    * Added double tap to zoom on map
    * Removed center to geo point when clicking on the map
* 1.7.5
    * Fixed bug related to alternatives when one stop had an location
* 1.7.4
    * Wrong map key
* 1.7.3
    * Added site suggestions if a routes was not found because sl.se mixed up
      some sites, or if the user misspelled the site name. Resolves #3
    * Bug fix for networks problems in the RoutesActivity
    * Various clean up
    * Removed QR code, need to rework that part a bit
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

License
-------
STHLM Traveling is open source and licensed under 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

### Exceptions

* Transport icons is copyright Storstockholms Lokaltrafik and used with their
  permission.
* Icons by Olof Brickarp is copyright Olof Brickarp
* Icons by Fredrik Broman is licensed under [CC Attribution 3.0](http://creativecommons.org/licenses/by/3.0/)
* SectionedAdapter released under [GNU General Public License](http://www.gnu.org/licenses/gpl.html)
    
